package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary information for a single order in the simulation.
 * Used for both preview (before simulation) and real-time tracking (during simulation).
 */
public class OrderSummaryDTO {
    @JsonProperty("id")
    public int id;

    @JsonProperty("code")
    public String code;              // "PED-1234"

    // Origin and destination
    @JsonProperty("originCode")
    public String originCode;        // "SPIM"

    @JsonProperty("originName")
    public String originName;        // "Lima"

    @JsonProperty("destinationCode")
    public String destinationCode;   // "VABB"

    @JsonProperty("destinationName")
    public String destinationName;   // "Mumbai"

    // Quantities
    @JsonProperty("totalQuantity")
    public int totalQuantity;        // 45

    @JsonProperty("assignedQuantity")
    public int assignedQuantity;     // 34 (0 in preview mode)

    @JsonProperty("progressPercent")
    public double progressPercent;   // 75.5% (0 in preview mode)

    // Times
    @JsonProperty("requestDateISO")
    public String requestDateISO;    // "2025-12-01T08:00:00"

    @JsonProperty("etaISO")
    public String etaISO;            // Estimated delivery (null in preview mode)

    // Status
    @JsonProperty("status")
    public OrderStatus status;       // PENDING, IN_TRANSIT, COMPLETED, UNASSIGNED

    // Assigned flights (empty in preview mode)
    @JsonProperty("assignedFlights")
    public List<String> assignedFlights = new ArrayList<>(); // ["VU81-001", "VU81-002"]

    // Priority (optional, for future use)
    @JsonProperty("priority")
    public int priority = 3;         // 1-5 (3 = normal)

    public enum OrderStatus {
        PENDING,      // In queue, not assigned yet
        IN_TRANSIT,   // Partially or fully assigned, on the way
        COMPLETED,    // 100% delivered
        UNASSIGNED    // Could not be assigned
    }
}






