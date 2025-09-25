package pe.edu.pucp.morapack.algos.algorithm.tabu;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.stream.Collectors;

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
public class TabuSearchPlanner implements IOptimizer {
    private final TabuSearchConfig config;
    private final TabuSearchConstraints constraints;
    private final int tabuListSize;
    private final int maxIterations;
    private final int maxIterationsWithoutImprovement;
    private static final double UTILIZATION_TARGET = 0.75; // Target load factor 75%

    public TabuSearchPlanner() {
        this(10, 100, 20);
    }
    
    public TabuSearchPlanner(int tabuSize, int maxIter, int maxWithoutImprovement) {
        this.tabuListSize = tabuSize;
        this.maxIterations = maxIter;
        this.maxIterationsWithoutImprovement = maxWithoutImprovement;
        this.config = new TabuSearchConfig();
        this.constraints = new TabuSearchConstraints(this.config);
    }

    @Override
    public Solution optimize(List<Order> orders, List<Flight> flights, List<Airport> airports) {
        if (orders == null || orders.isEmpty()) return new Solution();
        
        // First convert orders to shipments
        List<Shipment> shipments = partitionOrdersIntoShipments(orders, flights);
        
        Solution currentSolution = generateInitialSolution(shipments, flights);
        Solution bestGlobalSolution = new Solution(currentSolution);
        Queue<TabuMove> tabuList = new LinkedList<>();
        int iterationsWithoutImprovement = 0;

        // Create handler for cancellations
        TabuSearchCancellationHandler cancellationHandler = new TabuSearchCancellationHandler(config, constraints);

        // Filter out cancelled flights
        List<Flight> activeFlights = flights.stream()
            .filter(f -> f.getStatus() != Flight.Status.CANCELLED)
            .toList();

        for (int i = 0; i < maxIterations; i++) {
            List<Solution> neighborhood = generateNeighborhood(currentSolution, activeFlights);
            if(neighborhood.isEmpty() && i == 0) return currentSolution;

            Solution bestIterationNeighbor = null;
            double bestNeighborCost = Double.POSITIVE_INFINITY;

            for (Solution neighbor : neighborhood) {
                TabuMove move = deduceMove(currentSolution, neighbor);
                double neighborCost = TabuSearchPlannerCostFunction.calculateCost(neighbor, flights, airports, i, maxIterations);
                
                if (tabuList.contains(move)) {
                    if (neighborCost < TabuSearchPlannerCostFunction.calculateCost(bestGlobalSolution, flights, airports, i, maxIterations)) {
                        // Aspiration Criteria
                    } else {
                        continue;
                    }
                }
                if (neighborCost < bestNeighborCost) {
                    bestNeighborCost = neighborCost;
                    bestIterationNeighbor = neighbor;
                }
            }

            if (bestIterationNeighbor != null) {
                currentSolution = bestIterationNeighbor;
                TabuMove movePerformed = deduceMove(currentSolution, bestIterationNeighbor);
                if(movePerformed != null) {
                    tabuList.add(movePerformed);
                    if (tabuList.size() > tabuListSize) {
                        tabuList.poll();
                    }
                }
            }

            if (TabuSearchPlannerCostFunction.calculateCost(currentSolution, flights, airports, i, maxIterations) < TabuSearchPlannerCostFunction.calculateCost(bestGlobalSolution, flights, airports, i, maxIterations)) {
                bestGlobalSolution = new Solution(currentSolution);
                iterationsWithoutImprovement = 0;
            } else {
                iterationsWithoutImprovement++;
            }

            if (iterationsWithoutImprovement >= maxIterationsWithoutImprovement) {
                currentSolution = diversify(bestGlobalSolution, shipments, flights);
                iterationsWithoutImprovement = 0;
                tabuList.clear();
            }
            
            // Verificar si hay vuelos cancelados y replanificar si es necesario
            boolean hasCancellations = currentSolution.getRouteMap().values().stream()
                .flatMap(r -> r.getSegments().stream())
                .anyMatch(s -> s.getFlight().getStatus() == Flight.Status.CANCELLED);
            
            if (hasCancellations) {
                currentSolution = cancellationHandler.handleCancellation(null, currentSolution, activeFlights);
                // Después de replanificar, actualizar la mejor solución si es necesario
                double currentCost = TabuSearchPlannerCostFunction.calculateCost(currentSolution, activeFlights, airports, i, maxIterations);
                if (currentCost < TabuSearchPlannerCostFunction.calculateCost(bestGlobalSolution, activeFlights, airports, i, maxIterations)) {
                    bestGlobalSolution = new Solution(currentSolution);
                    iterationsWithoutImprovement = 0;
                }
            }
        }
        return bestGlobalSolution;
    }
    
    private boolean canUseFlight(Flight flight, Shipment shipment, Map<Flight,Integer> currentLoads) {
        // Check current load plus new shipment against capacity
        int potentialLoad = currentLoads.getOrDefault(flight, 0) + shipment.getQuantity();
        if (potentialLoad > flight.getCapacity()) {
            return false;
        }

        // Flight must depart after order time and arrive within max time limit
        LocalDateTime orderTime = shipment.getParentOrder().getOrderTime();
        long maxHours = shipment.isInterContinental() ? 72 : 48;

        return !flight.getDepartureTime().isBefore(orderTime) &&
               ChronoUnit.HOURS.between(orderTime, flight.getArrivalTime().plusHours(2)) <= maxHours;
    }

    private Solution generateInitialSolution(List<Shipment> shipments, List<Flight> flights) {
        Solution sol = new Solution();
        Map<Flight, Integer> currentLoads = new HashMap<>();
        
        // Sort shipments by priority (urgent first, then by size)
        List<Shipment> sortedShipments = new ArrayList<>(shipments);
        sortedShipments.sort((a, b) -> {
            // First by urgency (earlier order time = more urgent)
            int timeCompare = a.getParentOrder().getOrderTime().compareTo(b.getParentOrder().getOrderTime());
            if (timeCompare != 0) return timeCompare;
            // Then by size (larger shipments first)
            return Integer.compare(b.getQuantity(), a.getQuantity());
        });
        
        for(Shipment s : sortedShipments) {
            List<Flight> availableFlights = flights.stream()
                .filter(f -> canUseFlight(f, s, currentLoads))
                .sorted((a, b) -> {
                    // First by utilization (prefer flights closer to target utilization)
                    double utilizationA = (double) (currentLoads.getOrDefault(a, 0) + s.getQuantity()) / a.getCapacity();
                    double utilizationB = (double) (currentLoads.getOrDefault(b, 0) + s.getQuantity()) / b.getCapacity();
                    double diffToTargetA = Math.abs(utilizationA - UTILIZATION_TARGET);
                    double diffToTargetB = Math.abs(utilizationB - UTILIZATION_TARGET);
                    int utilCompare = Double.compare(diffToTargetA, diffToTargetB);
                    if (utilCompare != 0) return utilCompare;
                    
                    // Then by departure time (earlier better)
                    return a.getDepartureTime().compareTo(b.getDepartureTime());
                })
                .collect(Collectors.toList());

            // Find direct flight if possible
            Optional<Flight> directFlight = availableFlights.stream()
                .filter(f -> f.getOrigin().equals(s.getOrigin()) &&
                           f.getDestination().equals(s.getDestination()))
                .findFirst();

            if (directFlight.isPresent()) {
                PlannerRoute route = new PlannerRoute();
                route.getSegments().add(new PlannerSegment(directFlight.get()));
                sol.getRouteMap().put(s, route);
                currentLoads.merge(directFlight.get(), s.getQuantity(), Integer::sum);
                continue;
            }

            // Try one-stop flights
            LocalDateTime orderTime = s.getParentOrder().getOrderTime();
            long maxHours = s.isInterContinental() ? 72 : 48;
            boolean foundRoute = false;

            for (Flight firstLeg : availableFlights) {
                if (!firstLeg.getOrigin().equals(s.getOrigin())) continue;

                List<Flight> secondLegCandidates = availableFlights.stream()
                    .filter(f -> f.getOrigin().equals(firstLeg.getDestination()) &&
                               f.getDestination().equals(s.getDestination()) &&
                               ChronoUnit.HOURS.between(firstLeg.getArrivalTime(), f.getDepartureTime()) >= 1 && // Min 1 hour connection
                               ChronoUnit.HOURS.between(orderTime, f.getArrivalTime().plusHours(2)) <= maxHours) // Within time limit
                    .toList();

                for (Flight secondLeg : secondLegCandidates) {
                    // Verify total load
                    int firstLegLoad = currentLoads.getOrDefault(firstLeg, 0) + s.getQuantity();
                    int secondLegLoad = currentLoads.getOrDefault(secondLeg, 0) + s.getQuantity();

                    if (firstLegLoad <= firstLeg.getCapacity() && secondLegLoad <= secondLeg.getCapacity()) {
                        PlannerRoute route = new PlannerRoute();
                        route.getSegments().add(new PlannerSegment(firstLeg));
                        route.getSegments().add(new PlannerSegment(secondLeg));
                        sol.getRouteMap().put(s, route);
                        currentLoads.merge(firstLeg, s.getQuantity(), Integer::sum);
                        currentLoads.merge(secondLeg, s.getQuantity(), Integer::sum);
                        foundRoute = true;
                        break;
                    }
                }
                if (foundRoute) break;
            }
        }
        return sol;
    }

    private List<Shipment> partitionOrdersIntoShipments(List<Order> orders, List<Flight> flights) {
        List<Shipment> shipments = new ArrayList<>();
        int shipmentIdCounter = 100;

        for (Order o : orders) {
            int remainingQuantity = o.getTotalQuantity();

            // Strategy: Try to fill direct flights first
            List<Flight> directFlights = flights.stream()
                .filter(f -> f.getOrigin().equals(o.getOrigin()) && f.getDestination().equals(o.getDestination()))
                .sorted(Comparator.comparingInt(Flight::getCapacity).reversed()) // Use largest first
                .collect(Collectors.toList());

            for (Flight direct : directFlights) {
                if (remainingQuantity > 0) {
                    int quantityToShip = Math.min(remainingQuantity, direct.getCapacity());
                    shipments.add(new Shipment(shipmentIdCounter++, o, quantityToShip, o.getOrigin(), o.getDestination()));
                    remainingQuantity -= quantityToShip;
                }
            }

            // If products still remain, partition for multi-stop routes
            while (remainingQuantity > 0) {
                int referenceCapacity = 200; // Bottleneck capacity
                int quantityInThisShipment = Math.min(remainingQuantity, referenceCapacity);
                shipments.add(new Shipment(shipmentIdCounter++, o, quantityInThisShipment, o.getOrigin(), o.getDestination()));
                remainingQuantity -= quantityInThisShipment;
            }
        }
        return shipments;
    }
    
    private List<Solution> generateNeighborhood(Solution current, List<Flight> flights) {
        List<Solution> neighbors = new ArrayList<>();
        
        // For each shipment
        for (Map.Entry<Shipment, PlannerRoute> entry : current.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute currentRoute = entry.getValue();
            
            // Try direct routes first
            List<Flight> directFlights = flights.stream()
                .filter(f -> f.getOrigin().equals(shipment.getOrigin()) && 
                            f.getDestination().equals(shipment.getDestination()) &&
                            f.getCapacity() >= shipment.getQuantity())
                .collect(Collectors.toList());

            for (Flight direct : directFlights) {
                PlannerRoute newRoute = new PlannerRoute();
                newRoute.getSegments().add(new PlannerSegment(direct));
                if (!newRoute.equals(currentRoute)) {
                    Solution neighbor = new Solution(current);
                    neighbor.getRouteMap().put(shipment, newRoute);
                    neighbors.add(neighbor);
                }
            }

            // Try 1-stop routes
            for (Flight f1 : flights) {
                if (f1.getOrigin().equals(shipment.getOrigin()) && f1.getCapacity() >= shipment.getQuantity()) {
                    for (Flight f2 : flights) {
                        if (f2.getOrigin().equals(f1.getDestination()) && 
                            f2.getDestination().equals(shipment.getDestination()) && 
                            f2.getCapacity() >= shipment.getQuantity() && 
                            f1.getArrivalTime().isBefore(f2.getDepartureTime())) {
                                PlannerRoute newRoute = new PlannerRoute();
                                newRoute.getSegments().add(new PlannerSegment(f1));
                                newRoute.getSegments().add(new PlannerSegment(f2));
                                if (!newRoute.equals(currentRoute)) {
                                    Solution neighbor = new Solution(current);
                                    neighbor.getRouteMap().put(shipment, newRoute);
                                    neighbors.add(neighbor);
                                }
                        }
                    }
                }
            }

            // Add empty route as last resort
            PlannerRoute emptyRoute = new PlannerRoute();
            if (!emptyRoute.equals(currentRoute)) {
                Solution neighbor = new Solution(current);
                neighbor.getRouteMap().put(shipment, emptyRoute);
                neighbors.add(neighbor);
            }
        }
        
        return neighbors;
    }
    
    private TabuMove deduceMove(Solution base, Solution neighbor) {
        if (base == null || neighbor == null) return null;
        
        // Find the shipment and route that changed
        for (Map.Entry<Shipment, PlannerRoute> entry : neighbor.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute newRoute = entry.getValue();
            PlannerRoute oldRoute = base.getRouteMap().get(shipment);
            
            if (!Objects.equals(newRoute, oldRoute)) {
                return new TabuMove(shipment, oldRoute, newRoute);
            }
        }
        
        return null;
    }
    
    private Solution diversify(Solution bestSolution, List<Shipment> shipments, List<Flight> flights) { 
        return generateInitialSolution(shipments, flights); 
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
}