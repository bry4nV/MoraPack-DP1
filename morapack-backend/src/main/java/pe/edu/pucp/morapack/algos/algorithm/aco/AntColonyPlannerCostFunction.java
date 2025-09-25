package pe.edu.pucp.morapack.algos.algorithm.aco;

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

public class AntColonyPlannerCostFunction {
    public static double calculateCost(Solution solution, List<Flight> flights, List<Airport> airports) {
        double totalCost = 0.0;
        Map<Flight, Integer> loadPerFlight = new HashMap<>();
        
        // Calculate load per flight
        for (Map.Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute route = entry.getValue();
            
            for (PlannerSegment segment : route.getSegments()) {
                Flight flight = segment.getFlight();
                loadPerFlight.merge(flight, shipment.getQuantity(), Integer::sum);
            }
        }
        
        // Capacity violation penalty
        for (Map.Entry<Flight, Integer> entry : loadPerFlight.entrySet()) {
            if (entry.getValue() > entry.getKey().getCapacity()) {
                totalCost += 20000;
            }
        }
        
        // Empty routes and delays penalties
        for (Map.Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute route = entry.getValue();
            
            if (route.getSegments().isEmpty()) {
                totalCost += 50000;
                continue;
            }
            
            // Delay penalty
            long travelHours = ChronoUnit.HOURS.between(
                route.getInitialDepartureTime(),
                route.getFinalArrivalTime()
            );
            
            if (travelHours > shipment.getParentOrder().getMaxDeliveryHours()) {
                long delayHours = travelHours - shipment.getParentOrder().getMaxDeliveryHours();
                totalCost += 10000 + (delayHours * 100);
            }
            
            // Cost per stopover
            totalCost += route.getSegments().size() * 100;
        }
        
        return totalCost;
    }
}
