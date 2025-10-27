package pe.edu.pucp.morapack.algos.algorithm.tabu.moves;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;

/**
 * Movimiento: Transferir N productos de un shipment a otro.
 * Ejemplo: PS1(100) y PS2(50) → PS1(80) y PS2(70) [transferir 20]
 */
public class TransferQuantityMove extends TabuMoveBase {
    private PlannerShipment fromShipment;
    private PlannerShipment toShipment;
    private int quantity;
    
    public TransferQuantityMove(PlannerShipment from, PlannerShipment to, int quantity) {
        super("TRANSFER");
        this.fromShipment = from;
        this.toShipment = to;
        this.quantity = quantity;
    }
    
    @Override
    public void apply(TabuSolution solution) {
        // Validar que son del mismo Order
        if (!fromShipment.getOrder().equals(toShipment.getOrder())) {
            return;
        }
        
        // Validar que 'from' tiene suficientes productos
        if (fromShipment.getQuantity() < quantity) {
            return;
        }
        
        // Validar que 'to' tiene capacidad en todos sus vuelos
        for (PlannerFlight flight : toShipment.getFlights()) {
            int currentLoad = solution.getFlightLoad(flight);
            if (currentLoad + quantity > flight.getCapacity()) {
                return;  // No hay capacidad
            }
        }
        
        // Realizar transferencia
        fromShipment.setQuantity(fromShipment.getQuantity() - quantity);
        toShipment.setQuantity(toShipment.getQuantity() + quantity);
        
        // Si 'from' queda vacío, eliminarlo
        if (fromShipment.getQuantity() == 0) {
            solution.removePlannerShipment(fromShipment);
        }
    }
    
    @Override
    public String getMoveKey() {
        return String.format("TRANSFER_%d_%d_%d", 
            fromShipment.getId(), toShipment.getId(), quantity);
    }
    
    @Override
    public String toString() {
        return String.format("TransferQuantityMove{from=%d, to=%d, qty=%d}",
            fromShipment.getId(), toShipment.getId(), quantity);
    }
}

