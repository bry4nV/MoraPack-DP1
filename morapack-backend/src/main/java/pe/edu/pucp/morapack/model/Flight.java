package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "flight")
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idVuelo")
    private Long id;

    @Column(name = "idAeropuertoOrigen")
    private String originAirportId; // ← CAMBIO: de idAeropuertoOrigen a originAirportId

    @Column(name = "idAeropuertoDestino")
    private String destinationAirportId; // ← CAMBIO: de idAeropuertoDestino a destinationAirportId

    @Column(name = "horaSalida")
    private LocalTime departureTime; // ← CAMBIO: de horaSalida a departureTime

    @Column(name = "horaLlegada")
    private LocalTime arrivalTime; // ← CAMBIO: de horaLlegada a arrivalTime

    @Column(name = "capacidad")
    private Integer capacity; // ← CAMBIO: de capacidad a capacity

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "ENUM('SCHEDULED','DELAYED','CANCELLED','COMPLETED')")
    private FlightStatus status; // ← CAMBIO: de estado a status

    // domain fields kept for compatibility with algos domain; not persisted here
    private transient Airport origin;
    private transient Airport destination;

    public Flight() {}

    // JPA-friendly getters/setters for persisted columns
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginAirportId() { return originAirportId; }
    public void setOriginAirportId(String originAirportId) { this.originAirportId = originAirportId; }

    public String getDestinationAirportId() { return destinationAirportId; }
    public void setDestinationAirportId(String destinationAirportId) { this.destinationAirportId = destinationAirportId; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }

    // Domain accessors (transient)
    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }

    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }
}
