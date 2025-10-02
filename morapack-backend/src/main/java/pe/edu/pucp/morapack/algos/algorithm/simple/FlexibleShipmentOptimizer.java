package pe.edu.pucp.morapack.algos.algorithm.simple;

import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple optimizer that assigns products directly to flights, then creates shipments
 * No minimum shipment size restrictions - shipments can have any quantity from 1 to flight capacity
 */
public class FlexibleShipmentOptimizer implements IOptimizer {
    
    private AirportStorageManager storageManager;
    
    @Override
    public Solution optimize(List<Order> orders, List<Flight> availableFlights, List<Airport> airports) {
        this.storageManager = new AirportStorageManager();
        
        System.out.println("=== Flexible Shipment Optimizer ===");
        System.out.println("Strategy: Assign products to flights first, create shipments second");
        System.out.println("Orders: " + orders.size() + ", Flights: " + availableFlights.size());
        
        // Track product assignments: Order -> List<(Flight, Quantity)>
        Map<Order, List<ProductAssignment>> productAssignments = new HashMap<>();
        
        // Initialize assignments for all orders
        for (Order order : orders) {
            productAssignments.put(order, new ArrayList<>());
        }
        
        // Step 1: Assign products to flights (greedy approach for now)
        assignProductsToFlights(orders, availableFlights, productAssignments);
        
        // Step 2: Convert product assignments to shipments and routes
        Solution solution = createSolutionFromAssignments(productAssignments, availableFlights);
        
        // Print results
        printOptimizationResults(solution, productAssignments);
        
        return solution;
    }
    
    private void assignProductsToFlights(List<Order> orders, List<Flight> availableFlights, 
                                       Map<Order, List<ProductAssignment>> productAssignments) {
        
        System.out.println("\n--- Step 1: Assigning Products to Flights ---");
        
        Map<String, Integer> flightUtilization = new HashMap<>();
        int assignmentId = 1;
        int totalAssigned = 0;
        int totalProducts = orders.stream().mapToInt(Order::getTotalQuantity).sum();
        
        for (Order order : orders) {
            int remainingQuantity = order.getTotalQuantity();
            
            // Find direct flights for this order
            List<Flight> directFlights = availableFlights.stream()
                .filter(f -> f.getOrigin().equals(order.getOrigin()) && 
                           f.getDestination().equals(order.getDestination()))
                .sorted((f1, f2) -> Integer.compare(f2.getCapacity(), f1.getCapacity())) // Prefer larger flights
                .collect(Collectors.toList());
            
            // Assign to direct flights
            for (Flight flight : directFlights) {
                if (remainingQuantity <= 0) break;
                
                int currentUsed = flightUtilization.getOrDefault(flight.getCode(), 0);
                int availableCapacity = flight.getCapacity() - currentUsed;
                
                if (availableCapacity > 0) {
                    // Check storage capacity at destination
                    if (storageManager.hasAvailableCapacity(flight.getDestination(), Math.min(remainingQuantity, availableCapacity))) {
                        
                        int quantityToAssign = Math.min(remainingQuantity, availableCapacity);
                        
                        // Reserve storage capacity
                        if (storageManager.reserveCapacity(flight.getDestination(), quantityToAssign)) {
                            
                            ProductAssignment assignment = new ProductAssignment(
                                assignmentId++, order, flight, quantityToAssign);
                            
                            productAssignments.get(order).add(assignment);
                            flightUtilization.put(flight.getCode(), currentUsed + quantityToAssign);
                            remainingQuantity -= quantityToAssign;
                            totalAssigned += quantityToAssign;
                        }
                    }
                }
            }
            
            // Log unassigned products
            if (remainingQuantity > 0) {
                System.out.println("  Order #" + order.getId() + ": " + remainingQuantity + 
                                 " products could not be assigned (out of " + order.getTotalQuantity() + ")");
            }
        }
        
        System.out.printf("Total products assigned: %d/%d (%.1f%%)%n", 
                         totalAssigned, totalProducts, (double)totalAssigned/totalProducts*100);
    }
    
    private Solution createSolutionFromAssignments(Map<Order, List<ProductAssignment>> productAssignments,
                                                 List<Flight> availableFlights) {
        
        System.out.println("\n--- Step 2: Creating Shipments from Assignments ---");
        
        Solution solution = new Solution();
        List<Order> allOrders = new ArrayList<>(productAssignments.keySet());
        solution.setAllOrders(allOrders);
        
        int shipmentId = 1;
        
        // Group assignments by flight to create shipments
        Map<Flight, List<ProductAssignment>> assignmentsByFlight = new HashMap<>();
        
        for (List<ProductAssignment> assignments : productAssignments.values()) {
            for (ProductAssignment assignment : assignments) {
                assignmentsByFlight.computeIfAbsent(assignment.getFlight(), k -> new ArrayList<>()).add(assignment);
            }
        }
        
        // Create routes and shipments
        for (Map.Entry<Flight, List<ProductAssignment>> entry : assignmentsByFlight.entrySet()) {
            Flight flight = entry.getKey();
            List<ProductAssignment> assignments = entry.getValue();
            
            // Create a route for this flight
            PlannerRoute route = new PlannerRoute(flight);
            
            // The constructor already adds the segment, so we don't need to add it manually
            
            // Group assignments by order to create shipments
            Map<Order, Integer> quantityByOrder = new HashMap<>();
            for (ProductAssignment assignment : assignments) {
                quantityByOrder.merge(assignment.getOrder(), assignment.getQuantity(), Integer::sum);
            }
            
            // Create shipments
            for (Map.Entry<Order, Integer> orderEntry : quantityByOrder.entrySet()) {
                Order order = orderEntry.getKey();
                int quantity = orderEntry.getValue();
                
                Shipment shipment = new Shipment(shipmentId++, order, quantity, 
                                               flight.getOrigin(), flight.getDestination());
                route.addShipment(shipment);
                order.addShipment(shipment);
                
                System.out.println("  Created Shipment #" + shipment.getId() + 
                                 ": " + quantity + " products from Order #" + order.getId() + 
                                 " on Flight " + flight.getCode());
            }
            
            solution.addRoute(route);
        }
        
        // Update completed orders
        List<Order> completedOrders = allOrders.stream()
            .filter(order -> {
                int totalAssigned = productAssignments.get(order).stream()
                    .mapToInt(ProductAssignment::getQuantity).sum();
                return totalAssigned >= order.getTotalQuantity();
            })
            .collect(Collectors.toList());
        
        solution.setCompletedOrders(completedOrders);
        
        System.out.println("Created " + solution.getRoutes().size() + " routes with shipments");
        System.out.println("Completed orders: " + completedOrders.size() + "/" + allOrders.size());
        
        return solution;
    }
    
    private void printOptimizationResults(Solution solution, Map<Order, List<ProductAssignment>> productAssignments) {
        System.out.println("\n=== Optimization Results ===");
        
        int totalOrders = productAssignments.size();
        int completedOrders = solution.getCompletedOrders().size();
        int totalRoutes = solution.getRoutes().size();
        int totalShipments = solution.getRoutes().stream()
            .mapToInt(route -> route.getShipments().size()).sum();
        
        System.out.printf("Orders: %d total, %d completed (%.1f%%)%n", 
                         totalOrders, completedOrders, (double)completedOrders/totalOrders*100);
        System.out.printf("Routes created: %d%n", totalRoutes);
        System.out.printf("Shipments created: %d%n", totalShipments);
        
        // Show shipment size distribution
        List<Integer> shipmentSizes = solution.getRoutes().stream()
            .flatMap(route -> route.getShipments().stream())
            .map(Shipment::getQuantity)
            .sorted()
            .collect(Collectors.toList());
        
        if (!shipmentSizes.isEmpty()) {
            System.out.printf("Shipment sizes - Min: %d, Max: %d, Avg: %.1f%n",
                             shipmentSizes.get(0),
                             shipmentSizes.get(shipmentSizes.size()-1),
                             shipmentSizes.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
    }
}