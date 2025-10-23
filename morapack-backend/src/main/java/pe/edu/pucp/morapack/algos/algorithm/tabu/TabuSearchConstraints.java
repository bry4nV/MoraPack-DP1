package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;

import java.util.*;

/**
 * Validador de restricciones para TabuSolution basado en PlannerShipments.
 * Nota: Las validaciones principales están en TabuSearchPlannerCostFunction.
 * Esta clase se mantiene para compatibilidad y validaciones rápidas.
 */
public class TabuSearchConstraints {

    public TabuSearchConstraints(TabuSearchConfig config) {
        // Config pasado para compatibilidad futura
    }

    /**
     * Evaluar restricciones de una solución
     */
    public double evaluateConstraints(TabuSolution solution) {
        // Delegar al cost function
        return TabuSearchPlannerCostFunction.calculateCost(
            solution, 
            new ArrayList<>(),  // flights (no usado en validación)
            new ArrayList<>(),  // airports (no usado en validación)
            0,                  // iteration
            1                   // maxIterations
        );
    }

    /**
     * Verificar si una solución es factible (sin violaciones críticas)
     */
    public boolean isSolutionFeasible(TabuSolution solution) {
        List<PlannerShipment> shipments = solution.getPlannerShipments();
        
        // 1. Verificar que todas las secuencias son válidas
        for (PlannerShipment shipment : shipments) {
            if (!shipment.isValidSequence()) {
                return false;
            }
        }
        
        // 2. Verificar capacidades de vuelos
        Map<PlannerFlight, Integer> flightLoads = new HashMap<>();
        for (PlannerShipment shipment : shipments) {
            for (PlannerFlight flight : shipment.getFlights()) {
                flightLoads.merge(flight, shipment.getQuantity(), Integer::sum);
            }
        }
        
        for (Map.Entry<PlannerFlight, Integer> entry : flightLoads.entrySet()) {
            if (entry.getValue() > entry.getKey().getCapacity()) {
                return false;  // Excede capacidad
            }
        }
        
        // 3. Verificar que todas las órdenes están asignadas
        for (PlannerShipment shipment : shipments) {
            if (!solution.isOrderFullyAssigned(shipment.getOrder())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Verificar si un movimiento es válido
     */
    public boolean isMoveFeasible(TabuSolution solution) {
        return isSolutionFeasible(solution);
    }
}
