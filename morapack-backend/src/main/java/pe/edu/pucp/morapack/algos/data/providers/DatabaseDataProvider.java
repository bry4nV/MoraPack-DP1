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
import pe.edu.pucp.morapack.util.CoordinateUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DatabaseDataProvider: Provides data from moraTravelSimulation schema for simulations.
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

        // Calculate date range (inclusive of end date to capture all flights in time window)
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate().plusDays(1); // +1 to make query inclusive

        List<SimFlight> dbFlights = flightRepository.findFlightsBetweenDates(startDate, endDate);

        // Asegurarse de que tenemos el cache de aeropuertos
        if (airportCache == null || airportCache.isEmpty()) {
            getAirports();
        }

        List<PlannerFlight> plannerFlights = new ArrayList<>();
        for (SimFlight dbFlight : dbFlights) {
            // Filtrar por tiempo exacto
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

        // Calculate date range (inclusive of end date to capture all orders in time window)
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate().plusDays(1); // +1 to make query inclusive

        List<SimOrder> dbOrders = orderRepository.findOrdersBetweenDates(startDate, endDate);

        // Asegurarse de que tenemos el cache de aeropuertos
        if (airportCache == null || airportCache.isEmpty()) {
            getAirports();
        }

        List<PlannerOrder> plannerOrders = new ArrayList<>();
        int filteredSameOriginDest = 0;

        for (SimOrder dbOrder : dbOrders) {
            // Filtrar por tiempo exacto
            LocalDateTime orderDT = LocalDateTime.of(dbOrder.getOrderDate(), dbOrder.getOrderTime());

            if (!orderDT.isBefore(startTime) && orderDT.isBefore(endTime)) {
                PlannerOrder plannerOrder = convertOrderToPlanner(dbOrder);
                if (plannerOrder != null) {
                    // ✅ FILTRAR pedidos donde origen == destino (ej: Lima→Lima, Baku→Baku, Bruselas→Bruselas)
                    String originCode = plannerOrder.getOrigin().getCode();
                    String destCode = plannerOrder.getDestination().getCode();

                    if (originCode.equals(destCode)) {
                        filteredSameOriginDest++;
                        continue; // Saltar este pedido inválido
                    }

                    plannerOrders.add(plannerOrder);
                }
            }
        }

        if (filteredSameOriginDest > 0) {
            System.out.println("   ⚠️  Filtered " + filteredSameOriginDest + " orders with same origin/destination");
        }
        System.out.println("   ✓ Loaded " + plannerOrders.size() + " valid orders");
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
        int filteredSameOriginDest = 0;

        for (SimOrder dbOrder : dbOrders) {
            PlannerOrder plannerOrder = convertOrderToPlanner(dbOrder);
            if (plannerOrder != null) {
                // ✅ FILTRAR pedidos donde origen == destino
                String originCode = plannerOrder.getOrigin().getCode();
                String destCode = plannerOrder.getDestination().getCode();

                if (originCode.equals(destCode)) {
                    filteredSameOriginDest++;
                    continue; // Saltar este pedido inválido
                }

                plannerOrders.add(plannerOrder);
            }
        }

        if (filteredSameOriginDest > 0) {
            System.out.println("   ⚠️  Filtered " + filteredSameOriginDest + " pending orders with same origin/destination");
        }
        System.out.println("   ✓ Loaded " + plannerOrders.size() + " valid pending orders");
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

        // Parsear latitude y longitude usando CoordinateUtils
        // Soporte para formato DMS (04°42'08"S) o decimal (-4.702222)
        double latitude = CoordinateUtils.dmsToDecimalSafe(
            dbAirport.getLatitude(),
            dbAirport.getCode(),
            "latitude"
        );
        double longitude = CoordinateUtils.dmsToDecimalSafe(
            dbAirport.getLongitude(),
            dbAirport.getCode(),
            "longitude"
        );

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
     *
     * IMPORTANTE - CONTRATO DE TIMEZONES:
     * - Los campos departure_time + arrival_time en la BD están almacenados en UTC
     * - Se combinan con flight_date en LocalDateTime que representa UTC
     * - La simulación compara estos tiempos contra currentTime (también UTC)
     *
     * EJEMPLO:
     * - BD: SKBO → SEQM, departure = 03:34:00, arrival = 05:21:00
     * - Ambas en UTC
     * - Bogotá y Quito están en GMT-5, por lo que localmente serían 22:34 y 00:21
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

        // Si arrival < departure, significa que llega al día siguiente (en UTC)
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
     *
     * IMPORTANTE - CONTRATO DE TIMEZONES:
     * - Los campos order_date + order_time en la BD están almacenados en UTC
     * - Se combinan en un LocalDateTime que representa UTC
     * - El deadline se calculará posteriormente usando timezone del destino
     *   (ver PlannerOrder.getDeadlineInDestinationTimezone())
     *
     * EJEMPLO:
     * - BD: order_time = 01:38:00, destination = EBCI (Bruselas, GMT+2)
     * - Se interpreta como: 01:38:00 UTC = 03:38:00 hora local de Bruselas
     * - Deadline (48h): 03:38:00 + 48h en timezone de Bruselas = 01:38:00 UTC (+2 días)
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

        // Setear el timestamp del pedido (UTC)
        LocalDateTime orderTime = LocalDateTime.of(dbOrder.getOrderDate(), dbOrder.getOrderTime());
        plannerOrder.setOrderTime(orderTime);  // UTC

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
