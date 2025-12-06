package pe.edu.pucp.morapack.algos.entities;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import pe.edu.pucp.morapack.model.Continent;
// ❌ ELIMINA: import pe.edu.pucp.morapack.model.Shipment; 
// (Así desacoplamos el algoritmo de la base de datos)

public class PlannerOrder {
    private int id;
    private int totalQuantity;
    private PlannerAirport origin;
    private PlannerAirport destination;
    private long maxDeliveryHours;
    private LocalDateTime orderTime;
    private String clientId;
    
    // ✅ CAMBIO CLAVE: Usar la entidad del algoritmo
    private List<PlannerShipment> shipments = new ArrayList<>();

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
    
    // ✅ GETTER CORREGIDO
    public List<PlannerShipment> getShipments() {
        return new ArrayList<>(shipments);
    }
    
    // ✅ SETTER CORREGIDO
    public void setShipments(List<PlannerShipment> shipments) {
        this.shipments = new ArrayList<>(shipments);
    }
    
    public void addShipment(PlannerShipment shipment) {
        this.shipments.add(shipment);
    }
    
    public Duration getTotalDeliveryTime() {
        if (shipments.isEmpty() || orderTime == null) return Duration.ZERO;
        
        LocalDateTime lastDelivery = shipments.stream()
            .map(PlannerShipment::getFinalArrivalTime)
            .filter(arrival -> arrival != null)
            .max(LocalDateTime::compareTo)
            .orElse(orderTime);
            
        return Duration.between(orderTime, lastDelivery);
    }
    
    public boolean isInterContinental() {
        if (origin == null || destination == null) return false;
        if (origin.getCountry() == null || destination.getCountry() == null) return false;
        return !origin.getCountry().getContinent().equals(destination.getCountry().getContinent());
    }

    public LocalDateTime getDeadlineInDestinationTimezone() {
        if (orderTime == null) return null;
        if (destination == null) return orderTime.plusHours(maxDeliveryHours);

        ZoneOffset destOffset = ZoneOffset.ofHours(destination.getGmt());
        ZonedDateTime orderTimeAtDest = orderTime.atZone(ZoneOffset.UTC).withZoneSameInstant(destOffset);
        return orderTimeAtDest.plusHours(maxDeliveryHours).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public boolean isDeliveredOnTime() {
        if (shipments.isEmpty() || orderTime == null) return false;
        LocalDateTime deadline = getDeadlineInDestinationTimezone();
        if (deadline == null) return false;

        boolean anyArrival = shipments.stream().anyMatch(s -> s.getFinalArrivalTime() != null);
        if (!anyArrival) return false;

        return shipments.stream()
            .map(PlannerShipment::getFinalArrivalTime)
            .filter(arrival -> arrival != null)
            .allMatch(arrival -> arrival.isBefore(deadline) || arrival.isEqual(deadline));
    }

    public long getDelayHours() {
        if (shipments.isEmpty() || orderTime == null) return 0;
        LocalDateTime deadline = getDeadlineInDestinationTimezone();
        if (deadline == null) return 0;

        LocalDateTime lastArrival = shipments.stream()
            .map(PlannerShipment::getFinalArrivalTime)
            .filter(arrival -> arrival != null)
            .max(LocalDateTime::compareTo)
            .orElse(orderTime);

        if (lastArrival.isBefore(deadline) || lastArrival.isEqual(deadline)) return 0;
        return Duration.between(deadline, lastArrival).toHours();
    }
}