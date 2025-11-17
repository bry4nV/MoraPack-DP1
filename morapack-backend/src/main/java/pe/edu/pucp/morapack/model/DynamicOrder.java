package pe.edu.pucp.morapack.model;

import java.time.LocalDateTime;

/**
 * Representa un pedido dinámico que se agrega durante la simulación.
 * 
 * Puede ser:
 * - Programado: Cargado desde archivo, se inyecta en momento específico
 * - Manual: Agregado por el usuario durante la simulación
 */
public class DynamicOrder {
    
    /**
     * Tipo de pedido dinámico
     */
    public enum OrderType {
        SCHEDULED,  // Programado (desde archivo)
        MANUAL      // Manual (desde UI)
    }
    
    /**
     * Estado del pedido dinámico
     */
    public enum OrderStatus {
        PENDING,     // Pendiente de inyección
        INJECTED,    // Inyectado en la simulación
        ASSIGNED,    // Asignado a vuelos
        IN_TRANSIT,  // En tránsito
        DELIVERED,   // Entregado
        FAILED       // Falló (ej: sin vuelos disponibles)
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CAMPOS
    // ═══════════════════════════════════════════════════════════════
    
    private String id;                      // ID único del pedido dinámico
    private OrderType type;                 // Tipo de pedido
    private OrderStatus status;             // Estado actual
    
    // Datos del pedido
    private String origin;                  // Origen (IATA code)
    private String destination;             // Destino (IATA code)
    private int quantity;                   // Cantidad de productos
    private int deadlineHours;              // Deadline en horas (48 o 72)
    
    // Tiempos
    private LocalDateTime injectionTime;    // Cuándo se debe inyectar
    private LocalDateTime injectedTime;     // Cuándo se inyectó realmente
    private LocalDateTime deadline;         // Fecha límite de entrega
    
    // Metadatos
    private String reason;                  // Razón del pedido urgente
    private boolean autoAssigned;           // Si fue asignado automáticamente
    private String errorMessage;            // Mensaje de error (si falló)
    
    // ID del pedido real (una vez inyectado en el sistema)
    private Integer systemOrderId;
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTORES
    // ═══════════════════════════════════════════════════════════════
    
    public DynamicOrder() {
        this.status = OrderStatus.PENDING;
        this.autoAssigned = false;
    }
    
    /**
     * Constructor para pedido programado (desde archivo)
     */
    public DynamicOrder(
            String origin,
            String destination,
            int quantity,
            int deadlineHours,
            LocalDateTime injectionTime,
            String reason) {
        this();
        this.id = generateId(origin, destination, injectionTime);
        this.type = OrderType.SCHEDULED;
        this.origin = origin;
        this.destination = destination;
        this.quantity = quantity;
        this.deadlineHours = deadlineHours;
        this.injectionTime = injectionTime;
        this.reason = reason;
    }
    
    /**
     * Constructor para pedido manual (desde UI)
     */
    public static DynamicOrder createManualOrder(
            String origin,
            String destination,
            int quantity,
            int deadlineHours,
            LocalDateTime currentSimulationTime,
            String reason) {
        DynamicOrder order = new DynamicOrder();
        order.id = generateId(origin, destination, currentSimulationTime);
        order.type = OrderType.MANUAL;
        order.origin = origin;
        order.destination = destination;
        order.quantity = quantity;
        order.deadlineHours = deadlineHours;
        order.injectionTime = currentSimulationTime;
        order.reason = reason;
        return order;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS DE NEGOCIO
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Marca el pedido como inyectado
     */
    public void markAsInjected(LocalDateTime injectedTime, Integer systemOrderId) {
        this.status = OrderStatus.INJECTED;
        this.injectedTime = injectedTime;
        this.systemOrderId = systemOrderId;
        this.deadline = injectedTime.plusHours(deadlineHours);
    }
    
    /**
     * Marca el pedido como asignado
     */
    public void markAsAssigned() {
        this.status = OrderStatus.ASSIGNED;
        this.autoAssigned = true;
    }
    
    /**
     * Marca el pedido como fallido
     */
    public void markAsFailed(String errorMessage) {
        this.status = OrderStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Verifica si el pedido debe inyectarse en el tiempo dado
     */
    public boolean shouldInjectAt(LocalDateTime simulationTime) {
        return status == OrderStatus.PENDING 
            && !injectionTime.isAfter(simulationTime);
    }
    
    /**
     * Genera un ID único para el pedido dinámico
     */
    private static String generateId(String origin, String destination, LocalDateTime injectTime) {
        return String.format("DYN_%s-%s_%s", 
            origin, 
            destination,
            injectTime.toString().replace(":", "").replace("-", "").substring(0, 13)
        );
    }
    
    /**
     * Valida que el pedido tenga datos correctos
     */
    public boolean isValid() {
        return origin != null && !origin.isEmpty()
            && destination != null && !destination.isEmpty()
            && !origin.equals(destination)
            && quantity > 0
            && (deadlineHours == 48 || deadlineHours == 72)
            && injectionTime != null;
    }
    
    /**
     * Descripción corta del pedido
     */
    public String getDescription() {
        return String.format("%s → %s (%d units, %dh deadline)", 
            origin, destination, quantity, deadlineHours);
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
    
    public OrderType getType() {
        return type;
    }
    
    public void setType(OrderType type) {
        this.type = type;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
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
    
    public LocalDateTime getInjectionTime() {
        return injectionTime;
    }
    
    public void setInjectionTime(LocalDateTime injectionTime) {
        this.injectionTime = injectionTime;
    }
    
    public LocalDateTime getInjectedTime() {
        return injectedTime;
    }
    
    public void setInjectedTime(LocalDateTime injectedTime) {
        this.injectedTime = injectedTime;
    }
    
    public LocalDateTime getDeadline() {
        return deadline;
    }
    
    public void setDeadline(LocalDateTime deadline) {
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
    
    @Override
    public String toString() {
        return String.format("DynamicOrder[%s | %s→%s | %d units | %dh | Status: %s]",
            type, origin, destination, quantity, deadlineHours, status);
    }
}


