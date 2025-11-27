package pe.edu.pucp.morapack.dto;

/**
 * DTO para transferir información de cancelaciones de vuelo al frontend.
 */
public class FlightCancellationDTO {
    
    private String id;
    private String type;                        // "SCHEDULED" o "MANUAL"
    private String status;                      // "PENDING", "EXECUTED", "FAILED", "CANCELLED"
    
    // Identificación del vuelo
    private String flightOrigin;
    private String flightDestination;
    private String scheduledDepartureTime;
    private String flightIdentifier;            // "SPIM-SEQM-03:34"
    
    // Tiempos
    private String cancellationTime;            // ISO 8601
    private String executedTime;                // ISO 8601 (null si no ejecutado)
    
    // Metadatos
    private String reason;
    private int affectedProductsCount;
    private boolean replanificationTriggered;
    private String errorMessage;

    // 🆕 Detalles de replanificación (incluye pedidos afectados, nuevos shipments, etc.)
    private ReplanificationResultDTO replanificationDetails;

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════

    public FlightCancellationDTO() {
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ═══════════════════════════════════════════════════════════════
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getFlightOrigin() {
        return flightOrigin;
    }
    
    public void setFlightOrigin(String flightOrigin) {
        this.flightOrigin = flightOrigin;
    }
    
    public String getFlightDestination() {
        return flightDestination;
    }
    
    public void setFlightDestination(String flightDestination) {
        this.flightDestination = flightDestination;
    }
    
    public String getScheduledDepartureTime() {
        return scheduledDepartureTime;
    }
    
    public void setScheduledDepartureTime(String scheduledDepartureTime) {
        this.scheduledDepartureTime = scheduledDepartureTime;
    }
    
    public String getFlightIdentifier() {
        return flightIdentifier;
    }
    
    public void setFlightIdentifier(String flightIdentifier) {
        this.flightIdentifier = flightIdentifier;
    }
    
    public String getCancellationTime() {
        return cancellationTime;
    }
    
    public void setCancellationTime(String cancellationTime) {
        this.cancellationTime = cancellationTime;
    }
    
    public String getExecutedTime() {
        return executedTime;
    }
    
    public void setExecutedTime(String executedTime) {
        this.executedTime = executedTime;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public int getAffectedProductsCount() {
        return affectedProductsCount;
    }
    
    public void setAffectedProductsCount(int affectedProductsCount) {
        this.affectedProductsCount = affectedProductsCount;
    }
    
    public boolean isReplanificationTriggered() {
        return replanificationTriggered;
    }
    
    public void setReplanificationTriggered(boolean replanificationTriggered) {
        this.replanificationTriggered = replanificationTriggered;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ReplanificationResultDTO getReplanificationDetails() {
        return replanificationDetails;
    }

    public void setReplanificationDetails(ReplanificationResultDTO replanificationDetails) {
        this.replanificationDetails = replanificationDetails;
    }
}


