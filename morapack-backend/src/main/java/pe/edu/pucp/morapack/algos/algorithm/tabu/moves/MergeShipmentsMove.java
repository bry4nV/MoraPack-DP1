package pe.edu.pucp.morapack.algos.algorithm.tabu.moves;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;

/**
 * Movimiento: Fusionar dos shipments del mismo Order y misma ruta.
 * Ejemplo: PS1(60 productos) + PS2(40 productos) → PS1(100 productos)
 */
public class MergeShipmentsMove extends TabuMoveBase {
    private PlannerShipment shipment1;
    private PlannerShipment shipment2;
    
    public MergeShipmentsMove(PlannerShipment shipment1, PlannerShipment shipment2) {
        super("MERGE");
        this.shipment1 = shipment1;
        this.shipment2 = shipment2;
    }
    
    @Override
    public void apply(TabuSolution solution) {
        // Validar que son del mismo Order
        if (!shipment1.getOrder().equals(shipment2.getOrder())) {
            return;
        }
        
        // Validar que tienen la misma ruta
        if (!shipment1.getFlights().equals(shipment2.getFlights())) {
            return;
        }
        
        // Combinar cantidades en shipment1
        int totalQuantity = shipment1.getQuantity() + shipment2.getQuantity();
        shipment1.setQuantity(totalQuantity);
        
        // Eliminar shipment2
        solution.removePlannerShipment(shipment2);
    }
    
    @Override
    public String getMoveKey() {
        int id1 = Math.min(shipment1.getId(), shipment2.getId());
        int id2 = Math.max(shipment1.getId(), shipment2.getId());
        return String.format("MERGE_%d_%d", id1, id2);
    }
    
    @Override
    public String toString() {
        return String.format("MergeShipmentsMove{shipment1=%d(%d), shipment2=%d(%d)}",
            shipment1.getId(), shipment1.getQuantity(),
            shipment2.getId(), shipment2.getQuantity());
    }
}

