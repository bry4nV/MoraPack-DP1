package pe.edu.pucp.morapack.dto;

import pe.edu.pucp.morapack.model.FlightStatus;
import java.time.LocalTime; 

public class FlightDto {

    // --- CAMPOS SINCRONIZADOS ---
    
    private Long id;
    private String airportOriginCode;    // <-- CAMBIADO (antes originAirportId)
    private String airportDestinationCode; // <-- CAMBIADO (antes destinationAirportId)
    private String flightDate;           // <-- ¡AÑADIDO!
    private String departureTime;
    private String arrivalTime;
    private Integer capacity;
    private FlightStatus status; // Esto está bien porque tu enum ya coincidía

    public FlightDto() {}

    // --- Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAirportOriginCode() { return airportOriginCode; }
    public void setAirportOriginCode(String airportOriginCode) { this.airportOriginCode = airportOriginCode; }

    public String getAirportDestinationCode() { return airportDestinationCode; }
    public void setAirportDestinationCode(String airportDestinationCode) { this.airportDestinationCode = airportDestinationCode; }

    public String getFlightDate() { return flightDate; }
    public void setFlightDate(String flightDate) { this.flightDate = flightDate; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }
}