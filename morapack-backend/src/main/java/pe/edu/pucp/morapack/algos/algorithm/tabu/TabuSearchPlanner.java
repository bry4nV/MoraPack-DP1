package pe.edu.pucp.morapack.algos.algorithm.tabu;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlannerCostFunction;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution.TimeRange;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.model.AirportStorageManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class TabuSearchPlanner implements IOptimizer {
    // Main hubs
    private static final String LIMA_CODE = "SPIM";
    private static final String BRUSSELS_CODE = "EBCI";
    private static final String BAKU_CODE = "UBBB";

    // Time constraints
    private static final int SAME_CONTINENT_MAX_HOURS = 48;  // 2 days
    private static final int DIFF_CONTINENT_MAX_HOURS = 72;  // 3 days
    private static final int SAME_CONTINENT_FLIGHT_HOURS = 12;  // 0.5 days
    private static final int DIFF_CONTINENT_FLIGHT_HOURS = 24;  // 1 day

    // Capacity thresholds
    private static final int MIN_SAME_CONTINENT_CAPACITY = 200;
    private static final int MAX_SAME_CONTINENT_CAPACITY = 300;
    private static final int MIN_DIFF_CONTINENT_CAPACITY = 250;
    private static final int MAX_DIFF_CONTINENT_CAPACITY = 400;
    
    // Storage capacities
    private static final int MIN_STORAGE_CAPACITY = 600;
    private static final int MAX_STORAGE_CAPACITY = 1000;

    // Algorithm configuration
    private final TabuSearchConfig config;
    private final TabuSearchConstraints constraints;
    private final int maxIterations;
    private final int maxIterationsWithoutImprovement;
    private static final double UTILIZATION_TARGET = 0.90; // Increased target load factor
    private static final double MIN_SIGNIFICANT_IMPROVEMENT = 0.003; // 0.3% minimum improvement per iteration
    private static final double MIN_AVG_IMPROVEMENT = 0.0005; // 0.05% minimum average improvement to continue
    private static final int IMPROVEMENT_WINDOW = 15; // Larger window for average improvement calculation
    
    // EXPERIMENTAL: Store average delivery time for analysis
    private double averageDeliveryTimeMinutes = 0.0;
    
    // Airport storage management
    private AirportStorageManager storageManager;

    public TabuSearchPlanner() {
        this(20, 10);  // EXPERIMENT: 20 iterations for timing measurement
    }
    
    public TabuSearchPlanner(int maxIter, int maxWithoutImprovement) {
        this.maxIterations = maxIter;
        this.maxIterationsWithoutImprovement = maxWithoutImprovement;
        this.config = new TabuSearchConfig();
        this.constraints = new TabuSearchConstraints(this.config);
    }
    
    // EXPERIMENTAL: Getter for average delivery time (Factor 2)
    public double getAverageDeliveryTimeMinutes() {
        return averageDeliveryTimeMinutes;
    }

    @Override
    public Solution optimize(List<Order> orders, List<Flight> flights, List<Airport> airports) {
        if (orders == null || orders.isEmpty()) return new TabuSolution();
        
        long startTime = System.currentTimeMillis();
        System.out.println("\n=== Starting Tabu Search Optimization ===");
        System.out.println("Orders to process: " + orders.size());
        System.out.println("Available flights: " + flights.size());
        
        // Generate initial solution with dynamic product assignment
        TabuSolution initialSolution = (TabuSolution) generateInitialSolutionDynamic(orders, flights, airports);
        System.out.println("Dynamic assignment completed");
        System.out.println("\nInitial solution summary:");
        List<PlannerRoute> assignedRoutes = initialSolution.getAssignedRoutes();
        List<PlannerRoute> emptyRoutes = initialSolution.getEmptyRoutes();
        
        System.out.println("Total routes: " + initialSolution.getRoutes().size());
        System.out.println("Assigned routes: " + assignedRoutes.size());
        System.out.println("Empty routes: " + emptyRoutes.size());
            
        // Create initial Tabu solution and debug its state
        TabuSolution currentSolution = new TabuSolution(initialSolution);
        currentSolution.setAllOrders(orders);
        
        System.out.println("\n=== Verifying Tabu Solution State ===");
        List<PlannerRoute> routes = currentSolution.getRoutes();
        assignedRoutes = currentSolution.getAssignedRoutes();
        emptyRoutes = currentSolution.getEmptyRoutes();
        
        System.out.println("Routes in TabuSolution: " + routes.size());
        System.out.println("Assigned routes in TabuSolution: " + assignedRoutes.size());
        System.out.println("Empty routes in TabuSolution: " + emptyRoutes.size());
        
        TabuSolution[] bestSolution = {new TabuSolution(currentSolution)};
        Queue<TabuMove> tabuList = new LinkedList<>();
        int iterationsWithoutImprovement = 0;
        double bestGlobalCost = Double.POSITIVE_INFINITY;
        
        // Initialize Adaptive Memory System
        TabuAdaptiveMemory adaptiveMemory = new TabuAdaptiveMemory(15); // Base tenure of 15
        
        // Dynamic tabu list parameters with adaptive size
        int minTabuSize = 5;
        int maxTabuSize = 30;
        int baseTabuSize = 15;
        double tabuSizeIncreaseFactor = 1.5;
        int currentTabuSize = baseTabuSize;
        double intensificationThreshold = 0.03;
        int stagnationThreshold = 10;
        
        System.out.println("\n=== Starting Tabu Search ===");
        System.out.println("EXPERIMENT: Running with " + maxIterations + " iterations");
        
        // ⏱️ START TIMING MEASUREMENT
        long tabuStartTime = System.nanoTime();
        
        // Array to store iteration times in milliseconds
        double[] iterationTimes = new double[maxIterations];
        
        // Create handler for cancellations
        TabuSearchCancellationHandler cancellationHandler = new TabuSearchCancellationHandler(constraints);

        // Filter out cancelled flights
        List<Flight> activeFlights = flights.stream()
            .filter(f -> f.getStatus() != Flight.Status.CANCELLED)
            .toList();

        for (int i = 0; i < maxIterations; i++) {
            // ⏱️ START ITERATION TIMING
            long iterationStartTime = System.nanoTime();
            
            List<TabuSolution> neighborhood = generateNeighborhood(currentSolution, activeFlights).stream()
                .map(s -> {
                    TabuSolution ts = new TabuSolution(s);
                    ts.setAllOrders(orders);
                    return ts;
                })
                .collect(Collectors.toList());
            
            // Solo retornar si no hay vecinos en la primera iteración Y no se encontró ninguna solución
            if(neighborhood.isEmpty() && i == 0) {
                System.out.println("\nNo se pudieron encontrar vecinos en la primera iteración.");
                List<PlannerRoute> currentAssigned = currentSolution.getAssignedRoutes();
                System.out.println("Rutas asignadas: " + currentAssigned.size());
                // Count total shipments from current solution
                int totalShipments = currentAssigned.stream().mapToInt(r -> r.getShipments().size()).sum();
                System.out.println("Total envíos en solución: " + totalShipments);
                
                // Si al menos encontramos algunas rutas, continuemos con la búsqueda
                if (!currentAssigned.isEmpty()) {
                    System.out.println("Continuando con búsqueda tabú usando solución parcial...\n");
                    neighborhood = new ArrayList<>();  // Lista vacía pero continuamos
                } else {
                    return currentSolution;  // Solo retornamos si realmente no hay nada que hacer
                }
            }

            TabuSolution bestIterationNeighbor = null;
            double bestNeighborCost = Double.POSITIVE_INFINITY;
            double currentCostForComparison = TabuSearchPlannerCostFunction.calculateCost(currentSolution, flights, airports, i, maxIterations);

            // Update best global cost if needed
            if (currentCostForComparison < bestGlobalCost) {
                bestGlobalCost = currentCostForComparison;
                // Dynamic tabu list size adjustment
                if ((bestGlobalCost - currentCostForComparison) / bestGlobalCost > intensificationThreshold) {
                    currentTabuSize = Math.min(currentTabuSize + 1, maxTabuSize); // Intensify search
                } else {
                    currentTabuSize = Math.max(currentTabuSize - 1, minTabuSize); // Diversify search
                }
            }

            for (TabuSolution neighbor : neighborhood) {
                TabuMove move = deduceMove(currentSolution, neighbor);
                double neighborCost = TabuSearchPlannerCostFunction.calculateCost(neighbor, flights, airports, i, maxIterations);
                
                // Apply frequency penalty from adaptive memory
                if (move != null) {
                    String moveSignature = generateMoveSignature(move);
                    double frequencyPenalty = adaptiveMemory.getMoveFrequencyPenalty(moveSignature);
                    neighborCost *= frequencyPenalty; // Penalize frequently used moves
                }
                
                boolean isTabu = tabuList.contains(move);
                double bestCurrentCost = TabuSearchPlannerCostFunction.calculateCost(bestSolution[0], flights, airports, i, maxIterations);
                
                // Enhanced aspiration criteria with more aggressive conditions
                double stagnationFactor = 1.0 + (iterationsWithoutImprovement * 0.01); // Increases acceptance threshold as we stagnate
                boolean hasMoreCompletedOrders = neighbor.getCompletedOrders().size() > currentSolution.getCompletedOrders().size();
                boolean hasMoreAssignedRoutes = neighbor.getAssignedRoutes().size() > currentSolution.getAssignedRoutes().size();
                boolean significantImprovement = currentCostForComparison > 0 && 
                                               ((currentCostForComparison - neighborCost) / currentCostForComparison) > intensificationThreshold;
                
                boolean meetsAspiration = neighborCost < bestCurrentCost || // Better than best solution
                                        (neighborCost < currentCostForComparison * stagnationFactor && // Allow worse moves with dynamic threshold
                                         (hasMoreCompletedOrders || // Prioritize completing orders
                                          hasMoreAssignedRoutes || // Prioritize using more routes
                                          significantImprovement)) || // Accept significant improvements
                                        (iterationsWithoutImprovement > 50 && // When stuck for a while
                                         (hasMoreCompletedOrders || Math.random() < 0.2)); // Be more aggressive
                
                if (!isTabu || meetsAspiration) {
                    if (neighborCost < bestNeighborCost) {
                        bestNeighborCost = neighborCost;
                        bestIterationNeighbor = neighbor;
                    }
                }
            }

            if (bestIterationNeighbor != null) {
                currentSolution = bestIterationNeighbor;
                TabuMove movePerformed = deduceMove(currentSolution, bestIterationNeighbor);
                if(movePerformed != null) {
                    // Record move in adaptive memory
                    String moveSignature = generateMoveSignature(movePerformed);
                    adaptiveMemory.recordMove(moveSignature);
                    
                    tabuList.add(movePerformed);
                    
                    // Adaptive tabu list management using adaptive memory
                    double improvementPercentage = (currentCostForComparison - bestNeighborCost) / currentCostForComparison * 100;
                    adaptiveMemory.recordImprovement(improvementPercentage, bestNeighborCost);
                    
                    // Update tabu tenure based on adaptive memory
                    currentTabuSize = adaptiveMemory.adaptTabuTenure();
                    
                    // Maintain tabu list size
                    while (tabuList.size() > currentTabuSize) {
                        tabuList.poll();
                    }
                }
                double improvementPercentage = (currentCostForComparison - bestNeighborCost) / currentCostForComparison * 100;
                System.out.printf("Found better solution with cost: %.2f (Improvement: %.2f%%, %s)\n", 
                                bestNeighborCost, improvementPercentage, adaptiveMemory.getPhaseInfo());
            } else {
                System.out.println("No improvement found in this iteration - Adjusting tabu strategy...");
                
                // Adjust tabu list size based on search state with adaptive memory
                if (adaptiveMemory.shouldDiversify()) {
                    // Strong diversification triggered by adaptive memory
                    currentTabuSize = minTabuSize;
                    System.out.println("Adaptive memory triggered diversification - Reducing tabu size to minimum");
                } else if (iterationsWithoutImprovement > stagnationThreshold) {
                    // Long stagnation - aggressive diversification
                    currentTabuSize = minTabuSize;
                    System.out.println("Aggressive diversification - Reducing tabu size to minimum");
                } else {
                    // Short-term stagnation - moderate adjustment
                    currentTabuSize = Math.max((int)(currentTabuSize / tabuSizeIncreaseFactor), minTabuSize);
                    System.out.printf("Moderate diversification - Adjusted tabu size to %d\n", currentTabuSize);
                }
                
                while (tabuList.size() > currentTabuSize) {
                    tabuList.poll();
                }
            }

            double currentCost = TabuSearchPlannerCostFunction.calculateCost(currentSolution, flights, airports, i, maxIterations);
            double bestCost = TabuSearchPlannerCostFunction.calculateCost(bestSolution[0], flights, airports, i, maxIterations);

            // Calcular la mejora relativa
            double relativeImprovement = Math.abs((bestCost - currentCost) / bestCost);
            double minSignificantImprovement = 0.001; // 0.1% de mejora mínima

            if (currentCost < bestCost && relativeImprovement > minSignificantImprovement) {
                bestSolution[0] = new TabuSolution(currentSolution);
                bestSolution[0].setAllOrders(orders);
                iterationsWithoutImprovement = 0;
                System.out.println("New best global solution found!");
                System.out.println("Current cost: " + currentCost);
                System.out.println("Previous best: " + bestCost);
                System.out.println("Improvement: " + (relativeImprovement * 100) + "%");
            } else {
                iterationsWithoutImprovement++;
                System.out.println("Iterations without significant improvement: " + iterationsWithoutImprovement);
                if (relativeImprovement <= minSignificantImprovement && currentCost < bestCost) {
                    System.out.println("Minimal improvement detected: " + (relativeImprovement * 100) + "% - Not significant enough");
                }
            }

            // Criterios de parada:
            // 1. Demasiadas iteraciones sin mejora significativa
            // 2. Se alcanzó un nivel de mejora muy pequeño en las últimas N iteraciones
            if (iterationsWithoutImprovement >= maxIterationsWithoutImprovement) {
                // Calculamos la mejora promedio de las últimas 10 iteraciones
                double avgImprovement = 0;
                if (i >= 10) {
                    double costBeforeTenIter = TabuSearchPlannerCostFunction.calculateCost(bestSolution[0], flights, airports, i-10, maxIterations);
                    avgImprovement = Math.abs((bestCost - costBeforeTenIter) / costBeforeTenIter) / 10;
                    
                    if (avgImprovement < 0.0001) { // Si la mejora promedio es menor a 0.01%
                        System.out.println("\nDetecting convergence - Average improvement per iteration: " + 
                                        String.format("%.6f%%", avgImprovement * 100));
                        System.out.println("Stopping search as improvements are minimal");
                        break;
                    }
                }
                
                System.out.println("Diversifying after " + iterationsWithoutImprovement + 
                                 " iterations without improvement (Avg improvement: " + 
                                 String.format("%.6f%%", avgImprovement * 100) + " per iter)");
                // Extract shipments from current best solution
                List<Shipment> currentShipments = extractShipmentsFromSolution(bestSolution[0]);
                TabuSolution diversified = new TabuSolution(diversify(bestSolution[0], currentShipments, flights, airports));
                diversified.setAllOrders(orders);
                currentSolution = diversified;
                iterationsWithoutImprovement = 0;
                tabuList.clear();
            }
            
            // Verificar si hay vuelos cancelados y replanificar si es necesario
            boolean hasCancellations = currentSolution.getRoutes().stream()
                .flatMap(r -> r.getSegments().stream())
                .anyMatch(s -> s.getFlight().getStatus() == Flight.Status.CANCELLED);
            
            if (hasCancellations) {
                System.out.println("Detected cancelled flights, replanning...");
                Solution handledSol = cancellationHandler.handleCancellation(null, currentSolution, activeFlights);
                TabuSolution handledTabuSol = new TabuSolution(handledSol);
                handledTabuSol.setAllOrders(orders);
                currentSolution = handledTabuSol;
                
                currentCost = TabuSearchPlannerCostFunction.calculateCost(currentSolution, activeFlights, airports, i, maxIterations);
                bestCost = TabuSearchPlannerCostFunction.calculateCost(bestSolution[0], activeFlights, airports, i, maxIterations);
                if (currentCost < bestCost) {
                    TabuSolution newBestSol = new TabuSolution(currentSolution);
                    newBestSol.setAllOrders(orders);
                    bestSolution[0] = newBestSol;
                    iterationsWithoutImprovement = 0;
                    System.out.println("Found better solution after handling cancellations");
                }
            }
            
            // ⏱️ END ITERATION TIMING - Convert to milliseconds
            long iterationEndTime = System.nanoTime();
            iterationTimes[i] = (iterationEndTime - iterationStartTime) / 1_000_000.0; // Convert to ms
        }

        // ⏱️ END TIMING MEASUREMENT
        long tabuEndTime = System.nanoTime();
        double tabuExecutionTimeSeconds = (tabuEndTime - tabuStartTime) / 1_000_000_000.0;
        
        System.out.println("\n=== EXPERIMENT TIMING RESULTS ===");
        System.out.println("⏱️ TabuSearch Execution Time (20 iterations): " + String.format("%.6f", tabuExecutionTimeSeconds) + " seconds");
        System.out.println("⏱️ Average time per iteration: " + String.format("%.6f", tabuExecutionTimeSeconds / maxIterations) + " seconds");
        
        // Print individual iteration times in milliseconds
        System.out.println("\n📊 ITERATION TIMES (milliseconds):");
        for (int i = 0; i < maxIterations; i++) {
            System.out.printf("Iteration %2d: %8.3f ms%n", (i + 1), iterationTimes[i]);
        }
        
        // Calculate statistics
        double minTime = Double.MAX_VALUE;
        double maxTime = Double.MIN_VALUE;
        double totalTime = 0;
        
        for (double time : iterationTimes) {
            if (time < minTime) minTime = time;
            if (time > maxTime) maxTime = time;
            totalTime += time;
        }
        
        System.out.println("\n📈 STATISTICS:");
        System.out.printf("Min time: %8.3f ms%n", minTime);
        System.out.printf("Max time: %8.3f ms%n", maxTime);
        System.out.printf("Avg time: %8.3f ms%n", totalTime / maxIterations);
        System.out.printf("Total:    %8.3f ms%n", totalTime);
        System.out.println("================================================");

        long endTime = System.currentTimeMillis();
        
        // Print detailed solution statistics
        System.out.println("\n=== Final Solution Statistics ===");
        TabuSearchPlannerCostFunction.printStatistics((TabuSolution)bestSolution[0], flights, airports);
        System.out.println("\nAlgorithm execution time: " + (endTime - startTime) / 1000.0 + " seconds");
        
        // Get statistics for assigned routes and shipments
        List<PlannerRoute> finalAssignedRoutes = bestSolution[0].getAssignedRoutes();
            
        // Find earliest order and latest delivery
        LocalDateTime currentTime = LocalDateTime.now();
        // Extract shipments from best solution for statistics
        List<Shipment> allShipments = extractShipmentsFromSolution((TabuSolution)bestSolution[0]);
        LocalDateTime earliestOrder = allShipments.stream()
            .map(s -> s.getParentOrder().getOrderTime())
            .min(LocalDateTime::compareTo)
            .orElse(currentTime);
            
        LocalDateTime latestDelivery = assignedRoutes.stream()
            .map(PlannerRoute::getFinalArrivalTime)
            .max(LocalDateTime::compareTo)
            .orElse(null);
            
        // Calculate average delivery time for assigned shipments
        double avgDeliveryHours = routes.stream()
            .flatMap(route -> route.getShipments().stream()
                .map(shipment -> new AbstractMap.SimpleEntry<>(shipment, route)))
            .mapToDouble(entry -> {
                LocalDateTime orderTime = entry.getKey().getParentOrder().getOrderTime();
                LocalDateTime deliveryTime = entry.getValue().getFinalArrivalTime();
                return ChronoUnit.MINUTES.between(orderTime, deliveryTime) / 60.0;
            })
            .average()
            .orElse(0.0);
            
        System.out.println("\n=== Optimization Complete ===");
        System.out.println("Algorithm execution time: " + (endTime - startTime) / 1000.0 + " seconds");
        
        System.out.println("\n=== Delivery Time Analysis ===");
        System.out.println("Earliest order time: " + earliestOrder);
        if (latestDelivery != null) {
            System.out.println("Latest delivery time: " + latestDelivery);
            Duration totalTimeSpan = Duration.between(earliestOrder, latestDelivery);
            System.out.println("Total time span: " + 
                String.format("%d days, %d hours, %d minutes",
                    totalTimeSpan.toDays(),
                    totalTimeSpan.toHoursPart(),
                    totalTimeSpan.toMinutesPart()));
            
            // Calculate statistics per order
            Map<Order, List<PlannerRoute>> orderRoutes = routes.stream()
                .flatMap(route -> route.getShipments().stream()
                    .map(shipment -> new AbstractMap.SimpleEntry<>(shipment.getParentOrder(), route)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, 
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
                
            double avgTimePerOrder = orderRoutes.entrySet().stream()
                .mapToDouble(entry -> {
                    Order order = entry.getKey();
                    List<PlannerRoute> orderDeliveries = entry.getValue();
                    LocalDateTime lastDelivery = orderDeliveries.stream()
                        .map(PlannerRoute::getFinalArrivalTime)
                        .max(LocalDateTime::compareTo)
                        .orElse(order.getOrderTime());
                    return ChronoUnit.MINUTES.between(order.getOrderTime(), lastDelivery) / 60.0;
                })
                .average()
                .orElse(0.0);
                
            System.out.println("Average delivery time per shipment: " + String.format("%.2f hours", avgDeliveryHours));
            System.out.println("Average delivery time per order: " + String.format("%.2f hours", avgTimePerOrder));
        }
        
        System.out.println("\n=== Complete Orders Summary ===");
        
        // Agrupar envíos por pedido (using shipments from the best solution)
        Map<Order, List<Shipment>> orderShipments = allShipments.stream()
            .collect(Collectors.groupingBy(Shipment::getParentOrder));
            
        // Contar y mostrar detalles de pedidos completos e incompletos
        int completeOrders = 0;
        int incompleteOrders = 0;
        double totalDeliveryTime = 0;
        
        List<String> completedOrderDetails = new ArrayList<>();
        
        for (Map.Entry<Order, List<Shipment>> entry : orderShipments.entrySet()) {
            Order order = entry.getKey();
            List<Shipment> orderDeliveries = entry.getValue();
            
            boolean allShipmentsAssigned = orderDeliveries.stream()
                .allMatch(s -> bestSolution[0].getRoutes().stream()
                    .anyMatch(r -> r.getShipments().contains(s) && !r.getSegments().isEmpty()));
            
            if (allShipmentsAssigned) {
                completeOrders++;
                StringBuilder details = new StringBuilder();
                details.append(String.format("\nOrder #%d:", order.getId()));
                
                // Información básica del pedido
                int totalQuantity = orderDeliveries.stream()
                    .mapToInt(Shipment::getQuantity)
                    .sum();
                    
                details.append(String.format("\n  Origin: %s", order.getOrigin().getCode()));
                details.append(String.format("\n  Destination: %s", order.getDestination().getCode()));
                details.append(String.format("\n  Total products: %d", totalQuantity));
                details.append(String.format("\n  Number of shipments: %d", orderDeliveries.size()));
                
                // Detalles de tiempos
                LocalDateTime orderTime = order.getOrderTime();
                LocalDateTime lastDelivery = orderDeliveries.stream()
                    .flatMap(s -> bestSolution[0].getRoutes().stream()
                        .filter(r -> r.getShipments().contains(s))
                        .map(PlannerRoute::getFinalArrivalTime))
                    .max(LocalDateTime::compareTo)
                    .orElse(orderTime);
                
                Duration deliveryTime = Duration.between(orderTime, lastDelivery);
                details.append(String.format("\n  Order time: %s", orderTime));
                details.append(String.format("\n  Final delivery: %s", lastDelivery));
                details.append(String.format("\n  Total delivery time: %d hours, %d minutes",
                    deliveryTime.toHours(),
                    deliveryTime.toMinutesPart()));
                
                // Detalles de cada envío
                details.append("\n  Shipment details:");
                for (Shipment shipment : orderDeliveries) {
                    PlannerRoute route = bestSolution[0].getRoutes().stream()
                        .filter(r -> r.getShipments().contains(shipment))
                        .findFirst()
                        .orElse(null);
                    details.append(String.format("\n    - Shipment #%d (%d products):", 
                        shipment.getId(), shipment.getQuantity()));
                    
                    if (route.getSegments().size() == 1) {
                        Flight direct = route.getSegments().get(0).getFlight();
                        details.append(String.format("\n      Direct flight: %s (%s -> %s)", 
                            direct.getCode(),
                            direct.getOrigin().getCode(),
                            direct.getDestination().getCode()));
                    } else {
                        details.append("\n      Connecting flights:");
                        for (PlannerSegment segment : route.getSegments()) {
                            Flight flight = segment.getFlight();
                            details.append(String.format("\n      * %s (%s -> %s)", 
                                flight.getCode(),
                                flight.getOrigin().getCode(),
                                flight.getDestination().getCode()));
                        }
                    }
                    details.append(String.format("\n      Arrival: %s", route.getFinalArrivalTime()));
                }
                
                totalDeliveryTime += deliveryTime.toHours() + (deliveryTime.toMinutesPart() / 60.0);
                completedOrderDetails.add(details.toString());
            } else {
                incompleteOrders++;
            }
        }
        
        // Imprimir resumen general
        System.out.println("\n=== Orders Summary ===");
        System.out.println("Complete orders: " + completeOrders);
        System.out.println("Incomplete orders: " + incompleteOrders);
        System.out.println("Total orders: " + (completeOrders + incompleteOrders));
        if (completeOrders > 0) {
            System.out.printf("Average delivery time per completed order: %.2f hours\n", 
                totalDeliveryTime / completeOrders);
        }
        
        // Imprimir detalles de pedidos completados
        System.out.println("\n=== Completed Orders Details ===");
        completedOrderDetails.forEach(System.out::println);
        
        System.out.println("\nShipment Statistics:");
        int totalRoutes = bestSolution[0].getRoutes().size();
        long finalAssignedCount = finalAssignedRoutes.size();
        long finalEmptyRoutes = bestSolution[0].getEmptyRoutes().size();
        System.out.println("Final solution routes: " + totalRoutes);
        System.out.println("Assigned routes: " + finalAssignedCount);
        System.out.println("Empty routes: " + finalEmptyRoutes);
        
        // EXPERIMENTAL: Calculate average delivery time (Factor 2)
        this.averageDeliveryTimeMinutes = calculateAverageDeliveryTime(orders);
        System.out.println("Average delivery time: " + String.format("%.2f", averageDeliveryTimeMinutes) + " minutes");
        
        // EXPERIMENTAL: Analyze delivery time compliance with restrictions
        analyzeDeliveryTimeCompliance(orders);
        
        return bestSolution[0];
    }
    
    private void processDirectFlights(List<Flight> flights, Order order, Airport origin, Airport destination,
                                    AtomicInteger remainingQuantity, int maxDeliveryHours, int minCapacity,
                                    double utilizationTarget, List<Shipment> shipments, int shipmentIdCounter) {
        List<Flight> directFlights = findDirectFlights(flights, order, origin, destination, minCapacity);
        
        for (Flight flight : directFlights) {
            if (remainingQuantity.get() <= 0) break;
            
            int optimalCapacity = (int)(flight.getCapacity() * utilizationTarget);
            int quantityToShip = Math.min(remainingQuantity.get(), optimalCapacity);
            
            if (quantityToShip >= flight.getCapacity() * 0.3) { // Mínimo 30% de utilización
                Shipment shipment = new Shipment(shipmentIdCounter++, order, quantityToShip, origin, destination);
                shipments.add(shipment);
                remainingQuantity.addAndGet(-quantityToShip);
            }
        }
    }

    private List<Flight> findDirectFlights(List<Flight> flights, Order order, Airport origin, Airport destination, 
                                         int minCapacity) {
        return flights.stream()
            .filter(f -> f.getOrigin().equals(origin) && 
                       f.getDestination().equals(destination) &&
                       f.getCapacity() >= minCapacity &&
                       isValidDepartureTime(f, order))
            .sorted((f1, f2) -> compareFlights(f1, f2, order))
            .limit(3) // Limitar a los 3 mejores vuelos
            .collect(Collectors.toList());
    }

    private boolean isValidDepartureTime(Flight flight, Order order) {
        long hoursUntilDeparture = ChronoUnit.HOURS.between(order.getOrderTime(), flight.getDepartureTime());
        return hoursUntilDeparture >= 0 && hoursUntilDeparture <= order.getMaxDeliveryHours();
    }

    private int compareFlights(Flight f1, Flight f2, Order order) {
        // Comparar por tiempo disponible primero
        long remainingTime1 = order.getMaxDeliveryHours() - 
            ChronoUnit.HOURS.between(order.getOrderTime(), f1.getDepartureTime());
        long remainingTime2 = order.getMaxDeliveryHours() - 
            ChronoUnit.HOURS.between(order.getOrderTime(), f2.getDepartureTime());
        
        if (remainingTime1 != remainingTime2) {
            return Long.compare(remainingTime2, remainingTime1);
        }
        
        // Si tienen el mismo tiempo, preferir mayor capacidad
        return Integer.compare(f2.getCapacity(), f1.getCapacity());
    }

    private void processHubConnection(List<Flight> flights, Order order, Airport origin, Airport destination,
                                    Airport hub, AtomicInteger remainingQuantity, int maxDeliveryHours,
                                    int minCapacity, List<Shipment> shipments, int shipmentIdCounter) {
        List<Flight> firstLeg = findConnectionFlights(flights, origin, hub, minCapacity, order);
        if (firstLeg.isEmpty()) return;
        
        for (Flight f1 : firstLeg) {
            if (remainingQuantity.get() <= 0) break;
            
            List<Flight> secondLeg = findValidConnections(flights, f1, hub, destination, minCapacity, order);
            for (Flight f2 : secondLeg) {
                if (remainingQuantity.get() <= 0) break;
                
                processConnectionPair(f1, f2, order, remainingQuantity, maxDeliveryHours, 
                                   shipments, shipmentIdCounter);
            }
        }
    }

    private List<Flight> findConnectionFlights(List<Flight> flights, Airport from, Airport to, 
                                             int minCapacity, Order order) {
        return flights.stream()
            .filter(f -> f.getOrigin().equals(from) && 
                       f.getDestination().equals(to) &&
                       f.getCapacity() >= minCapacity &&
                       isValidDepartureTime(f, order))
            .sorted((f1, f2) -> compareFlights(f1, f2, order))
            .limit(2)
            .collect(Collectors.toList());
    }

    private List<Flight> findValidConnections(List<Flight> flights, Flight firstFlight, 
                                            Airport connection, Airport destination,
                                            int minCapacity, Order order) {
        return flights.stream()
            .filter(f -> f.getOrigin().equals(connection) && 
                       f.getDestination().equals(destination) &&
                       f.getCapacity() >= minCapacity &&
                       isValidConnection(firstFlight, f))
            .sorted(Comparator.comparingInt(Flight::getCapacity).reversed())
            .limit(2)
            .collect(Collectors.toList());
    }

    private boolean isValidConnection(Flight first, Flight second) {
        long connectionHours = ChronoUnit.HOURS.between(first.getArrivalTime(), second.getDepartureTime());
        return connectionHours >= 2 && connectionHours <= 4;
    }

    private void processConnectionPair(Flight first, Flight second, Order order,
                                     AtomicInteger remainingQuantity, int maxDeliveryHours,
                                     List<Shipment> shipments, int shipmentIdCounter) {
        long connectionHours = ChronoUnit.HOURS.between(first.getArrivalTime(), second.getDepartureTime());
        double connectionEfficiency = 1.0 - (connectionHours - 2.0) / 2.0;
        
        int maxCapacity = (int)(Math.min(first.getCapacity(), second.getCapacity()) * 0.85 * connectionEfficiency);
        int quantityToShip = Math.min(remainingQuantity.get(), maxCapacity);
        
        if (quantityToShip >= Math.min(first.getCapacity(), second.getCapacity()) * 0.4) {
            Shipment shipment = new Shipment(shipmentIdCounter++, order, quantityToShip, 
                                           first.getOrigin(), second.getDestination());
            shipments.add(shipment);
            remainingQuantity.addAndGet(-quantityToShip);
        }
    }

    private Set<Airport> findPotentialStops(List<Flight> flights, Airport origin, Airport destination, 
                                          int quantity) {
        return flights.stream()
            .filter(f -> (f.getOrigin().equals(origin) || f.getDestination().equals(destination)) &&
                       f.getCapacity() >= quantity * 0.4)
            .map(f -> f.getOrigin().equals(origin) ? f.getDestination() : f.getOrigin())
            .filter(a -> !a.equals(origin) && !a.equals(destination))
            .collect(Collectors.toSet());
    }

    private void processConnection(List<Flight> flights, Order order, Airport origin, Airport destination,
                                 Airport stop, AtomicInteger remainingQuantity, int maxDeliveryHours,
                                 int minCapacity, List<Shipment> shipments, int shipmentIdCounter) {
        processHubConnection(flights, order, origin, destination, stop, remainingQuantity,
                           maxDeliveryHours, minCapacity, shipments, shipmentIdCounter);
    }

    private int calculateAverageFlightCapacity(List<Flight> flights, boolean isInterContinental) {
        int minCap = isInterContinental ? MIN_DIFF_CONTINENT_CAPACITY : MIN_SAME_CONTINENT_CAPACITY;
        int maxCap = isInterContinental ? MAX_DIFF_CONTINENT_CAPACITY : MAX_SAME_CONTINENT_CAPACITY;
        return (minCap + maxCap) / 2;
    }
    
    private boolean canUseFlight(Flight flight, Shipment shipment, Map<Flight,Integer> currentLoads) {
        // Check current load plus new shipment against capacity
        int potentialLoad = currentLoads.getOrDefault(flight, 0) + shipment.getQuantity();
        if (potentialLoad > flight.getCapacity()) {
            return false;
        }

        // Flight must depart after order time and arrive within max time limit
        LocalDateTime orderTime = shipment.getParentOrder().getOrderTime();
        boolean isInterContinental = 
            flight.getOrigin().getCountry().getContinent() != 
            flight.getDestination().getCountry().getContinent();
        int maxHours = isInterContinental ? DIFF_CONTINENT_MAX_HOURS : SAME_CONTINENT_MAX_HOURS;
        
        boolean departureValid = !flight.getDepartureTime().isBefore(orderTime);
        boolean arrivalValid = ChronoUnit.HOURS.between(
            orderTime, flight.getArrivalTime().plusHours(2)) <= maxHours;
            
        return departureValid && arrivalValid;
    }

    private TabuSolution generateInitialSolutionDynamic(List<Order> orders, List<Flight> flights, List<Airport> airports) {
        System.out.println("\n=== Generating Initial Solution with Dynamic Assignment ===");
        TabuSolution solution = new TabuSolution();
        Map<Flight, Integer> flightLoads = new HashMap<>();
        
        // Initialize flight loads
        for (Flight flight : flights) {
            flightLoads.put(flight, 0);
        }
        
        // Create pool of pending products per order
        Map<Order, Integer> pendingProducts = new HashMap<>();
        for (Order order : orders) {
            pendingProducts.put(order, order.getTotalQuantity());
        }
        
        // Sort orders by priority (oldest first, then by size, then by deadline)
        List<Order> prioritizedOrders = new ArrayList<>(orders);
        prioritizedOrders.sort((a, b) -> {
            // First by order time (oldest first)
            int orderTimeCompare = a.getOrderTime().compareTo(b.getOrderTime());
            if (orderTimeCompare != 0) return orderTimeCompare;
            
            // Then by size (larger orders first)
            int sizeCompare = Integer.compare(b.getTotalQuantity(), a.getTotalQuantity());
            if (sizeCompare != 0) return sizeCompare;
            
            // Finally by deadline as tiebreaker
            LocalDateTime deadlineA = a.getOrderTime().plusHours(a.getMaxDeliveryHours());
            LocalDateTime deadlineB = b.getOrderTime().plusHours(b.getMaxDeliveryHours());
            return deadlineA.compareTo(deadlineB);
        });
        
        System.out.println("Processing " + prioritizedOrders.size() + " orders in priority order...");
        
        int shipmentIdCounter = 100;
        
        // Process each order until all products are assigned
        for (Order order : prioritizedOrders) {
            while (pendingProducts.get(order) > 0) {
                // Find best available flight for this order
                Flight bestFlight = findBestAvailableFlightForOrder(flights, order, flightLoads);
                
                if (bestFlight == null) {
                    System.out.println("No more available flights for order " + order.getId() + 
                                     " with " + pendingProducts.get(order) + " products remaining");
                    break; // No more flights available for this order
                }
                
                // Calculate how many products to assign dynamically
                int availableCapacity = bestFlight.getCapacity() - flightLoads.get(bestFlight);
                int productsToAssign = Math.min(pendingProducts.get(order), availableCapacity);
                
                // Only create shipment if it's worth it (min 30% utilization)
                if (productsToAssign >= bestFlight.getCapacity() * 0.3 || pendingProducts.get(order) <= productsToAssign) {
                    Shipment newShipment = new Shipment(shipmentIdCounter++, order, productsToAssign, 
                                                       order.getOrigin(), order.getDestination());
                    
                    // Find existing route or create new one
                    PlannerRoute route = findOrCreateRouteForFlight(solution, bestFlight, order);
                    route.addShipment(newShipment);
                    
                    // Update loads and pending products
                    flightLoads.put(bestFlight, flightLoads.get(bestFlight) + productsToAssign);
                    pendingProducts.put(order, pendingProducts.get(order) - productsToAssign);
                    
                    System.out.println("Assigned " + productsToAssign + " products from order " + 
                                     order.getId() + " to flight " + bestFlight.getCode());
                } else {
                    // Can't use this flight efficiently, try multi-stop routes
                    List<List<Flight>> multiStopOptions = generateMultiStopRoutes(flights, 
                        new Shipment(0, order, pendingProducts.get(order), order.getOrigin(), order.getDestination()), 
                        2, 3);
                    
                    if (!multiStopOptions.isEmpty()) {
                        List<Flight> bestRoute = multiStopOptions.get(0);
                        boolean canUseRoute = true;
                        
                        // Check if all flights in route have capacity
                        for (Flight flight : bestRoute) {
                            if (flightLoads.get(flight) + pendingProducts.get(order) > flight.getCapacity()) {
                                canUseRoute = false;
                                break;
                            }
                        }
                        
                        if (canUseRoute) {
                            int remainingForOrder = pendingProducts.get(order);
                            Shipment multiStopShipment = new Shipment(shipmentIdCounter++, order, remainingForOrder,
                                                                    order.getOrigin(), order.getDestination());
                            
                            // Create multi-stop route
                            PlannerRoute multiRoute = new PlannerRoute();
                            for (Flight flight : bestRoute) {
                                multiRoute.addSegment(new PlannerSegment(flight));
                                flightLoads.put(flight, flightLoads.get(flight) + remainingForOrder);
                            }
                            multiRoute.addShipment(multiStopShipment);
                            solution.addRoute(multiRoute);
                            
                            pendingProducts.put(order, 0);
                            System.out.println("Assigned " + remainingForOrder + " products from order " + 
                                             order.getId() + " to multi-stop route with " + bestRoute.size() + " flights");
                        } else {
                            break; // Cannot find suitable route
                        }
                    } else {
                        break; // No multi-stop options available
                    }
                }
            }
        }
        
        // Report results
        int totalAssignedProducts = 0;
        int totalPendingProducts = 0;
        for (Order order : orders) {
            int assigned = order.getTotalQuantity() - pendingProducts.get(order);
            totalAssignedProducts += assigned;
            totalPendingProducts += pendingProducts.get(order);
        }
        
        System.out.println("Dynamic assignment results:");
        System.out.println("- Assigned products: " + totalAssignedProducts);
        System.out.println("- Pending products: " + totalPendingProducts);
        System.out.println("- Assignment rate: " + String.format("%.1f%%", 
                         (totalAssignedProducts * 100.0) / (totalAssignedProducts + totalPendingProducts)));
        
        solution.setCost(TabuSearchPlannerCostFunction.calculateCost(solution, flights, airports, 0, maxIterations));
        return solution;
    }
    
    /**
     * Finds the best available flight for an order considering current loads
     */
    private Flight findBestAvailableFlightForOrder(List<Flight> flights, Order order, Map<Flight, Integer> flightLoads) {
        return flights.stream()
            .filter(f -> isFlightCompatibleWithOrder(f, order))
            .filter(f -> flightLoads.get(f) < f.getCapacity()) // Has available capacity
            .max(Comparator.comparing(f -> calculateFlightScoreForOrder(f, order, flightLoads)))
            .orElse(null);
    }
    
    /**
     * Checks if a flight is compatible with an order (origin, destination, timing)
     */
    private boolean isFlightCompatibleWithOrder(Flight flight, Order order) {
        // Check origin and destination
        if (!flight.getOrigin().equals(order.getOrigin()) || 
            !flight.getDestination().equals(order.getDestination())) {
            return false;
        }
        
        // Check timing constraints
        LocalDateTime orderTime = order.getOrderTime();
        LocalDateTime deadline = orderTime.plusHours(order.getMaxDeliveryHours());
        
        boolean timeValid = !flight.getDepartureTime().isBefore(orderTime) && 
                           !flight.getArrivalTime().isAfter(deadline);
        
        // Debug logging for temporal issues
        if (!timeValid && flight.getDepartureTime().isBefore(orderTime)) {
            System.out.println("DEBUG FLIGHT REJECTION: Order #" + order.getId() + 
                " (OrderTime: " + orderTime + ") rejected flight departing at " + 
                flight.getDepartureTime() + " - Flight departs BEFORE order time!");
        }
        
        return timeValid;
    }
    
    /**
     * Calculates a score for how good a flight is for a specific order
     */
    private double calculateFlightScoreForOrder(Flight flight, Order order, Map<Flight, Integer> flightLoads) {
        double score = 0.0;
        
        // Factor 1: Available capacity utilization (prefer flights that will be well-utilized)
        int currentLoad = flightLoads.get(flight);
        int availableCapacity = flight.getCapacity() - currentLoad;
        double utilizationAfterAssignment = Math.min(1.0, (currentLoad + Math.min(order.getTotalQuantity(), availableCapacity)) / (double)flight.getCapacity());
        double utilizationScore = Math.min(1.0, utilizationAfterAssignment / UTILIZATION_TARGET);
        score += utilizationScore * 0.4; // 40% weight
        
        // Factor 2: Time to deadline (prefer flights that leave more buffer time)
        LocalDateTime deadline = order.getOrderTime().plusHours(order.getMaxDeliveryHours());
        long hoursToDeadline = ChronoUnit.HOURS.between(flight.getArrivalTime(), deadline);
        double timeScore = Math.min(1.0, Math.max(0.0, hoursToDeadline / 48.0)); // normalized to 48 hours
        score += timeScore * 0.3; // 30% weight
        
        // Factor 3: Departure timing (prefer flights that depart soon after order)
        long hoursFromOrder = ChronoUnit.HOURS.between(order.getOrderTime(), flight.getDepartureTime());
        double departureScore = Math.min(1.0, Math.max(0.0, 1.0 - (hoursFromOrder / 24.0))); // normalized to 24 hours
        score += departureScore * 0.3; // 30% weight
        
        return score;
    }
    
    /**
     * Finds an existing route using the flight or creates a new one
     */
    private PlannerRoute findOrCreateRouteForFlight(TabuSolution solution, Flight flight, Order order) {
        // Look for existing route using this flight
        PlannerRoute existingRoute = solution.getRoutes().stream()
            .filter(r -> !r.getSegments().isEmpty() && 
                        r.getSegments().get(0).getFlight().equals(flight))
            .findFirst()
            .orElse(null);
        
        if (existingRoute != null) {
            return existingRoute;
        } else {
            // Create new route
            PlannerRoute newRoute = new PlannerRoute();
            newRoute.addSegment(new PlannerSegment(flight));
            solution.addRoute(newRoute);
            return newRoute;
        }
    }
    
    /**
     * Extracts all shipments from a solution
     */
    private List<Shipment> extractShipmentsFromSolution(TabuSolution solution) {
        return solution.getRoutes().stream()
            .flatMap(route -> route.getShipments().stream())
            .collect(Collectors.toList());
    }

    private TabuSolution generateInitialSolution(List<Shipment> shipments, List<Flight> flights, List<Airport> airports) {
        System.out.println("\n=== Generating Initial Solution ===");
        TabuSolution solution = new TabuSolution();
        Map<Flight, Integer> flightLoads = new HashMap<>();
        
        // Inicializar cargas
        for (Flight flight : flights) {
            flightLoads.put(flight, 0);
        }
        
        // Ordenar envíos por una combinación de antigüedad, tamaño y deadline
        List<Shipment> sortedShipments = new ArrayList<>(shipments);
        sortedShipments.sort((a, b) -> {
            // Primero por tiempo de orden (más antiguos primero)
            LocalDateTime orderTimeA = a.getOrder().getOrderTime();
            LocalDateTime orderTimeB = b.getOrder().getOrderTime();
            int orderTimeCompare = orderTimeA.compareTo(orderTimeB);
            if (orderTimeCompare != 0) return orderTimeCompare;
            
            // Luego por tamaño (envíos más grandes primero)
            int sizeCompare = Integer.compare(b.getQuantity(), a.getQuantity());
            if (sizeCompare != 0) return sizeCompare;
            
            // Finalmente por deadline como desempate
            LocalDateTime deadlineA = orderTimeA.plusHours(a.getOrder().getMaxDeliveryHours());
            LocalDateTime deadlineB = orderTimeB.plusHours(b.getOrder().getMaxDeliveryHours());
            return deadlineA.compareTo(deadlineB);
        });
        
        System.out.println("Processing " + sortedShipments.size() + " shipments in priority order...");
        
        // Procesar cada envío
        for (Shipment shipment : sortedShipments) {
            // Si el envío ya está asignado, continuar
            if (solution.isShipmentAssigned(shipment)) continue;
            
            // Encontrar vuelos compatibles y calcular sus puntajes
            List<Flight> compatibleFlights = flights.stream()
                .filter(f -> isCompatibleFlight(f, shipment))
                .sorted((f1, f2) -> {
                    double score1 = calculateFlightScore(f1, shipment, flightLoads);
                    double score2 = calculateFlightScore(f2, shipment, flightLoads);
                    return Double.compare(score2, score1); // Mayor puntaje primero
                })
                .collect(Collectors.toList());
            
            if (!compatibleFlights.isEmpty()) {
                // Usar el vuelo con mejor puntaje
                Flight bestFlight = compatibleFlights.get(0);
                
                // Buscar una ruta existente que use este vuelo
                PlannerRoute existingRoute = solution.getRoutes().stream()
                    .filter(r -> !r.getSegments().isEmpty() && 
                               r.getSegments().get(0).getFlight().equals(bestFlight))
                    .findFirst()
                    .orElse(null);
                
                PlannerRoute route;
                if (existingRoute != null) {
                    route = existingRoute;
                } else {
                    route = new PlannerRoute();
                    route.addSegment(new PlannerSegment(bestFlight));
                    solution.addRoute(route);
                }
                
                route.addShipment(shipment);
                
                // Actualizar la carga del vuelo
                flightLoads.put(bestFlight, flightLoads.get(bestFlight) + shipment.getQuantity());
            } else {
                // No hay vuelos directos disponibles, intentar rutas multi-escala
                List<List<Flight>> multiStopOptions = generateMultiStopRoutes(flights, shipment, 2, 3); // Max 2 stops, top 3 options
                
                if (!multiStopOptions.isEmpty()) {
                    // Seleccionar la mejor opción (ya están ordenadas por tiempo de llegada)
                    List<Flight> bestMultiStopRoute = multiStopOptions.get(0);
                    
                    // Verificar disponibilidad de capacidad en todos los vuelos
                    boolean canUseRoute = true;
                    for (Flight flight : bestMultiStopRoute) {
                        int currentLoad = flightLoads.getOrDefault(flight, 0);
                        if (currentLoad + shipment.getQuantity() > flight.getCapacity()) {
                            canUseRoute = false;
                            break;
                        }
                    }
                    
                    if (canUseRoute) {
                        PlannerRoute multiStopRoute = new PlannerRoute();
                        for (Flight flight : bestMultiStopRoute) {
                            multiStopRoute.addSegment(new PlannerSegment(flight));
                            // Actualizar cargas de vuelos
                            flightLoads.put(flight, flightLoads.get(flight) + shipment.getQuantity());
                        }
                        multiStopRoute.addShipment(shipment);
                        solution.addRoute(multiStopRoute);
                    }
                }
            }
        }
        
        solution.setCost(TabuSearchPlannerCostFunction.calculateCost(solution, flights, airports, 0, maxIterations));
        return solution;
    }
    
    private boolean isCompatibleFlight(Flight flight, Shipment shipment) {
        // Verificar capacidad
        if (flight.getCapacity() < shipment.getQuantity()) {
            return false;
        }
        
        // Verificar origen y destino
        if (!flight.getOrigin().equals(shipment.getOrigin())) {
            return false;
        }
        
        if (!flight.getDestination().equals(shipment.getDestination())) {
            return false;
        }
        
        // Verificar tiempo de salida vs tiempo de orden
        LocalDateTime orderTime = shipment.getOrder().getOrderTime();
        LocalDateTime deadline = orderTime.plusHours(shipment.getOrder().getMaxDeliveryHours());
        
        // El vuelo debe salir después de la hora de orden
        if (flight.getDepartureTime().isBefore(orderTime)) {
            return false;
        }
        
        // El vuelo debe llegar antes del deadline
        return !flight.getArrivalTime().isAfter(deadline);
    }
    
    private double calculateFlightScore(Flight flight, Shipment shipment, Map<Flight, Integer> flightLoads) {
        double score = 0.0;
        
        // Factor 1: Utilización actual del vuelo (preferir vuelos con carga cercana al objetivo)
        double currentUtilization = flightLoads.get(flight) / (double)flight.getCapacity();
        double newUtilization = (flightLoads.get(flight) + shipment.getQuantity()) / (double)flight.getCapacity();
        double targetDiff = Math.abs(UTILIZATION_TARGET - newUtilization);
        score += (1.0 - targetDiff) * 0.4; // 40% del peso
        
        // Factor 2: Tiempo hasta deadline
        LocalDateTime deadline = shipment.getOrder().getOrderTime().plusHours(shipment.getOrder().getMaxDeliveryHours());
        long hoursUntilDeadline = ChronoUnit.HOURS.between(flight.getArrivalTime(), deadline);
        double timeScore = Math.min(1.0, Math.max(0.0, hoursUntilDeadline / 48.0)); // normalizado a 48 horas
        score += timeScore * 0.3; // 30% del peso
        
        // Factor 3: Tiempo desde orden
        long hoursFromOrder = ChronoUnit.HOURS.between(shipment.getOrder().getOrderTime(), flight.getDepartureTime());
        double orderTimeScore = Math.min(1.0, Math.max(0.0, 1.0 - (hoursFromOrder / 24.0))); // normalizado a 24 horas
        score += orderTimeScore * 0.3; // 30% del peso
        
        return score;
    }

    private List<Shipment> partitionOrdersIntoShipments(List<Order> orders, List<Flight> flights) {
        List<Shipment> shipments = new ArrayList<>();
        int shipmentIdCounter = 100;

        // Identify hub airports
        Set<Airport> hubAirports = flights.stream()
            .map(Flight::getOrigin)
            .filter(a -> a.getCode().equals(LIMA_CODE) || 
                        a.getCode().equals(BRUSSELS_CODE) || 
                        a.getCode().equals(BAKU_CODE))
            .collect(Collectors.toSet());

        // Process each order
        for (Order order : orders) {
            AtomicInteger remainingQuantity = new AtomicInteger(order.getTotalQuantity());
            Airport origin = order.getOrigin();
            Airport destination = order.getDestination();
            boolean isFromHub = hubAirports.contains(origin);
            
            // Determine if this is an intercontinental order
            boolean isInterContinental = origin.getCountry().getContinent() != destination.getCountry().getContinent();
            int maxDeliveryHours = isInterContinental ? DIFF_CONTINENT_MAX_HOURS : SAME_CONTINENT_MAX_HOURS;
            int minCapacity = isInterContinental ? MIN_DIFF_CONTINENT_CAPACITY : MIN_SAME_CONTINENT_CAPACITY;
            
            // First pass: Try direct flights from hubs with higher priority
            if (isFromHub) {
                processDirectFlights(flights, order, origin, destination, remainingQuantity, maxDeliveryHours, 
                                  minCapacity, 0.85, shipments, shipmentIdCounter);
            }
            
            // Second pass: If not from hub or still has remaining, try direct flights with lower threshold
            if (remainingQuantity.get() > 0) {
                processDirectFlights(flights, order, origin, destination, remainingQuantity, maxDeliveryHours, 
                                  minCapacity, 0.70, shipments, shipmentIdCounter);
            }

            // Third pass: Try routes through hubs if origin is not a hub
            if (!isFromHub && remainingQuantity.get() > 0) {
                for (Airport hub : hubAirports) {
                    if (hub.equals(origin) || hub.equals(destination)) continue;
                    
                    processHubConnection(flights, order, origin, destination, hub, remainingQuantity,
                                      maxDeliveryHours, minCapacity, shipments, shipmentIdCounter);
                }
            }

            // Fourth pass: Consider other connections if still needed
            if (remainingQuantity.get() > 0) {
                Set<Airport> potentialStops = findPotentialStops(flights, origin, destination, remainingQuantity.get());
                for (Airport stop : potentialStops) {
                    if (hubAirports.contains(stop)) continue; // Skip hubs already processed
                    processConnection(flights, order, origin, destination, stop, remainingQuantity,
                                   maxDeliveryHours, minCapacity, shipments, shipmentIdCounter);
                }
            }

            // Final pass: Handle remaining quantity with smaller shipments if necessary
            while (remainingQuantity.get() > 0) {
                int avgCapacity = calculateAverageFlightCapacity(flights, isInterContinental);
                int minEfficientSize = (int)(avgCapacity * 0.3); // Minimum 30% of average capacity
                
                int quantityToShip = Math.min(remainingQuantity.get(), avgCapacity);
                if (quantityToShip < minEfficientSize) {
                    // If shipment would be too small, force a final shipment
                    quantityToShip = remainingQuantity.get();
                }
                
                Shipment finalShipment = new Shipment(shipmentIdCounter++, order, quantityToShip, origin, destination);
                shipments.add(finalShipment);
                remainingQuantity.addAndGet(-quantityToShip);
            }
        }
        
        return shipments;
    }
    
    private List<TabuSolution> generateNeighborhood(TabuSolution current, List<Flight> flights) {
        List<TabuSolution> neighbors = new ArrayList<>();
        Random rand = new Random();
        
        // Phase 2A: Diversified neighborhood with intelligent move selection
        final int MAX_NEIGHBORS = 24; // Increased for more diverse exploration
        final int MAX_ROUTES_TO_PROCESS = 10; // More routes for better coverage
        final int MAX_SHIPMENTS_PER_ROUTE = 4; // More shipments per route
        
        // Get assigned routes for exploration
        List<PlannerRoute> assignedRoutes = current.getAssignedRoutes();
        
        // Sample routes to avoid processing all
        if (assignedRoutes.size() > MAX_ROUTES_TO_PROCESS) {
            Collections.shuffle(assignedRoutes);
            assignedRoutes = assignedRoutes.subList(0, MAX_ROUTES_TO_PROCESS);
        }
        
        // Process each route with shipments
        for (PlannerRoute currentRoute : assignedRoutes) {
            if (neighbors.size() >= MAX_NEIGHBORS) break;
            
            List<Shipment> routeShipments = new ArrayList<>(currentRoute.getShipments());
            // Limit shipments processed per route
            if (routeShipments.size() > MAX_SHIPMENTS_PER_ROUTE) {
                Collections.shuffle(routeShipments);
                routeShipments = routeShipments.subList(0, MAX_SHIPMENTS_PER_ROUTE);
            }
            
            for (Shipment shipment : routeShipments) {
                if (neighbors.size() >= MAX_NEIGHBORS) break;
                
                try {
                    // Try direct routes with reduced probability and limited flights
                    if (rand.nextDouble() < 0.4 && neighbors.size() < MAX_NEIGHBORS - 5) { // 40% chance, save space for other moves
                        List<Flight> directFlights = flights.stream()
                            .filter(f -> f.getOrigin().equals(shipment.getOrigin()) && 
                                        f.getDestination().equals(shipment.getDestination()) &&
                                        f.getCapacity() >= shipment.getQuantity())
                            .limit(2) // Only top 2 direct flights
                            .collect(Collectors.toList());

                        for (Flight direct : directFlights) {
                            if (neighbors.size() >= MAX_NEIGHBORS) break;
                            
                            TabuSolution neighbor = new TabuSolution(current);
                            
                            // Remove shipment from current route
                            PlannerRoute oldRoute = neighbor.findRouteForShipment(shipment);
                            if (oldRoute != null) {
                                oldRoute.removeShipment(shipment);
                            }
                            
                            // Create new route with direct flight
                            PlannerRoute newRoute = new PlannerRoute(direct);
                            newRoute.addShipment(shipment);
                            neighbor.addRoute(newRoute);
                            
                            neighbors.add(neighbor);
                        }
                    }

                    // Try multi-stop routes with very reduced probability
                    if (rand.nextDouble() < 0.2 && neighbors.size() < MAX_NEIGHBORS - 3) { // 20% chance, very limited
                        List<List<Flight>> multiStopRoutes = generateMultiStopRoutes(flights, shipment, 2, 1); // Max 2 stops, only 1 route
                        
                        if (!multiStopRoutes.isEmpty()) {
                            List<Flight> routeFlights = multiStopRoutes.get(0);
                            if (isValidMultiStopRoute(routeFlights, shipment)) {
                                TabuSolution neighbor = new TabuSolution(current);
                                
                                // Remove shipment from current route
                                PlannerRoute oldRoute = neighbor.findRouteForShipment(shipment);
                                if (oldRoute != null) {
                                    oldRoute.removeShipment(shipment);
                                }
                                
                                // Create new route with multi-stop flights
                                PlannerRoute newRoute = new PlannerRoute();
                                for (Flight flight : routeFlights) {
                                    newRoute.addSegment(new PlannerSegment(flight));
                                }
                                newRoute.addShipment(shipment);
                                neighbor.addRoute(newRoute);
                                
                                neighbors.add(neighbor);
                            }
                        }
                    }

                    // Try very limited route merging
                    if (rand.nextDouble() < 0.3 && neighbors.size() < MAX_NEIGHBORS - 2) { // 30% chance, very limited
                        // Find only first compatible route
                        PlannerRoute compatibleRoute = current.getAssignedRoutes().stream()
                            .filter(r -> !r.equals(currentRoute) &&
                                       !r.getShipments().isEmpty() &&
                                       r.getSegments().get(0).getFlight().getDestination().equals(shipment.getDestination()))
                            .findFirst()
                            .orElse(null);

                        if (compatibleRoute != null) {
                            TabuSolution neighbor = new TabuSolution(current);
                            
                            // Remove shipment from current route
                            PlannerRoute oldRoute = neighbor.findRouteForShipment(shipment);
                            if (oldRoute != null) {
                                oldRoute.removeShipment(shipment);
                                
                                // Try adding to existing route
                                if (!compatibleRoute.getShipments().isEmpty()) {
                                    PlannerRoute targetRoute = neighbor.findRouteForShipment(compatibleRoute.getShipments().get(0));
                                    if (targetRoute != null && canAddShipmentToRoute(targetRoute, shipment)) {
                                        targetRoute.addShipment(shipment);
                                        neighbors.add(neighbor);
                                    }
                                }
                            }
                        }
                    }

                    // PHASE 2A: New Intelligent Move Types
                    
                    // 1. Shipment Splitting: Split large shipments into smaller ones for better optimization
                    if (rand.nextDouble() < 0.15 && shipment.getQuantity() > 200 && neighbors.size() < MAX_NEIGHBORS - 2) {
                        TabuSolution neighbor = new TabuSolution(current);
                        PlannerRoute oldRoute = neighbor.findRouteForShipment(shipment);
                        if (oldRoute != null) {
                            oldRoute.removeShipment(shipment);
                            
                            // Split into two shipments
                            int half = shipment.getQuantity() / 2;
                            Shipment split1 = new Shipment(shipment.getId() + 1000, shipment.getOrder(), half, 
                                                          shipment.getOrigin(), shipment.getDestination());
                            Shipment split2 = new Shipment(shipment.getId() + 2000, shipment.getOrder(), 
                                                          shipment.getQuantity() - half, shipment.getOrigin(), shipment.getDestination());
                            
                            // Try to place splits in different routes
                            oldRoute.addShipment(split1);
                            
                            // Find alternative route for split2
                            PlannerRoute altRoute = current.getAssignedRoutes().stream()
                                .filter(r -> !r.equals(currentRoute) && canAddShipmentToRoute(r, split2))
                                .findFirst().orElse(null);
                            
                            if (altRoute != null && !altRoute.getShipments().isEmpty()) {
                                PlannerRoute targetRoute = neighbor.findRouteForShipment(altRoute.getShipments().get(0));
                                if (targetRoute != null) {
                                    targetRoute.addShipment(split2);
                                    neighbors.add(neighbor);
                                }
                            }
                        }
                    }
                    
                    // 2. Route Swapping: Swap entire routes between shipments
                    if (rand.nextDouble() < 0.12 && neighbors.size() < MAX_NEIGHBORS - 2) {
                        List<PlannerRoute> otherRoutes = current.getAssignedRoutes().stream()
                            .filter(r -> !r.equals(currentRoute) && !r.getShipments().isEmpty())
                            .limit(2).collect(Collectors.toList());
                            
                        for (PlannerRoute otherRoute : otherRoutes) {
                            if (neighbors.size() >= MAX_NEIGHBORS) break;
                            
                            if (otherRoute.getShipments().isEmpty()) continue;
                            Shipment otherShipment = otherRoute.getShipments().get(0);
                            
                            // Check if swapping makes sense (different destinations)
                            if (!shipment.getDestination().equals(otherShipment.getDestination())) {
                                TabuSolution neighbor = new TabuSolution(current);
                                
                                // Find routes in neighbor
                                PlannerRoute route1 = neighbor.findRouteForShipment(shipment);
                                PlannerRoute route2 = neighbor.findRouteForShipment(otherShipment);
                                
                                if (route1 != null && route2 != null) {
                                    // Swap shipments between routes
                                    route1.removeShipment(shipment);
                                    route2.removeShipment(otherShipment);
                                    route1.addShipment(otherShipment);
                                    route2.addShipment(shipment);
                                    neighbors.add(neighbor);
                                }
                            }
                        }
                    }
                    
                    // 3. Capacity Optimization: Try to fill underutilized routes
                    if (rand.nextDouble() < 0.18 && neighbors.size() < MAX_NEIGHBORS - 1) {
                        // Find underutilized routes (with spare capacity for this shipment)
                        List<PlannerRoute> underutilizedRoutes = current.getAssignedRoutes().stream()
                            .filter(r -> !r.equals(currentRoute) && 
                                   !r.getShipments().isEmpty() &&
                                   canAddShipmentToRoute(r, shipment))
                            .limit(2).collect(Collectors.toList());
                            
                        for (PlannerRoute underRoute : underutilizedRoutes) {
                            if (neighbors.size() >= MAX_NEIGHBORS) break;
                            
                            if (underRoute.getShipments().isEmpty()) continue;
                            
                            TabuSolution neighbor = new TabuSolution(current);
                            PlannerRoute oldRoute = neighbor.findRouteForShipment(shipment);
                            PlannerRoute targetRoute = neighbor.findRouteForShipment(underRoute.getShipments().get(0));
                            
                            if (oldRoute != null && targetRoute != null) {
                                oldRoute.removeShipment(shipment);
                                targetRoute.addShipment(shipment);
                                neighbors.add(neighbor);
                            }
                        }
                    }

                    // 4. Original empty route move (reduced probability)
                    if (rand.nextDouble() < 0.08 && neighbors.size() < MAX_NEIGHBORS - 1) { // Reduced from 10% to 8%
                        TabuSolution neighbor = new TabuSolution(current);
                        PlannerRoute oldRoute = neighbor.findRouteForShipment(shipment);
                        if (oldRoute != null) {
                            oldRoute.removeShipment(shipment);
                        }
                        neighbors.add(neighbor);
                    }
                    
                } catch (OutOfMemoryError e) {
                    System.out.println("Memory limit reached during neighbor generation, stopping at " + neighbors.size() + " neighbors");
                    break;
                }
            }
        }
        
        System.out.println("Generated " + neighbors.size() + " neighbors (memory optimized)");
        return neighbors;
    }
    
    private boolean canAddShipmentToRoute(PlannerRoute route, Shipment shipment) {
        // Check if all flights in route can handle the additional shipment
        int totalQuantity = route.getShipments().stream().mapToInt(Shipment::getQuantity).sum() + shipment.getQuantity();
        return route.getSegments().stream()
            .allMatch(seg -> seg.getFlight().getCapacity() >= totalQuantity);
    }

    private TabuMove deduceMove(TabuSolution base, TabuSolution neighbor) {
        if (base == null || neighbor == null) return null;
        
        // Get all shipments in the neighbor solution
        List<Shipment> allShipments = neighbor.getRoutes().stream()
            .flatMap(r -> r.getShipments().stream())
            .collect(Collectors.toList());
            
        // For each shipment, find if its route changed
        for (Shipment shipment : allShipments) {
            PlannerRoute newRoute = neighbor.findRouteForShipment(shipment);
            PlannerRoute oldRoute = base.findRouteForShipment(shipment);
            
            if (!Objects.equals(newRoute, oldRoute)) {
                return new TabuMove(shipment, oldRoute, newRoute);
            }
        }
        
        return null;
    }
    
    private Solution diversify(Solution bestSolution, List<Shipment> shipments, List<Flight> flights, List<Airport> airports) {
        Random rand = new Random();
        TabuSolution diversified = new TabuSolution(bestSolution);
        
        // Strategy 1: Randomly remove shipments with higher probability for routes with many stops
        List<PlannerRoute> assignedRoutes = new ArrayList<>(diversified.getAssignedRoutes());
        for (PlannerRoute route : assignedRoutes) {
            double removalProb = 0.3 + (route.getSegments().size() > 1 ? 0.2 : 0); // Higher prob for multi-stop
            if (rand.nextDouble() < removalProb) {
                List<Shipment> shipmentsToRemove = new ArrayList<>(route.getShipments());
                for (Shipment shipment : shipmentsToRemove) {
                    route.removeShipment(shipment);
                }
            }
        }
        
        // Strategy 2: Shuffle existing shipments between compatible routes (35% of remaining routes)
        assignedRoutes = diversified.getAssignedRoutes(); // Get updated list after removals
        int shuffleCount = Math.max(1, (int)(assignedRoutes.size() * 0.35)); // 35% of routes
        
        for (int i = 0; i < shuffleCount && assignedRoutes.size() >= 2; i++) {
            int idx1 = rand.nextInt(assignedRoutes.size());
            int idx2 = rand.nextInt(assignedRoutes.size());
            
            if (idx1 != idx2) {
                PlannerRoute route1 = assignedRoutes.get(idx1);
                PlannerRoute route2 = assignedRoutes.get(idx2);
                
                // Only swap if routes have similar total shipment quantities
                int totalQuantity1 = route1.getShipments().stream().mapToInt(Shipment::getQuantity).sum();
                int totalQuantity2 = route2.getShipments().stream().mapToInt(Shipment::getQuantity).sum();
                
                // Check if routes are compatible for swap (similar quantities and compatible stops)
                if (totalQuantity1 <= totalQuantity2 * 1.3 && totalQuantity1 >= totalQuantity2 * 0.7) { // More flexible margin
                    boolean canSwap = true;
                    
                    // Check if destinations are reachable for both routes
                    for (Shipment s : route1.getShipments()) {
                        if (!route2.getSegments().isEmpty() && 
                            !route2.getSegments().get(route2.getSegments().size()-1)
                                   .getFlight().getDestination().equals(s.getDestination())) {
                            canSwap = false;
                            break;
                        }
                    }
                    
                    for (Shipment s : route2.getShipments()) {
                        if (!route1.getSegments().isEmpty() && 
                            !route1.getSegments().get(route1.getSegments().size()-1)
                                   .getFlight().getDestination().equals(s.getDestination())) {
                            canSwap = false;
                            break;
                        }
                    }
                    
                    if (canSwap) {
                        // Swap all shipments between routes
                        List<Shipment> shipments1 = new ArrayList<>(route1.getShipments());
                        List<Shipment> shipments2 = new ArrayList<>(route2.getShipments());
                        
                        for (Shipment s : shipments1) route1.removeShipment(s);
                        for (Shipment s : shipments2) route2.removeShipment(s);
                        
                        for (Shipment s : shipments1) route2.addShipment(s);
                        for (Shipment s : shipments2) route1.addShipment(s);
                    }
                }
            }
        }
        
        // Strategy 3: Try to find new routes for unassigned shipments
        List<Shipment> unassignedShipments = shipments.stream()
            .filter(s -> diversified.getRoutes().stream()
                .noneMatch(r -> r.getShipments().contains(s)))
            .collect(Collectors.toList());
            
        if (!unassignedShipments.isEmpty()) {
            TabuSolution newSolution = generateInitialSolution(unassignedShipments, flights, airports);
            for (PlannerRoute route : newSolution.getRoutes()) {
                if (!route.getSegments().isEmpty()) {
                    diversified.addRoute(route);
                }
            }
        }
        
        // Strategy 4: Randomly modify multi-stop routes
        for (PlannerRoute route : diversified.getAssignedRoutes()) {
            if (route.getSegments().size() > 1 && rand.nextDouble() < 0.5) { // Increased probability
                List<Shipment> routeShipments = new ArrayList<>(route.getShipments());
                
                double r = rand.nextDouble();
                if (r < 0.4) { // 40% chance to reverse
                    // Reverse the segments order
                    Collections.reverse(route.getSegments());
                } else if (r < 0.7 && route.getSegments().size() > 2) { // 30% chance to remove middle
                    // Remove a random middle segment
                    int removeIdx = rand.nextInt(route.getSegments().size() - 2) + 1;
                    route.getSegments().remove(removeIdx);
                } else if (route.getSegments().size() >= 2) { // 30% chance to swap adjacent segments
                    // Swap two adjacent segments
                    int idx = rand.nextInt(route.getSegments().size() - 1);
                    PlannerSegment temp = route.getSegments().get(idx);
                    route.getSegments().set(idx, route.getSegments().get(idx + 1));
                    route.getSegments().set(idx + 1, temp);
                }
                
                // Verify if the modified route can still handle all shipments
                for (Shipment shipment : routeShipments) {
                    // Check if the route's capacity can still handle the shipment
                    List<Flight> routeFlights = route.getSegments().stream()
                        .map(PlannerSegment::getFlight)
                        .collect(Collectors.toList());
                    if (routeFlights.stream().anyMatch(f -> !canUseFlight(f, shipment, new HashMap<>()))) {
                        route.removeShipment(shipment);
                    }
                }
            }
        }
        
        return diversified;
    }

    private static class TabuMove {
        private final Shipment shipment;
        private final PlannerRoute fromRoute;
        private final PlannerRoute toRoute;
        
        public TabuMove(Shipment s, PlannerRoute from, PlannerRoute to) {
            this.shipment = s;
            this.fromRoute = from;
            this.toRoute = to;
        }
        
        @Override 
        public boolean equals(Object o) { 
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TabuMove other = (TabuMove) o;
            return Objects.equals(shipment, other.shipment) &&
                   Objects.equals(fromRoute, other.fromRoute) &&
                   Objects.equals(toRoute, other.toRoute);
        }
        
        @Override 
        public int hashCode() { 
            return Objects.hash(shipment, fromRoute, toRoute);
        }
    }

    @SuppressWarnings("unused")
    private void printPlanningStatistics(TabuSolution solution) {
        // Estadísticas de tiempo de entrega
        Map<TimeRange, Integer> timeDistribution = solution.getDeliveryTimeDistribution();
        System.out.println("\nDelivery Time Distribution:");
        for (Map.Entry<TimeRange, Integer> entry : timeDistribution.entrySet()) {
            int count = entry.getValue();
            double percentage = solution.getCompletedOrders().isEmpty() ? 0 :
                (count * 100.0) / solution.getCompletedOrders().size();
            System.out.printf("%s: %d orders (%.1f%%)\n", entry.getKey(), count, percentage);
        }

        // Estadísticas por continente
        Map<String, Double> continentSuccess = solution.getDeliverySuccessRateByContinent();
        Map<String, Duration> avgDeliveryTimes = solution.getAverageDeliveryTimeByContinent();
        
        System.out.println("\nDelivery Statistics by Continent:");
        for (Map.Entry<String, Double> entry : continentSuccess.entrySet()) {
            String continent = entry.getKey();
            double successRate = entry.getValue() * 100;
            Duration avgTime = avgDeliveryTimes.get(continent);
            
            System.out.printf("%s:\n", continent);
            System.out.printf("  Success Rate: %.1f%%\n", successRate);
            if (avgTime != null) {
                System.out.printf("  Average Delivery Time: %d hours %d minutes\n", 
                    avgTime.toHours(), avgTime.toMinutesPart());
            }
        }

        // Estadísticas de rutas
        Map<String, Double> routeTypes = solution.getRouteTypeDistribution();
        System.out.println("\nRoute Type Distribution:");
        System.out.printf("Direct Flights: %.1f%%\n", routeTypes.get("direct") * 100);
        System.out.printf("Connecting Flights: %.1f%%\n", routeTypes.get("connecting") * 100);

        // Utilización de capacidad
        double avgUtilization = solution.getAverageFlightUtilization() * 100;
        System.out.printf("\nAverage Flight Utilization: %.1f%%\n", avgUtilization);
    }
    
    /**
     * Generates multi-stop routes for a shipment using breadth-first search approach
     * @param flights Available flights
     * @param shipment Target shipment
     * @param maxStops Maximum number of stops allowed (not counting origin and destination)
     * @param maxRoutes Maximum number of routes to return to avoid combinatorial explosion
     * @return List of flight sequences that form valid multi-stop routes
     */
    private List<List<Flight>> generateMultiStopRoutes(List<Flight> flights, Shipment shipment, int maxStops, int maxRoutes) {
        List<List<Flight>> validRoutes = new ArrayList<>();
        Airport origin = shipment.getOrigin();
        Airport destination = shipment.getDestination();
        LocalDateTime orderTime = shipment.getOrder().getOrderTime();
        long maxDeliveryHours = shipment.getOrder().getMaxDeliveryHours();
        
        // Use BFS to explore possible routes with different numbers of stops
        Queue<RouteInProgress> queue = new ArrayDeque<>();
        
        // Initialize with all flights from origin
        List<Flight> originFlights = flights.stream()
            .filter(f -> f.getOrigin().equals(origin) && 
                       f.getCapacity() >= shipment.getQuantity() &&
                       !f.getDepartureTime().isBefore(orderTime))
            .sorted(Comparator.comparing(Flight::getDepartureTime))
            .limit(10) // Limit initial candidates to control explosion
            .collect(Collectors.toList());
            
        for (Flight initialFlight : originFlights) {
            queue.offer(new RouteInProgress(Arrays.asList(initialFlight), initialFlight.getDestination(), initialFlight.getArrivalTime()));
        }
        
        while (!queue.isEmpty() && validRoutes.size() < maxRoutes) {
            RouteInProgress current = queue.poll();
            
            // Check if we reached destination
            if (current.getCurrentAirport().equals(destination)) {
                // Verify total time constraint
                LocalDateTime finalArrival = current.getLastArrivalTime();
                if (ChronoUnit.HOURS.between(orderTime, finalArrival) <= maxDeliveryHours) {
                    validRoutes.add(new ArrayList<>(current.getFlights()));
                }
                continue;
            }
            
            // If we haven't reached max stops, continue exploring
            if (current.getFlights().size() < maxStops + 1) { // +1 because maxStops doesn't count final destination
                List<Flight> nextFlights = flights.stream()
                    .filter(f -> f.getOrigin().equals(current.getCurrentAirport()) &&
                               f.getCapacity() >= shipment.getQuantity() &&
                               isValidConnection(current.getLastArrivalTime(), f.getDepartureTime()) &&
                               !current.getFlights().contains(f) && // Avoid cycles
                               ChronoUnit.HOURS.between(orderTime, f.getArrivalTime()) <= maxDeliveryHours)
                    .sorted(Comparator.comparing(Flight::getDepartureTime))
                    .limit(5) // Limit branching factor
                    .collect(Collectors.toList());
                
                for (Flight nextFlight : nextFlights) {
                    List<Flight> newRoute = new ArrayList<>(current.getFlights());
                    newRoute.add(nextFlight);
                    queue.offer(new RouteInProgress(newRoute, nextFlight.getDestination(), nextFlight.getArrivalTime()));
                }
            }
        }
        
        // Sort routes by total travel time (prefer shorter routes)
        validRoutes.sort((r1, r2) -> {
            LocalDateTime arrival1 = r1.get(r1.size()-1).getArrivalTime();
            LocalDateTime arrival2 = r2.get(r2.size()-1).getArrivalTime();
            return arrival1.compareTo(arrival2);
        });
        
        return validRoutes;
    }
    
    /**
     * Validates if a connection between two flights is acceptable
     */
    private boolean isValidConnection(LocalDateTime arrivalTime, LocalDateTime departureTime) {
        if (departureTime.isBefore(arrivalTime)) {
            return false;
        }
        long connectionHours = ChronoUnit.HOURS.between(arrivalTime, departureTime);
        return connectionHours >= 1 && connectionHours <= 12; // 1-12 hour connection window
    }
    
    /**
     * Validates if a multi-stop route is valid for a shipment
     */
    private boolean isValidMultiStopRoute(List<Flight> routeFlights, Shipment shipment) {
        if (routeFlights.isEmpty()) {
            return false;
        }
        
        // Check origin and destination match
        Flight firstFlight = routeFlights.get(0);
        Flight lastFlight = routeFlights.get(routeFlights.size() - 1);
        
        if (!firstFlight.getOrigin().equals(shipment.getOrigin()) ||
            !lastFlight.getDestination().equals(shipment.getDestination())) {
            return false;
        }
        
        // Check all flights have sufficient capacity
        if (routeFlights.stream().anyMatch(f -> f.getCapacity() < shipment.getQuantity())) {
            return false;
        }
        
        // Check flight connectivity
        for (int i = 0; i < routeFlights.size() - 1; i++) {
            Flight current = routeFlights.get(i);
            Flight next = routeFlights.get(i + 1);
            
            if (!current.getDestination().equals(next.getOrigin())) {
                return false; // Flights don't connect
            }
            
            if (!isValidConnection(current.getArrivalTime(), next.getDepartureTime())) {
                return false; // Invalid connection time
            }
        }
        
        // Check total delivery time constraint
        LocalDateTime orderTime = shipment.getOrder().getOrderTime();
        LocalDateTime finalArrival = lastFlight.getArrivalTime();
        long totalHours = ChronoUnit.HOURS.between(orderTime, finalArrival);
        
        return totalHours <= shipment.getOrder().getMaxDeliveryHours() && 
               !firstFlight.getDepartureTime().isBefore(orderTime);
    }
    
    /**
     * Helper class to track route construction progress during BFS
     */
    private static class RouteInProgress {
        private final List<Flight> flights;
        private final Airport currentAirport;
        private final LocalDateTime lastArrivalTime;
        
        public RouteInProgress(List<Flight> flights, Airport currentAirport, LocalDateTime lastArrivalTime) {
            this.flights = flights;
            this.currentAirport = currentAirport;
            this.lastArrivalTime = lastArrivalTime;
        }
        
        public List<Flight> getFlights() { return flights; }
        public Airport getCurrentAirport() { return currentAirport; }
        public LocalDateTime getLastArrivalTime() { return lastArrivalTime; }
    }
    
    /**
     * EXPERIMENTAL: Calculate average delivery time for all orders (Factor 2)
     * Returns the average delivery time in minutes
     */
    private double calculateAverageDeliveryTime(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return 0.0;
        }
        
        double totalDeliveryMinutes = 0.0;
        int ordersWithDeliveryTime = 0;
        int negativeTimeOrders = 0;
        
        for (Order order : orders) {
            Duration deliveryTime = order.getTotalDeliveryTime();
            if (deliveryTime != null && !deliveryTime.isZero()) {
                double minutes = deliveryTime.toMinutes();
                if (minutes < 0) {
                    negativeTimeOrders++;
                    // Debug: Print negative delivery time cases (only first 2 to avoid spam)
                    if (negativeTimeOrders <= 2) {
                        System.err.printf("DEBUG: Order #%d has negative delivery time: %.2f minutes (OrderTime: %s)%n", 
                                        order.getId(), minutes, order.getOrderTime());
                        if (!order.getShipments().isEmpty()) {
                            order.getShipments().forEach(s -> 
                                System.err.printf("  -> Shipment #%d EstimatedArrival: %s%n", s.getId(), s.getEstimatedArrival()));
                        } else {
                            System.err.println("  -> No shipments found!");
                        }
                    }
                }
                totalDeliveryMinutes += minutes;
                ordersWithDeliveryTime++;
            }
        }
        
        if (negativeTimeOrders > 0) {
            System.err.printf("WARNING: Found %d orders with negative delivery times out of %d total orders%n", 
                            negativeTimeOrders, ordersWithDeliveryTime);
        }
        
        return ordersWithDeliveryTime > 0 ? totalDeliveryMinutes / ordersWithDeliveryTime : 0.0;
    }
    
    /**
     * EXPERIMENTAL: Analyze delivery time compliance with restrictions
     * Reports how many orders exceed the time limits (48h intra-continental, 72h inter-continental)
     */
    private void analyzeDeliveryTimeCompliance(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            System.out.println("=== DELIVERY TIME COMPLIANCE ANALYSIS ===");
            System.out.println("No orders to analyze");
            return;
        }
        
        int totalOrders = 0;
        int intraContinentalOrders = 0;
        int interContinentalOrders = 0;
        int intraContinentalViolations = 0;
        int interContinentalViolations = 0;
        double maxIntraDeliveryHours = 0;
        double maxInterDeliveryHours = 0;
        
        System.out.println("=== DELIVERY TIME COMPLIANCE ANALYSIS ===");
        
        for (Order order : orders) {
            Duration deliveryTime = order.getTotalDeliveryTime();
            if (deliveryTime != null && !deliveryTime.isZero()) {
                totalOrders++;
                double deliveryHours = deliveryTime.toMinutes() / 60.0;
                long maxAllowedHours = order.getMaxDeliveryHours();
                
                if (maxAllowedHours == 48) {
                    // Intra-continental
                    intraContinentalOrders++;
                    maxIntraDeliveryHours = Math.max(maxIntraDeliveryHours, deliveryHours);
                    if (deliveryHours > 48) {
                        intraContinentalViolations++;
                        System.out.printf("  VIOLATION: Intra-continental order #%d took %.2f hours (limit: 48h)%n", 
                                        order.getId(), deliveryHours);
                    }
                } else if (maxAllowedHours == 72) {
                    // Inter-continental
                    interContinentalOrders++;
                    maxInterDeliveryHours = Math.max(maxInterDeliveryHours, deliveryHours);
                    if (deliveryHours > 72) {
                        interContinentalViolations++;
                        System.out.printf("  VIOLATION: Inter-continental order #%d took %.2f hours (limit: 72h)%n", 
                                        order.getId(), deliveryHours);
                    }
                }
            }
        }
        
        System.out.printf("Total Orders Analyzed: %d%n", totalOrders);
        System.out.printf("Intra-continental Orders: %d (Max delivery: %.2f hours)%n", 
                        intraContinentalOrders, maxIntraDeliveryHours);
        System.out.printf("Inter-continental Orders: %d (Max delivery: %.2f hours)%n", 
                        interContinentalOrders, maxInterDeliveryHours);
        System.out.printf("Intra-continental Violations: %d/%d (%.1f%%)%n", 
                        intraContinentalViolations, intraContinentalOrders, 
                        intraContinentalOrders > 0 ? (100.0 * intraContinentalViolations / intraContinentalOrders) : 0);
        System.out.printf("Inter-continental Violations: %d/%d (%.1f%%)%n", 
                        interContinentalViolations, interContinentalOrders,
                        interContinentalOrders > 0 ? (100.0 * interContinentalViolations / interContinentalOrders) : 0);
        System.out.println("=======================================");
    }
    
    /**
     * Generates a unique signature for a TabuMove to track frequency in adaptive memory
     */
    private String generateMoveSignature(TabuMove move) {
        if (move == null) return "NULL_MOVE";
        
        // Generate signature based on shipment and routes involved
        String fromRouteId = (move.fromRoute != null) ? String.valueOf(move.fromRoute.hashCode()) : "NULL";
        String toRouteId = (move.toRoute != null) ? String.valueOf(move.toRoute.hashCode()) : "NULL";
        String shipmentId = (move.shipment != null) ? String.valueOf(move.shipment.hashCode()) : "NULL";
        
        return String.format("MOVE_%s_%s_%s", fromRouteId, toRouteId, shipmentId);
    }
}