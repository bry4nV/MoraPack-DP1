package pe.edu.pucp.morapack.model.simulation;

import jakarta.persistence.*;

/**
 * Entidad JPA para la tabla shipment del esquema moraTravelSimulation.
 * Este esquema contiene datos históricos (~2 años) para simulaciones.
 */
@Entity
@Table(name = "shipment", schema = "moraTravelSimulation")
public class SimShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "quantity_products", nullable = false)
    private Integer quantityProducts;

    // Constructors
    public SimShipment() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getQuantityProducts() {
        return quantityProducts;
    }

    public void setQuantityProducts(Integer quantityProducts) {
        this.quantityProducts = quantityProducts;
    }
}
