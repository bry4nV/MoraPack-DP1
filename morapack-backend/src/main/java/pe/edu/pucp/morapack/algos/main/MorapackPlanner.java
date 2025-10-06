package pe.edu.pucp.morapack.algos.main;

import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.model.*;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.data.DataLoader;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MorapackPlanner {
    private static final String DEFAULT_AIRPORTS_FILE = "data/airports.txt";
    private static final String DEFAULT_FLIGHTS_FILE = "data/flights.csv";
    private static final String DEFAULT_ORDERS_FILE = "data/pedidos.csv";

    public static void main(String[] args) {
        String airportsFile = args.length > 0 ? args[0] : DEFAULT_AIRPORTS_FILE;
        String flightsFile = args.length > 1 ? args[1] : DEFAULT_FLIGHTS_FILE;
        String ordersFile = args.length > 2 ? args[2] : DEFAULT_ORDERS_FILE;

        System.out.println("--- Iniciando Prueba del Planificador MoraPack ---");
        System.out.println("Usando archivos:");
        System.out.println("- Aeropuertos: " + airportsFile);
        System.out.println("- Vuelos: " + flightsFile);
        System.out.println("- Órdenes: " + ordersFile);

        try {
            // Load airports
            List<Airport> airports = DataLoader.loadAirports(airportsFile);
            System.out.println("Loaded " + airports.size() + " airports");

            // Create map for quick airport lookup
            Map<String, Airport> airportMap = airports.stream()
                .collect(Collectors.toMap(Airport::getCode, a -> a));

            // Load flights
            List<Flight> availableFlights = DataLoader.loadFlights(flightsFile, airportMap);
            System.out.println("Loaded " + availableFlights.size() + " flights");

            // Create or load orders
            List<Order> pendingOrders = DataLoader.loadOrders(ordersFile, airportMap);
            System.out.println("Loaded " + pendingOrders.size() + " orders");
            
            System.out.println("\n[INITIAL INPUT DATA]");
            pendingOrders.forEach(p -> 
                System.out.println("  - Order #" + p.getId() + ": " + p.getTotalQuantity() + 
                                 " products to " + p.getDestination().getCity() + 
                                 " (Deadline: " + p.getMaxDeliveryHours() + "h)")
            );

            System.out.println("\n=== EXPERIMENT: 20 ALGORITHM EXECUTIONS FOR DUAL FACTOR ANALYSIS ===");
            System.out.println("Running TabuSearch algorithm 20 times to measure:");
            System.out.println("- Factor 1: Execution time (milliseconds)");
            System.out.println("- Factor 2: Average delivery time (minutes)");
            
            // Arrays to store both factors
            double[] executionTimes = new double[20];      // Factor 1: Execution time (ms)
            double[] averageDeliveryTimes = new double[20]; // Factor 2: Average delivery time (minutes)
            Solution bestSolution = null;
            double bestExecutionTime = 0;
            
            // Execute algorithm 20 times
            for (int run = 1; run <= 20; run++) {
                System.out.println("\n--- Execution " + run + "/20 ---");
                
                // Measure execution time (Factor 1)
                long startTime = System.nanoTime();
                
                TabuSearchPlanner planner = new TabuSearchPlanner();
                Solution solution = planner.optimize(pendingOrders, availableFlights, airports);
                
                long endTime = System.nanoTime();
                double executionTimeMs = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
                executionTimes[run - 1] = executionTimeMs;
                
                // Get average delivery time (Factor 2)
                double avgDeliveryTimeMinutes = planner.getAverageDeliveryTimeMinutes();
                averageDeliveryTimes[run - 1] = avgDeliveryTimeMinutes;
                
                // Keep the best solution for final display
                if (bestSolution == null) {
                    bestSolution = solution;
                    bestExecutionTime = executionTimeMs;
                }
                
                System.out.printf("Execution %d completed - Time: %.3f ms, Avg Delivery: %.2f min%n", 
                                run, executionTimeMs, avgDeliveryTimeMinutes);
            }
            
            // Store arrays for final printing
            double[] finalExecutionTimes = executionTimes.clone();
            double[] finalDeliveryTimes = averageDeliveryTimes.clone();
            
            // Print results of best solution
            System.out.println("\n[BEST SOLUTION RESULT]");
            printSolution(bestSolution);
            
            // Print detailed statistics
            System.out.println("\n=== FINAL STATISTICS ===");
            printDetailedStatistics(bestSolution, pendingOrders);

            // Print experimental data for both factors
            printExperimentalResults(finalExecutionTimes, finalDeliveryTimes);

        } catch (IOException e) {
            System.err.println("Error loading data files: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        System.out.println("\n--- Test Finished ---");
    }

    private static void printDetailedStatistics(Solution solution, List<Order> originalOrders) {
        if (solution == null) {
            System.out.println("No solution to analyze.");
            return;
        }
        
        // Count assigned shipments and products
        int totalShipments = solution.getRoutes().stream()
            .mapToInt(r -> r.getShipments().size())
            .sum();
            
        int assignedProducts = solution.getRoutes().stream()
            .flatMap(r -> r.getShipments().stream())
            .mapToInt(s -> s.getQuantity())
            .sum();
            
        int totalProducts = originalOrders.stream()
            .mapToInt(Order::getTotalQuantity)
            .sum();
            
        // Count completed orders
        int completedOrders = 0;
        int totalOrders = originalOrders.size();
        
        for (Order order : originalOrders) {
            boolean isComplete = solution.getRoutes().stream()
                .flatMap(r -> r.getShipments().stream())
                .filter(s -> s.getParentOrder().getId() == order.getId())
                .mapToInt(Shipment::getQuantity)
                .sum() == order.getTotalQuantity();
                
            if (isComplete) completedOrders++;
        }
        
        // Count routes with assignments
        long assignedRoutes = solution.getRoutes().stream()
            .filter(r -> !r.getShipments().isEmpty() && !r.getSegments().isEmpty())
            .count();
            
        long emptyRoutes = solution.getRoutes().stream()
            .filter(r -> r.getShipments().isEmpty() || r.getSegments().isEmpty())
            .count();
        
        // Print statistics
        System.out.println("Total Orders: " + totalOrders);
        System.out.println("Completed Orders: " + completedOrders + " (" + 
            String.format("%.1f%%", (double) completedOrders / totalOrders * 100) + ")");
        System.out.println("Incomplete Orders: " + (totalOrders - completedOrders));
        
        System.out.println("\nProduct Assignment:");
        System.out.println("Total Products: " + totalProducts);
        System.out.println("Assigned Products: " + assignedProducts + " (" + 
            String.format("%.1f%%", (double) assignedProducts / totalProducts * 100) + ")");
        System.out.println("Unassigned Products: " + (totalProducts - assignedProducts));
        
        System.out.println("\nRoute Statistics:");
        System.out.println("Total Routes: " + solution.getRoutes().size());
        System.out.println("Assigned Routes: " + assignedRoutes);
        System.out.println("Empty Routes: " + emptyRoutes);
        System.out.println("Total Shipments: " + totalShipments);
        
        if (assignedProducts > 0) {
            System.out.printf("Average Products per Shipment: %.1f\n", 
                (double) assignedProducts / totalShipments);
        }
    }

    private static void printSolution(Solution solution) {
        if (solution != null && !solution.getRoutes().isEmpty()) {
            System.out.println("Solution found!");
            solution.getRoutes().forEach(route -> {
                route.getShipments().forEach(shipment -> {
                    System.out.println("\n  > For Shipment #" + shipment.getId() + 
                                     " (from Order #" + shipment.getParentOrder().getId() + 
                                     " with " + shipment.getQuantity() + " products)");
                    if (route.getSegments().isEmpty()) {
                        System.out.println("    Route: Could not assign a route!");
                    } else {
                        String routeStr = route.getSegments().stream()
                            .map(s -> s.getFlight().getCode() + " (" + 
                                    s.getFlight().getOrigin().getName() + " -> " + 
                                    s.getFlight().getDestination().getName() + ")")
                            .collect(Collectors.joining(" | "));
                        System.out.println("    Route: " + routeStr);
                        System.out.println("    Estimated arrival: " + route.getFinalArrivalTime());
                    }
                });
            });
        } else {
            System.out.println("Could not find a solution.");
        }
    }
    
    /**
     * EXPERIMENTAL: Print results for both factors (execution time and delivery time)
     * Formatted for statistical analysis in R
     */
    private static void printExperimentalResults(double[] executionTimes, double[] deliveryTimes) {
        System.out.println("\n=== EXPERIMENTAL RESULTS FOR STATISTICAL ANALYSIS ===");
        System.out.println("TABÚ SEARCH ALGORITHM - 20 EXECUTIONS");
        System.out.println("=".repeat(80));
        
        // Print table header
        System.out.println("\n📊 DUAL FACTOR EXPERIMENTAL DATA:");
        System.out.println("-".repeat(80));
        System.out.printf("%-10s | %-20s | %-25s%n", "Execution", "Factor 1 (ms)", "Factor 2 (minutes)");
        System.out.printf("%-10s | %-20s | %-25s%n", "Number", "Execution Time", "Avg Delivery Time");
        System.out.println("-".repeat(80));
        
        // Print all data points
        for (int i = 0; i < executionTimes.length; i++) {
            System.out.printf("%-10d | %-20.3f | %-25.2f%n", 
                            (i + 1), executionTimes[i], deliveryTimes[i]);
        }
        
        // Calculate and print statistics for both factors
        System.out.println("\n📈 STATISTICAL SUMMARY:");
        System.out.println("=".repeat(80));
        
        // Factor 1 Statistics (Execution Time)
        double minExecTime = Arrays.stream(executionTimes).min().orElse(0);
        double maxExecTime = Arrays.stream(executionTimes).max().orElse(0);
        double avgExecTime = Arrays.stream(executionTimes).average().orElse(0);
        double totalExecTime = Arrays.stream(executionTimes).sum();
        double stdExecTime = calculateStandardDeviation(executionTimes, avgExecTime);
        
        // Factor 2 Statistics (Delivery Time)
        double minDelTime = Arrays.stream(deliveryTimes).min().orElse(0);
        double maxDelTime = Arrays.stream(deliveryTimes).max().orElse(0);
        double avgDelTime = Arrays.stream(deliveryTimes).average().orElse(0);
        double totalDelTime = Arrays.stream(deliveryTimes).sum();
        double stdDelTime = calculateStandardDeviation(deliveryTimes, avgDelTime);
        
        System.out.println("\nFACTOR 1 - EXECUTION TIME (milliseconds):");
        System.out.printf("  Minimum:        %10.3f ms%n", minExecTime);
        System.out.printf("  Maximum:        %10.3f ms%n", maxExecTime);
        System.out.printf("  Average:        %10.3f ms%n", avgExecTime);
        System.out.printf("  Total:          %10.3f ms%n", totalExecTime);
        System.out.printf("  Std Deviation:  %10.3f ms%n", stdExecTime);
        
        System.out.println("\nFACTOR 2 - AVERAGE DELIVERY TIME (minutes):");
        System.out.printf("  Minimum:        %10.2f min%n", minDelTime);
        System.out.printf("  Maximum:        %10.2f min%n", maxDelTime);
        System.out.printf("  Average:        %10.2f min%n", avgDelTime);
        System.out.printf("  Total:          %10.2f min%n", totalDelTime);
        System.out.printf("  Std Deviation:  %10.2f min%n", stdDelTime);
        
        // Print data for easy copy-paste to R
        System.out.println("\n🔬 DATA FOR R ANALYSIS:");
        System.out.println("-".repeat(50));
        System.out.print("execution_times_tabu <- c(");
        for (int i = 0; i < executionTimes.length; i++) {
            System.out.printf("%.3f", executionTimes[i]);
            if (i < executionTimes.length - 1) System.out.print(", ");
        }
        System.out.println(")");
        
        System.out.print("delivery_times_tabu <- c(");
        for (int i = 0; i < deliveryTimes.length; i++) {
            System.out.printf("%.2f", deliveryTimes[i]);
            if (i < deliveryTimes.length - 1) System.out.print(", ");
        }
        System.out.println(")");
        
        System.out.println("\n✅ Ready for statistical analysis with Shapiro-Wilk and Wilcoxon/T-Student tests");
    }
    
    private static double calculateStandardDeviation(double[] values, double mean) {
        double sum = 0.0;
        for (double value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / values.length);
    }
}