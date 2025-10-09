package pe.edu.pucp.morapack.model;
import pe.edu.pucp.morapack.sim.PendingOrder;   
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private int totalQuantity;
    private Airport origin;
    private Airport destination;
    private long maxDeliveryHours;
    private LocalDateTime orderTime;
    private List<Shipment> shipments = new ArrayList<>();

    public Order(int id, int quantity, Airport origin, Airport destination) {
        this.id = id;
        this.totalQuantity = quantity;
        this.origin = origin;
        this.destination = destination;
        this.maxDeliveryHours = origin.getCountry().getContinent() == destination.getCountry().getContinent() ? 48 : 72;
        this.orderTime = LocalDateTime.now();
    }

    public int getId() { return id; }
    public int getTotalQuantity() { return totalQuantity; }
    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
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

    private static List<Order> buildDailyOrdersFromPending(List<PendingOrder> backlog) {
    List<Order> daily = new ArrayList<>();
    for (PendingOrder p : backlog) {
        if (p.getRemainingQuantity() <= 0) continue;
        // Crea una Order efímera con la cantidad pendiente.
        Order o = new Order(p.getId(), p.getRemainingQuantity(), p.getOrigin(), p.getDestination());
        // Respetar el orderTime original para que el deadline se calcule correctamente.
        o.setOrderTime(p.getOrderTime());
        // maxDeliveryHours se calcula en el constructor según continentes (no hay setter).
        daily.add(o);
    }
    return daily;
}


}