package pe.edu.pucp.morapack.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una tarea de replanificación de pedidos afectados por una cancelación.
 */
public class ReplanificationTask {
    
    /**
     * Estado de la replanificación
     */
    public enum ReplanificationStatus {
        PENDING,        // Pendiente de ejecutar
        IN_PROGRESS,    // Ejecutándose
        COMPLETED,      // Completada exitosamente
        FAILED          // Falló
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CAMPOS
    // ═══════════════════════════════════════════════════════════════
    
    private String id;
    private ReplanificationStatus status;
    
    // Causa de la replanificación
    private String cancellationId;              // ID de la cancelación que la disparó
    private String cancelledFlightId;           // ID del vuelo cancelado
    
    // Tiempos
    private LocalDateTime triggeredTime;        // Cuándo se disparó
    private LocalDateTime startedTime;          // Cuándo comenzó a ejecutarse
    private LocalDateTime completedTime;        // Cuándo terminó
    private long executionTimeMs;               // Tiempo de ejecución en ms
    
    // Pedidos afectados
    private List<Integer> affectedOrderIds;     // IDs de pedidos afectados
    private int totalAffectedProducts;          // Total de productos afectados
    
    // Envíos
    private int cancelledShipmentsCount;        // Envíos cancelados
    private int newShipmentsCount;              // Nuevos envíos creados
    
    // Resultado
    private boolean successful;                 // Si fue exitosa
    private String errorMessage;                // Mensaje de error (si falló)
    private double reassignmentRate;            // % de productos reasignados
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════
    
    public ReplanificationTask() {
        this.status = ReplanificationStatus.PENDING;
        this.affectedOrderIds = new ArrayList<>();
        this.successful = false;
    }
    
    public ReplanificationTask(String cancellationId, String cancelledFlightId, LocalDateTime triggeredTime) {
        this();
        this.id = generateId(cancellationId, triggeredTime);
        this.cancellationId = cancellationId;
        this.cancelledFlightId = cancelledFlightId;
        this.triggeredTime = triggeredTime;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Marca la replanificación como iniciada
     */
    public void markAsStarted(LocalDateTime startedTime) {
        this.status = ReplanificationStatus.IN_PROGRESS;
        this.startedTime = startedTime;
    }
    
    /**
     * Marca la replanificación como completada
     */
    public void markAsCompleted(
            LocalDateTime completedTime,
            int cancelledShipments,
            int newShipments,
            int totalProducts) {
        this.status = ReplanificationStatus.COMPLETED;
        this.completedTime = completedTime;
        this.cancelledShipmentsCount = cancelledShipments;
        this.newShipmentsCount = newShipments;
        this.totalAffectedProducts = totalProducts;
        this.successful = true;
        
        if (startedTime != null) {
            this.executionTimeMs = java.time.Duration.between(startedTime, completedTime).toMillis();
        }
        
        // Calcular tasa de reasignación
        if (totalProducts > 0) {
            this.reassignmentRate = (double) newShipments / totalProducts * 100.0;
        }
    }
    
    /**
     * Marca la replanificación como fallida
     */
    public void markAsFailed(LocalDateTime failedTime, String errorMessage) {
        this.status = ReplanificationStatus.FAILED;
        this.completedTime = failedTime;
        this.errorMessage = errorMessage;
        this.successful = false;
        
        if (startedTime != null) {
            this.executionTimeMs = java.time.Duration.between(startedTime, failedTime).toMillis();
        }
    }
    
    /**
     * Agrega pedidos afectados
     */
    public void addAffectedOrders(List<Integer> orderIds) {
        this.affectedOrderIds.addAll(orderIds);
    }
    
    /**
     * Genera un ID único para la replanificación
     */
    private static String generateId(String cancellationId, LocalDateTime triggeredTime) {
        return String.format("REPLAN_%s_%s",
            cancellationId,
            triggeredTime.toString().replace(":", "").replace("-", "").substring(0, 13)
        );
    }
    
    /**
     * Resumen de la replanificación
     */
    public String getSummary() {
        return String.format(
            "Replanification[%s | Orders: %d | Products: %d | Cancelled: %d | New: %d | Rate: %.1f%% | %dms]",
            status,
            affectedOrderIds.size(),
            totalAffectedProducts,
            cancelledShipmentsCount,
            newShipmentsCount,
            reassignmentRate,
            executionTimeMs
        );
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
    
    public ReplanificationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReplanificationStatus status) {
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
    
    public LocalDateTime getTriggeredTime() {
        return triggeredTime;
    }
    
    public void setTriggeredTime(LocalDateTime triggeredTime) {
        this.triggeredTime = triggeredTime;
    }
    
    public LocalDateTime getStartedTime() {
        return startedTime;
    }
    
    public void setStartedTime(LocalDateTime startedTime) {
        this.startedTime = startedTime;
    }
    
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(LocalDateTime completedTime) {
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
    
    @Override
    public String toString() {
        return getSummary();
    }
}


