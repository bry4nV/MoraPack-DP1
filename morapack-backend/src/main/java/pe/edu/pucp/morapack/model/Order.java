package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalDate; // <-- IMPORTADO
import java.time.LocalTime; // <-- IMPORTADO
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order") // Esto ya estaba correcto
public class Order {

    // --- CAMPOS PERSISTIDOS (SINCRONIZADOS CON LA NUEVA BD) ---

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // <-- CAMBIADO (antes ID_Pedido)
    private Long id;

    @Column(name = "order_number") // <-- NUEVO CAMPO
    private String orderNumber;

    @Column(name = "order_date") // <-- NUEVO CAMPO (reemplaza 'dia')
    private LocalDate orderDate;

    @Column(name = "order_time") // <-- NUEVO CAMPO (reemplaza 'hora' y 'minutos')
    private LocalTime persistedOrderTime; // <-- RENOMBRADO para evitar conflicto con tu transient 'orderTime'

    @Column(name = "airport_destination_code") // <-- CAMBIADO (antes 'dest')
    private String airportDestinationCode;

    @Column(name = "quantity") // <-- CAMBIADO (antes 'CantidadPedidos')
    private Integer quantity;

    @Column(name = "client_code") // <-- CAMBIADO (antes 'idClien')
    private String clientCode;

    @Column(name = "status") // <-- CAMBIADO (antes 'estado')
    private String status;

    // --- CAMPOS TRANSIENT (TU LÓGICA DE DOMINIO - CONSERVADA) ---
    // (Estos campos NO se guardan en la BD, los usa tu lógica)

    private transient int totalQuantity;
    private transient Airport origin;
    private transient Airport destination;
    private transient long maxDeliveryHours;
    private transient LocalDateTime orderTime; // <-- CONSERVADO (tu campo transient)
    private transient List<Shipment> shipments = new ArrayList<>();

    
    // --- CONSTRUCTORES (CONSERVADOS) ---

    public Order() {}

    // convenience constructor for domain usage (CONSERVADO)
    public Order(int id, int quantity, Airport origin, Airport destination) {
        this.id = (long) id;
        this.totalQuantity = quantity;
        this.origin = origin;
        this.destination = destination;
        this.maxDeliveryHours = origin.getContinent().equals(destination.getContinent()) ? 48 : 72;
        this.orderTime = LocalDateTime.now();
    }

    // --- GETTERS/SETTERS PERSISTIDOS (ACTUALIZADOS) ---

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

    
    // --- GETTERS/SETTERS Y MÉTODOS DE DOMINIO (CONSERVADOS) ---

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
    
    public boolean isInterContinental() {
        if (origin == null || destination == null) return false;
        
        String originContinent = origin.getContinent();
        String destContinent = destination.getContinent();
        
        if (originContinent == null || destContinent == null) return false;
        
        return !originContinent.equals(destContinent);
    }
}