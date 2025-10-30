package pe.edu.pucp.morapack.algos.scheduler;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for providing data to the scheduler.
 * 
 * Two implementations:
 * - DatabaseDataProvider: For daily operations (real-time)
 * - FileDataProvider: For simulations (weekly, collapse)
 * 
 * This abstraction allows the Tabu algorithm to remain unchanged
 * regardless of the data source.
 */
public interface DataProvider {
    
    /**
     * Get all airports (static data)
     */
    List<PlannerAirport> getAirports();
    
    /**
     * Get flights within a time window.
     * 
     * For daily operations: Query database for scheduled flights
     * For simulations: Generate flights from templates based on simulation time
     * 
     * @param startTime Start of time window
     * @param endTime End of time window
     * @return List of flights in the time window
     */
    List<PlannerFlight> getFlights(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Get orders within a time window.
     * 
     * For daily operations: Query database for new orders
     * For simulations: Filter orders from CSV based on simulation time
     * 
     * @param startTime Start of time window
     * @param endTime End of time window
     * @return List of orders in the time window
     */
    List<PlannerOrder> getOrders(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Get all pending orders (not yet assigned to routes).
     * Used for re-planning when new capacity becomes available.
     * 
     * @return List of pending orders
     */
    List<PlannerOrder> getPendingOrders();
    
    /**
     * Get cancelled flights within a time window.
     * Only relevant for simulations with pre-defined cancellations.
     * 
     * @param startTime Start of time window
     * @param endTime End of time window
     * @return List of cancelled flight IDs
     */
    List<Integer> getCancelledFlightIds(LocalDateTime startTime, LocalDateTime endTime);
}

