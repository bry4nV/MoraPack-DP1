package pe.edu.pucp.morapack.dto;

import pe.edu.pucp.morapack.model.FlightStatus;

public class FlightDto {
    private Long id;
    private String originAirportId; // ← CAMBIO
    private String destinationAirportId; // ← CAMBIO
    private String departureTime; // ← CAMBIO
    private String arrivalTime; // ← CAMBIO
    private Integer capacity; // ← CAMBIO
    private FlightStatus status; // ← CAMBIO

    public FlightDto() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginAirportId() { return originAirportId; }
    public void setOriginAirportId(String originAirportId) { this.originAirportId = originAirportId; }

    public String getDestinationAirportId() { return destinationAirportId; }
    public void setDestinationAirportId(String destinationAirportId) { this.destinationAirportId = destinationAirportId; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }
}
