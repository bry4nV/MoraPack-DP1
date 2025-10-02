package pe.edu.pucp.morapack.algos.entities;

import java.util.Objects;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;

public class PlannerSegment {
    private Flight flight;
    private Shipment shipment;
    
    public PlannerSegment() {}
    
    public PlannerSegment(Flight flight) {
        this.flight = flight;
    }
    
    public PlannerSegment(Shipment shipment) {
        this.shipment = shipment;
    }
    
    public PlannerSegment(Flight flight, Shipment shipment) {
        this.flight = flight;
        this.shipment = shipment;
    }
    
    public PlannerSegment(PlannerSegment other) {
        this.flight = other.flight;
        this.shipment = other.shipment;
    }
    
    public Flight getFlight() {
        return flight;
    }
    
    public void setFlight(Flight flight) {
        this.flight = flight;
    }
    
    public Shipment getShipment() {
        return shipment;
    }
    
    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(flight, ((PlannerSegment) o).flight);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(flight);
    }
}