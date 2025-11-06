package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "flight")
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "origin_code")
    private String originCode;

    @Column(name = "destination_code")
    private String destinationCode;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "capacity")
    private Integer capacity;

    // domain fields kept for compatibility with algos domain; not persisted here
    private transient Airport origin;
    private transient Airport destination;

    public Flight() {}

    // convenience constructor for domain usage
    public Flight(Integer id, String originCode, String destinationCode, 
                  LocalTime departureTime, LocalTime arrivalTime, Integer capacity) {
        this.id = id;
        this.originCode = originCode;
        this.destinationCode = destinationCode;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.capacity = capacity;
    }

    // JPA-friendly getters/setters for persisted columns
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getOriginCode() { return originCode; }
    public void setOriginCode(String originCode) { this.originCode = originCode; }

    public String getDestinationCode() { return destinationCode; }
    public void setDestinationCode(String destinationCode) { this.destinationCode = destinationCode; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    // Domain accessors (transient)
    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }

    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }
}
