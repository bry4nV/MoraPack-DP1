package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Flight;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Solución del Tabu Search basada en PlannerShipments.
 * 
 * Cada PlannerShipment representa un envío de N productos siguiendo una ruta específica.
 * Un Order puede tener múltiples PlannerShipments (diferentes rutas, diferentes cantidades).
 */
public class TabuSolution extends Solution {
    private List<PlannerShipment> plannerShipments;

    public TabuSolution() {
        super();
        this.plannerShipments = new ArrayList<>();
    }

    public TabuSolution(Solution solution) {
        super(solution);
        this.plannerShipments = new ArrayList<>();
        if (solution instanceof TabuSolution) {
            TabuSolution tabuSol = (TabuSolution) solution;
            // Copia profunda de shipments
            this.plannerShipments = tabuSol.getPlannerShipments().stream()
                .map(PlannerShipment::new)  // Constructor de copia
                .collect(Collectors.toList());
        }
    }

    // ========== Getters/Setters ==========

    public List<PlannerShipment> getPlannerShipments() {
        return plannerShipments != null ? plannerShipments : new ArrayList<>();
    }

    public void setPlannerShipments(List<PlannerShipment> plannerShipments) {
        this.plannerShipments = plannerShipments != null ? plannerShipments : new ArrayList<>();
    }

    public void addPlannerShipment(PlannerShipment shipment) {
        if (this.plannerShipments == null) {
            this.plannerShipments = new ArrayList<>();
        }
        this.plannerShipments.add(shipment);
    }
    
    public void addAllPlannerShipments(List<PlannerShipment> shipments) {
        if (this.plannerShipments == null) {
            this.plannerShipments = new ArrayList<>();
        }
        this.plannerShipments.addAll(shipments);
    }

    public void removePlannerShipment(PlannerShipment shipment) {
        if (this.plannerShipments != null) {
            this.plannerShipments.remove(shipment);
        }
    }
    
    // ========== Métodos de Consulta ==========
    
    /**
     * Obtener todos los shipments de un Order específico
     */
    public List<PlannerShipment> getShipmentsForOrder(Order order) {
        return plannerShipments.stream()
            .filter(ps -> ps.getOrder().equals(order))
            .collect(Collectors.toList());
    }
    
    /**
     * Calcular cuántos productos de un Order están asignados
     */
    public int getAssignedQuantityForOrder(Order order) {
        return plannerShipments.stream()
            .filter(ps -> ps.getOrder().equals(order))
            .mapToInt(PlannerShipment::getQuantity)
            .sum();
    }
    
    /**
     * Verificar si un Order está completamente asignado
     */
    public boolean isOrderFullyAssigned(Order order) {
        int assigned = getAssignedQuantityForOrder(order);
        return assigned >= order.getTotalQuantity();
    }
    
    /**
     * Calcular carga de un vuelo específico
     */
    public int getFlightLoad(Flight flight) {
        return plannerShipments.stream()
            .filter(ps -> ps.getFlights().contains(flight))
            .mapToInt(PlannerShipment::getQuantity)
            .sum();
    }
    
    /**
     * Obtener estadísticas de la solución
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalOrders = (int) plannerShipments.stream()
            .map(ps -> ps.getOrder().getId())
            .distinct()
            .count();
        
        int totalShipments = plannerShipments.size();
        
        int directShipments = (int) plannerShipments.stream()
            .filter(PlannerShipment::isDirect)
            .count();
        
        int connectionShipments = totalShipments - directShipments;
        
        int totalProducts = plannerShipments.stream()
            .mapToInt(PlannerShipment::getQuantity)
            .sum();
        
        stats.put("totalOrders", totalOrders);
        stats.put("totalShipments", totalShipments);
        stats.put("directShipments", directShipments);
        stats.put("connectionShipments", connectionShipments);
        stats.put("totalProducts", totalProducts);
        
        return stats;
    }
    
    @Override
    public String toString() {
        Map<String, Object> stats = getStatistics();
        return String.format("TabuSolution{orders=%d, shipments=%d, products=%d, direct=%d, connections=%d}",
            stats.get("totalOrders"),
            stats.get("totalShipments"),
            stats.get("totalProducts"),
            stats.get("directShipments"),
            stats.get("connectionShipments"));
    }
}
