package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Airport information in simulation context.
 * Contains both static airport data and dynamic runtime metrics.
 */
public class AirportDTO {
    @JsonProperty("id")
    public int id;

    @JsonProperty("name")
    public String name;

    @JsonProperty("code")
    public String code;

    @JsonProperty("city")
    public String city;

    @JsonProperty("country")
    public String country;

    @JsonProperty("continent")
    public String continent;

    @JsonProperty("latitude")
    public double latitude;

    @JsonProperty("longitude")
    public double longitude;

    @JsonProperty("gmt")
    public int gmt;

    @JsonProperty("isHub")
    public boolean isHub;

    // Storage capacity information
    @JsonProperty("totalCapacity")
    public int totalCapacity;

    @JsonProperty("usedCapacity")
    public int usedCapacity;

    @JsonProperty("availableCapacity")
    public int availableCapacity;

    @JsonProperty("usagePercentage")
    public double usagePercentage;

    // Dynamic runtime information
    @JsonProperty("waitingOrders")
    public int waitingOrders;

    @JsonProperty("destinationOrders")
    public int destinationOrders;

    @JsonProperty("waitingProducts")
    public int waitingProducts;

    @JsonProperty("activeFlightsFrom")
    public int activeFlightsFrom;

    @JsonProperty("activeFlightsTo")
    public int activeFlightsTo;

    @JsonProperty("groundedFlights")
    public List<String> groundedFlights = new ArrayList<>();
}
