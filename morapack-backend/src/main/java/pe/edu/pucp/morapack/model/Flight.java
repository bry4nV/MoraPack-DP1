package pe.edu.pucp.morapack.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Flight {
    public enum Status {
        SCHEDULED,   // Vuelo programado normal
        DELAYED,     // Vuelo con retraso
        CANCELLED,   // Vuelo cancelado
        COMPLETED    // Vuelo ya realizado
    }

    private String code;
    private Airport origin;
    private Airport destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private int capacity;
    private boolean preplanned;
    private Status status;
    
    private static int flightIdCounter = 1;

    public Flight(int id, Airport origin, Airport destination, 
                 LocalDateTime departure, LocalDateTime arrival, int capacity) {
        this.code = String.format("F%s%s%03d", 
            origin.getCode().substring(0, 2),
            destination.getCode().substring(0, 2),
            id);
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departure;
        this.arrivalTime = arrival;
        this.capacity = capacity;
        this.preplanned = false;
        this.status = Status.SCHEDULED;
    }

    public Flight(Airport origin, Airport destination, 
                 LocalDateTime departure, LocalDateTime arrival, int capacity) {
        this(flightIdCounter++, origin, destination, departure, arrival, capacity);
    }
    
    public Flight(String code, Airport origin, Airport destination, 
                 LocalDateTime departure, LocalDateTime arrival, int capacity) {
        this.code = code;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departure;
        this.arrivalTime = arrival;
        this.capacity = capacity;
        this.preplanned = false;
    }

    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
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
        return Objects.equals(code, ((Flight) o).code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}