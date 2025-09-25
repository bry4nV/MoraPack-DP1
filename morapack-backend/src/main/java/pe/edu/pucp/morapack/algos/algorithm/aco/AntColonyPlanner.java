package pe.edu.pucp.morapack.algos.algorithm.aco;

import org.springframework.stereotype.Service;

import pe.edu.pucp.morapack.algos.algorithm.IOptimizer;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Shipment;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AntColonyPlanner implements IOptimizer {
    private static final int COLONY_SIZE = 20;
    private static final int MAX_ITERATIONS = 100;
    private static final double ALPHA = 1.0; // Pheromone importance
    private static final double BETA = 2.0;  // Heuristic importance
    private static final double RHO = 0.1;   // Evaporation rate
    private static final double Q = 100.0;   // Pheromone deposit factor
    
    private Map<Flight, Map<Flight, Double>> pheromones;
    private Map<Flight, Map<Flight, Double>> heuristics;
    private Random random;
    
    public AntColonyPlanner() {
        this.random = new Random();
    }
    
    @Override
    public Solution optimize(List<Order> orders, List<Flight> flights, List<Airport> airports) {
        // Convert orders to shipments
        List<Shipment> shipments = partitionOrdersIntoShipments(orders);
        
        // Initialize pheromone trails and heuristic information
        initializePheromones(flights);
        initializeHeuristics(flights);
        
        Solution bestSolution = null;
        double bestCost = Double.POSITIVE_INFINITY;
        
        // Main ACO loop
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            List<Solution> antSolutions = new ArrayList<>();
            
            // Each ant constructs a solution
            for (int ant = 0; ant < COLONY_SIZE; ant++) {
                Solution solution = constructSolution(shipments, flights, airports);
                antSolutions.add(solution);
                
                double solutionCost = AntColonyPlannerCostFunction.calculateCost(solution, flights, airports);
                if (solutionCost < bestCost) {
                    bestCost = solutionCost;
                    bestSolution = new Solution(solution);
                }
            }
            
            // Update pheromone trails
            evaporatePheromones();
            for (Solution solution : antSolutions) {
                updatePheromones(solution, AntColonyPlannerCostFunction.calculateCost(solution, flights, airports));
            }
        }
        
        return bestSolution != null ? bestSolution : new Solution();
    }
    
    private List<Shipment> partitionOrdersIntoShipments(List<Order> orders) {
        List<Shipment> shipments = new ArrayList<>();
        int shipmentIdCounter = 100;
        
        for (Order order : orders) {
            int remainingQuantity = order.getTotalQuantity();
            
            while (remainingQuantity > 0) {
                int quantityForThisShipment = Math.min(remainingQuantity, 200);
                shipments.add(new Shipment(
                    shipmentIdCounter++, 
                    order, 
                    quantityForThisShipment,
                    order.getOrigin(),
                    order.getDestination()
                ));
                remainingQuantity -= quantityForThisShipment;
            }
        }
        
        return shipments;
    }
    
    private void initializePheromones(List<Flight> flights) {
        pheromones = new HashMap<>();
        for (Flight from : flights) {
            Map<Flight, Double> connections = new HashMap<>();
            for (Flight to : flights) {
                if (from.getDestination().equals(to.getOrigin()) &&
                    from.getArrivalTime().isBefore(to.getDepartureTime())) {
                    connections.put(to, 1.0);
                }
            }
            pheromones.put(from, connections);
        }
    }
    
    private void initializeHeuristics(List<Flight> flights) {
        heuristics = new HashMap<>();
        for (Flight from : flights) {
            Map<Flight, Double> connections = new HashMap<>();
            for (Flight to : flights) {
                if (from.getDestination().equals(to.getOrigin()) &&
                    from.getArrivalTime().isBefore(to.getDepartureTime())) {
                    // Simple heuristic: prefer shorter connection times
                    double waitTime = to.getDepartureTime().getHour() - from.getArrivalTime().getHour();
                    connections.put(to, 1.0 / (1.0 + waitTime));
                }
            }
            heuristics.put(from, connections);
        }
    }
    
    private Solution constructSolution(List<Shipment> shipments, List<Flight> flights, List<Airport> airports) {
        Solution solution = new Solution();
        
        for (Shipment shipment : shipments) {
            PlannerRoute route = constructRoute(shipment, flights);
            solution.getRouteMap().put(shipment, route);
        }
        
        return solution;
    }
    
    private PlannerRoute constructRoute(Shipment shipment, List<Flight> flights) {
        PlannerRoute route = new PlannerRoute();
        Airport current = shipment.getOrigin();
        
        // Try to build route with maximum 2 segments
        List<Flight> possibleFirst = flights.stream()
            .filter(f -> f.getOrigin().equals(current) && f.getCapacity() >= shipment.getQuantity())
            .collect(Collectors.toList());
            
        if (possibleFirst.isEmpty()) return route;
        
        Flight firstFlight = selectNextFlight(null, possibleFirst, shipment.getDestination());
        route.getSegments().add(new PlannerSegment(firstFlight));
        
        if (firstFlight.getDestination().equals(shipment.getDestination())) {
            return route;
        }
        
        List<Flight> possibleSecond = flights.stream()
            .filter(f -> f.getOrigin().equals(firstFlight.getDestination()) && 
                        f.getDestination().equals(shipment.getDestination()) &&
                        f.getCapacity() >= shipment.getQuantity() &&
                        f.getDepartureTime().isAfter(firstFlight.getArrivalTime()))
            .collect(Collectors.toList());
            
        if (!possibleSecond.isEmpty()) {
            Flight secondFlight = selectNextFlight(firstFlight, possibleSecond, shipment.getDestination());
            route.getSegments().add(new PlannerSegment(secondFlight));
        } else {
            route.getSegments().clear();
        }
        
        return route;
    }
    
    private Flight selectNextFlight(Flight current, List<Flight> candidates, Airport destination) {
        double[] probabilities = new double[candidates.size()];
        double total = 0.0;
        
        for (int i = 0; i < candidates.size(); i++) {
            Flight candidate = candidates.get(i);
            double pheromone = current == null ? 1.0 : 
                pheromones.get(current).getOrDefault(candidate, 0.0);
            double heuristic = current == null ? 1.0 : 
                heuristics.get(current).getOrDefault(candidate, 0.0);
                
            probabilities[i] = Math.pow(pheromone, ALPHA) * Math.pow(heuristic, BETA);
            total += probabilities[i];
        }
        
        if (total == 0.0) return candidates.get(0);
        
        double r = random.nextDouble() * total;
        double sum = 0.0;
        for (int i = 0; i < candidates.size(); i++) {
            sum += probabilities[i];
            if (r <= sum) return candidates.get(i);
        }
        
        return candidates.get(candidates.size() - 1);
    }
    
    private void evaporatePheromones() {
        for (Map<Flight, Double> connections : pheromones.values()) {
            for (Map.Entry<Flight, Double> entry : connections.entrySet()) {
                connections.put(entry.getKey(), entry.getValue() * (1.0 - RHO));
            }
        }
    }
    
    private void updatePheromones(Solution solution, double cost) {
        if (cost == 0.0) return;
        double deposit = Q / cost;
        
        for (PlannerRoute route : solution.getRouteMap().values()) {
            List<PlannerSegment> segments = route.getSegments();
            for (int i = 0; i < segments.size() - 1; i++) {
                Flight from = segments.get(i).getFlight();
                Flight to = segments.get(i + 1).getFlight();
                double currentPheromone = pheromones.get(from).get(to);
                pheromones.get(from).put(to, currentPheromone + deposit);
            }
        }
    }
}