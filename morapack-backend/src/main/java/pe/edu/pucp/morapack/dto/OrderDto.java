package pe.edu.pucp.morapack.dto;

// Importamos los tipos que usará el Service para la conversión
import java.time.LocalDate;
import java.time.LocalTime;

public class OrderDto {

    // --- CAMPOS SINCRONIZADOS ---
    private Long id;
    private String orderNumber;
    private String orderDate; // Se convertirá de LocalDate a String
    private String orderTime; // Se convertirá de LocalTime a String
    private String airportDestinationCode;
    private Integer quantity;
    private String clientCode;
    private String status;

    public OrderDto() {}

    // --- GETTERS Y SETTERS ---

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

    public String getOrderDate() {
        return orderDate;
    }

    // El Service llamará a este método con un String
    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderTime() {
        return orderTime;
    }

    // El Service llamará a este método con un String
    public void setOrderTime(String orderTime) {
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