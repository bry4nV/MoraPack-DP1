package pe.edu.pucp.morapack.dto;

import pe.edu.pucp.morapack.model.ShipmentStatus;

public class ShipmentDto {
    private Integer id;
    private Long idPedido;
    private String idAeropuertoActual;
    private Long idVueloActual;
    private ShipmentStatus estado;

    public ShipmentDto() {}

    // Getters y Setters
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
}
