package pe.edu.pucp.morapack.model.simulation;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad JPA para la tabla order del esquema moraTravelSimulation.
 * Este esquema contiene datos históricos (~2 años) para simulaciones.
 */
@Entity
@Table(name = "`order`", schema = "moraTravelSimulation")
public class SimOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_number", length = 9, nullable = false)
    private String orderNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "order_time", nullable = false)
    private LocalTime orderTime;

    @Column(name = "airport_destination_code", length = 4, nullable = false)
    private String airportDestinationCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "client_code", length = 7, nullable = false)
    private String clientCode;

    @Column(name = "status", nullable = false)
    private String status;

    // Constructors
    public SimOrder() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalTime orderTime) {
        this.orderTime = orderTime;
    }

    public String getAirportDestinationCode() {
        return airportDestinationCode;
    }

    public void setAirportDestinationCode(String airportDestinationCode) {
        this.airportDestinationCode = airportDestinationCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
