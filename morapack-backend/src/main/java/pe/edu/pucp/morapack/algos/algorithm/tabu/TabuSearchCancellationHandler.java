package pe.edu.pucp.morapack.algos.algorithm.tabu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;
import pe.edu.pucp.morapack.algos.entities.Solution;

public class TabuSearchCancellationHandler {
    private final TabuSearchConfig config;
    private final TabuSearchConstraints constraints;

    public TabuSearchCancellationHandler(TabuSearchConfig config, TabuSearchConstraints constraints) {
        this.config = config;
        this.constraints = constraints;
    }

    public Solution handleCancellation(Flight cancelledFlight, Solution currentSolution, List<Flight> availableFlights) {
        Solution newSolution = new Solution(currentSolution);
        List<Shipment> affectedShipments = findAffectedShipments(cancelledFlight, currentSolution);

        // Marcar el vuelo como cancelado
        cancelledFlight.setStatus(Flight.Status.CANCELLED);

        // Replanificar cada envío afectado
        for (Shipment shipment : affectedShipments) {
            PlannerRoute currentRoute = currentSolution.getRouteMap().get(shipment);
            PlannerRoute newRoute = findAlternativeRoute(shipment, cancelledFlight, currentRoute, availableFlights);
            newSolution.getRouteMap().put(shipment, newRoute);
        }

        return newSolution;
    }

    private List<Shipment> findAffectedShipments(Flight cancelledFlight, Solution solution) {
        List<Shipment> affected = new ArrayList<>();
        
        for (Map.Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            PlannerRoute route = entry.getValue();
            if (route.getSegments().stream()
                    .anyMatch(segment -> segment.getFlight().equals(cancelledFlight))) {
                affected.add(entry.getKey());
            }
        }
        
        return affected;
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

                        // Verificar viabilidad de la ruta con las restricciones existentes
                        Solution tempSolution = new Solution(currentRoute);
                        tempSolution.getRouteMap().put(shipment, tempRoute);
                        
                        if (constraints.isSolutionFeasible(tempSolution)) {
                            return tempRoute;
                        }
                    }
                }
            }
        } else {
            // Si hay vuelos directos disponibles, usar el primero viable
            Flight directFlight = directFlights.get(0);
            newRoute.getSegments().add(new PlannerSegment(directFlight));
            
            Solution tempSolution = new Solution(currentRoute);
            tempSolution.getRouteMap().put(shipment, newRoute);
            
            if (constraints.isSolutionFeasible(tempSolution)) {
                return newRoute;
            }
        }

        // Si no se encuentra una ruta alternativa viable, retornar una ruta vacía
        return new PlannerRoute();
    }
}