package pe.edu.pucp.morapack.algos.scheduler;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;
import pe.edu.pucp.morapack.algos.entities.Solution;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PlannerScheduler: Orchestrates periodic execution of the Tabu Search planner.
 * 
 * Implements "planned programming" scheduling based on:
 * - Ta (Algorithm Time) = 1 minute [FIXED]
 * - Sa (Algorithm Jump) = 5 minutes [FIXED]
 * - K (Proportionality Constant) = Varies by scenario:
 *   • K=1  → DAILY:    Sc = 1 × 5 = 5 minutes of data per iteration
 *   • K=14 → WEEKLY:   Sc = 14 × 5 = 70 minutes of data per iteration (7 days)
 *   • K=75 → COLLAPSE: Sc = 75 × 5 = 375 minutes of data per iteration (6.25 hours)
 * - Sc (Consumption Jump) = K × Sa [CALCULATED]
 * 
 * In each iteration:
 * 1. Get new flights for the next Sc minutes (from data timeline)
 * 2. Get new orders for the next Sc minutes (from data timeline)
 * 3. Run Tabu Search to assign orders to flights (takes Ta = 1 minute in real-time)
 * 4. Advance simulation time by Sc minutes
 * 5. Wait Sa minutes in real-time before next iteration
 * 6. Repeat
 * 
 * NOTE: The algorithm execution time (Ta) and wait time (Sa) are conceptual
 * for the planned programming model. In this implementation, we run
 * iterations as fast as possible since we're simulating accelerated time.
 */
public class PlannerScheduler {
    
    private final DataProvider dataProvider;
    private final ScenarioConfig scenario;
    private final TabuSearchPlanner planner;
    
    // Simulation state
    private LocalDateTime currentTime;
    private final LocalDateTime endTime;
    private int iterationCount = 0;
    
    // Results
    private final List<PlannerShipment> allShipments = new ArrayList<>();
    private final List<PlannerOrder> unassignedOrders = new ArrayList<>();
    
    /**
     * Create a scheduler for a specific scenario.
     * 
     * @param dataProvider Source of flights and orders
     * @param scenario Simulation scenario (DAILY, WEEKLY, COLLAPSE)
     * @param startTime Start time of simulation
     * @param endTime End time of simulation
     */
    public PlannerScheduler(
            DataProvider dataProvider,
            ScenarioConfig scenario,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        
        this.dataProvider = dataProvider;
        this.scenario = scenario;
        this.currentTime = startTime;
        this.endTime = endTime;
        
        // Initialize TabuSearchPlanner
        this.planner = new TabuSearchPlanner();
        
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           PLANNER SCHEDULER INITIALIZED                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println("   Scenario:     " + scenario.getType());
        System.out.println("   K:            " + scenario.getK() + " (proportionality constant)");
        System.out.println("   Sa:           " + scenario.getSaMinutes() + " minutes (algorithm jump)");
        System.out.println("   Sc:           " + scenario.getScMinutes() + " minutes (data consumption = K × Sa)");
        System.out.println("   Start Time:   " + startTime);
        System.out.println("   End Time:     " + endTime);
        System.out.println("   Duration:     " + java.time.Duration.between(startTime, endTime).toDays() + " days");
        System.out.println();
    }
    
    /**
     * Run the scheduler until the end time is reached.
     * 
     * @return List of all shipments created
     */
    public List<PlannerShipment> runSimulation() {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           STARTING SIMULATION                                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        while (currentTime.isBefore(endTime)) {
            iterationCount++;
            
            // Calculate time window for this iteration using Sc = K × Sa
            // Sc is the amount of simulation time to consume in this iteration
            int scMinutes = scenario.getScMinutes(); // Sc = K × Sa
            
            LocalDateTime windowStart = currentTime;
            LocalDateTime windowEnd = currentTime.plusMinutes(scMinutes);
            
            // Don't go past end time
            if (windowEnd.isAfter(endTime)) {
                windowEnd = endTime;
            }
            
            System.out.println("─────────────────────────────────────────────────────────────────");
            System.out.println("⏰ ITERATION #" + iterationCount);
            System.out.println("   Current Time:    " + currentTime);
            System.out.println("   Window:          " + windowStart + " → " + windowEnd);
            System.out.println("   Consuming Sc:    " + scMinutes + " minutes (K=" + scenario.getK() + " × Sa=" + scenario.getSaMinutes() + ")");
            System.out.println("─────────────────────────────────────────────────────────────────");
            
            // Execute one iteration
            executeIteration(windowStart, windowEnd);
            
            // Advance simulation time
            currentTime = windowEnd;
        }
        
        printFinalSummary();
        return new ArrayList<>(allShipments);
    }
    
    /**
     * Execute one scheduler iteration.
     * 
     * @param windowStart Start of time window
     * @param windowEnd End of time window
     */
    private void executeIteration(LocalDateTime windowStart, LocalDateTime windowEnd) {
        // 1. Get new data for this time window
        List<PlannerFlight> newFlights = dataProvider.getFlights(windowStart, windowEnd);
        List<PlannerOrder> newOrders = dataProvider.getOrders(windowStart, windowEnd);
        List<Integer> cancelledFlights = dataProvider.getCancelledFlightIds(windowStart, windowEnd);
        
        System.out.println("\n📊 DATA FOR THIS ITERATION:");
        System.out.println("   New Flights:  " + newFlights.size());
        System.out.println("   New Orders:   " + newOrders.size());
        System.out.println("   Cancellations: " + cancelledFlights.size());
        
        // If no new data, skip this iteration
        if (newOrders.isEmpty() && unassignedOrders.isEmpty()) {
            System.out.println("   ⏭️  No orders to process, skipping iteration\n");
            return;
        }
        
        // 2. Combine new orders with previously unassigned orders
        List<PlannerOrder> ordersToProcess = new ArrayList<>(unassignedOrders);
        ordersToProcess.addAll(newOrders);
        
        System.out.println("   Total Orders to Process: " + ordersToProcess.size());
        System.out.println("      (New: " + newOrders.size() + ", Pending: " + unassignedOrders.size() + ")");
        
        // 3. Run Tabu Search
        System.out.println("\n🔄 RUNNING TABU SEARCH...");
        Solution solution = planner.optimize(ordersToProcess, newFlights, dataProvider.getAirports());
        List<PlannerShipment> newShipments = ((TabuSolution) solution).getPlannerShipments();
        
        // 4. Process results
        allShipments.addAll(newShipments);
        
        // Determine which orders were assigned
        List<Integer> assignedOrderIds = new ArrayList<>();
        for (PlannerShipment s : newShipments) {
            assignedOrderIds.add(s.getOrder().getId());
        }
        
        // Update unassigned orders
        unassignedOrders.clear();
        for (PlannerOrder order : ordersToProcess) {
            if (!assignedOrderIds.contains(order.getId())) {
                unassignedOrders.add(order);
            }
        }
        
        System.out.println("\n✅ ITERATION RESULTS:");
        System.out.println("   Shipments Created: " + newShipments.size());
        System.out.println("   Orders Assigned:   " + assignedOrderIds.size() + "/" + ordersToProcess.size());
        System.out.println("   Still Pending:     " + unassignedOrders.size());
        System.out.println();
    }
    
    /**
     * Print final simulation summary.
     */
    private void printFinalSummary() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║           SIMULATION COMPLETED                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println("\n📊 FINAL SUMMARY:");
        System.out.println("   Total Iterations:     " + iterationCount);
        System.out.println("   Total Shipments:      " + allShipments.size());
        System.out.println("   Unassigned Orders:    " + unassignedOrders.size());
        System.out.println("   Scenario:             " + scenario.getType() + " (K=" + scenario.getK() + ", Sc=" + scenario.getScMinutes() + " min)");
        System.out.println();
        
        // Calculate total products assigned
        int totalProducts = 0;
        for (PlannerShipment s : allShipments) {
            totalProducts += s.getQuantity();
        }
        System.out.println("   Total Products Assigned: " + totalProducts);
        
        if (!unassignedOrders.isEmpty()) {
            System.out.println("\n⚠️  WARNING: " + unassignedOrders.size() + " orders could not be assigned");
            int unassignedProducts = 0;
            for (PlannerOrder o : unassignedOrders) {
                unassignedProducts += o.getTotalQuantity();
            }
            System.out.println("   Unassigned Products: " + unassignedProducts);
        }
        
        System.out.println("\n═══════════════════════════════════════════════════════════════════\n");
    }
    
    // =========================================================================
    // GETTERS
    // =========================================================================
    
    public List<PlannerShipment> getAllShipments() {
        return new ArrayList<>(allShipments);
    }
    
    public List<PlannerOrder> getUnassignedOrders() {
        return new ArrayList<>(unassignedOrders);
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }
    
    public ScenarioConfig getScenario() {
        return scenario;
    }
}

