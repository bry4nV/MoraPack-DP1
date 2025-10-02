package pe.edu.pucp.morapack.algos.algorithm.tabu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;
import pe.edu.pucp.morapack.algos.entities.Solution;

public class TabuSearchCancellationHandler {
    private final TabuSearchConstraints constraints;

    public TabuSearchCancellationHandler(TabuSearchConstraints constraints) {
        this.constraints = constraints;
    }

    public Solution handleCancellation(Flight cancelledFlight, Solution currentSolution, List<Flight> availableFlights) {
        Solution newSolution = new Solution(currentSolution);
        List<Shipment> affectedShipments = findAffectedShipments(cancelledFlight, currentSolution);

        // Marcar el vuelo como cancelado
        cancelledFlight.setStatus(Flight.Status.CANCELLED);

        // Replanificar cada envío afectado
        for (Shipment shipment : affectedShipments) {
            PlannerRoute currentRoute = findRouteForShipment(currentSolution, shipment);
            if (currentRoute != null) {
                PlannerRoute newRoute = findAlternativeRoute(shipment, cancelledFlight, currentRoute, availableFlights);
                updateRouteInSolution(newSolution, shipment, newRoute);
            }
        }

        return newSolution;
    }

    private List<Shipment> findAffectedShipments(Flight cancelledFlight, Solution solution) {
        List<Shipment> affected = new ArrayList<>();
        
        for (PlannerRoute route : solution.getRoutes()) {
            if (route.getSegments().stream()
                    .anyMatch(segment -> segment.getFlight().equals(cancelledFlight))) {
                affected.addAll(route.getShipments());
            }
        }
        
        return affected;
    }

    private PlannerRoute findRouteForShipment(Solution solution, Shipment shipment) {
        for (PlannerRoute route : solution.getRoutes()) {
            if (route.getShipments().contains(shipment)) {
                return route;
            }
        }
        return null;
    }

    private void updateRouteInSolution(Solution solution, Shipment shipment, PlannerRoute newRoute) {
        // Primero, remover el envío de su ruta actual si existe
        for (PlannerRoute route : solution.getRoutes()) {
            if (route.getShipments().contains(shipment)) {
                route.getShipments().remove(shipment);
                // Si la ruta queda vacía, removerla de la solución
                if (route.getShipments().isEmpty()) {
                    solution.getRoutes().remove(route);
                }
                break;
            }
        }

        // Luego, agregar el envío a la nueva ruta
        if (newRoute != null) {
            // Buscar si ya existe una ruta con el mismo vuelo
            for (PlannerRoute route : solution.getRoutes()) {
                if (route.getFlight().equals(newRoute.getFlight())) {
                    route.getShipments().add(shipment);
                    return;
                }
            }
            // Si no existe, agregar la nueva ruta
            newRoute.getShipments().add(shipment);
            solution.getRoutes().add(newRoute);
        }
    }

    private PlannerRoute findAlternativeRoute(
            Shipment shipment, 
            Flight cancelledFlight,
            PlannerRoute currentRoute,
            List<Flight> availableFlights) {
        
        LocalDateTime cancellationTime = LocalDateTime.now();
        PlannerRoute newRoute = new PlannerRoute();

        // Buscar vuelos alternativos que cumplan con las restricciones
        List<Flight> validReplacements = availableFlights.stream()
            .filter(f -> f.getStatus() == Flight.Status.SCHEDULED)
            .filter(f -> f.getCapacity() >= shipment.getQuantity())
            .filter(f -> !f.equals(cancelledFlight))
            .filter(f -> f.getDepartureTime().isAfter(cancellationTime))
            .toList();

        // Intentar encontrar una ruta directa primero
        List<Flight> directFlights = validReplacements.stream()
            .filter(f -> f.getOrigin().equals(shipment.getOrigin()) &&
                        f.getDestination().equals(shipment.getDestination()))
            .toList();

        // Si no hay rutas directas, buscar rutas con una escala
        if (directFlights.isEmpty()) {
            for (Flight firstLeg : validReplacements) {
                if (!firstLeg.getOrigin().equals(shipment.getOrigin())) continue;

                for (Flight secondLeg : validReplacements) {
                    if (secondLeg.getOrigin().equals(firstLeg.getDestination()) &&
                        secondLeg.getDestination().equals(shipment.getDestination()) &&
                        secondLeg.getDepartureTime().isAfter(firstLeg.getArrivalTime())) {

                        // Crear ruta temporal y verificar restricciones
                        PlannerRoute tempRoute = new PlannerRoute();
                        tempRoute.getSegments().add(new PlannerSegment(firstLeg));
                        tempRoute.getSegments().add(new PlannerSegment(secondLeg));
                        tempRoute.getShipments().add(shipment);

                        // Verificar viabilidad de la ruta con las restricciones existentes
                        Solution tempSolution = new Solution();
                        tempSolution.getRoutes().add(tempRoute);
                        
                        if (constraints.isSolutionFeasible(tempSolution)) {
                            return tempRoute;
                        }
                    }
                }
            }
        } else {
            // Si hay vuelos directos disponibles, usar el primero viable
            Flight directFlight = directFlights.get(0);
            newRoute = new PlannerRoute(directFlight);
            newRoute.getShipments().add(shipment);
            
            Solution tempSolution = new Solution();
            tempSolution.getRoutes().add(newRoute);
            
            if (constraints.isSolutionFeasible(tempSolution)) {
                return newRoute;
            }
        }

        // Si no se encuentra una ruta alternativa viable, retornar una ruta vacía
        return new PlannerRoute();
    }
}