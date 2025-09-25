package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TabuSearchPlannerCostFunction {
    // Base penalties
    private static final double CAPACITY_VIOLATION_PENALTY = 20000;
    private static final double EMPTY_ROUTE_PENALTY = 50000;
    private static final double DELAY_BASE_PENALTY = 10000;
    private static final double DELAY_HOUR_PENALTY = 100;
    private static final double STOPOVER_PENALTY = 100;
    private static final double INVALID_STOPOVER_TIME_PENALTY = 15000;
    
    // Load balancing penalties
    private static final double LOAD_BALANCE_PENALTY = 5000;
    private static final double UTILIZATION_TARGET = 0.75; // Target load factor 75%
    private static final double UNDERUTILIZATION_PENALTY = 2000;  // Penalty for using too few flights
    private static final double DISTRIBUTION_PENALTY = 3000;  // Penalty for poor load distribution
    
    public static double calculateCost(Solution solution, List<Flight> flights, List<Airport> airports, int currentIteration, int maxIterations) {
        double totalCost = 0.0;
        Map<Flight, Integer> loadPerFlight = new HashMap<>();
        
        // Calculate load per flight
        for (PlannerRoute route : solution.getRouteMap().values()) {
            for (PlannerSegment segment : route.getSegments()) {
                Flight flight = segment.getFlight();
                Shipment associatedShipment = solution.getRouteMap().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(route))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
                
                if (associatedShipment != null) {
                    loadPerFlight.merge(flight, associatedShipment.getQuantity(), Integer::sum);
                }
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
        
        // Route validations and penalties
        for (Map.Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute route = entry.getValue();
            
            if (route.getSegments().isEmpty()) {
                totalCost += EMPTY_ROUTE_PENALTY;
                continue;
            }
            
            // Validate minimum stopover times (1 hour)
            List<PlannerSegment> segments = route.getSegments();
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
            
            // Delay penalty (considering destination timezone and 2-hour processing)
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
            
            // Check if max delivery time is exceeded
            long maxHours = shipment.isInterContinental() ? 72 : 48; // 3 days or 2 days
            if (totalHours > maxHours) {
                long delayHours = totalHours - maxHours;
                totalCost += DELAY_BASE_PENALTY + (delayHours * DELAY_HOUR_PENALTY);
            }
            
            // Cost per stopover (considering both flight changes and processing time)
            totalCost += segments.size() * STOPOVER_PENALTY;
        }
        
        return totalCost;
    }
}