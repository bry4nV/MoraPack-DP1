package pe.edu.pucp.morapack.sim;
import pe.edu.pucp.morapack.model.*;
import java.time.LocalDateTime;

public class PendingOrder {
    private final int id;
    private final Airport origin;
    private final Airport destination;
    private final long maxDeliveryHours;
    private final LocalDateTime orderTime;
    private int remainingQuantity;

    public PendingOrder(Order o) {
        this.id = o.getId();
        this.origin = o.getOrigin();
        this.destination = o.getDestination();
        this.maxDeliveryHours = o.getMaxDeliveryHours();
        this.orderTime = o.getOrderTime();
        this.remainingQuantity = o.getTotalQuantity();
    }

    public int getId() { return id; }
    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
    public long getMaxDeliveryHours() { return maxDeliveryHours; }
    public LocalDateTime getOrderTime() { return orderTime; }

    public int getRemainingQuantity() { return remainingQuantity; }
    public void subtractDelivered(int qty) {
        if (qty <= 0) return;
        this.remainingQuantity = Math.max(0, this.remainingQuantity - qty);
    }
    public boolean isComplete() { return remainingQuantity == 0; }
}
