package pe.edu.pucp.morapack.algos.data.providers;

import pe.edu.pucp.morapack.algos.data.DataLoader;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FileDataProvider: Provides data from CSV files for simulations.
 * 
 * This implementation is used for:
 * - Weekly simulations (K = 60 minutes)
 * - Collapse simulations (K = 1440 minutes)
 * 
 * It loads all data upfront from CSV files and filters based on time windows.
 */
public class FileDataProvider implements DataProvider {
    
    private final List<PlannerAirport> airports;
    private final List<PlannerFlight> allFlights;
    private final List<PlannerOrder> allOrders;
    private final List<Integer> cancelledFlightIds;
    private final Map<String, PlannerAirport> airportMap;
    
    /**
     * Create a FileDataProvider by loading data from CSV files.
     * 
     * @param airportsFile Path to airports.txt
     * @param flightsFile Path to flights.csv
     * @param ordersFile Path to pedidos_generados.csv
     * @param year Year for simulation (e.g., 2025)
     * @param month Month for simulation (e.g., 12 for December)
     * @param daysToGenerate Number of days to generate flights for
     * @throws IOException If files cannot be read
     */
    public FileDataProvider(
            String airportsFile, 
            String flightsFile, 
            String ordersFile,
            int year,
            int month,
            int daysToGenerate) throws IOException {
        
        System.out.println("\n[FileDataProvider] Initializing for simulations...");
        System.out.println("   Year: " + year + ", Month: " + month + ", Days: " + daysToGenerate);
        
        // Load airports
        this.airports = DataLoader.loadAirports(airportsFile);
        this.airportMap = airports.stream()
            .collect(Collectors.toMap(PlannerAirport::getCode, a -> a));
        
        // Load flights (generate for entire simulation period)
        this.allFlights = DataLoader.loadFlights(flightsFile, airportMap, year, month, daysToGenerate);
        
        // Load orders
        this.allOrders = DataLoader.loadOrders(ordersFile, airportMap, year, month);
        
        // For now, no cancellations (future enhancement)
        this.cancelledFlightIds = new ArrayList<>();
        
        System.out.println("[FileDataProvider] Loaded:");
        System.out.println("   Airports: " + airports.size());
        System.out.println("   Flights: " + allFlights.size());
        System.out.println("   Orders: " + allOrders.size());
        System.out.println();
    }
    
    /**
     * Alternative constructor with cancellations file.
     * 
     * @param airportsFile Path to airports.txt
     * @param flightsFile Path to flights.csv
     * @param ordersFile Path to pedidos_generados.csv
     * @param cancellationsFile Path to cancellations CSV (future)
     * @param year Year for simulation
     * @param month Month for simulation
     * @param daysToGenerate Number of days to generate flights for
     * @throws IOException If files cannot be read
     */
    public FileDataProvider(
            String airportsFile, 
            String flightsFile, 
            String ordersFile,
            String cancellationsFile,
            int year,
            int month,
            int daysToGenerate) throws IOException {
        
        this(airportsFile, flightsFile, ordersFile, year, month, daysToGenerate);
        
        // TODO: Load cancellations from file
        // Format: dd,flight-id (day, flight-id)
        // For now, this is a placeholder
    }
    
    @Override
    public List<PlannerAirport> getAirports() {
        return new ArrayList<>(airports);
    }
    
    @Override
    public List<PlannerFlight> getFlights(LocalDateTime startTime, LocalDateTime endTime) {
        // Filter flights that depart within the time window
        return allFlights.stream()
            .filter(f -> !f.getDepartureTime().isBefore(startTime))
            .filter(f -> f.getDepartureTime().isBefore(endTime))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerOrder> getOrders(LocalDateTime startTime, LocalDateTime endTime) {
        // Filter orders that were placed within the time window
        return allOrders.stream()
            .filter(o -> !o.getOrderTime().isBefore(startTime))
            .filter(o -> o.getOrderTime().isBefore(endTime))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<PlannerOrder> getPendingOrders() {
        // In simulations, we don't track which orders are pending
        // The scheduler will maintain this state
        // Return empty list (scheduler maintains pending orders internally)
        return new ArrayList<>();
    }
    
    @Override
    public List<Integer> getCancelledFlightIds(LocalDateTime startTime, LocalDateTime endTime) {
        // TODO: Filter cancellations by time window
        // For now, return empty list
        return new ArrayList<>(cancelledFlightIds);
    }
    
    /**
     * Get all flights (useful for debugging/testing).
     * 
     * @return All flights loaded from CSV
     */
    public List<PlannerFlight> getAllFlights() {
        return new ArrayList<>(allFlights);
    }
    
    /**
     * Get all orders (useful for debugging/testing).
     * 
     * @return All orders loaded from CSV
     */
    public List<PlannerOrder> getAllOrders() {
        return new ArrayList<>(allOrders);
    }
    
    /**
     * Get airport by code.
     * 
     * @param code Airport code (e.g., "SPIM")
     * @return PlannerAirport or null if not found
     */
    public PlannerAirport getAirportByCode(String code) {
        return airportMap.get(code);
    }
}





