package pe.edu.pucp.morapack.dto;

import pe.edu.pucp.morapack.model.FlightStatus;

public class FlightDto {
    private Long id;
    private String idAeropuertoOrigen;
    private String idAeropuertoDestino;
    private String horaSalida;
    private String horaLlegada;
    private Integer capacidad;
    private FlightStatus estado;

    public FlightDto() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdAeropuertoOrigen() { return idAeropuertoOrigen; }
    public void setIdAeropuertoOrigen(String idAeropuertoOrigen) { this.idAeropuertoOrigen = idAeropuertoOrigen; }

    public String getIdAeropuertoDestino() { return idAeropuertoDestino; }
    public void setIdAeropuertoDestino(String idAeropuertoDestino) { this.idAeropuertoDestino = idAeropuertoDestino; }

    public String getHoraSalida() { return horaSalida; }
    public void setHoraSalida(String horaSalida) { this.horaSalida = horaSalida; }

    public String getHoraLlegada() { return horaLlegada; }
    public void setHoraLlegada(String horaLlegada) { this.horaLlegada = horaLlegada; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public FlightStatus getEstado() { return estado; }
    public void setEstado(FlightStatus estado) { this.estado = estado; }
}
