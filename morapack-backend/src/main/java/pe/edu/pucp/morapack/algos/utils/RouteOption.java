package pe.edu.pucp.morapack.algos.utils;

import java.time.temporal.ChronoUnit;
import java.util.List;

import pe.edu.pucp.morapack.algos.entities.PlannerFlight;

import java.util.ArrayList;

/**
 * Representa una opción de ruta (secuencia de vuelos) con métricas calculadas.
 * Usado para comparar y ordenar rutas durante la generación de solución inicial.
 */
public class RouteOption {
    private List<PlannerFlight> flights;
    private double cost;
    private long totalTravelMinutes;
    private int minCapacity;  // Cuello de botella
    
    public RouteOption(List<PlannerFlight> flights) {
        this.flights = new ArrayList<>(flights);
        this.cost = calculateRouteCost();
        this.totalTravelMinutes = calculateTravelTime();
        this.minCapacity = Integer.MAX_VALUE;  // Se actualizará externamente
    }
    
    public List<PlannerFlight> getFlights() {
        return new ArrayList<>(flights);
    }
    
    public double getCost() {
        return cost;
    }
    
    public long getTotalTravelMinutes() {
        return totalTravelMinutes;
    }
    
    public int getMinCapacity() {
        return minCapacity;
    }
    
    public void setMinCapacity(int capacity) {
        this.minCapacity = capacity;
    }
    
    /**
     * ¿Es ruta directa?
     */
    public boolean isDirect() {
        return flights.size() == 1;
    }
    
    /**
     * Número de escalas
     */
    public int getNumberOfStops() {
        return Math.max(0, flights.size() - 1);
    }
    
    /**
     * Calcular costo de la ruta (suma de costos de vuelos + penalización por escalas)
     */
    private double calculateRouteCost() {
        double totalCost = flights.stream()
            .mapToDouble(PlannerFlight::getCost)
            .sum();
        
        // Penalizar escalas (preferir rutas directas)
        int stops = getNumberOfStops();
        totalCost += stops * 500;  // 500 por cada escala
        
        return totalCost;
    }
    
    /**
     * Calcular tiempo total de viaje en minutos
     */
    private long calculateTravelTime() {
        if (flights.isEmpty()) return 0;
        
        return ChronoUnit.MINUTES.between(
            flights.get(0).getDepartureTime(),
            flights.get(flights.size() - 1).getArrivalTime()
        );
    }
    
    /**
     * Comparar rutas por prioridad:
     * 1. Rutas directas primero
     * 2. Menor tiempo de viaje
     * 3. Menor costo
     */
    public int compareTo(RouteOption other) {
        // 1. Preferir directas sobre conexiones
        if (this.isDirect() && !other.isDirect()) return -1;
        if (!this.isDirect() && other.isDirect()) return 1;
        
        // 2. Menor tiempo de viaje
        int timeCompare = Long.compare(this.totalTravelMinutes, other.totalTravelMinutes);
        if (timeCompare != 0) return timeCompare;
        
        // 3. Menor costo
        return Double.compare(this.cost, other.cost);
    }
    
    @Override
    public String toString() {
        String type = isDirect() ? "DIRECT" : String.format("WITH %d STOP(S)", getNumberOfStops());
        return String.format("RouteOption{%s, flights=%d, time=%dmin, cost=%.2f, capacity=%d}",
            type, flights.size(), totalTravelMinutes, cost, minCapacity);
    }
}

