package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for shipment itinerary in simulation context.
 * Represents the complete route of a shipment through multiple flight segments.
 */
public class ItineraryDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("orderId")
    public int orderId;

    @JsonProperty("segments")
    public RouteSegmentDTO[] segments;

    @JsonProperty("progressMeters")
    public double progressMeters;

    @JsonProperty("positionLon")
    public double positionLon;

    @JsonProperty("positionLat")
    public double positionLat;
}
