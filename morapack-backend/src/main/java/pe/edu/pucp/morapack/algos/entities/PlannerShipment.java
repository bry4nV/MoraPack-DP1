package pe.edu.pucp.morapack.algos.entities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Representa un envío de N productos siguiendo una RUTA (secuencia de vuelos).
 * - Si ruta tiene 1 vuelo → Envío DIRECTO (sin escalas)
 * - Si ruta tiene 2+ vuelos → Envío con ESCALAS
 *
 * Ejemplo:
 *   Order #100: 500 productos Lima → Miami
 *
 *   PlannerShipment #1: 200 productos, ruta [LIM→MIA] (directo)
 *   PlannerShipment #2: 150 productos, ruta [LIM→MEX, MEX→MIA] (1 escala)
 *   PlannerShipment #3: 150 productos, ruta [LIM→PTY, PTY→MIA] (1 escala)
 */
public class PlannerShipment {

    /**
     * Estado del shipment en el ciclo de vida de replanificación
     */
    public enum Status {
        ACTIVE,      // Shipment activo (usado en la solución actual)
        CANCELLED    // Shipment cancelado (reemplazado por replanificación)
    }

    private int id;
    private PlannerOrder order;
    private List<PlannerFlight> flightSequence;  // Secuencia de vuelos (ruta completa)
    private int quantity;                  // Cantidad de productos en ESTE envío
    private Status status;                 // Estado del shipment (ACTIVE por defecto)
    
    public PlannerShipment(int id, PlannerOrder order, List<PlannerFlight> flights, int quantity) {
        this.id = id;
        this.order = order;
        this.flightSequence = new ArrayList<>(flights);
        this.quantity = quantity;
        this.status = Status.ACTIVE;  // Por defecto, los shipments son activos
    }

    // Constructor de copia
    public PlannerShipment(PlannerShipment other) {
        this.id = other.id;
        this.order = other.order;
        this.flightSequence = new ArrayList<>(other.flightSequence);
        this.quantity = other.quantity;
        this.status = other.status;  // Copiar también el estado
    }
    
    // ========== Getters/Setters ==========
    
    public int getId() { 
        return id; 
    }
    
    public PlannerOrder getOrder() { 
        return order; 
    }
    
    public List<PlannerFlight> getFlights() { 
        return new ArrayList<>(flightSequence); 
    }
    
    public int getQuantity() { 
        return quantity; 
    }
    
    public void setQuantity(int quantity) { 
        this.quantity = quantity; 
    }
    
    public void setFlights(List<PlannerFlight> flights) {
        this.flightSequence = new ArrayList<>(flights);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    // ========== Métodos de Consulta ==========
    
    /**
     * ¿Es ruta directa (sin escalas)?
     */
    public boolean isDirect() {
        return flightSequence.size() == 1;
    }
    
    /**
     * Número de escalas (conexiones)
     * - 0 = directo
     * - 1 = una escala
     * - 2 = dos escalas, etc.
     */
    public int getNumberOfStops() {
        return Math.max(0, flightSequence.size() - 1);
    }
    
    /**
     * Tiempo de llegada final (último vuelo de la ruta)
     */
    public LocalDateTime getFinalArrivalTime() {
        if (flightSequence.isEmpty()) return null;
        return flightSequence.get(flightSequence.size() - 1).getArrivalTime();
    }
    
    /**
     * Tiempo de salida inicial (primer vuelo de la ruta)
     */
    public LocalDateTime getInitialDepartureTime() {
        if (flightSequence.isEmpty()) return null;
        return flightSequence.get(0).getDepartureTime();
    }
    
    /**
     * Duración total del viaje en horas
     */
    public long getTotalTravelHours() {
        if (flightSequence.isEmpty()) return 0;
        LocalDateTime start = getInitialDepartureTime();
        LocalDateTime end = getFinalArrivalTime();
        return ChronoUnit.HOURS.between(start, end);
    }
    
    /**
     * Tiempo total de entrega (desde que se hizo el pedido hasta la llegada)
     */
    public long getDeliveryTimeHours() {
        if (order == null || order.getOrderTime() == null) return 0;
        LocalDateTime arrival = getFinalArrivalTime();
        if (arrival == null) return 0;
        return ChronoUnit.HOURS.between(order.getOrderTime(), arrival);
    }
    
    // ========== Validaciones ==========
    
    /**
     * Validar que la secuencia de vuelos es coherente:
     * 1. El destino de un vuelo debe ser el origen del siguiente
     * 2. Debe haber tiempo suficiente para la conexión (mínimo 1 hora)
     * 3. Los tiempos deben ser lógicos
     */
    public boolean isValidSequence() {
        if (flightSequence.isEmpty()) return false;
        if (flightSequence.size() == 1) return true;  // Directo siempre es válido
        
        for (int i = 0; i < flightSequence.size() - 1; i++) {
            PlannerFlight current = flightSequence.get(i);
            PlannerFlight next = flightSequence.get(i + 1);
            
            // Validación 1: Conectividad geográfica
            if (!current.getDestination().equals(next.getOrigin())) {
                return false;
            }
            
            // Validación 2: Tiempo de conexión
            long connectionHours = ChronoUnit.HOURS.between(
                current.getArrivalTime(), 
                next.getDepartureTime()
            );
            
            if (connectionHours < 1) {  // Mínimo 1 hora
                return false;
            }
            
            // ✅ NO hay máximo de horas de conexión (no está en el enunciado)
        }
        
        return true;
    }
    
    /**
     * Validar que el shipment cumple con el plazo de entrega del pedido
     */
    public boolean meetsDeadline() {
        if (order == null) return false;
        
        long deliveryHours = getDeliveryTimeHours();
        long maxHours = order.getMaxDeliveryHours();
        
        return deliveryHours <= maxHours;
    }
    
    // ========== Descripción y Visualización ==========
    
    /**
     * Descripción de la ruta en formato: ORIGIN → HUB1 → HUB2 → DESTINATION
     */
    public String getRouteDescription() {
        if (flightSequence.isEmpty()) return "No route";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < flightSequence.size(); i++) {
            PlannerFlight f = flightSequence.get(i);
            if (i > 0) sb.append(" → ");
            sb.append(f.getOrigin().getCode());
        }
        sb.append(" → ");
        sb.append(flightSequence.get(flightSequence.size() - 1)
                    .getDestination().getCode());
        
        return sb.toString();
    }
    
    /**
     * Descripción detallada con códigos de vuelo
     */
    public String getDetailedRouteDescription() {
        if (flightSequence.isEmpty()) return "No route";
        
        return flightSequence.stream()
            .map(f -> String.format("%s (%s→%s)", 
                f.getCode(),
                f.getOrigin().getCode(),
                f.getDestination().getCode()))
            .collect(Collectors.joining(" → "));
    }
    
    /**
     * Obtener aeropuertos de escala (intermedios)
     */
    public List<String> getStopoverAirports() {
        if (flightSequence.size() <= 1) return new ArrayList<>();
        
        List<String> stopovers = new ArrayList<>();
        for (int i = 0; i < flightSequence.size() - 1; i++) {
            stopovers.add(flightSequence.get(i).getDestination().getCode());
        }
        return stopovers;
    }
    
    @Override
    public String toString() {
        return String.format("PlannerShipment{id=%d, order=%d, route=%s, qty=%d, stops=%d, status=%s, valid=%s}",
            id,
            order != null ? order.getId() : -1,
            getRouteDescription(),
            quantity,
            getNumberOfStops(),
            status,
            isValidSequence() ? "✓" : "✗");
    }
    
    /**
     * Descripción completa para logging
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n  PlannerShipment #%d:\n", id));
        sb.append(String.format("    Order: #%d (%d/%d productos)\n", 
            order.getId(), quantity, order.getTotalQuantity()));
        sb.append(String.format("    Route: %s\n", getRouteDescription()));
        sb.append(String.format("    Type: %s (%d stops)\n", 
            isDirect() ? "DIRECT" : "WITH CONNECTIONS", getNumberOfStops()));
        
        if (!isDirect()) {
            sb.append(String.format("    Stopovers: %s\n", 
                String.join(", ", getStopoverAirports())));
        }
        
        sb.append(String.format("    Flights: %s\n", getDetailedRouteDescription()));
        sb.append(String.format("    Departure: %s\n", getInitialDepartureTime()));
        sb.append(String.format("    Arrival: %s\n", getFinalArrivalTime()));
        sb.append(String.format("    Travel Time: %d hours\n", getTotalTravelHours()));
        sb.append(String.format("    Delivery Time: %d hours (max: %d)\n", 
            getDeliveryTimeHours(), order.getMaxDeliveryHours()));
        sb.append(String.format("    Valid: %s, Meets Deadline: %s\n", 
            isValidSequence() ? "YES" : "NO",
            meetsDeadline() ? "YES" : "NO"));
        
        return sb.toString();
    }
}
