package pe.edu.pucp.morapack.algos.entities;

import pe.edu.pucp.morapack.model.Order;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase base que representa una solución al problema de planificación de rutas.
 * Contiene la información básica de rutas y órdenes.
 */
public class Solution {
    protected List<Order> completedOrders;
    protected List<Order> allOrders;

    public Solution() {
        this.completedOrders = new ArrayList<>();
        this.allOrders = new ArrayList<>();
    }

    public Solution(Solution other) {
        this.completedOrders = new ArrayList<>(other.completedOrders);
        this.allOrders = new ArrayList<>(other.allOrders);
    }

    public Solution(List<Order> orders) {
        this();
        this.allOrders = new ArrayList<>(orders);
    }

    public List<Order> getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(List<Order> completedOrders) {
        this.completedOrders = completedOrders;
    }

    public List<Order> getAllOrders() {
        return allOrders;
    }

    public void setAllOrders(List<Order> allOrders) {
        this.allOrders = allOrders;
    }

    @Override
    public String toString() {
        return "Solution{orders=" + allOrders.size() + "}";
    }
}