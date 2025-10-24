package pe.edu.pucp.morapack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
// Map to a safe table name (avoid SQL reserved word `order`)
@Table(name = "order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPedido")
    private Long id;

    @Column(name = "cantPaquetes")
    private Integer packageCount;

    @Column(name = "idAeropuertoDestino")
    // DB column is VARCHAR(4) (airport code) according to provided DDL
    private String airportDestinationId;

    @Column(name = "prioridad")
    private Integer priority;

    @Column(name = "idCliente")
    // DB column is VARCHAR(10) according to provided DDL
    private String clientId;

    @Column(name = "estado")
    private String status;

    @Column(name = "diaPedido")
    private Integer day;

    @Column(name = "horaPedido")
    private Integer hour;

    @Column(name = "minutoPedido")
    private Integer minute;

    // domain fields kept for compatibility with algos domain; not persisted here
    private transient int totalQuantity;
    private transient Airport origin;
    private transient Airport destination;
    private transient long maxDeliveryHours;
    private transient LocalDateTime orderTime;
    private transient List<Shipment> shipments = new ArrayList<>();

    public Order() {}

    // convenience constructor for domain usage
    public Order(int id, int quantity, Airport origin, Airport destination) {
        this.id = (long) id;
        this.totalQuantity = quantity;
        this.origin = origin;
        this.destination = destination;
        this.maxDeliveryHours = origin.getCountry().getContinent() == destination.getCountry().getContinent() ? 48 : 72;
        this.orderTime = LocalDateTime.now();
    }

    // JPA-friendly getters/setters for persisted columns
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPackageCount() { return packageCount; }
    public void setPackageCount(Integer packageCount) { this.packageCount = packageCount; }

    public String getAirportDestinationId() { return airportDestinationId; }
    public void setAirportDestinationId(String airportDestinationId) { this.airportDestinationId = airportDestinationId; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }

    public Integer getHour() { return hour; }
    public void setHour(Integer hour) { this.hour = hour; }

    public Integer getMinute() { return minute; }
    public void setMinute(Integer minute) { this.minute = minute; }

    // Domain accessors (transient)
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
}