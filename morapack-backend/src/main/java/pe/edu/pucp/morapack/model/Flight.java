package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalDate; // <-- IMPORTADO PARA LA NUEVA COLUMNA
import java.time.LocalTime;

@Entity
@Table(name = "flight")
public class Flight {

    // --- CAMPOS PERSISTIDOS (SINCRONIZADOS CON LA NUEVA BD) ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // <-- CAMBIADO (antes "idVuelo")
    private Long id;

    @Column(name = "airport_origin_code") // <-- CAMBIADO (antes "idAeropuertoOrigen")
    private String airportOriginCode; // Renombré la variable para que coincida

    @Column(name = "airport_destination_code") // <-- CAMBIADO (antes "idAeropuertoDestino")
    private String airportDestinationCode; // Renombré la variable

    @Column(name = "flight_date") // <-- ¡NUEVO CAMPO!
    private LocalDate flightDate;

    @Column(name = "departure_time") // <-- CAMBIADO (antes "horaSalida")
    private LocalTime departureTime;

    @Column(name = "arrival_time") // <-- CAMBIADO (antes "horaLlegada")
    private LocalTime arrivalTime;

    @Column(name = "capacity") // <-- CAMBIADO (antes "capacidad")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status") // <-- CAMBIADO (antes "estado")
    private FlightStatus status;

    // --- CAMPOS TRANSIENT (TU LÓGICA DE DOMINIO - CONSERVADA) ---
    private transient Airport origin;
    private transient Airport destination;

    
    // --- CONSTRUCTOR (CONSERVADO) ---
    public Flight() {}

    
    // --- GETTERS/SETTERS PERSISTIDOS (ACTUALIZADOS) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAirportOriginCode() { return airportOriginCode; }
    public void setAirportOriginCode(String airportOriginCode) { this.airportOriginCode = airportOriginCode; }

    public String getAirportDestinationCode() { return airportDestinationCode; }
    public void setAirportDestinationCode(String airportDestinationCode) { this.airportDestinationCode = airportDestinationCode; }

    public LocalDate getFlightDate() { return flightDate; }
    public void setFlightDate(LocalDate flightDate) { this.flightDate = flightDate; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }

    
    // --- GETTERS/SETTERS DE DOMINIO (CONSERVADOS) ---

    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }

    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }
}