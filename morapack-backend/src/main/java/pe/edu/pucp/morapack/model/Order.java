package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`order`")
public class Order {

    // --- CAMPOS PERSISTIDOS ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "order_time")
    private LocalTime persistedOrderTime; 

    @Column(name = "airport_destination_code")
    private String airportDestinationCode;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "client_code")
    private String clientCode;

    // --- ¡ARREGLO DEL ESTADO! ---
    // Le damos un valor por defecto en Java para que los
    // nuevos pedidos se creen con "UNASSIGNED".
    @Column(name = "status")
    private String status = "UNASSIGNED"; 

    
    // --- CAMPOS TRANSIENT ---
    // ... (El resto de tu archivo no necesita cambios) ...

    private transient int totalQuantity;
    private transient Airport origin;
    private transient Airport destination;
    private transient long maxDeliveryHours;
    private transient LocalDateTime orderTime; 
    private transient List<Shipment> shipments = new ArrayList<>();

    
    // --- CONSTRUCTORES ---
    public Order() {}

    public Order(int id, int quantity, Airport origin, Airport destination) {
        this.id = (long) id;
        this.totalQuantity = quantity;
        this.origin = origin;
        this.destination = destination;
        this.maxDeliveryHours = origin.getContinent().equals(destination.getContinent()) ? 48 : 72;
        this.orderTime = LocalDateTime.now();
        // Nota: el status se inicializará a "UNASSIGNED" gracias al valor por defecto
    }

    // --- GETTERS/SETTERS PERSISTIDOS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalTime getPersistedOrderTime() { return persistedOrderTime; }
    public void setPersistedOrderTime(LocalTime persistedOrderTime) { this.persistedOrderTime = persistedOrderTime; }

    public String getAirportDestinationCode() { return airportDestinationCode; }
    public void setAirportDestinationCode(String airportDestinationCode) { this.airportDestinationCode = airportDestinationCode; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    
    // --- GETTERS/SETTERS Y MÉTODOS DE DOMINIO ---
    // ... (El resto de tus métodos están perfectos) ...
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
            .filter(arrival -> arrival != null)
            .max(LocalDateTime::compareTo)
            .orElse(orderTime);
            
        return Duration.between(orderTime, lastDelivery);
    }
    
    public boolean isInterContinental() {
        if (origin == null || destination == null) return false;
        
        String originContinent = origin.getContinent();
        String destContinent = destination.getContinent();
        
        if (originContinent == null || destContinent == null) return false;
        
        return !originContinent.equals(destContinent);
    }
}