package pe.edu.pucp.morapack.algos.algorithm.tabu.moves;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;

/**
 * Movimiento: Dividir un shipment en dos.
 * Ejemplo: PlannerShipment(200 productos) → PS1(120) + PS2(80)
 */
public class SplitShipmentMove extends TabuMoveBase {
    private PlannerShipment shipment;
    private int splitQuantity;  // Cantidad para el nuevo shipment
    private int nextShipmentId;
    
    public SplitShipmentMove(PlannerShipment shipment, int splitQuantity, int nextShipmentId) {
        super("SPLIT");
        this.shipment = shipment;
        this.splitQuantity = splitQuantity;
        this.nextShipmentId = nextShipmentId;
    }
    
    @Override
    public void apply(TabuSolution solution) {
        // Validar que hay suficiente cantidad
        if (shipment.getQuantity() <= splitQuantity) {
            return;  // No se puede dividir
        }
        
        // Reducir cantidad del original
        int originalQuantity = shipment.getQuantity();
        shipment.setQuantity(originalQuantity - splitQuantity);
        
        // Crear nuevo shipment con la misma ruta
        PlannerShipment newShipment = new PlannerShipment(
            nextShipmentId,
            shipment.getOrder(),
            shipment.getFlights(),  // Misma ruta
            splitQuantity
        );
        
        solution.addPlannerShipment(newShipment);
    }
    
    @Override
    public String getMoveKey() {
        return String.format("SPLIT_%d_%d", shipment.getId(), splitQuantity);
    }
    
    @Override
    public String toString() {
        return String.format("SplitShipmentMove{shipment=%d, split=%d, newQty=%d}",
            shipment.getId(), splitQuantity, shipment.getQuantity() - splitQuantity);
    }
}

