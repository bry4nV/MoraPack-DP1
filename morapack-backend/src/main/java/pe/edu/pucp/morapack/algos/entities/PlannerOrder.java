package pe.edu.pucp.morapack.algos.entities;
import java.time.LocalDateTime;
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
}