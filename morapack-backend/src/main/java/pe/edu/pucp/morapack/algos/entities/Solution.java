package pe.edu.pucp.morapack.algos.entities;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase base que representa una solución al problema de planificación de rutas.
 * Contiene la información básica de rutas y órdenes.
 */
public class Solution {
    protected List<PlannerOrder> completedOrders;
    protected List<PlannerOrder> allOrders;

    public Solution() {
        this.completedOrders = new ArrayList<>();
        this.allOrders = new ArrayList<>();
    }

    public Solution(Solution other) {
        this.completedOrders = new ArrayList<>(other.completedOrders);
        this.allOrders = new ArrayList<>(other.allOrders);
    }

    public Solution(List<PlannerOrder> orders) {
        this();
        this.allOrders = new ArrayList<>(orders);
    }

    public List<PlannerOrder> getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(List<PlannerOrder> completedOrders) {
        this.completedOrders = completedOrders;
    }

    public List<PlannerOrder> getAllOrders() {
        return allOrders;
    }

    public void setAllOrders(List<PlannerOrder> allOrders) {
        this.allOrders = allOrders;
    }

    @Override
    public String toString() {
        return "Solution{orders=" + allOrders.size() + "}";
    }
}