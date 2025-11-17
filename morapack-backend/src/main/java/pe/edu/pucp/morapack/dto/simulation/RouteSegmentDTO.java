package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for a segment of a shipment route in simulation context.
 * Each segment represents one flight in the multi-hop journey.
 */
public class RouteSegmentDTO {
    @JsonProperty("order")
    public int order;

    @JsonProperty("flight")
    public FlightDTO flight;
}
