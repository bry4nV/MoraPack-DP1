package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal flight segment information for order tracking.
 * Contains just enough data to show the route without storing full itinerary details.
 */
public class FlightSegmentInfo {
    @JsonProperty("flightCode")
    public String flightCode;       // "VU81-001"

    @JsonProperty("originCode")
    public String originCode;       // "SPIM"

    @JsonProperty("destinationCode")
    public String destinationCode;  // "SCEL"

    // Constructor vacío para Jackson
    public FlightSegmentInfo() {}

    public FlightSegmentInfo(String flightCode, String originCode, String destinationCode) {
        this.flightCode = flightCode;
        this.originCode = originCode;
        this.destinationCode = destinationCode;
    }
}
