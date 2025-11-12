package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.algos.data.DataLoader;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO;
import pe.edu.pucp.morapack.dto.simulation.SimulationPreviewResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for loading and filtering simulation data (orders, flights, airports).
 * Provides preview functionality and caching for performance.
 */
@Service
public class SimulationDataService {
    
    // Cache to avoid re-parsing CSV files
    private final Map<String, List<PlannerOrder>> ordersCache = new ConcurrentHashMap<>();
    private final Map<String, List<PlannerFlight>> flightsCache = new ConcurrentHashMap<>();
    private List<PlannerAirport> airportsCache = null;
    
    // Data file paths
    private static final String AIRPORTS_FILE = "data/airports_real.txt";
    private static final String FLIGHTS_FILE = "data/flights.csv";
    private static final String ORDERS_FILE = "data/_pedidos_SUAA_.txt";
    
    /**
     * Get orders within a specific date range.
     * Results are cached to improve performance.
     */
    public List<PlannerOrder> getOrdersForSimulation(LocalDate startDate, int durationDays) {
        String cacheKey = startDate + "-" + durationDays;
        
        return ordersCache.computeIfAbsent(cacheKey, k -> {
            try {
                System.out.println("[SimulationDataService] Loading orders from CSV for " + cacheKey);
                
                // Load airports first (needed for loadOrders)
                List<PlannerAirport> airports = getAirports();
                Map<String, PlannerAirport> airportMap = airports.stream()
                    .collect(Collectors.toMap(PlannerAirport::getCode, a -> a));
                
                // Load orders with airport map
                int year = startDate.getYear();
                int month = startDate.getMonthValue();
                List<PlannerOrder> allOrders = DataLoader.loadOrders(ORDERS_FILE, airportMap, year, month);
                
                LocalDateTime rangeStart = startDate.atStartOfDay();
                LocalDateTime rangeEnd = rangeStart.plusDays(durationDays);
                
                List<PlannerOrder> filtered = allOrders.stream()
                    .filter(o -> !o.getOrderTime().isBefore(rangeStart) && 
                                 o.getOrderTime().isBefore(rangeEnd))
                    .sorted(Comparator.comparing(PlannerOrder::getOrderTime))
                    .collect(Collectors.toList());
                
                System.out.println("[SimulationDataService] Loaded " + filtered.size() + " orders for range");
                return filtered;
                
            } catch (IOException e) {
                System.err.println("[SimulationDataService] Error loading orders: " + e.getMessage());
                throw new RuntimeException("Failed to load orders", e);
            }
        });
    }
    
    /**
     * Get flights within a specific date range.
     */
    public List<PlannerFlight> getFlightsForSimulation(LocalDate startDate, int durationDays) {
        String cacheKey = startDate + "-" + durationDays;
        
        return flightsCache.computeIfAbsent(cacheKey, k -> {
            try {
                System.out.println("[SimulationDataService] Loading flights from CSV for " + cacheKey);
                
                // Load airports first (needed for loadFlights)
                List<PlannerAirport> airports = getAirports();
                Map<String, PlannerAirport> airportMap = airports.stream()
                    .collect(Collectors.toMap(PlannerAirport::getCode, a -> a));
                
                // Generate flights for the simulation period starting from the exact start date
                List<PlannerFlight> allFlights = DataLoader.loadFlights(
                    FLIGHTS_FILE, 
                    airportMap, 
                    startDate,  // Use LocalDate directly instead of year/month
                    durationDays
                );
                
                System.out.println("[SimulationDataService] Generated " + allFlights.size() + " flights for range");
                return allFlights;
                
            } catch (IOException e) {
                System.err.println("[SimulationDataService] Error loading flights: " + e.getMessage());
                throw new RuntimeException("Failed to load flights", e);
            }
        });
    }
    
    /**
     * Get all airports (cached).
     */
    public List<PlannerAirport> getAirports() {
        if (airportsCache == null) {
            try {
                System.out.println("[SimulationDataService] Loading airports from file");
                airportsCache = DataLoader.loadAirports(AIRPORTS_FILE);
                System.out.println("[SimulationDataService] Loaded " + airportsCache.size() + " airports");
            } catch (IOException e) {
                System.err.println("[SimulationDataService] Error loading airports: " + e.getMessage());
                throw new RuntimeException("Failed to load airports", e);
            }
        }
        return airportsCache;
    }
    
    /**
     * Build a preview of what will be in the simulation.
     * This is called BEFORE the simulation starts.
     */
    public SimulationPreviewResponse buildPreview(LocalDate startDate, ScenarioConfig scenario) {
        int durationDays = (int) Math.ceil(scenario.getTotalDurationMinutes() / 1440.0);
        
        List<PlannerOrder> orders = getOrdersForSimulation(startDate, durationDays);
        List<PlannerFlight> flights = getFlightsForSimulation(startDate, durationDays);
        
        SimulationPreviewResponse response = new SimulationPreviewResponse();
        
        // Basic counts
        response.totalPedidos = orders.size();
        response.totalProductos = orders.stream()
            .mapToInt(PlannerOrder::getTotalQuantity)
            .sum();
        response.totalVuelos = flights.size();
        
        // Date range
        response.dateRange = new SimulationPreviewResponse.DateRangeDTO(
            startDate.atStartOfDay().toString(),
            startDate.plusDays(durationDays).atStartOfDay().toString()
        );
        
        // Convert ALL orders to DTOs (no limit)
        response.pedidos = orders.stream()
            .map(this::orderToPreviewDTO)
            .collect(Collectors.toList());
        
        // Unique airports involved
        Set<String> airportCodes = new HashSet<>();
        orders.forEach(o -> {
            airportCodes.add(o.getOrigin().getCode());
            airportCodes.add(o.getDestination().getCode());
        });
        response.aeropuertosInvolucrados = new ArrayList<>(airportCodes);
        
        // ✅ Include full airport data for map display
        List<PlannerAirport> airports = getAirports();
        response.aeropuertos = pe.edu.pucp.morapack.utils.TabuSolutionToDtoConverter.toAirportDtos(airports);
        
        // Statistics
        response.estadisticas = calculateStatistics(orders, durationDays);
        
        return response;
    }
    
    /**
     * Convert PlannerOrder to OrderSummaryDTO for preview (no assignment data yet).
     */
    private OrderSummaryDTO orderToPreviewDTO(PlannerOrder order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.id = order.getId();
        dto.codigo = "PED-" + order.getId();
        
        dto.origenCodigo = order.getOrigin().getCode();
        dto.origenNombre = order.getOrigin().getName();
        dto.destinoCodigo = order.getDestination().getCode();
        dto.destinoNombre = order.getDestination().getName();
        
        dto.cantidadTotal = order.getTotalQuantity();
        dto.cantidadAsignada = 0;  // Not assigned yet in preview
        dto.progresoPercent = 0.0;
        
        dto.fechaSolicitudISO = order.getOrderTime().toString();
        dto.fechaETA_ISO = null;  // Unknown in preview
        
        dto.estado = OrderSummaryDTO.OrderStatus.PENDING;
        dto.vuelosAsignados = new ArrayList<>();
        
        return dto;
    }
    
    /**
     * Calculate statistics about the orders.
     */
    private SimulationPreviewResponse.EstadisticasDTO calculateStatistics(
            List<PlannerOrder> orders, int durationDays) {
        
        SimulationPreviewResponse.EstadisticasDTO stats = 
            new SimulationPreviewResponse.EstadisticasDTO();
        
        if (orders.isEmpty()) {
            stats.pedidosPorDia = new int[durationDays];
            stats.productosPromedioPorPedido = 0;
            stats.pedidoMaxProductos = 0;
            stats.pedidoMinProductos = 0;
            return stats;
        }
        
        // Orders per day
        stats.pedidosPorDia = new int[durationDays];
        LocalDate firstDay = orders.get(0).getOrderTime().toLocalDate();
        
        for (PlannerOrder order : orders) {
            LocalDate orderDate = order.getOrderTime().toLocalDate();
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(firstDay, orderDate);
            if (dayIndex >= 0 && dayIndex < durationDays) {
                stats.pedidosPorDia[dayIndex]++;
            }
        }
        
        // Product statistics
        IntSummaryStatistics productStats = orders.stream()
            .mapToInt(PlannerOrder::getTotalQuantity)
            .summaryStatistics();
        
        stats.productosPromedioPorPedido = productStats.getAverage();
        stats.pedidoMaxProductos = productStats.getMax();
        stats.pedidoMinProductos = productStats.getMin();
        
        return stats;
    }
    
    /**
     * Clear the cache (useful for testing or when data changes).
     */
    public void clearCache() {
        ordersCache.clear();
        flightsCache.clear();
        airportsCache = null;
        System.out.println("[SimulationDataService] Cache cleared");
    }
}

