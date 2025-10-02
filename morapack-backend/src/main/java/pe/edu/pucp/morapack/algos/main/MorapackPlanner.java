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

            System.out.println("\n--- Starting Product-to-Flight Assignment Optimization ---");
            System.out.println("The optimizer will assign products directly to flights,");
            System.out.println("then generate shipments automatically based on those assignments.");

            // Create planner and execute
            IOptimizer planner = new TabuSearchPlanner();
            Solution solution = planner.optimize(pendingOrders, availableFlights, airports);

            // Print results
            System.out.println("\n[PLANNING RESULT]");
            printSolution(solution);
            
            // Print detailed statistics
            System.out.println("\n=== FINAL STATISTICS ===");
            printDetailedStatistics(solution, pendingOrders);

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
}