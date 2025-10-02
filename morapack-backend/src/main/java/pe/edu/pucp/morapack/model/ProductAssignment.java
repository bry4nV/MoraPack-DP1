package pe.edu.pucp.morapack.model;

import java.time.LocalDateTime;

/**
 * Represents the assignment of a specific quantity of products from an order to a flight
 */
public class ProductAssignment {
    private int id;
    private Order order;
    private Flight flight;
    private int quantity;
    private LocalDateTime assignmentTime;
    
    public ProductAssignment(int id, Order order, Flight flight, int quantity) {
        this.id = id;
        this.order = order;
        this.flight = flight;
        this.quantity = quantity;
        this.assignmentTime = LocalDateTime.now();
    }
    
    public int getId() { return id; }
    public Order getOrder() { return order; }
    public Flight getFlight() { return flight; }
    public int getQuantity() { return quantity; }
    public LocalDateTime getAssignmentTime() { return assignmentTime; }
    
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setFlight(Flight flight) { this.flight = flight; }
    
    public Airport getOrigin() { return flight.getOrigin(); }
    public Airport getDestination() { return flight.getDestination(); }
    
    public LocalDateTime getEstimatedArrival() {
        return flight.getArrivalTime();
    }
    
    public boolean isDeliveredOnTime() {
        if (order.getOrderTime() == null) return false;
        
        LocalDateTime arrivalTime = getEstimatedArrival();
        long hoursFromOrder = java.time.Duration.between(order.getOrderTime(), arrivalTime).toHours();
        return hoursFromOrder <= order.getMaxDeliveryHours();
    }
    
    @Override
    public String toString() {
        return String.format("ProductAssignment{orderId=%d, flightCode=%s, quantity=%d, arrival=%s}",
                order.getId(), flight.getCode(), quantity, getEstimatedArrival());
    }
}