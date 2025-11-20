package pe.edu.pucp.morapack.algos.entities;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.morapack.model.Continent;
import pe.edu.pucp.morapack.model.Shipment;

public class PlannerOrder {
    private int id;
    private int totalQuantity;
    private PlannerAirport origin;
    private PlannerAirport destination;
    private long maxDeliveryHours;
    private LocalDateTime orderTime;
    private String clientId;  // ID del cliente que realizó el pedido
    private List<Shipment> shipments = new ArrayList<>();

    public PlannerOrder(int id, int quantity, PlannerAirport origin, PlannerAirport destination) {
        this.id = id;
        this.totalQuantity = quantity;
        this.origin = origin;
        this.destination = destination;
        this.maxDeliveryHours = origin.getCountry().getContinent() == destination.getCountry().getContinent() ? 48 : 72;
        this.orderTime = LocalDateTime.now();
    }

    public int getId() { return id; }
    public int getTotalQuantity() { return totalQuantity; }
    public PlannerAirport getOrigin() { return origin; }
    public PlannerAirport getDestination() { return destination; }
    public long getMaxDeliveryHours() { return maxDeliveryHours; }

    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public List<Shipment> getShipments() {
        return new ArrayList<>(shipments);
    }
    
    public void setShipments(List<Shipment> shipments) {
        this.shipments = new ArrayList<>(shipments);
    }
    
    public void addShipment(Shipment shipment) {
        this.shipments.add(shipment);
    }
    
    public Duration getTotalDeliveryTime() {
        if (shipments.isEmpty() || orderTime == null) {
            return Duration.ZERO;
        }
        
        LocalDateTime lastDelivery = shipments.stream()
            .map(Shipment::getEstimatedArrival)
            .filter(arrival -> arrival != null)  // Filter out null values
            .max(LocalDateTime::compareTo)
            .orElse(orderTime);
            
        return Duration.between(orderTime, lastDelivery);
    }
    
    /**
     * Determina si el pedido es intercontinental
     */
    public boolean isInterContinental() {
        if (origin == null || destination == null) return false;
        if (origin.getCountry() == null || destination.getCountry() == null) return false;

        Continent originContinent = origin.getCountry().getContinent();
        Continent destContinent = destination.getCountry().getContinent();

        // Si alguno de los continentes es null, asumimos que NO es intercontinental
        if (originContinent == null || destContinent == null) return false;

        return !originContinent.equals(destContinent);
    }

    /**
     * Calcula el deadline del pedido considerando el timezone del destino.
     *
     * REGLA: El plazo de entrega se mide respecto de la hora en que se hizo el pedido
     * en el uso horario del DESTINO.
     *
     * Ejemplo:
     * - Pedido realizado a las 10:00 UTC
     * - Destino: Lima (GMT-5)
     * - Hora local en Lima: 05:00
     * - Deadline (48h): 05:00 + 48h = 05:00 (dos días después) en hora de Lima
     *
     * @return Deadline en tiempo UTC
     */
    public LocalDateTime getDeadlineInDestinationTimezone() {
        if (orderTime == null) {
            return null;
        }

        if (destination == null) {
            // Fallback: sin timezone del destino, usar cálculo simple en UTC
            return orderTime.plusHours(maxDeliveryHours);
        }

        // Convertir orderTime (UTC) a timezone del destino
        ZoneOffset destOffset = ZoneOffset.ofHours(destination.getGmt());
        ZonedDateTime orderTimeAtDest = orderTime.atZone(ZoneOffset.UTC)
                                                  .withZoneSameInstant(destOffset);

        // Agregar las horas del deadline en el timezone del destino
        ZonedDateTime deadlineAtDest = orderTimeAtDest.plusHours(maxDeliveryHours);

        // Convertir de vuelta a UTC para comparaciones
        return deadlineAtDest.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Determina si el pedido fue entregado a tiempo considerando el timezone del destino.
     *
     * @return true si todos los shipments llegaron antes del deadline, false si no
     */
    public boolean isDeliveredOnTime() {
        if (shipments.isEmpty() || orderTime == null) {
            return false;  // Sin shipments no puede estar "a tiempo"
        }

        LocalDateTime deadline = getDeadlineInDestinationTimezone();
        if (deadline == null) {
            return false;  // Sin deadline calculable, no se puede determinar
        }

        // Verificar que TODOS los shipments hayan llegado antes del deadline
        return shipments.stream()
            .map(Shipment::getEstimatedArrival)
            .filter(arrival -> arrival != null)
            .allMatch(arrival -> arrival.isBefore(deadline) || arrival.isEqual(deadline));
    }

    /**
     * Obtiene el retraso en horas si el pedido llegó tarde.
     *
     * @return Horas de retraso (positivo si tarde, 0 si a tiempo o antes)
     */
    public long getDelayHours() {
        if (shipments.isEmpty() || orderTime == null) {
            return 0;
        }

        LocalDateTime deadline = getDeadlineInDestinationTimezone();
        if (deadline == null) {
            return 0;  // Sin deadline calculable
        }

        LocalDateTime lastArrival = shipments.stream()
            .map(Shipment::getEstimatedArrival)
            .filter(arrival -> arrival != null)
            .max(LocalDateTime::compareTo)
            .orElse(orderTime);

        if (lastArrival.isBefore(deadline) || lastArrival.isEqual(deadline)) {
            return 0;  // A tiempo
        }

        return Duration.between(deadline, lastArrival).toHours();
    }
}