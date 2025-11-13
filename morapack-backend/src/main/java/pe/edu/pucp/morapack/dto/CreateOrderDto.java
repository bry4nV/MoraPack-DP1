package pe.edu.pucp.morapack.dto;

// Este DTO solo contiene los campos que el usuario

public class CreateOrderDto {

    private String orderNumber;
    private String airportDestinationCode;
    private Integer quantity;
    private String clientCode;

    // --- Getters y Setters ---

    public String getOrderNumber() {
        return orderNumber;
    }
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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
}