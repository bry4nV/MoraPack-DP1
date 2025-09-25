package pe.edu.pucp.morapack.model;

import java.util.Objects;

public class Shipment {
    private int id;
    private Order parentOrder;
    private int quantity;
    private Airport origin;
    private Airport destination;

    public Shipment(int id, Order order, int quantity, Airport origin, Airport destination) {
        this.id = id;
        this.parentOrder = order;
        this.quantity = quantity;
        this.origin = origin;
        this.destination = destination;
    }

    public int getId() { return id; }
    public Order getParentOrder() { return parentOrder; }
    public int getQuantity() { return quantity; }
    public Airport getOrigin() { return origin; }
    public Airport getDestination() { return destination; }
    
    public boolean isInterContinental() {
        return origin.getCountry().getContinent() != destination.getCountry().getContinent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Shipment) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}