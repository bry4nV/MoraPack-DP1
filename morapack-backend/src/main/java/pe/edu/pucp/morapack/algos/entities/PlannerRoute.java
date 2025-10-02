package pe.edu.pucp.morapack.algos.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Shipment;

public class PlannerRoute {
    private Flight flight;
    private List<PlannerSegment> segments = new ArrayList<>();
    private List<Shipment> shipments = new ArrayList<>();
    
    public PlannerRoute() {}
    
    public PlannerRoute(Flight flight) {
        this.flight = flight;
        if (flight != null) {
            this.segments.add(new PlannerSegment(flight));
        }
    }
    
    public void addSegment(PlannerSegment segment) {
        this.segments.add(segment);
    }
    
    public void removeSegment(PlannerSegment segment) {
        this.segments.remove(segment);
    }
    
    public void addShipment(Shipment shipment) {
        this.shipments.add(shipment);
    }
    
    public void removeShipment(Shipment shipment) {
        this.shipments.remove(shipment);
    }
    
    public PlannerRoute(PlannerRoute other) {
        this.flight = other.flight;
        this.segments = other.segments.stream()
            .map(PlannerSegment::new)
            .collect(Collectors.toList());
        this.shipments = new ArrayList<>(other.shipments);
    }

    public Flight getFlight() {
        return flight;
    }
    
    public void setFlight(Flight flight) {
        this.flight = flight;
    }
    
    public List<PlannerSegment> getSegments() {
        return segments;
    }

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments;
    }
    
    /**
     * Obtiene todas las órdenes asociadas a los envíos en esta ruta
     */
    public List<Order> getOrders() {
        return shipments.stream()
                .map(Shipment::getOrder)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Verifica si la ruta es válida según las restricciones del problema
     */
    public boolean isValid() {
        // Verificar que haya un vuelo asignado
        if (flight == null) {
            return false;
        }

        // Verificar que la capacidad del vuelo no sea excedida
        double totalQuantity = shipments.stream()
                .mapToDouble(Shipment::getQuantity)
                .sum();
        if (totalQuantity > flight.getCapacity()) {
            return false;
        }

        // Verificar que los segmentos sean válidos
        if (segments.isEmpty() && !shipments.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Calcula el costo total de la ruta
     */
    public double calculateCost() {
        // Costo base del vuelo
        double cost = flight != null ? flight.getCost() : 0;
        
        // Costo adicional por cantidad transportada
        double totalQuantity = shipments.stream()
                .mapToDouble(Shipment::getQuantity)
                .sum();
        cost += totalQuantity * 0.1; // Factor de costo por cantidad

        return cost;
    }

    /**
     * Crea una copia profunda de la ruta actual
     */
    public PlannerRoute deepCopy() {
        PlannerRoute copy = new PlannerRoute();
        copy.flight = this.flight;
        copy.segments = this.segments.stream()
                .map(PlannerSegment::new)
                .collect(Collectors.toList());
        copy.shipments = new ArrayList<>(this.shipments);
        return copy;
    }
    
    public List<Flight> getFlights() {
        List<Flight> flights = new ArrayList<>();
        if (flight != null) {
            flights.add(flight);
        }
        return flights;
    }
    
    public LocalDateTime getFirstDepartureTime() {
        return flight != null ? flight.getDepartureTime() : null;
    }
    
    public LocalDateTime getInitialDepartureTime() {
        return getFirstDepartureTime();
    }
    
    public LocalDateTime getFinalArrivalTime() {
        return segments.isEmpty() ? null : segments.get(segments.size() - 1).getFlight().getArrivalTime();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlannerRoute that = (PlannerRoute) o;
        return Objects.equals(flight, that.flight) &&
               Objects.equals(segments, that.segments) &&
               Objects.equals(shipments, that.shipments);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(flight, segments, shipments);
    }
}