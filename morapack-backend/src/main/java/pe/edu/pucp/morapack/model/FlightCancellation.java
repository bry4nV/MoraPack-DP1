package pe.edu.pucp.morapack.model;

import java.time.LocalDateTime;

/**
 * Representa una cancelación de vuelo (programada o manual).
 * 
 * Una cancelación puede ser:
 * - Programada: Cargada desde archivo antes de la simulación
 * - Manual: Ejecutada por el usuario durante la simulación
 */
public class FlightCancellation {
    
    /**
     * Tipo de cancelación
     */
    public enum CancellationType {
        SCHEDULED,  // Programada (desde archivo)
        MANUAL      // Manual (desde UI)
    }
    
    /**
     * Estado de la cancelación
     */
    public enum CancellationStatus {
        PENDING,     // Pendiente de ejecución
        EXECUTED,    // Ejecutada correctamente
        FAILED,      // Falló (ej: vuelo ya en aire)
        CANCELLED    // Cancelación cancelada (usuario se arrepintió)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CAMPOS
    // ═══════════════════════════════════════════════════════════════
    
    private String id;                          // ID único de la cancelación
    private CancellationType type;              // Tipo de cancelación
    private CancellationStatus status;          // Estado actual
    
    // Identificación del vuelo a cancelar
    private String flightOrigin;                // Origen (IATA code)
    private String flightDestination;           // Destino (IATA code)
    private String scheduledDepartureTime;      // Hora programada del vuelo (HH:mm)
    
    // Momento de cancelación
    private LocalDateTime cancellationTime;     // Cuándo se debe cancelar
    private LocalDateTime executedTime;         // Cuándo se ejecutó realmente
    
    // Metadatos
    private String reason;                      // Razón de la cancelación
    private int affectedProductsCount;          // Cantidad de productos afectados
    private boolean replanificationTriggered;   // Si se disparó replanificación
    private String errorMessage;                // Mensaje de error (si falló)
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════
    
    public FlightCancellation() {
        this.status = CancellationStatus.PENDING;
        this.replanificationTriggered = false;
    }
    
    /**
     * Constructor para cancelación programada (desde archivo)
     */
    public FlightCancellation(
            String flightOrigin,
            String flightDestination,
            String scheduledDepartureTime,
            LocalDateTime cancellationTime,
            String reason) {
        this();
        this.id = generateId(flightOrigin, flightDestination, scheduledDepartureTime, cancellationTime);
        this.type = CancellationType.SCHEDULED;
        this.flightOrigin = flightOrigin;
        this.flightDestination = flightDestination;
        this.scheduledDepartureTime = scheduledDepartureTime;
        this.cancellationTime = cancellationTime;
        this.reason = reason;
    }
    
    /**
     * Constructor para cancelación manual (desde UI)
     */
    public static FlightCancellation createManualCancellation(
            String flightOrigin,
            String flightDestination,
            String scheduledDepartureTime,
            LocalDateTime currentSimulationTime,
            String reason) {
        FlightCancellation cancellation = new FlightCancellation();
        cancellation.id = generateId(flightOrigin, flightDestination, scheduledDepartureTime, currentSimulationTime);
        cancellation.type = CancellationType.MANUAL;
        cancellation.flightOrigin = flightOrigin;
        cancellation.flightDestination = flightDestination;
        cancellation.scheduledDepartureTime = scheduledDepartureTime;
        cancellation.cancellationTime = currentSimulationTime;
        cancellation.reason = reason;
        return cancellation;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Marca la cancelación como ejecutada
     */
    public void markAsExecuted(LocalDateTime executionTime, int affectedProducts) {
        this.status = CancellationStatus.EXECUTED;
        this.executedTime = executionTime;
        this.affectedProductsCount = affectedProducts;
    }
    
    /**
     * Marca la cancelación como fallida
     */
    public void markAsFailed(String errorMessage) {
        this.status = CancellationStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Indica que se disparó la replanificación
     */
    public void triggerReplanification() {
        this.replanificationTriggered = true;
    }
    
    /**
     * Verifica si la cancelación debe ejecutarse en el tiempo dado
     */
    public boolean shouldExecuteAt(LocalDateTime simulationTime) {
        return status == CancellationStatus.PENDING 
            && !cancellationTime.isAfter(simulationTime);
    }
    
    /**
     * Genera un ID único para la cancelación
     */
    private static String generateId(String origin, String destination, String schedTime, LocalDateTime cancTime) {
        return String.format("CANCEL_%s-%s-%s_%s", 
            origin, 
            destination, 
            schedTime.replace(":", ""),
            cancTime.toString().replace(":", "").replace("-", "").substring(0, 13)
        );
    }
    
    /**
     * Identificador del vuelo (para matching)
     */
    public String getFlightIdentifier() {
        return String.format("%s-%s-%s", flightOrigin, flightDestination, scheduledDepartureTime);
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
    
    public CancellationType getType() {
        return type;
    }
    
    public void setType(CancellationType type) {
        this.type = type;
    }
    
    public CancellationStatus getStatus() {
        return status;
    }
    
    public void setStatus(CancellationStatus status) {
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
    
    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }
    
    public void setCancellationTime(LocalDateTime cancellationTime) {
        this.cancellationTime = cancellationTime;
    }
    
    public LocalDateTime getExecutedTime() {
        return executedTime;
    }
    
    public void setExecutedTime(LocalDateTime executedTime) {
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
    
    @Override
    public String toString() {
        return String.format("FlightCancellation[%s | %s→%s @ %s | Status: %s | Affected: %d]",
            type, flightOrigin, flightDestination, scheduledDepartureTime, status, affectedProductsCount);
    }
}


