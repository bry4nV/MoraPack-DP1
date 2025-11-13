package pe.edu.pucp.morapack.algos.data.providers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.model.Continent;
import pe.edu.pucp.morapack.model.Country;
import pe.edu.pucp.morapack.model.simulation.SimAirport;
import pe.edu.pucp.morapack.model.simulation.SimFlight;
import pe.edu.pucp.morapack.model.simulation.SimOrder;
import pe.edu.pucp.morapack.repository.simulation.SimAirportRepository;
import pe.edu.pucp.morapack.repository.simulation.SimFlightRepository;
import pe.edu.pucp.morapack.repository.simulation.SimOrderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DatabaseDataProvider: Provides data from moraTravelSimulation schema for simulations.
 *
 * This implementation queries the simulation database (moraTravelSimulation) which contains
 * historical data (~2 years) specifically for running simulations.
 *
 * It replaces the file-based data loading with database queries.
 */
@Component
public class DatabaseDataProvider implements DataProvider {

    private final SimAirportRepository airportRepository;
    private final SimFlightRepository flightRepository;
    private final SimOrderRepository orderRepository;

    // Cache de aeropuertos para evitar múltiples conversiones
    private Map<String, PlannerAirport> airportCache;

    @Autowired
    public DatabaseDataProvider(
            SimAirportRepository airportRepository,
            SimFlightRepository flightRepository,
            SimOrderRepository orderRepository) {
        this.airportRepository = airportRepository;
        this.flightRepository = flightRepository;
        this.orderRepository = orderRepository;

        System.out.println("\n[DatabaseDataProvider] Initialized for simulation operations");
        System.out.println("   ✓ Connected to moraTravelSimulation schema");
        System.out.println();
    }

    @Override
    public List<PlannerAirport> getAirports() {
        System.out.println("[DatabaseDataProvider] Loading airports from database...");

        List<SimAirport> dbAirports = airportRepository.findAll();

        // Inicializar el cache
        airportCache = new HashMap<>();

        List<PlannerAirport> plannerAirports = new ArrayList<>();
        for (SimAirport dbAirport : dbAirports) {
            PlannerAirport plannerAirport = convertAirportToPlanner(dbAirport);
            plannerAirports.add(plannerAirport);
            airportCache.put(dbAirport.getCode(), plannerAirport);
        }

        System.out.println("   ✓ Loaded " + plannerAirports.size() + " airports");
        return plannerAirports;
    }

    @Override
    public List<PlannerFlight> getFlights(LocalDateTime startTime, LocalDateTime endTime) {
        System.out.println("[DatabaseDataProvider] Loading flights between " + startTime + " and " + endTime);

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        List<SimFlight> dbFlights = flightRepository.findFlightsBetweenDates(startDate, endDate);

        // Asegurarse de que tenemos el cache de aeropuertos
        if (airportCache == null || airportCache.isEmpty()) {
            getAirports();
        }

        List<PlannerFlight> plannerFlights = new ArrayList<>();
        for (SimFlight dbFlight : dbFlights) {
            // Filtrar por tiempo si es necesario
            LocalDateTime departureDT = LocalDateTime.of(dbFlight.getFlightDate(), dbFlight.getDepartureTime());

            if (!departureDT.isBefore(startTime) && departureDT.isBefore(endTime)) {
                PlannerFlight plannerFlight = convertFlightToPlanner(dbFlight);
                if (plannerFlight != null) {
                    plannerFlights.add(plannerFlight);
                }
            }
        }

        System.out.println("   ✓ Loaded " + plannerFlights.size() + " flights");
        return plannerFlights;
    }

    @Override
    public List<PlannerOrder> getOrders(LocalDateTime startTime, LocalDateTime endTime) {
        System.out.println("[DatabaseDataProvider] Loading orders between " + startTime + " and " + endTime);

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        List<SimOrder> dbOrders = orderRepository.findOrdersBetweenDates(startDate, endDate);

        // Asegurarse de que tenemos el cache de aeropuertos
        if (airportCache == null || airportCache.isEmpty()) {
            getAirports();
        }

        List<PlannerOrder> plannerOrders = new ArrayList<>();
        for (SimOrder dbOrder : dbOrders) {
            // Filtrar por tiempo si es necesario
            LocalDateTime orderDT = LocalDateTime.of(dbOrder.getOrderDate(), dbOrder.getOrderTime());

            if (!orderDT.isBefore(startTime) && orderDT.isBefore(endTime)) {
                PlannerOrder plannerOrder = convertOrderToPlanner(dbOrder);
                if (plannerOrder != null) {
                    plannerOrders.add(plannerOrder);
                }
            }
        }

        System.out.println("   ✓ Loaded " + plannerOrders.size() + " orders");
        return plannerOrders;
    }

    @Override
    public List<PlannerOrder> getPendingOrders() {
        System.out.println("[DatabaseDataProvider] Loading pending orders...");

        List<SimOrder> dbOrders = orderRepository.findPendingOrders();

        // Asegurarse de que tenemos el cache de aeropuertos
        if (airportCache == null || airportCache.isEmpty()) {
            getAirports();
        }

        List<PlannerOrder> plannerOrders = new ArrayList<>();
        for (SimOrder dbOrder : dbOrders) {
            PlannerOrder plannerOrder = convertOrderToPlanner(dbOrder);
            if (plannerOrder != null) {
                plannerOrders.add(plannerOrder);
            }
        }

        System.out.println("   ✓ Loaded " + plannerOrders.size() + " pending orders");
        return plannerOrders;
    }

    @Override
    public List<Integer> getCancelledFlightIds(LocalDateTime startTime, LocalDateTime endTime) {
        System.out.println("[DatabaseDataProvider] Loading cancelled flights between " + startTime + " and " + endTime);

        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        List<SimFlight> cancelledFlights = flightRepository.findCancelledFlights(startDate, endDate);

        List<Integer> cancelledIds = cancelledFlights.stream()
            .map(f -> f.getId().intValue())
            .collect(Collectors.toList());

        System.out.println("   ✓ Found " + cancelledIds.size() + " cancelled flights");
        return cancelledIds;
    }

    // =========================================================================
    // HELPER METHODS - Conversion from DB entities to Planner entities
    // =========================================================================

    /**
     * Convert SimAirport to PlannerAirport
     */
    private PlannerAirport convertAirportToPlanner(SimAirport dbAirport) {
        // Mapear el continente del string de BD al enum Continent
        Continent continent = mapContinent(dbAirport.getContinent());

        // Crear el Country
        Country country = new Country(
            0, // ID no es relevante para el algoritmo
            dbAirport.getCountry(),
            continent
        );

        // Parsear latitude y longitude
        double latitude = 0.0;
        double longitude = 0.0;
        try {
            latitude = Double.parseDouble(dbAirport.getLatitude());
            longitude = Double.parseDouble(dbAirport.getLongitude());
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse coordinates for airport " + dbAirport.getCode());
        }

        return new PlannerAirport(
            dbAirport.getId().intValue(),
            dbAirport.getCode(),
            dbAirport.getCity(), // Usamos city como name
            dbAirport.getCity(),
            country,
            dbAirport.getCapacity(),
            dbAirport.getGmt(),
            latitude,
            longitude
        );
    }

    /**
     * Convert SimFlight to PlannerFlight
     */
    private PlannerFlight convertFlightToPlanner(SimFlight dbFlight) {
        PlannerAirport origin = airportCache.get(dbFlight.getAirportOriginCode());
        PlannerAirport destination = airportCache.get(dbFlight.getAirportDestinationCode());

        if (origin == null || destination == null) {
            System.err.println("Warning: Could not find airports for flight " + dbFlight.getId());
            return null;
        }

        LocalDateTime departure = LocalDateTime.of(dbFlight.getFlightDate(), dbFlight.getDepartureTime());
        LocalDateTime arrival = LocalDateTime.of(dbFlight.getFlightDate(), dbFlight.getArrivalTime());

        // Si arrival < departure, significa que llega al día siguiente
        if (arrival.isBefore(departure)) {
            arrival = arrival.plusDays(1);
        }

        return new PlannerFlight(
            dbFlight.getId().intValue(),
            origin,
            destination,
            departure,
            arrival,
            dbFlight.getCapacity(),
            0.0 // El costo se calculará automáticamente en PlannerFlight
        );
    }

    /**
     * Convert SimOrder to PlannerOrder
     */
    private PlannerOrder convertOrderToPlanner(SimOrder dbOrder) {
        // Para orders, necesitamos origin y destination
        // Según la BD, solo tenemos destination code
        // Asumimos que el origin es el hub principal (SPIM, EBCI, o UBBB)
        // basado en el continente del destino

        PlannerAirport destination = airportCache.get(dbOrder.getAirportDestinationCode());
        if (destination == null) {
            System.err.println("Warning: Could not find destination airport for order " + dbOrder.getId());
            return null;
        }

        // Determinar el hub de origen basado en el continente del destino
        PlannerAirport origin = determineOriginHub(destination);
        if (origin == null) {
            System.err.println("Warning: Could not determine origin hub for order " + dbOrder.getId());
            return null;
        }

        PlannerOrder plannerOrder = new PlannerOrder(
            dbOrder.getId().intValue(),
            dbOrder.getQuantity(),
            origin,
            destination
        );

        // Setear el timestamp del pedido
        LocalDateTime orderTime = LocalDateTime.of(dbOrder.getOrderDate(), dbOrder.getOrderTime());
        plannerOrder.setOrderTime(orderTime);

        // Setear el client ID
        plannerOrder.setClientId(dbOrder.getClientCode());

        return plannerOrder;
    }

    /**
     * Mapea el string de continente de la BD al enum Continent
     */
    private Continent mapContinent(String continentStr) {
        if (continentStr == null) return Continent.AMERICA;

        switch (continentStr.toLowerCase()) {
            case "america del sur.":
            case "america":
                return Continent.AMERICA;
            case "europa":
            case "europe":
                return Continent.EUROPE;
            case "asia":
                return Continent.ASIA;
            case "africa":
                return Continent.AFRICA;
            case "oceania":
                return Continent.OCEANIA;
            default:
                System.err.println("Warning: Unknown continent '" + continentStr + "', defaulting to AMERICA");
                return Continent.AMERICA;
        }
    }

    /**
     * Determina el hub de origen basado en el continente del destino
     */
    private PlannerAirport determineOriginHub(PlannerAirport destination) {
        if (destination == null || destination.getCountry() == null) return null;

        Continent continent = destination.getCountry().getContinent();

        switch (continent) {
            case AMERICA:
                return airportCache.get("SPIM"); // Lima
            case EUROPE:
                return airportCache.get("EBCI"); // Bruselas
            case ASIA:
                return airportCache.get("UBBB"); // Baku
            default:
                // Por defecto, usar SPIM
                return airportCache.get("SPIM");
        }
    }
}
