package pe.edu.pucp.morapack.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.data.providers.DataProvider;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.simulation.TabuSimulationResponse;
import pe.edu.pucp.morapack.dto.websocket.SimulationState;
import pe.edu.pucp.morapack.dto.websocket.SimulationStatusUpdate;
import pe.edu.pucp.morapack.model.FlightCancellation;
import pe.edu.pucp.morapack.model.ReplanificationTask;
import pe.edu.pucp.morapack.model.DynamicOrder;
import pe.edu.pucp.morapack.utils.TabuSolutionToDtoConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A single simulation session that can be controlled in real-time.
 * Runs in its own thread and responds to control commands (pause, resume, stop, speed).
 */
public class SimulationSession implements Runnable {
    
    private final String sessionId;
    private final String userId;
    private final DataProvider dataProvider;
    private final ScenarioConfig scenario;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final SimpMessagingTemplate messagingTemplate;
    
    // 🆕 Dynamic event services
    private final CancellationService cancellationService;
    private final DynamicOrderService dynamicOrderService;
    private final OrderInjectionService orderInjectionService;
    private final ReplanificationService replanificationService;
    private final FlightStatusTracker flightStatusTracker;
    
    // State management
    private final AtomicReference<SimulationState> state = new AtomicReference<>(SimulationState.IDLE);
    private final AtomicBoolean pauseRequested = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private volatile double speedMultiplier = 1.0;  // 1.0 = normal speed
    
    // Simulation progress
    private LocalDateTime currentTime;
    private int iterationCount = 0;
    private int totalExpectedIterations = 0;
    
    // Tabu Search planner
    private final TabuSearchPlanner planner;
    
    // Results accumulation
    private final List<TabuSimulationResponse> allResults = new ArrayList<>();
    
    // Pending orders accumulation (orders not fully assigned in previous iterations)
    private final List<PlannerOrder> pendingOrders = new ArrayList<>();
    
    // Metrics tracking
    private final java.util.Map<Integer, Integer> totalAssignedPerOrder = new java.util.HashMap<>();
    private final java.util.Map<Integer, PlannerOrder> allProcessedOrdersMap = new java.util.HashMap<>();
    private int totalShipmentsCreated = 0;
    
    // 🆕 Tracking for replanification and rendering
    private TabuSolution lastSolution = null;
    
    // 🆕 Accumulated shipments from ALL iterations (for continuous plane rendering)
    private java.util.List<pe.edu.pucp.morapack.algos.entities.PlannerShipment> allShipments = new java.util.ArrayList<>();
    
    // 🆕 Track completed orders (once completed, they stay completed)
    private java.util.Set<Integer> completedOrderIds = new java.util.HashSet<>();
    
    public SimulationSession(
            String userId,
            DataProvider dataProvider,
            ScenarioConfig scenario,
            LocalDateTime startTime,
            LocalDateTime endTime,
            SimpMessagingTemplate messagingTemplate,
            CancellationService cancellationService,
            DynamicOrderService dynamicOrderService,
            OrderInjectionService orderInjectionService,
            ReplanificationService replanificationService,
            FlightStatusTracker flightStatusTracker) {
        
        this.sessionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.dataProvider = dataProvider;
        this.scenario = scenario;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentTime = startTime;
        this.messagingTemplate = messagingTemplate;
        this.planner = new TabuSearchPlanner();
        
        // 🆕 Assign dynamic services
        this.cancellationService = cancellationService;
        this.dynamicOrderService = dynamicOrderService;
        this.orderInjectionService = orderInjectionService;
        this.replanificationService = replanificationService;
        this.flightStatusTracker = flightStatusTracker;
        
        // Calculate expected iterations
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        this.totalExpectedIterations = (int) Math.ceil((double) totalMinutes / scenario.getScMinutes());
        
        System.out.println("[SimulationSession] Created: " + sessionId + " for user: " + userId);
        System.out.println("   Scenario: " + scenario.getType() + " (K=" + scenario.getK() + ", Sc=" + scenario.getScMinutes() + ")");
        System.out.println("   Expected iterations: " + totalExpectedIterations);
        
        // 🆕 Load scheduled cancellations and dynamic orders
        initializeDynamicEvents();
    }
    
    /**
     * Initialize dynamic events (cancellations and orders) from files.
     */
    private void initializeDynamicEvents() {
        try {
            // Load scheduled cancellations
            String cancellationFile = "data/cancellations/cancellations_2025_01.txt";
            int loadedCancellations = cancellationService.loadScheduledCancellations(
                cancellationFile,
                startTime.toLocalDate()
            );
            System.out.println("   📄 Loaded " + loadedCancellations + " scheduled cancellations");
            
            // Load scheduled dynamic orders
            String ordersFile = "data/dynamic_orders/dynamic_orders_2025_01.txt";
            int loadedOrders = dynamicOrderService.loadScheduledOrders(
                ordersFile,
                startTime.toLocalDate()
            );
            System.out.println("   📄 Loaded " + loadedOrders + " scheduled dynamic orders");
            
            // Configure airports in OrderInjectionService
            List<PlannerAirport> airports = new ArrayList<>(dataProvider.getAirports());
            orderInjectionService.setAirports(airports);
            System.out.println("   📄 Configured " + airports.size() + " airports for order injection");
            
        } catch (Exception e) {
            System.err.println("   ⚠️ Warning: Could not load dynamic events: " + e.getMessage());
            System.err.println("   Continuing simulation without scheduled events...");
        }
    }
    
    @Override
    public void run() {
        try {
            state.set(SimulationState.STARTING);
            sendStatusUpdate(SimulationStatusUpdate.starting());
            
            // Brief delay to ensure frontend is ready
            Thread.sleep(500);
            
            state.set(SimulationState.RUNNING);
            runSimulation();
            
            if (state.get() == SimulationState.RUNNING) {
                state.set(SimulationState.COMPLETED);
                sendStatusUpdate(SimulationStatusUpdate.completed(iterationCount));
                System.out.println("[SimulationSession] " + sessionId + " completed successfully");
            }
            
        } catch (InterruptedException e) {
            state.set(SimulationState.STOPPED);
            sendStatusUpdate(new SimulationStatusUpdate(SimulationState.STOPPED, "Simulation interrupted"));
            System.out.println("[SimulationSession] " + sessionId + " interrupted");
            Thread.currentThread().interrupt();
            
        } catch (Exception e) {
            state.set(SimulationState.ERROR);
            sendStatusUpdate(SimulationStatusUpdate.error(
                "Simulation error: " + e.getMessage(),
                e.toString()
            ));
            System.err.println("[SimulationSession] " + sessionId + " error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void runSimulation() throws InterruptedException {
        while (currentTime.isBefore(endTime) && !stopRequested.get()) {
            
            // Check for pause
            while (pauseRequested.get() && !stopRequested.get()) {
                if (state.get() != SimulationState.PAUSED) {
                    state.set(SimulationState.PAUSED);
                    sendStatusUpdate(SimulationStatusUpdate.paused(iterationCount, totalExpectedIterations));
                    System.out.println("[SimulationSession] " + sessionId + " paused at iteration " + iterationCount);
                }
                Thread.sleep(200); // Check every 200ms
            }
            
            if (stopRequested.get()) {
                break;
            }
            
            // Resume if we were paused
            if (state.get() == SimulationState.PAUSED) {
                state.set(SimulationState.RUNNING);
                sendStatusUpdate(new SimulationStatusUpdate(SimulationState.RUNNING, "Simulation resumed"));
                System.out.println("[SimulationSession] " + sessionId + " resumed");
            }
            
            // Execute one iteration
            executeIteration();
            
            // Speed-controlled delay (ALWAYS apply, even for empty iterations)
            applySpeedControlledDelay();
        }
        
        System.out.println("\n[SimulationSession] " + sessionId + " ending simulation loop");
        printFinalMetrics();
    }
    
    private void executeIteration() {
        iterationCount++;
        
        // Calculate time window for this iteration
        int scMinutes = scenario.getScMinutes();
        LocalDateTime windowStart = currentTime;
        LocalDateTime windowEnd = currentTime.plusMinutes(scMinutes);
        
        if (windowEnd.isAfter(endTime)) {
            windowEnd = endTime;
        }
        
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("[SimulationSession] " + sessionId + " - Iteration #" + iterationCount);
        System.out.println("   Window: " + windowStart + " → " + windowEnd + " (+" + scMinutes + " min)");
        System.out.println("   Current time will advance from " + currentTime + " to " + windowEnd);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 🆕 STEP 1: Process cancellations at current time
        List<FlightCancellation> newCancellations = cancellationService.processCancellationsAt(currentTime);
        if (!newCancellations.isEmpty()) {
            System.out.println("   🚫 " + newCancellations.size() + " flight(s) cancelled:");
            for (FlightCancellation c : newCancellations) {
                System.out.println("      • " + c.getFlightOrigin() + " → " + c.getFlightDestination() + 
                                 " @ " + c.getScheduledDepartureTime());
            }
        }
        
        // 🆕 STEP 2: Process dynamic order injection
        List<DynamicOrder> newDynamicOrders = dynamicOrderService.getOrdersToInjectAt(currentTime);
        if (!newDynamicOrders.isEmpty()) {
            System.out.println("   📦 " + newDynamicOrders.size() + " dynamic order(s) injected:");
            for (DynamicOrder o : newDynamicOrders) {
                System.out.println("      • " + o.getOrigin() + " → " + o.getDestination() + 
                                 " (" + o.getQuantity() + " units)");
            }
        }
        
        // 🆕 STEP 3: Convert dynamic orders to PlannerOrders
        List<PlannerOrder> injectedOrders = orderInjectionService.processOrderInjections(currentTime);
        if (!injectedOrders.isEmpty()) {
            System.out.println("   ✅ " + injectedOrders.size() + " order(s) converted to planner format");
        }
        
        // Get data for this window
        List<PlannerFlight> flights = dataProvider.getFlights(windowStart, windowEnd);
        List<PlannerOrder> newOrders = dataProvider.getOrders(windowStart, windowEnd);
        
        // 🆕 STEP 4: Filter out cancelled flights
        List<PlannerFlight> activeFlights = filterActiveFlight(flights, newCancellations);
        if (flights.size() != activeFlights.size()) {
            System.out.println("   ⚠️ " + (flights.size() - activeFlights.size()) + 
                             " cancelled flight(s) filtered out");
        }
        
        // Add new orders to pending queue (including injected ones)
        pendingOrders.addAll(newOrders);
        pendingOrders.addAll(injectedOrders);
        
        // Track all orders for metrics
        for (PlannerOrder order : newOrders) {
            allProcessedOrdersMap.put(order.getId(), order);
        }
        for (PlannerOrder order : injectedOrders) {
            allProcessedOrdersMap.put(order.getId(), order);
        }
        
        // Process ALL pending orders (not just new ones)
        List<PlannerOrder> allOrders = new ArrayList<>(pendingOrders);
        
        System.out.println("   Flights: " + activeFlights.size() + ", Orders: " + allOrders.size() + 
                         " (new: " + newOrders.size() + ", injected: " + injectedOrders.size() + 
                         ", pending: " + (allOrders.size() - newOrders.size() - injectedOrders.size()) + ")");
        
        // 🆕 STEP 5: Handle replanification if there are new cancellations
        if (!newCancellations.isEmpty() && lastSolution != null) {
            handleReplanification(newCancellations, allOrders, activeFlights);
        }
        
        // Run Tabu Search
        if (!allOrders.isEmpty() && !activeFlights.isEmpty()) {
            // Get airports from data provider
            List<PlannerAirport> airports = new ArrayList<>(dataProvider.getAirports());
            
            Solution solution = planner.optimize(allOrders, activeFlights, airports);
            
                // Cast to TabuSolution and convert to DTO
                if (solution instanceof TabuSolution tabuSolution) {
                    // 🆕 Save solution for potential replanification
                    lastSolution = tabuSolution;
                    
                    // 🆕 Accumulate shipments for continuous rendering (don't lose old shipments)
                    if (tabuSolution.getPlannerShipments() != null) {
                        allShipments.addAll(tabuSolution.getPlannerShipments());
                        System.out.println("   📦 Accumulated " + tabuSolution.getPlannerShipments().size() + " new shipments (total: " + allShipments.size() + ")");
                    }
                    
                    // ✅ FIX: Actualizar solo los pedidos de esta iteración
                    // TabuSearch devuelve una solución para los pedidos pendientes actuales,
                    // no para todos los históricos, así que solo actualizamos esos.
                    
                    // 1. Identificar qué pedidos están en la solución actual
                    java.util.Set<Integer> currentOrderIds = new java.util.HashSet<>();
                    for (var shipment : tabuSolution.getPlannerShipments()) {
                        currentOrderIds.add(shipment.getOrder().getId());
                    }
                    
                    // 2. Resetear SOLO las asignaciones de pedidos actuales (no históricos)
                    for (Integer orderId : currentOrderIds) {
                        totalAssignedPerOrder.put(orderId, 0);
                    }
                    
                    // 3. Recalcular cantidades para esta solución
                    int shipmentsThisIteration = 0;
                    for (var shipment : tabuSolution.getPlannerShipments()) {
                        shipmentsThisIteration++;
                        int orderId = shipment.getOrder().getId();
                        int quantity = shipment.getQuantity();
                        totalAssignedPerOrder.put(orderId, 
                            totalAssignedPerOrder.getOrDefault(orderId, 0) + quantity);
                    }
                    
                    // Update total shipments counter (accumulative across all iterations)
                    totalShipmentsCreated += shipmentsThisIteration;
                    
                    // Remove fully assigned orders from pending queue
                    updatePendingOrders(tabuSolution, allOrders);
                
                // Build response manually (similar to TabuSimulationService)
                TabuSimulationResponse response = new TabuSimulationResponse();
                response.airports = TabuSolutionToDtoConverter.toAirportDtos(airports);

                // Enrich airports with dynamic runtime data
                enrichAirportData(response.airports, tabuSolution);
                
                // Convert simulated time to Instant for animation interpolation
                java.time.Instant simulatedInstant = currentTime
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant();
                
                // ✅ FIX: Generate itineraries from ALL accumulated shipments (not just new ones)
                // Filter to show only active flights (not yet arrived)
                java.util.List<pe.edu.pucp.morapack.algos.entities.PlannerShipment> activeShipments = new java.util.ArrayList<>();
                for (pe.edu.pucp.morapack.algos.entities.PlannerShipment shipment : allShipments) {
                    if (shipment.getFlights() != null && !shipment.getFlights().isEmpty()) {
                        PlannerFlight lastFlight = shipment.getFlights().get(shipment.getFlights().size() - 1);
                        if (lastFlight.getArrivalTime().isAfter(currentTime)) {
                            activeShipments.add(shipment);
                        }
                    }
                }
                
                TabuSolution accumulatedSolution = new TabuSolution();
                accumulatedSolution.addAllPlannerShipments(activeShipments);
                response.itineraries = TabuSolutionToDtoConverter.toItineraryDtos(accumulatedSolution, simulatedInstant);
                System.out.println("   ✈️  Itinerarios generated: " + activeShipments.size() + " planes in flight (from " + allShipments.size() + " total shipments)");
                
                // 🔍 DEBUG: Ver estructura del primer itinerario
                if (response.itineraries != null && response.itineraries.length > 0) {
                    var firstItin = response.itineraries[0];
                    System.out.println("   🔍 DEBUG Primer itinerario:");
                    System.out.println("      ID: " + firstItin.id);
                    System.out.println("      OrderID: " + firstItin.orderId);
                    System.out.println("      Segmentos: " + (firstItin.segments != null ? firstItin.segments.length : 0));
                    if (firstItin.segments != null && firstItin.segments.length > 0) {
                        var firstSeg = firstItin.segments[0];
                        System.out.println("      Primer segmento vuelo:");
                        System.out.println("         codigo: " + firstSeg.flight.code);
                        System.out.println("         salidaProgramadaISO: " + firstSeg.flight.scheduledDepartureISO);
                        System.out.println("         llegadaProgramadaISO: " + firstSeg.flight.scheduledArrivalISO);
                        System.out.println("         capacidad: " + firstSeg.flight.capacity);
                    }
                }
                
                // Add order tracking data
                response.orders = buildOrderSummaries();
                response.metrics = calculateOrderMetrics();
                
                // 🆕 Update flight status tracker
                flightStatusTracker.updateFlightStatuses(
                    java.util.Arrays.asList(response.itineraries), 
                    currentTime
                );
                
                // NOTE: response.meta not populated (algorithm doesn't expose costs)
                // All relevant metrics already included in pedidos/metricas
                allResults.add(response);
                
                // Send update to user
                SimulationStatusUpdate update = SimulationStatusUpdate.running(
                    iterationCount,
                    totalExpectedIterations,
                    currentTime
                );
                update.setCurrentSpeed(speedMultiplier);
                update.setLatestResult(response);
                sendStatusUpdate(update);
            }
            
        } else {
            System.out.println("   Skipping iteration (no orders or flights)");
            
            // IMPORTANT: Even without new data, we must:
            // 1. Update order statuses (IN_TRANSIT -> COMPLETED when flight arrives)
            // 2. Regenerate itinerarios with current simulated time (for plane positions)
            TabuSimulationResponse response = new TabuSimulationResponse();
            response.orders = buildOrderSummaries();
            response.metrics = calculateOrderMetrics();
            
            // ✅ Regenerate itinerarios with CURRENT simulated time for proper plane positions
            // Filter to show ONLY active shipments (flights still in transit, not arrived yet)
            if (!allShipments.isEmpty()) {
                java.time.Instant simulatedInstant = currentTime
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant();
                
                // Filter: only shipments whose last flight hasn't arrived yet
                java.util.List<pe.edu.pucp.morapack.algos.entities.PlannerShipment> activeShipments = new java.util.ArrayList<>();
                for (pe.edu.pucp.morapack.algos.entities.PlannerShipment shipment : allShipments) {
                    if (shipment.getFlights() != null && !shipment.getFlights().isEmpty()) {
                        PlannerFlight lastFlight = shipment.getFlights().get(shipment.getFlights().size() - 1);
                        // Keep if flight hasn't arrived yet
                        if (lastFlight.getArrivalTime().isAfter(currentTime)) {
                            activeShipments.add(shipment);
                        }
                    }
                }
                
                System.out.println("   ℹ️  Active shipments: " + activeShipments.size() + " / " + allShipments.size() + " (filtered out arrived flights)");
                
                // Create solution with only active shipments
                TabuSolution accumulatedSolution = new TabuSolution();
                accumulatedSolution.addAllPlannerShipments(activeShipments);
                
                response.itineraries = TabuSolutionToDtoConverter.toItineraryDtos(accumulatedSolution, simulatedInstant);
                System.out.println("   ✈️  Itinerarios generated: " + response.itineraries.length + " planes in flight");
                
                // 🔍 DEBUG: Ver estructura del primer itinerario
                if (response.itineraries != null && response.itineraries.length > 0) {
                    var firstItin = response.itineraries[0];
                    System.out.println("   🔍 DEBUG Primer itinerario (no-data path):");
                    System.out.println("      ID: " + firstItin.id);
                    System.out.println("      OrderID: " + firstItin.orderId);
                    System.out.println("      Segmentos: " + (firstItin.segments != null ? firstItin.segments.length : 0));
                    if (firstItin.segments != null && firstItin.segments.length > 0) {
                        var firstSeg = firstItin.segments[0];
                        System.out.println("      Primer segmento vuelo:");
                        System.out.println("         codigo: " + firstSeg.flight.code);
                        System.out.println("         salidaProgramadaISO: " + firstSeg.flight.scheduledDepartureISO);
                        System.out.println("         llegadaProgramadaISO: " + firstSeg.flight.scheduledArrivalISO);
                        System.out.println("         capacidad: " + firstSeg.flight.capacity);
                    }
                }
                
                System.out.println("   📊 Sending to frontend: " + response.itineraries.length + " itinerarios, " + 
                                 (response.orders != null ? response.orders.length : 0) + " orders");
            } else {
                response.itineraries = new pe.edu.pucp.morapack.dto.simulation.ItineraryDTO[0];
                System.out.println("   ⚠️  No accumulated shipments, no itinerarios to show");
            }
            
            // Send update with current state
            SimulationStatusUpdate update = SimulationStatusUpdate.running(
                iterationCount,
                totalExpectedIterations,
                currentTime
            );
            update.setCurrentSpeed(speedMultiplier);
            update.setMessage("Iteration " + iterationCount + " (no data to process)");
            update.setLatestResult(response);
            sendStatusUpdate(update);
        }
        
        // Advance simulation time
        LocalDateTime previousTime = currentTime;
        currentTime = windowEnd;
        
        System.out.println("   ⏰ Time advanced: " + previousTime + " → " + currentTime);
        System.out.println("   ✅ Iteration completed, sending update to frontend");
    }
    
    private void applySpeedControlledDelay() throws InterruptedException {
        // Base delay between iterations (in ms)
        // Ajustado para que la simulación dure ~30 minutos
        // Con K=12 → 168 iteraciones
        // 168 iter × ~10s (delay + processing) = ~1800s = 30 min
        long baseDelayMs = 8000;  // 8 seconds base (+ ~2s TabuSearch = ~10s total)
        
        // Adjust by speed multiplier
        // speedMultiplier = 1.0 → 8000ms delay
        // speedMultiplier = 2.0 → 4000ms delay (2x faster)
        // speedMultiplier = 0.5 → 16000ms delay (2x slower)
        long adjustedDelay = (long) (baseDelayMs / speedMultiplier);
        
        // Ensure minimum delay of 100ms (para speed muy alto)
        adjustedDelay = Math.max(100, adjustedDelay);
        
        Thread.sleep(adjustedDelay);
    }
    
    private void sendStatusUpdate(SimulationStatusUpdate update) {
        try {
            System.out.println("Sending update to session '" + sessionId + "': " + update.getState() + 
                             " (iter " + update.getCurrentIteration() + "/" + update.getTotalIterations() + ")");
            
            // Send to specific session topic
            // Each session has its own topic: /topic/simulation/{sessionId}
            messagingTemplate.convertAndSend(
                "/topic/simulation/" + sessionId,
                update
            );
            
            System.out.println("   ✅ Update sent to /topic/simulation/" + sessionId);
        } catch (Exception e) {
            System.err.println("[SimulationSession] ❌ Error sending update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update pending orders list by removing fully assigned orders.
     * Orders that were partially assigned remain in the pending queue.
     * 
     * @param solution The TabuSolution containing shipments
     * @param processedOrders The orders that were attempted to be assigned in this iteration
     */
    private void updatePendingOrders(TabuSolution solution, List<PlannerOrder> processedOrders) {
        // Calculate total assigned quantity per order
        java.util.Map<Integer, Integer> assignedPerOrder = new java.util.HashMap<>();
        
        for (var shipment : solution.getPlannerShipments()) {
            int orderId = shipment.getOrder().getId();
            assignedPerOrder.put(orderId, 
                assignedPerOrder.getOrDefault(orderId, 0) + shipment.getQuantity());
        }
        
        // Remove orders that were FULLY assigned
        int removedCount = 0;
        java.util.Iterator<PlannerOrder> iterator = pendingOrders.iterator();
        while (iterator.hasNext()) {
            PlannerOrder order = iterator.next();
            int assigned = assignedPerOrder.getOrDefault(order.getId(), 0);
            
            if (assigned >= order.getTotalQuantity()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0 || !assignedPerOrder.isEmpty()) {
            System.out.println(String.format("   ✅ Order queue updated: %d fully assigned removed, %d still pending",
                   removedCount, pendingOrders.size()));
        }
    }
    
    /**
     * Enrich airport DTOs with dynamic runtime information.
     * Calculates:
     * - Pending orders at this airport
     * - Orders with this airport as destination
     * - Products waiting at airport
     * - Active flights from/to this airport
     * - Capacity usage (products on ground + in transit)
     * 
     * @param airportDtos The airport DTOs to enrich
     * @param solution The current TabuSolution (can be null if no solution yet)
     */
    private void enrichAirportData(
            pe.edu.pucp.morapack.dto.simulation.AirportDTO[] airportDtos,
            TabuSolution solution) {
        
        if (airportDtos == null) return;
        
        // Create a map for quick lookup
        java.util.Map<String, pe.edu.pucp.morapack.dto.simulation.AirportDTO> airportMap = new java.util.HashMap<>();
        for (var dto : airportDtos) {
            airportMap.put(dto.code, dto);
            // Initialize capacity as available
            dto.usedCapacity = 0;
            dto.availableCapacity = dto.totalCapacity;
            dto.usagePercentage = 0.0;
        }
        
        // 1. Calculate pending orders per airport (origin) - these are ON GROUND
        for (PlannerOrder order : pendingOrders) {
            String originCode = order.getOrigin().getCode();
            var dto = airportMap.get(originCode);
            if (dto != null) {
                dto.waitingOrders++;
                dto.waitingProducts += order.getTotalQuantity();
                // Add to capacity usage (products waiting at origin)
                dto.usedCapacity += order.getTotalQuantity();
            }

            // Count destination orders
            String destCode = order.getDestination().getCode();
            var destDto = airportMap.get(destCode);
            if (destDto != null) {
                destDto.destinationOrders++;
            }
        }
        
        // 2. Calculate active flights and in-transit products if solution exists
        if (solution != null) {
            // Track products in transit to each airport
            java.util.Map<String, Integer> productsInTransit = new java.util.HashMap<>();
            
            for (var shipment : solution.getPlannerShipments()) {
                for (int i = 0; i < shipment.getFlights().size(); i++) {
                    var flight = shipment.getFlights().get(i);
                    String originCode = flight.getOrigin().getCode();
                    String destCode = flight.getDestination().getCode();
                    
                    var originDto = airportMap.get(originCode);
                    if (originDto != null) {
                        originDto.activeFlightsFrom++;
                    }

                    var destDto = airportMap.get(destCode);
                    if (destDto != null) {
                        destDto.activeFlightsTo++;

                        // Products are IN TRANSIT if flight hasn't arrived yet
                        if (currentTime.isBefore(flight.getArrivalTime())) {
                            productsInTransit.put(destCode,
                                productsInTransit.getOrDefault(destCode, 0) + shipment.getQuantity());
                        }
                    }

                    // Track flight at origin airport (before departure)
                    if (currentTime.isBefore(flight.getDepartureTime())) {
                        if (originDto != null && !originDto.groundedFlights.contains(flight.getCode())) {
                            originDto.groundedFlights.add(flight.getCode());
                        }
                    }
                }
            }
            
            // Add in-transit products to capacity usage
            for (var entry : productsInTransit.entrySet()) {
                var dto = airportMap.get(entry.getKey());
                if (dto != null) {
                    dto.usedCapacity += entry.getValue();
                }
            }
        }

        // 3. Calculate final capacity metrics
        for (var dto : airportDtos) {
            dto.availableCapacity = Math.max(0, dto.totalCapacity - dto.usedCapacity);
            dto.usagePercentage = dto.totalCapacity > 0 ?
                (dto.usedCapacity * 100.0) / dto.totalCapacity : 0.0;
        }
    }
    
    // ========== DYNAMIC EVENTS METHODS ==========
    
    /**
     * Filter out cancelled flights from the available flights list.
     * 
     * @param flights All flights in the current window
     * @param cancellations List of newly cancelled flights
     * @return Filtered list of active (non-cancelled) flights
     */
    private List<PlannerFlight> filterActiveFlight(
            List<PlannerFlight> flights, 
            List<FlightCancellation> cancellations) {
        
        if (cancellations.isEmpty()) {
            return flights;
        }
        
        // Filter flights by checking if they are cancelled
        return flights.stream()
            .filter(flight -> {
                // Check if this flight is cancelled
                String origin = flight.getOrigin().getCode();
                String destination = flight.getDestination().getCode();
                String scheduledTime = flight.getDepartureTime().toString();
                
                return !cancellationService.isFlightCancelled(origin, destination, scheduledTime);
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Handle replanification when flights are cancelled.
     * Triggers the ReplanificationService to reassign affected orders.
     * 
     * @param cancellations Newly cancelled flights
     * @param allOrders All pending orders
     * @param availableFlights All active (non-cancelled) flights
     */
    private void handleReplanification(
            List<FlightCancellation> cancellations,
            List<PlannerOrder> allOrders,
            List<PlannerFlight> availableFlights) {
        
        System.out.println("   🔄 REPLANIFICATION triggered for " + cancellations.size() + " cancellation(s)");
        
        // Get airports
        List<PlannerAirport> airports = new ArrayList<>(dataProvider.getAirports());
        
        // Trigger replanification for each cancellation
        for (FlightCancellation cancellation : cancellations) {
            try {
                ReplanificationTask task = replanificationService.triggerReplanification(
                    cancellation,
                    lastSolution,
                    allOrders,
                    availableFlights,
                    airports,
                    currentTime
                );
                
                if (task != null) {
                    int affectedCount = task.getAffectedOrderIds() != null ? 
                        task.getAffectedOrderIds().size() : 0;
                    System.out.println("      ✅ Replanification task " + task.getId() + " - " + 
                                     task.getStatus() + " (" + affectedCount + " affected orders)");
                } else {
                    System.out.println("      ⚠️ No replanification needed (no affected orders)");
                }
            } catch (Exception e) {
                System.err.println("      ❌ Replanification failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Get the current simulation time.
     * Used by external services to synchronize with simulation progress.
     * 
     * @return Current simulation time
     */
    public LocalDateTime getCurrentSimulationTime() {
        return currentTime;
    }
    
    // ========== METRICS METHODS ==========
    
    /**
     * Print comprehensive final metrics at the end of the simulation.
     * Analyzes all shipments across all iterations to calculate:
     * - Fully completed orders (100% assigned)
     * - Partially completed orders (1-99% assigned)
     * - Not completed orders (0% assigned)
     * - Product assignment rate
     * - Success rate
     */
    private void printFinalMetrics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("=== FINAL SIMULATION METRICS ===");
        System.out.println("=".repeat(80));
        
        // Categorize orders using accumulated metrics
        int fullyCompleted = 0;
        int partiallyCompleted = 0;
        int notCompleted = 0;
        long totalProductsRequested = 0;
        long totalProductsAssigned = 0;
        
        // Analyze all orders that entered the simulation
        int overAssignedCount = 0;
        long overAssignedProducts = 0;
        
        for (PlannerOrder order : allProcessedOrdersMap.values()) {
            int orderId = order.getId();
            int requested = order.getTotalQuantity();
            int assigned = totalAssignedPerOrder.getOrDefault(orderId, 0);
            
            totalProductsRequested += requested;
            
            // ✅ IMPLEMENTED: Cap assigned products to what was actually requested
            // Some orders may be over-assigned due to algorithm behavior
            if (assigned > requested) {
                overAssignedCount++;
                overAssignedProducts += (assigned - requested);
                totalProductsAssigned += requested; // Only count what was requested
            } else {
                totalProductsAssigned += assigned;
            }
            
            if (assigned == 0) {
                notCompleted++;
            } else if (assigned >= requested) {
                fullyCompleted++;
            } else {
                partiallyCompleted++;
            }
        }
        
        int totalOrders = allProcessedOrdersMap.size();
        
        // Calculate rates
        double completionRate = totalOrders > 0 ? (fullyCompleted * 100.0 / totalOrders) : 0.0;
        double partialRate = totalOrders > 0 ? (partiallyCompleted * 100.0 / totalOrders) : 0.0;
        double noAssignmentRate = totalOrders > 0 ? (notCompleted * 100.0 / totalOrders) : 0.0;
        double productAssignmentRate = totalProductsRequested > 0 
            ? (totalProductsAssigned * 100.0 / totalProductsRequested) : 0.0;
        
        // Print metrics
        System.out.println("\n📊 SIMULATION OVERVIEW:");
        System.out.println("   Scenario: " + scenario.getType());
        System.out.println("   K value: " + scenario.getK());
        System.out.println("   Sc (time window): " + scenario.getScMinutes() + " minutes");
        System.out.println("   Total iterations: " + iterationCount);
        System.out.println("   Simulation period: " + startTime + " → " + endTime);
        
        System.out.println("\n📦 ORDER COMPLETION STATUS:");
        System.out.println("   Total orders processed: " + totalOrders);
        System.out.println("   ├─ ✅ Fully completed: " + fullyCompleted + 
                         String.format(" (%.1f%%)", completionRate));
        System.out.println("   ├─ ⚠️  Partially completed: " + partiallyCompleted + 
                         String.format(" (%.1f%%)", partialRate));
        System.out.println("   └─ ❌ Not completed: " + notCompleted + 
                         String.format(" (%.1f%%)", noAssignmentRate));
        
        System.out.println("\n📈 PRODUCT ASSIGNMENT:");
        System.out.println("   Total products requested: " + String.format("%,d", totalProductsRequested));
        System.out.println("   Total products assigned: " + String.format("%,d", totalProductsAssigned));
        System.out.println("   Assignment rate: " + String.format("%.1f%%", productAssignmentRate));
        
        System.out.println("\n📦 SHIPMENTS GENERATED:");
        System.out.println("   Total shipments: " + totalShipmentsCreated);
        System.out.println("   Unique orders with shipments: " + totalAssignedPerOrder.size());
        
        // Report over-assignments if detected
        if (overAssignedCount > 0) {
            System.out.println("\nOVER-ASSIGNMENT DETECTED:");
            System.out.println("   Orders over-assigned: " + overAssignedCount);
            System.out.println("   Excess products: " + String.format("%,d", overAssignedProducts));
            System.out.println("   (This may indicate duplicate shipments or algorithm issues)");
        }
        
        System.out.println("\nPENDING ORDERS (at end of simulation):");
        System.out.println("   Still pending: " + pendingOrders.size());
        if (!pendingOrders.isEmpty()) {
            System.out.println("   (These orders likely have expired deadlines)");
        }
        
        // Success indicators
        System.out.println("\nSUCCESS INDICATORS:");
        if (completionRate >= 80.0) {
            System.out.println("  EXCELLENT: High completion rate (" + String.format("%.1f%%", completionRate) + ")");
        } else if (completionRate >= 50.0) {
            System.out.println("   MODERATE: Fair completion rate (" + String.format("%.1f%%", completionRate) + ")");
        } else if (completionRate >= 20.0) {
            System.out.println("   LOW: Poor completion rate (" + String.format("%.1f%%", completionRate) + ")");
        } else {
            System.out.println("  CRITICAL: Very low completion rate (" + String.format("%.1f%%", completionRate) + ")");
        }
        
        if (productAssignmentRate >= 80.0) {
            System.out.println("   EXCELLENT: High product assignment (" + String.format("%.1f%%", productAssignmentRate) + ")");
        } else if (productAssignmentRate >= 50.0) {
            System.out.println("   MODERATE: Fair product assignment (" + String.format("%.1f%%", productAssignmentRate) + ")");
        } else {
            System.out.println("   CRITICAL: Low product assignment (" + String.format("%.1f%%", productAssignmentRate) + ")");
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("=== END OF SIMULATION METRICS ===");
        System.out.println("=".repeat(80) + "\n");
    }
    
    // ========== CONTROL METHODS ==========
    
    public void pause() {
        if (state.get() == SimulationState.RUNNING) {
            pauseRequested.set(true);
            System.out.println("[SimulationSession] " + sessionId + " pause requested");
        }
    }
    
    public void resume() {
        if (state.get() == SimulationState.PAUSED) {
            pauseRequested.set(false);
            System.out.println("[SimulationSession] " + sessionId + " resume requested");
        }
    }
    
    public void stop() {
        stopRequested.set(true);
        pauseRequested.set(false);  // Unpause if paused
        state.set(SimulationState.STOPPED);
        sendStatusUpdate(new SimulationStatusUpdate(SimulationState.STOPPED, "Simulation stopped by user"));
        System.out.println("[SimulationSession] " + sessionId + " stop requested");
    }
    
    public void setSpeed(double multiplier) {
        // Validate speed multiplier
        if (multiplier < 0.1 || multiplier > 20.0) {
            System.err.println("[SimulationSession] Invalid speed multiplier: " + multiplier + " (must be 0.1-20.0)");
            return;
        }
        
        this.speedMultiplier = multiplier;
        System.out.println("[SimulationSession] " + sessionId + " speed changed to " + multiplier + "x");
        
        // Send update
        SimulationStatusUpdate update = new SimulationStatusUpdate(
            state.get(),
            "Speed changed to " + multiplier + "x"
        );
        update.setCurrentSpeed(multiplier);
        update.setCurrentIteration(iterationCount);
        update.setTotalIterations(totalExpectedIterations);
        sendStatusUpdate(update);
    }
    
    // ========== GETTERS ==========
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public SimulationState getState() {
        return state.get();
    }
    
    public int getIterationCount() {
        return iterationCount;
    }
    
    public LocalDateTime getCurrentTime() {
        return currentTime;
    }
    
    public List<TabuSimulationResponse> getAllResults() {
        return new ArrayList<>(allResults);
    }
    
    public boolean isRunning() {
        SimulationState currentState = state.get();
        return currentState == SimulationState.RUNNING || 
               currentState == SimulationState.PAUSED;
    }
    
    // ========== ORDER TRACKING METHODS ==========
    
    /**
     * Build order summaries for real-time tracking.
     * Shows current state of ALL orders processed so far.
     * Orders are sorted by request time (most recent first).
     */
    private pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO[] buildOrderSummaries() {
        java.util.List<pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO> summaries = new java.util.ArrayList<>();
        
        // Sort orders by request time (most recent first) - NO LIMIT, all orders
        java.util.List<PlannerOrder> sortedOrders = allProcessedOrdersMap.values().stream()
            .sorted(java.util.Comparator.comparing(PlannerOrder::getOrderTime).reversed())
            .collect(java.util.stream.Collectors.toList());
        
        for (PlannerOrder order : sortedOrders) {
            pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO dto = 
                new pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO();
            
            dto.id = order.getId();
            dto.code = "PED-" + order.getId();

            // Origin/Destination
            dto.originCode = order.getOrigin().getCode();
            dto.originName = order.getOrigin().getName();
            dto.destinationCode = order.getDestination().getCode();
            dto.destinationName = order.getDestination().getName();

            // Quantities
            dto.totalQuantity = order.getTotalQuantity();
            dto.assignedQuantity = totalAssignedPerOrder.getOrDefault(order.getId(), 0);
            dto.progressPercent = (dto.assignedQuantity * 100.0) / dto.totalQuantity;

            // Status
            dto.status = calculateOrderStatus(order, dto.assignedQuantity);

            // Times
            dto.requestDateISO = order.getOrderTime().toString();
            dto.etaISO = null; // Could estimate based on assigned flights

            // Assigned flights (search in latest results)
            dto.assignedFlights = findAssignedFlights(order.getId());
            
            summaries.add(dto);
        }
        
        return summaries.toArray(new pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO[0]);
    }
    
    /**
     * Calculate order status based on assignment and delivery time.
     * COMPLETED only when the flight has actually arrived at destination.
     * Once COMPLETED, the status is permanent (doesn't revert).
     */
    private pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus calculateOrderStatus(
            PlannerOrder order, int assigned) {
        
        // 🔒 Once completed, always completed (immutable state)
        if (completedOrderIds.contains(order.getId())) {
            return pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.COMPLETED;
        }
        
        if (assigned == 0) {
            // Check if still pending or truly unassigned
            return pendingOrders.contains(order) ? 
                pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.PENDING :
                pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.UNASSIGNED;
        }
        
        // Check if fully assigned
        if (assigned >= order.getTotalQuantity()) {
            // Verify if all shipments for this order have been delivered
            // by checking if their last flight has arrived
            java.time.LocalDateTime lastArrivalTime = getOrderLastArrivalTime(order.getId());
            
            // DEBUG: Log status determination
            if (lastArrivalTime != null) {
                boolean hasArrived = !currentTime.isBefore(lastArrivalTime);
                if (hasArrived) {
                    System.out.println("  ✅ Order " + order.getId() + " marked as COMPLETED (arrived at " + lastArrivalTime + ")");
                    completedOrderIds.add(order.getId()); // 🔒 Lock in completed state
                    return pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.COMPLETED;
                } else {
                    return pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.IN_TRANSIT;
                }
            } else {
                // ⚠️ Fully assigned but no arrival time found - itinerary missing in results
                System.out.println("  ⚠️ Order " + order.getId() + " fully assigned but no arrival time found (itinerary missing?)");
                return pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.IN_TRANSIT;
            }
        }
        
        return pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus.IN_TRANSIT;
    }
    
    /**
     * Get the arrival time of the last flight for an order.
     * Searches through ALL previous iteration results (not just the last one).
     * Returns null if the order has no assigned flights.
     */
    private java.time.LocalDateTime getOrderLastArrivalTime(int orderId) {
        if (allResults.isEmpty()) return null;
        
        java.time.LocalDateTime maxArrival = null;
        
        // 🔍 Search through ALL results (orders may be in older iterations)
        for (pe.edu.pucp.morapack.dto.simulation.TabuSimulationResponse result : allResults) {
            if (result.itineraries == null) continue;
            
            // Find itineraries for this order
            for (var itinerario : result.itineraries) {
                // ✅ Use orderId field directly (no parsing needed)
                if (itinerario.orderId == orderId && itinerario.segments != null && itinerario.segments.length > 0) {
                    try {
                        // Get the last segment's arrival time
                        var lastSegment = itinerario.segments[itinerario.segments.length - 1];
                        
                        if (lastSegment != null && lastSegment.flight != null && lastSegment.flight.scheduledArrivalISO != null) {
                            java.time.LocalDateTime arrival = java.time.LocalDateTime.parse(lastSegment.flight.scheduledArrivalISO);
                            if (maxArrival == null || arrival.isAfter(maxArrival)) {
                                maxArrival = arrival;
                            }
                        }
                    } catch (Exception e) {
                        // Skip parsing errors
                    }
                }
            }
        }
        
        return maxArrival;
    }
    
    /**
     * Find flight codes assigned to a specific order.
     * Searches through the most recent iteration results.
     */
    private java.util.List<String> findAssignedFlights(int orderId) {
        java.util.List<String> flightCodes = new java.util.ArrayList<>();
        
        // Search in the last result (most recent iteration)
        if (!allResults.isEmpty()) {
            pe.edu.pucp.morapack.dto.simulation.TabuSimulationResponse lastResult = 
                allResults.get(allResults.size() - 1);
            
            if (lastResult.itineraries != null) {
                for (var itinerario : lastResult.itineraries) {
                    // ✅ Check if this itinerary belongs to our order using orderId field
                    if (itinerario.orderId == orderId && itinerario.segments != null) {
                        // Extract segments and get flight codes
                        for (var segmento : itinerario.segments) {
                            if (segmento.flight != null && segmento.flight.code != null) {
                                flightCodes.add(segmento.flight.code);
                            }
                        }
                    }
                }
            }
        }
        
        return flightCodes;
    }
    
    /**
     * Calculate aggregate metrics about all orders.
     */
    private pe.edu.pucp.morapack.dto.simulation.OrderMetricsDTO calculateOrderMetrics() {
        pe.edu.pucp.morapack.dto.simulation.OrderMetricsDTO metrics = 
            new pe.edu.pucp.morapack.dto.simulation.OrderMetricsDTO();
        
        metrics.totalPedidos = allProcessedOrdersMap.size();
        metrics.pendientes = 0;
        metrics.enTransito = 0;
        metrics.completados = 0;
        metrics.sinAsignar = 0;
        metrics.totalProductos = 0;
        metrics.productosAsignados = 0;
        
        for (PlannerOrder order : allProcessedOrdersMap.values()) {
            int requested = order.getTotalQuantity();
            int assigned = totalAssignedPerOrder.getOrDefault(order.getId(), 0);
            
            metrics.totalProductos += requested;
            metrics.productosAsignados += assigned;
            
            pe.edu.pucp.morapack.dto.simulation.OrderSummaryDTO.OrderStatus status = 
                calculateOrderStatus(order, assigned);
            
            switch (status) {
                case PENDING -> metrics.pendientes++;
                case IN_TRANSIT -> metrics.enTransito++;
                case COMPLETED -> metrics.completados++;
                case UNASSIGNED -> metrics.sinAsignar++;
            }
        }
        
        metrics.tasaAsignacionPercent = metrics.totalProductos > 0 ?
            (metrics.productosAsignados * 100.0) / metrics.totalProductos : 0.0;
        
        return metrics;
    }
}

