package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.model.Continent;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TabuSearchPlannerCostFunction {
    // Base penalties
    private static final double CAPACITY_VIOLATION_PENALTY = 25000; // Balanced for feasibility and flexibility
    private static final double EMPTY_ROUTE_PENALTY = 35000; // Moderate reduction to allow some new routes
    private static final double DELAY_BASE_PENALTY = 10000; // Balanced for timeliness
    private static final double DELAY_HOUR_PENALTY = 300;  // Moderate reduction for flexibility
    private static final double STOPOVER_PENALTY = 600;    // Balanced for connections
    private static final double INVALID_STOPOVER_TIME_PENALTY = 22000; // Balanced for feasibility
    
    // Load balancing penalties
    private static final double LOAD_BALANCE_PENALTY = 6000; // Reduced to allow more focus on completion
    private static final double UTILIZATION_TARGET = 0.90; // Increased target for better capacity use
    private static final double UNDERUTILIZATION_PENALTY = 3000;  // Reduced to allow partial loads
    private static final double DISTRIBUTION_PENALTY = 4000;  // Reduced to focus on completion
    
    // Time-based penalties
    private static final double DELIVERY_TIME_PENALTY = 600; // Balanced for completion and timeliness
    private static final double DELIVERY_TIME_THRESHOLD = 60; // Moderate increase for flexibility
    private static final double TIME_PENALTY_FACTOR = 125;   // Balanced for delivery times
    
    public static void printStatistics(TabuSolution solution, List<Flight> flights, List<Airport> airports) {
        String outputPath = "statistics_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            Map<Flight, Integer> loadPerFlight = new HashMap<>();
            List<PlannerRoute> assignedRoutes = solution.getAssignedRoutes();
            
            // Calculate load statistics
            for (PlannerRoute route : assignedRoutes) {
                for (PlannerSegment segment : route.getSegments()) {
                    Flight flight = segment.getFlight();
                    double routeQuantity = route.getShipments().stream()
                            .mapToDouble(Shipment::getQuantity)
                            .sum();
                    loadPerFlight.merge(flight, (int)routeQuantity, Integer::sum);
                }
            }

            // Print general statistics
            writer.println("=== MoraPack Route Statistics ===");
            writer.println("Total routes: " + solution.getRoutes().size());
            writer.println("Assigned routes: " + assignedRoutes.size());
            writer.println("Empty routes: " + solution.getEmptyRoutes().size());

            // Print load statistics
            writer.println("\n=== Flight Load Statistics ===");
            double totalUtilization = 0.0;
            for (Map.Entry<Flight, Integer> entry : loadPerFlight.entrySet()) {
                Flight flight = entry.getKey();
                int load = entry.getValue();
                double utilizationRate = (double) load / flight.getCapacity();
                totalUtilization += utilizationRate;
                
                writer.printf("Flight %s (%s -> %s):\n", 
                    flight.getCode(),
                    flight.getOrigin().getCode(),
                    flight.getDestination().getCode());
                writer.printf("  Load: %d/%d (%.1f%%)\n", 
                    load, 
                    flight.getCapacity(),
                    utilizationRate * 100);
            }

            // Print average utilization
            if (!loadPerFlight.isEmpty()) {
                double avgUtilization = totalUtilization / loadPerFlight.size();
                writer.printf("\nAverage flight utilization: %.1f%%\n", avgUtilization * 100);
            }

            // Print delivery time statistics
            writer.println("\n=== Delivery Time Statistics ===");
            Map<Continent, List<Long>> deliveryHoursByContinent = new HashMap<>();
            
            for (PlannerRoute route : assignedRoutes) {
                for (Shipment shipment : route.getShipments()) {
                    long hours = ChronoUnit.HOURS.between(
                        shipment.getParentOrder().getOrderTime(),
                        route.getFinalArrivalTime()
                    );
                    
                    Continent continent = shipment.getDestination().getCountry().getContinent();
                    if (!deliveryHoursByContinent.containsKey(continent)) {
                        deliveryHoursByContinent.put(continent, new java.util.ArrayList<>());
                    }
                    deliveryHoursByContinent.get(continent).add(hours);
                }
            }

            for (Map.Entry<Continent, List<Long>> entry : deliveryHoursByContinent.entrySet()) {
                Continent continent = entry.getKey();
                List<Long> hours = entry.getValue();
                double avgHours = hours.stream().mapToLong(Long::longValue).average().orElse(0);
                
                String continentName = continent != null ? continent.name() : "UNSPECIFIED";
                writer.printf("%s:\n", continentName);
                writer.printf("  Average delivery time: %.1f hours\n", avgHours);
                writer.printf("  Shipments: %d\n", hours.size());
                
                long inTimeDeliveries = hours.stream()
                    .filter(h -> h <= 48) // Simplified to just use standard time
                    .count();
                
                writer.printf("  On-time deliveries: %.1f%%\n", 
                    hours.isEmpty() ? 0.0 : (double)inTimeDeliveries / hours.size() * 100);
            }

            // Print hub utilization
            writer.println("\n=== Hub Utilization ===");
            java.util.List<String> hubs = java.util.Arrays.asList("SPIM", "EBCI", "UBBB");
            Map<String, Integer> hubShipments = new HashMap<>();
            
            for (PlannerRoute route : assignedRoutes) {
                for (PlannerSegment segment : route.getSegments()) {
                    String origin = segment.getFlight().getOrigin().getCode();
                    if (hubs.contains(origin)) {
                        hubShipments.merge(origin, 
                            route.getShipments().stream()
                                .mapToInt(Shipment::getQuantity)
                                .sum(), 
                            Integer::sum);
                    }
                }
            }

            for (String hub : hubs) {
                writer.printf("%s (", hub);
                String hubName = switch (hub) {
                    case "SPIM" -> "Lima";
                    case "EBCI" -> "Brussels";
                    case "UBBB" -> "Baku";
                    default -> "";
                };
                writer.print(hubName);
                writer.printf("):\n  Packages processed: %d\n", 
                    hubShipments.getOrDefault(hub, 0));
            }
            
            System.out.println("Statistics have been written to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error writing statistics to file: " + e.getMessage());
        }
    }

    public static double calculateCost(TabuSolution solution, List<Flight> flights, List<Airport> airports, int currentIteration, int maxIterations) {
        double totalCost = 0.0;
        Map<Flight, Integer> loadPerFlight = new HashMap<>();
        
        // Get assigned routes
        List<PlannerRoute> assignedRoutes = solution.getAssignedRoutes();
        
        // Calculate load per flight using only assigned routes
        for (PlannerRoute route : assignedRoutes) {
            for (PlannerSegment segment : route.getSegments()) {
                Flight flight = segment.getFlight();
                double routeQuantity = route.getShipments().stream()
                        .mapToDouble(Shipment::getQuantity)
                        .sum();
                loadPerFlight.merge(flight, (int)routeQuantity, Integer::sum);
            }
        }
        
        // Dynamic factor increases penalties as iterations progress
        double dynamicFactor = 1.0 + ((double) currentIteration / maxIterations);
        
        // Calculate load balance penalties
        double totalLoadDeviation = 0.0;
        double totalUtilization = 0.0;
        double minUtilization = 1.0;
        double maxUtilization = 0.0;
        
        for (Map.Entry<Flight, Integer> entry : loadPerFlight.entrySet()) {
            Flight flight = entry.getKey();
            int load = entry.getValue();
            double utilizationRate = (double) load / flight.getCapacity();
            
            // Track utilization statistics
            totalUtilization += utilizationRate;
            minUtilization = Math.min(minUtilization, utilizationRate);
            maxUtilization = Math.max(maxUtilization, utilizationRate);
            
            // Calculate deviation from target utilization
            double deviation = Math.abs(utilizationRate - UTILIZATION_TARGET);
            totalLoadDeviation += deviation;
        }
        
        // Add load balance penalties
        if (!loadPerFlight.isEmpty()) {
            double avgUtilization = totalUtilization / loadPerFlight.size();
            double avgDeviation = totalLoadDeviation / loadPerFlight.size();
            
            // Penalize deviation from target utilization
            totalCost += LOAD_BALANCE_PENALTY * avgDeviation * dynamicFactor;
            
            // Penalize underutilization
            if (avgUtilization < UTILIZATION_TARGET) {
                totalCost += UNDERUTILIZATION_PENALTY * (UTILIZATION_TARGET - avgUtilization) * dynamicFactor;
            }
            
            // Penalize poor load distribution
            double distributionSpread = maxUtilization - minUtilization;
            totalCost += DISTRIBUTION_PENALTY * distributionSpread * dynamicFactor;
        }
        
        // Capacity violation penalty
        for (Map.Entry<Flight, Integer> entry : loadPerFlight.entrySet()) {
            // Dynamic capacity violation penalty that increases with iterations
            double iterationFactor = 1.0 + ((double) currentIteration / maxIterations);
            if (entry.getValue() > entry.getKey().getCapacity()) {
                totalCost += CAPACITY_VIOLATION_PENALTY * iterationFactor;
            }
        }
        
        // Penalize empty routes
        List<PlannerRoute> emptyRoutes = solution.getEmptyRoutes();
        totalCost += emptyRoutes.size() * EMPTY_ROUTE_PENALTY;
        
        // Route validations and penalties for assigned routes only
        for (PlannerRoute route : assignedRoutes) {
            // Skip empty routes
            if (route.getSegments().isEmpty()) {
                continue;
            }
            
            // Validate minimum stopover times (1 hour)
            List<PlannerSegment> segments = route.getSegments();
            
            for (Shipment shipment : route.getShipments()) {
                for (int i = 0; i < segments.size() - 1; i++) {
                    Flight current = segments.get(i).getFlight();
                    Flight next = segments.get(i + 1).getFlight();
                    
                    long stopoverHours = ChronoUnit.HOURS.between(
                        current.getArrivalTime(),
                        next.getDepartureTime()
                    );
                    
                    if (stopoverHours < 1) {
                        totalCost += INVALID_STOPOVER_TIME_PENALTY;
                    }
                }
                
                // Enhanced delivery time penalty calculation
                long totalHours = ChronoUnit.HOURS.between(
                    shipment.getParentOrder().getOrderTime(),
                    route.getFinalArrivalTime().plusHours(2) // Add 2 hours for processing
                );
                
                // Adjust for destination timezone
                Airport destAirport = airports.stream()
                    .filter(a -> a.getCode().equals(shipment.getDestination().getCode()))
                    .findFirst()
                    .orElse(null);
                    
                if (destAirport != null) {
                    totalHours += destAirport.getGmt(); // Adjust for destination timezone
                }
                
                // Base delivery time penalty
                totalCost += DELIVERY_TIME_PENALTY;
                
                // Penalty for exceeding target delivery time
                if (totalHours > DELIVERY_TIME_THRESHOLD) {
                    double excessHours = totalHours - DELIVERY_TIME_THRESHOLD;
                    double timePenalty = excessHours * TIME_PENALTY_FACTOR;
                    totalCost += timePenalty * dynamicFactor; // Scale with iteration progress
                }
                
                // Additional penalty for exceeding maximum time
                long maxHours = shipment.isInterContinental() ? 72 : 48;
                if (totalHours > maxHours) {
                    long delayHours = totalHours - maxHours;
                    totalCost += DELAY_BASE_PENALTY + (delayHours * DELAY_HOUR_PENALTY * dynamicFactor);
                }
                
                // Enhanced stopover penalties based on route complexity
                double stopoverMultiplier = Math.pow(1.2, segments.size() - 1); // Exponential penalty for more stopovers
                totalCost += segments.size() * STOPOVER_PENALTY * stopoverMultiplier;
            }
        }
        
        return totalCost;
    }
}