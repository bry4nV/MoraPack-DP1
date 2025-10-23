package pe.edu.pucp.morapack.algos.algorithm.tabu.moves;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;

import java.util.List;

/**
 * Movimiento: Cambiar la ruta completa de un shipment.
 * Ejemplo: PS ruta [LIM→MEX→MIA] → ruta [LIM→MIA] (cambio a directo)
 */
public class RerouteShipmentMove extends TabuMoveBase {
    private PlannerShipment shipment;
    private List<PlannerFlight> newRoute;
    
    public RerouteShipmentMove(PlannerShipment shipment, List<PlannerFlight> newRoute) {
        super("REROUTE");
        this.shipment = shipment;
        this.newRoute = newRoute;
    }
    
    @Override
    public void apply(TabuSolution solution) {
        // Validar que la nueva ruta tiene capacidad
        for (PlannerFlight flight : newRoute) {
            int currentLoad = solution.getFlightLoad(flight);
            // Restar la carga actual del shipment si ya está en ese vuelo
            if (shipment.getFlights().contains(flight)) {
                currentLoad -= shipment.getQuantity();
            }
            
            if (currentLoad + shipment.getQuantity() > flight.getCapacity()) {
                return;  // No hay capacidad
            }
        }
        
        // Cambiar la ruta
        shipment.setFlights(newRoute);
    }
    
    @Override
    public String getMoveKey() {
        String routeKey = newRoute.stream()
            .map(PlannerFlight::getCode)
            .reduce((a, b) -> a + "_" + b)
            .orElse("EMPTY");
        return String.format("REROUTE_%d_%s", shipment.getId(), routeKey);
    }
    
    @Override
    public String toString() {
        return String.format("RerouteShipmentMove{shipment=%d, newRoute=%d flights}",
            shipment.getId(), newRoute.size());
    }
}

