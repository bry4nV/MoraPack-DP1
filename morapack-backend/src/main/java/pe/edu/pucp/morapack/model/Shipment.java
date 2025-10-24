package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "paquete")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPaquete")
    private Integer id;

    @Column(name = "idPedido")
    private Long idPedido;

    @Column(name = "idAeropuertoActual")
    private String idAeropuertoActual;

    @Column(name = "idVueloActual")
    private Long idVueloActual;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "ENUM('IN_TRANSIT','IN_WAREHOUSE','DELIVERED','CANCELLED')")
    private ShipmentStatus estado;

    // Campos transient para algoritmos (no en BD)
    private transient Order parentOrder;
    private transient int quantity;
    private transient Airport origin;
    private transient Airport destination;
    private transient LocalDateTime estimatedArrival;

    public Shipment() {}

    public Shipment(int id, Order order, int quantity, Airport origin, Airport destination) {
        this.id = id;
        this.parentOrder = order;
        this.quantity = quantity;
        this.origin = origin;
        this.destination = destination;
    }

    // Getters y Setters (BD fields)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Long getIdPedido() { return idPedido; }
    public void setIdPedido(Long idPedido) { this.idPedido = idPedido; }

    public String getIdAeropuertoActual() { return idAeropuertoActual; }
    public void setIdAeropuertoActual(String idAeropuertoActual) { this.idAeropuertoActual = idAeropuertoActual; }

    public Long getIdVueloActual() { return idVueloActual; }
    public void setIdVueloActual(Long idVueloActual) { this.idVueloActual = idVueloActual; }

    public ShipmentStatus getEstado() { return estado; }
    public void setEstado(ShipmentStatus estado) { this.estado = estado; }

    // Getters y Setters (transient fields para algoritmos)
    public Order getOrder() { return parentOrder; }
    public Order getParentOrder() { return parentOrder; }
    public void setParentOrder(Order parentOrder) { this.parentOrder = parentOrder; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }

    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }

    public LocalDateTime getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(LocalDateTime arrival) { this.estimatedArrival = arrival; }

    public boolean isInterContinental() {
        // ✅ CORRECCIÓN: origin y destination son campos TRANSIENT del Shipment
        // No existen en la BD, solo se populan para los algoritmos
        if (origin == null || destination == null) {
            return false;
        }
        
        String originContinent = origin.getContinente();
        String destContinent = destination.getContinente();
        
        return originContinent != null 
            && destContinent != null 
            && !originContinent.equals(destContinent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(id, ((Shipment) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
