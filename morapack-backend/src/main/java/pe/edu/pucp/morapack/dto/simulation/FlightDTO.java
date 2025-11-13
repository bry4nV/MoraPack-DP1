package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Flight information in simulation context.
 */
public class FlightDTO {
    @JsonProperty("code")
    public String code;

    @JsonProperty("origin")
    public AirportDTO origin;

    @JsonProperty("destination")
    public AirportDTO destination;

    @JsonProperty("scheduledDepartureISO")
    public String scheduledDepartureISO;

    @JsonProperty("scheduledArrivalISO")
    public String scheduledArrivalISO;

    @JsonProperty("capacity")
    public int capacity;

    @JsonProperty("preplanned")
    public boolean preplanned;

    @JsonProperty("status")
    public String status;

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public AirportDTO getOrigin() {
        return origin;
    }

    public void setOrigin(AirportDTO origin) {
        this.origin = origin;
    }

    public AirportDTO getDestination() {
        return destination;
    }

    public void setDestination(AirportDTO destination) {
        this.destination = destination;
    }

    public String getScheduledDepartureISO() {
        return scheduledDepartureISO;
    }

    public void setScheduledDepartureISO(String scheduledDepartureISO) {
        this.scheduledDepartureISO = scheduledDepartureISO;
    }

    public String getScheduledArrivalISO() {
        return scheduledArrivalISO;
    }

    public void setScheduledArrivalISO(String scheduledArrivalISO) {
        this.scheduledArrivalISO = scheduledArrivalISO;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isPreplanned() {
        return preplanned;
    }

    public void setPreplanned(boolean preplanned) {
        this.preplanned = preplanned;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
