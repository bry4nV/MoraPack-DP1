package pe.edu.pucp.morapack.model.simulation;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad JPA para la tabla flight del esquema moraTravelSimulation.
 * Este esquema contiene datos históricos (~2 años) para simulaciones.
 */
@Entity
@Table(name = "flight", schema = "moraTravelSimulation")
public class SimFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "airport_origin_code", length = 4, nullable = false)
    private String airportOriginCode;

    @Column(name = "airport_destination_code", length = 4, nullable = false)
    private String airportDestinationCode;

    @Column(name = "flight_date", nullable = false)
    private LocalDate flightDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "status", nullable = false)
    private String status;

    // Constructors
    public SimFlight() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAirportOriginCode() {
        return airportOriginCode;
    }

    public void setAirportOriginCode(String airportOriginCode) {
        this.airportOriginCode = airportOriginCode;
    }

    public String getAirportDestinationCode() {
        return airportDestinationCode;
    }

    public void setAirportDestinationCode(String airportDestinationCode) {
        this.airportDestinationCode = airportDestinationCode;
    }

    public LocalDate getFlightDate() {
        return flightDate;
    }

    public void setFlightDate(LocalDate flightDate) {
        this.flightDate = flightDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
