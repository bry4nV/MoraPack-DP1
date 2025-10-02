package pe.edu.pucp.morapack.algos.entities;

import pe.edu.pucp.morapack.model.Order;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase base que representa una solución al problema de planificación de rutas.
 * Contiene la información básica de rutas y órdenes.
 */
public class Solution {
    protected List<PlannerRoute> routes;
    protected List<Order> completedOrders;
    protected List<Order> allOrders;

    public Solution() {
        this.routes = new ArrayList<>();
        this.completedOrders = new ArrayList<>();
        this.allOrders = new ArrayList<>();
    }

    public Solution(Solution other) {
        this.routes = other.routes.stream()
            .map(PlannerRoute::new)
            .collect(Collectors.toList());
        this.completedOrders = new ArrayList<>(other.completedOrders);
        this.allOrders = new ArrayList<>(other.allOrders);
    }

    public Solution(List<Order> orders) {
        this();
        this.allOrders = new ArrayList<>(orders);
    }

    public List<PlannerRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<PlannerRoute> routes) {
        this.routes = routes;
    }

    public void addRoute(PlannerRoute route) {
        this.routes.add(route);
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

    public List<PlannerRoute> getAssignedRoutes() {
        return routes.stream()
            .filter(r -> !r.getShipments().isEmpty())
            .collect(Collectors.toList());
    }

    public List<PlannerRoute> getEmptyRoutes() {
        return routes.stream()
            .filter(r -> r.getShipments().isEmpty())
            .collect(Collectors.toList());
    }

    public double getTotalCost() {
        return routes.stream()
            .flatMapToDouble(r -> r.getSegments().stream()
                .mapToDouble(s -> s.getFlight().getCost()))
            .sum();
    }

    @Override
    public String toString() {
        return "Solution{routes=" + routes.size() + "}";
    }
}