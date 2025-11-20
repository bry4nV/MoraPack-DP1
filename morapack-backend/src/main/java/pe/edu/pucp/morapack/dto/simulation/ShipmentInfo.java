package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single shipment (envío) within an order.
 * An order may be split into multiple shipments due to flight capacity constraints.
 *
 * Example:
 *   Order #100: 500 products Lima → Miami
 *
 *   Shipment #1: 200 products, route [LIM→MIA] (direct)
 *   Shipment #2: 150 products, route [LIM→MEX, MEX→MIA] (1 stopover)
 *   Shipment #3: 150 products, route [LIM→PTY, PTY→MIA] (1 stopover)
 */
public class ShipmentInfo {
    @JsonProperty("shipmentId")
    public int shipmentId;

    @JsonProperty("quantity")
    public int quantity;              // Number of products in THIS shipment

    @JsonProperty("route")
    public List<FlightSegmentInfo> route = new ArrayList<>();  // Flight segments forming the complete route

    @JsonProperty("isDirect")
    public boolean isDirect;          // true if route has only 1 flight

    @JsonProperty("numberOfStops")
    public int numberOfStops;         // Number of stopovers (route.size() - 1)

    // Empty constructor for Jackson
    public ShipmentInfo() {}

    public ShipmentInfo(int shipmentId, int quantity, List<FlightSegmentInfo> route) {
        this.shipmentId = shipmentId;
        this.quantity = quantity;
        this.route = route != null ? route : new ArrayList<>();
        this.isDirect = this.route.size() == 1;
        this.numberOfStops = Math.max(0, this.route.size() - 1);
    }
}