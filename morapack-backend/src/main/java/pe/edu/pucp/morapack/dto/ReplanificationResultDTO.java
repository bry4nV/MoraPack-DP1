package pe.edu.pucp.morapack.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO para transferir resultados de replanificación al frontend.
 */
public class ReplanificationResultDTO {

    private String id;
    private String status;                      // "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"

    // Causa
    private String cancellationId;
    private String cancelledFlightId;

    // Tiempos
    private String triggeredTime;               // ISO 8601
    private String startedTime;                 // ISO 8601
    private String completedTime;               // ISO 8601
    private long executionTimeMs;

    // Pedidos y envíos
    private List<Integer> affectedOrderIds;
    private int totalAffectedProducts;
    private int cancelledShipmentsCount;
    private int newShipmentsCount;

    // Resultado
    private boolean successful;
    private String errorMessage;
    private double reassignmentRate;            // Porcentaje de reasignación

    // Resumen visual
    private String summary;                     // Texto descriptivo

    // 🆕 Tracking detallado de productos por pedido
    private Map<Integer, Integer> productsToReassign;    // Productos esperados a reasignar por pedido
    private Map<Integer, Integer> productsReassigned;     // Productos efectivamente reasignados por pedido
    private Map<Integer, Integer> productsPending;        // Productos pendientes por pedido
    private int totalProductsPending;                     // Total de productos pendientes
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════
    
    public ReplanificationResultDTO() {
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCancellationId() {
        return cancellationId;
    }
    
    public void setCancellationId(String cancellationId) {
        this.cancellationId = cancellationId;
    }
    
    public String getCancelledFlightId() {
        return cancelledFlightId;
    }
    
    public void setCancelledFlightId(String cancelledFlightId) {
        this.cancelledFlightId = cancelledFlightId;
    }
    
    public String getTriggeredTime() {
        return triggeredTime;
    }
    
    public void setTriggeredTime(String triggeredTime) {
        this.triggeredTime = triggeredTime;
    }
    
    public String getStartedTime() {
        return startedTime;
    }
    
    public void setStartedTime(String startedTime) {
        this.startedTime = startedTime;
    }
    
    public String getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(String completedTime) {
        this.completedTime = completedTime;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public List<Integer> getAffectedOrderIds() {
        return affectedOrderIds;
    }
    
    public void setAffectedOrderIds(List<Integer> affectedOrderIds) {
        this.affectedOrderIds = affectedOrderIds;
    }
    
    public int getTotalAffectedProducts() {
        return totalAffectedProducts;
    }
    
    public void setTotalAffectedProducts(int totalAffectedProducts) {
        this.totalAffectedProducts = totalAffectedProducts;
    }
    
    public int getCancelledShipmentsCount() {
        return cancelledShipmentsCount;
    }
    
    public void setCancelledShipmentsCount(int cancelledShipmentsCount) {
        this.cancelledShipmentsCount = cancelledShipmentsCount;
    }
    
    public int getNewShipmentsCount() {
        return newShipmentsCount;
    }
    
    public void setNewShipmentsCount(int newShipmentsCount) {
        this.newShipmentsCount = newShipmentsCount;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public double getReassignmentRate() {
        return reassignmentRate;
    }
    
    public void setReassignmentRate(double reassignmentRate) {
        this.reassignmentRate = reassignmentRate;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }

    // 🆕 Getters y setters para tracking detallado
    public Map<Integer, Integer> getProductsToReassign() {
        return productsToReassign;
    }

    public void setProductsToReassign(Map<Integer, Integer> productsToReassign) {
        this.productsToReassign = productsToReassign;
    }

    public Map<Integer, Integer> getProductsReassigned() {
        return productsReassigned;
    }

    public void setProductsReassigned(Map<Integer, Integer> productsReassigned) {
        this.productsReassigned = productsReassigned;
    }

    public Map<Integer, Integer> getProductsPending() {
        return productsPending;
    }

    public void setProductsPending(Map<Integer, Integer> productsPending) {
        this.productsPending = productsPending;
    }

    public int getTotalProductsPending() {
        return totalProductsPending;
    }

    public void setTotalProductsPending(int totalProductsPending) {
        this.totalProductsPending = totalProductsPending;
    }
}


