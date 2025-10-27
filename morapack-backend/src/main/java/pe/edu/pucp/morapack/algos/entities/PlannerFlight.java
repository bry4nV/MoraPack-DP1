package pe.edu.pucp.morapack.algos.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.lang.Math;

public class PlannerFlight {
    public enum Status {
        SCHEDULED,   // Vuelo programado normal
        DELAYED,     // Vuelo con retraso
        CANCELLED,   // Vuelo cancelado
        COMPLETED    // Vuelo ya realizado
    }

    private String code;
    private PlannerAirport origin;
    private PlannerAirport destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int capacity;
    private boolean preplanned;
    private Status status;
    private double cost;
    
    private static int flightIdCounter = 1;
    private static final double BASE_COST = 500.0;  // Costo base para cualquier vuelo
    private static final double DISTANCE_FACTOR = 0.5;  // Factor de costo por kilómetro
    private static final double CAPACITY_FACTOR = 1.0;  // Factor de costo por unidad de capacidad

    private static double calculateCost(PlannerAirport origin, PlannerAirport destination, int capacity) {
        // Calcular distancia usando la fórmula del haversine
        double lat1 = Math.toRadians(origin.getLatitude());
        double lon1 = Math.toRadians(origin.getLongitude());
        double lat2 = Math.toRadians(destination.getLatitude());
        double lon2 = Math.toRadians(destination.getLongitude());

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat/2), 2) + 
                   Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon/2), 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double distance = 6371 * c; // Radio de la Tierra en kilómetros

        // El costo se calcula como:
        // Costo base + (distancia * factor_distancia) + (capacidad * factor_capacidad)
        return BASE_COST + (distance * DISTANCE_FACTOR) + (capacity * CAPACITY_FACTOR);
    }

    public PlannerFlight(int id, PlannerAirport origin, PlannerAirport destination, 
                 LocalDateTime departure, LocalDateTime arrival, int capacity, double cost) {
        this.code = String.format("F%s%s%03d", 
            origin.getCode().substring(0, 2),
            destination.getCode().substring(0, 2),
            id);
        this.cost = cost;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departure;
        this.arrivalTime = arrival;
        this.capacity = capacity;
        this.preplanned = false;
        this.status = Status.SCHEDULED;
    }

    public PlannerFlight(PlannerAirport origin, PlannerAirport destination, 
                 LocalDateTime departure, LocalDateTime arrival, int capacity) {
        this(flightIdCounter++, origin, destination, departure, arrival, capacity, calculateCost(origin, destination, capacity));
    }
    
    public PlannerFlight(String code, PlannerAirport origin, PlannerAirport destination, 
                 LocalDateTime departure, LocalDateTime arrival, int capacity) {
        this.code = code;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departure;
        this.arrivalTime = arrival;
        this.capacity = capacity;
        this.cost = calculateCost(origin, destination, capacity);
        this.preplanned = true;
        this.status = Status.SCHEDULED;
    }

    public double getCost() {
        return cost;
    }

    public PlannerAirport getOrigin() { return origin; }
    public PlannerAirport getDestination() { return destination; }
    public String getCode() { return code; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public int getCapacity() { return capacity; }
    public boolean isPreplanned() { return preplanned; }
    public void setPreplanned(boolean preplanned) { this.preplanned = preplanned; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(code, ((PlannerFlight) o).code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    public boolean isOnDay(LocalDate day) {
    return departureTime != null && departureTime.toLocalDate().isEqual(day);
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }

    public boolean isActive() {
        return this.status != Status.CANCELLED && this.status != Status.COMPLETED;
    }

    
    public void resetDailyStatus() {
        if (this.status == Status.COMPLETED) return; 
        this.status = Status.SCHEDULED;
    }

    

}