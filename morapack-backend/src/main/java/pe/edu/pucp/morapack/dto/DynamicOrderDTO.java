package pe.edu.pucp.morapack.dto;

/**
 * DTO para transferir información de pedidos dinámicos al frontend.
 */
public class DynamicOrderDTO {
    
    private String id;
    private String type;                    // "SCHEDULED" o "MANUAL"
    private String status;                  // "PENDING", "INJECTED", "ASSIGNED", etc.
    
    // Datos del pedido
    private String origin;
    private String destination;
    private int quantity;
    private int deadlineHours;
    
    // Tiempos
    private String injectionTime;           // ISO 8601
    private String injectedTime;            // ISO 8601 (null si no inyectado)
    private String deadline;                // ISO 8601 (calculado tras inyección)
    
    // Metadatos
    private String reason;
    private boolean autoAssigned;
    private String errorMessage;
    private Integer systemOrderId;
    
    // Descripción visual
    private String description;             // "SPIM → EBCI (250 units, 48h deadline)"
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════
    
    public DynamicOrderDTO() {
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
    
    public String getOrigin() {
        return origin;
    }
    
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public int getDeadlineHours() {
        return deadlineHours;
    }
    
    public void setDeadlineHours(int deadlineHours) {
        this.deadlineHours = deadlineHours;
    }
    
    public String getInjectionTime() {
        return injectionTime;
    }
    
    public void setInjectionTime(String injectionTime) {
        this.injectionTime = injectionTime;
    }
    
    public String getInjectedTime() {
        return injectedTime;
    }
    
    public void setInjectedTime(String injectedTime) {
        this.injectedTime = injectedTime;
    }
    
    public String getDeadline() {
        return deadline;
    }
    
    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public boolean isAutoAssigned() {
        return autoAssigned;
    }
    
    public void setAutoAssigned(boolean autoAssigned) {
        this.autoAssigned = autoAssigned;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getSystemOrderId() {
        return systemOrderId;
    }
    
    public void setSystemOrderId(Integer systemOrderId) {
        this.systemOrderId = systemOrderId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}


