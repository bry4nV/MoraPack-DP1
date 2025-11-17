package pe.edu.pucp.morapack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.algos.data.DataLoader;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO;
import pe.edu.pucp.morapack.dto.simulation.SimulationPreviewResponse;
import pe.edu.pucp.morapack.model.simulation.SimOrder;
import pe.edu.pucp.morapack.repository.simulation.SimAirportRepository;
import pe.edu.pucp.morapack.repository.simulation.SimFlightRepository;
import pe.edu.pucp.morapack.repository.simulation.SimOrderRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for loading and filtering simulation data (orders, flights, airports).
 * Provides preview functionality and caching for performance.
 *
 * Supports TWO data sources:
 * - "database": Load from moraTravelSimulation schema (RECOMMENDED)
 * - "files": Load from CSV files (legacy)
 */
@Service
public class SimulationDataService {

    @Autowired(required = false)
    private SimOrderRepository simOrderRepository;

    @Autowired(required = false)
    private SimFlightRepository simFlightRepository;

    @Autowired(required = false)
    private SimAirportRepository simAirportRepository;

    @Autowired(required = false)
    private pe.edu.pucp.morapack.repository.daily.AirportRepository dailyAirportRepository;

    // Switch for data source: "database" or "files"
    @Value("${simulation.data.source:database}")
    private String dataSource;

    // Cache to avoid re-loading data
    private final Map<String, List<PlannerOrder>> ordersCache = new ConcurrentHashMap<>();
    private final Map<String, List<PlannerFlight>> flightsCache = new ConcurrentHashMap<>();
    private List<PlannerAirport> airportsCache = null;

    // Data file paths (for legacy file-based loading)
    private static final String AIRPORTS_FILE = "data/airports_real.txt";
    private static final String FLIGHTS_FILE = "data/flights.csv";
    private static final String ORDERS_FILE = "data/_pedidos_SUAA_.txt";
    
    /**
     * Get orders within a specific date range.
     * Results are cached to improve performance.
     */
    public List<PlannerOrder> getOrdersForSimulation(LocalDate startDate, int durationDays) {
        String cacheKey = dataSource + "-" + startDate + "-" + durationDays;

        return ordersCache.computeIfAbsent(cacheKey, k -> {
            if ("database".equalsIgnoreCase(dataSource)) {
                return loadOrdersFromDatabase(startDate, durationDays);
            } else {
                return loadOrdersFromFiles(startDate, durationDays);
            }
        });
    }

    /**
     * Load orders from DATABASE (moraTravelSimulation schema).
     * Supports WEEKLY scenario with proper date range filtering.
     */
    private List<PlannerOrder> loadOrdersFromDatabase(LocalDate startDate, int durationDays) {
        System.out.println("[SimulationDataService] Loading orders from DATABASE");

        LocalDate endDate = startDate.plusDays(durationDays);

        // Load orders from DB
        List<SimOrder> dbOrders = simOrderRepository.findOrdersInDateRange(startDate, endDate);
        System.out.println("[SimulationDataService] Found " + dbOrders.size() + " orders in DB");

        // Load airports (needed for conversion)
        Map<String, PlannerAirport> airportMap = getAirports().stream()
            .collect(Collectors.toMap(PlannerAirport::getCode, a -> a));

        // Convert SimOrder → PlannerOrder
        List<PlannerOrder> plannerOrders = dbOrders.stream()
            .map(o -> convertToPlannerOrder(o, airportMap))
            .sorted(Comparator.comparing(PlannerOrder::getOrderTime))
            .collect(Collectors.toList());

        System.out.println("[SimulationDataService] Converted " + plannerOrders.size() + " orders");
        return plannerOrders;
    }

    /**
     * Load orders from FILES (legacy CSV files).
     */
    private List<PlannerOrder> loadOrdersFromFiles(LocalDate startDate, int durationDays) {
        try {
            System.out.println("[SimulationDataService] Loading orders from FILES");

            List<PlannerAirport> airports = getAirports();
            Map<String, PlannerAirport> airportMap = airports.stream()
                .collect(Collectors.toMap(PlannerAirport::getCode, a -> a));

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

            System.out.println("[SimulationDataService] Loaded " + filtered.size() + " orders from files");
            return filtered;

        } catch (IOException e) {
            System.err.println("[SimulationDataService] Error loading orders: " + e.getMessage());
            throw new RuntimeException("Failed to load orders", e);
        }
    }

    /**
     * Convert SimOrder (DB entity) → PlannerOrder (simulation model).
     */
    private PlannerOrder convertToPlannerOrder(SimOrder dbOrder, Map<String, PlannerAirport> airportMap) {
        PlannerAirport destination = airportMap.get(dbOrder.getAirportDestinationCode());

        if (destination == null) {
            throw new IllegalStateException("Airport not found: " + dbOrder.getAirportDestinationCode());
        }

        // Combine date + time
        LocalDateTime orderTime = LocalDateTime.of(dbOrder.getOrderDate(), dbOrder.getOrderTime());

        // For simulation: origin = destination (according to current logic)
        // PlannerOrder constructor: (int id, int quantity, PlannerAirport origin, PlannerAirport destination)
        PlannerOrder order = new PlannerOrder(
            dbOrder.getId().intValue(),
            dbOrder.getQuantity(),
            destination,  // origin
            destination   // destination
        );

        // Set additional fields via setters
        order.setOrderTime(orderTime);
        order.setClientId(dbOrder.getClientCode());

        return order;
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
     * Loads from DATABASE or FILES depending on configuration.
     */
    public List<PlannerAirport> getAirports() {
        if (airportsCache == null) {
            if ("database".equalsIgnoreCase(dataSource)) {
                airportsCache = loadAirportsFromDatabase();
            } else {
                airportsCache = loadAirportsFromFiles();
            }
        }
        return airportsCache;
    }

    /**
     * Load airports from DATABASE (moraTravelDaily schema).
     */
    private List<PlannerAirport> loadAirportsFromDatabase() {
        System.out.println("[SimulationDataService] Loading airports from DATABASE");

        // Load all airports from the database
        List<pe.edu.pucp.morapack.model.Airport> dbAirports = dailyAirportRepository.findAll();
        System.out.println("[SimulationDataService] Found " + dbAirports.size() + " airports in DB");

        // Convert to PlannerAirport
        List<PlannerAirport> plannerAirports = new ArrayList<>();
        int idCounter = 1;
        int skippedCount = 0;

        for (pe.edu.pucp.morapack.model.Airport dbAirport : dbAirports) {
            String code = dbAirport.getCode();
            String city = dbAirport.getCity();
            // ✅ FIX: Create descriptive name from city + code
            String name = (city != null && !city.isEmpty()) ? city : code;
            int gmt = dbAirport.getGmt() != null ? dbAirport.getGmt() : 0;
            int capacity = dbAirport.getCapacity() != null ? dbAirport.getCapacity() : 1000;

            // ✅ CRITICAL FIX: Convert DMS coordinates to decimal
            double lat = parseDMSToDecimal(dbAirport.getLatitude());
            double lon = parseDMSToDecimal(dbAirport.getLongitude());

            // ✅ FILTER: Skip airports with invalid coordinates (0,0) or missing data
            if (lat == 0.0 && lon == 0.0) {
                System.err.println("⚠️ Skipping airport " + code + " (" + name + ") - invalid coordinates (0,0)");
                skippedCount++;
                continue;
            }

            // Validate latitude/longitude ranges
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                System.err.println("⚠️ Skipping airport " + code + " - coordinates out of range: (" + lat + ", " + lon + ")");
                skippedCount++;
                continue;
            }

            // Create Country object (simplified - we don't have country data in Airport table)
            pe.edu.pucp.morapack.model.Continent continent = mapContinent(dbAirport.getContinent());
            pe.edu.pucp.morapack.model.Country country = new pe.edu.pucp.morapack.model.Country(
                1,
                dbAirport.getCountry() != null ? dbAirport.getCountry() : "Unknown",
                continent
            );

            // Use constructor with coordinates
            PlannerAirport pa = new PlannerAirport(idCounter++, code, name, city, country, capacity, gmt, lat, lon);
            plannerAirports.add(pa);
        }

        System.out.println("[SimulationDataService] ✅ Converted " + plannerAirports.size() + " valid airports");
        if (skippedCount > 0) {
            System.out.println("[SimulationDataService] ⚠️ Skipped " + skippedCount + " airports with invalid data");
        }
        return plannerAirports;
    }

    /**
     * Map continent string from database to Continent enum.
     * Handles various formats like "America del Sur.", "Europa", etc.
     */
    private pe.edu.pucp.morapack.model.Continent mapContinent(String continentStr) {
        if (continentStr == null) {
            return pe.edu.pucp.morapack.model.Continent.AMERICA;
        }

        switch (continentStr.toLowerCase().trim()) {
            case "america del sur.":
            case "america del sur":
            case "america":
                return pe.edu.pucp.morapack.model.Continent.AMERICA;
            case "europa":
            case "europe":
                return pe.edu.pucp.morapack.model.Continent.EUROPE;
            case "asia":
                return pe.edu.pucp.morapack.model.Continent.ASIA;
            case "africa":
            case "áfrica":
                return pe.edu.pucp.morapack.model.Continent.AFRICA;
            case "oceania":
            case "oceanía":
                return pe.edu.pucp.morapack.model.Continent.OCEANIA;
            default:
                System.err.println("⚠️ Unknown continent '" + continentStr + "', defaulting to AMERICA");
                return pe.edu.pucp.morapack.model.Continent.AMERICA;
        }
    }

    /**
     * Load airports from FILES (legacy).
     */
    private List<PlannerAirport> loadAirportsFromFiles() {
        try {
            System.out.println("[SimulationDataService] Loading airports from file");
            List<PlannerAirport> airports = DataLoader.loadAirports(AIRPORTS_FILE);
            System.out.println("[SimulationDataService] Loaded " + airports.size() + " airports");
            return airports;
        } catch (IOException e) {
            System.err.println("[SimulationDataService] Error loading airports: " + e.getMessage());
            throw new RuntimeException("Failed to load airports", e);
        }
    }

    /**
     * Convert DMS (Degrees Minutes Seconds) to decimal format.
     * Examples:
     *   "04 42 05 N" -> 4.701389
     *   "74 08 49 W" -> -74.146944
     *   "12 01 19 S" -> -12.021944
     */
    private double parseDMSToDecimal(String dms) {
        if (dms == null || dms.trim().isEmpty()) {
            System.err.println("⚠️ Invalid DMS coordinate: null or empty");
            return 0.0;
        }

        // If already decimal (no direction letters), parse directly
        if (!dms.matches(".*[NSEW].*")) {
            try {
                return Double.parseDouble(dms.trim());
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Invalid decimal coordinate: " + dms);
                return 0.0;
            }
        }

        // Parse DMS format: "04 42 05 N" or "74 08 49 W"
        String[] parts = dms.trim().split("\\s+");
        if (parts.length < 4) {
            System.err.println("⚠️ Invalid DMS format: " + dms);
            return 0.0;
        }

        try {
            int degrees = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            String direction = parts[3].toUpperCase();

            // Convert to decimal
            double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);

            // South and West are negative
            if ("S".equals(direction) || "W".equals(direction)) {
                decimal = -decimal;
            }

            return decimal;
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Error parsing DMS coordinate: " + dms);
            return 0.0;
        }
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
        response.totalOrders = orders.size();
        response.totalProducts = orders.stream()
            .mapToInt(PlannerOrder::getTotalQuantity)
            .sum();
        response.totalFlights = flights.size();

        // Date range
        response.dateRange = new SimulationPreviewResponse.DateRangeDTO(
            startDate.atStartOfDay().toString(),
            startDate.plusDays(durationDays).atStartOfDay().toString()
        );

        // Convert ALL orders to DTOs (no limit)
        response.orders = orders.stream()
            .map(this::orderToPreviewDTO)
            .collect(Collectors.toList());

        // Unique airports involved
        Set<String> airportCodes = new HashSet<>();
        orders.forEach(o -> {
            airportCodes.add(o.getOrigin().getCode());
            airportCodes.add(o.getDestination().getCode());
        });
        response.involvedAirports = new ArrayList<>(airportCodes);

        // Include full airport data for map display
        List<PlannerAirport> airports = getAirports();
        response.airports = pe.edu.pucp.morapack.utils.TabuSolutionToDtoConverter.toAirportDtos(airports);

        // Statistics
        response.statistics = calculateStatistics(orders, durationDays);
        
        return response;
    }
    
    /**
     * Convert PlannerOrder to OrderSummaryDTO for preview (no assignment data yet).
     */
    private OrderSummaryDTO orderToPreviewDTO(PlannerOrder order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.id = order.getId();
        dto.code = "PED-" + order.getId();

        dto.originCode = order.getOrigin().getCode();
        dto.originName = order.getOrigin().getName();
        dto.destinationCode = order.getDestination().getCode();
        dto.destinationName = order.getDestination().getName();

        dto.totalQuantity = order.getTotalQuantity();
        dto.assignedQuantity = 0;  // Not assigned yet in preview
        dto.progressPercent = 0.0;

        dto.requestDateISO = order.getOrderTime().toString();
        dto.etaISO = null;  // Unknown in preview

        dto.status = OrderSummaryDTO.OrderStatus.PENDING;
        dto.assignedFlights = new ArrayList<>();

        return dto;
    }
    
    /**
     * Calculate statistics about the orders.
     */
    private SimulationPreviewResponse.StatisticsDTO calculateStatistics(
            List<PlannerOrder> orders, int durationDays) {

        SimulationPreviewResponse.StatisticsDTO stats =
            new SimulationPreviewResponse.StatisticsDTO();

        if (orders.isEmpty()) {
            stats.ordersPerDay = new int[durationDays];
            stats.avgProductsPerOrder = 0;
            stats.maxProductsOrder = 0;
            stats.minProductsOrder = 0;
            return stats;
        }

        // Orders per day
        stats.ordersPerDay = new int[durationDays];
        LocalDate firstDay = orders.get(0).getOrderTime().toLocalDate();

        for (PlannerOrder order : orders) {
            LocalDate orderDate = order.getOrderTime().toLocalDate();
            int dayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(firstDay, orderDate);
            if (dayIndex >= 0 && dayIndex < durationDays) {
                stats.ordersPerDay[dayIndex]++;
            }
        }

        // Product statistics
        IntSummaryStatistics productStats = orders.stream()
            .mapToInt(PlannerOrder::getTotalQuantity)
            .summaryStatistics();

        stats.avgProductsPerOrder = productStats.getAverage();
        stats.maxProductsOrder = productStats.getMax();
        stats.minProductsOrder = productStats.getMin();

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

