package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "flight") // ← CAMBIO: de "vuelo" a "flight"
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idVuelo")
    private Long id;

    @Column(name = "idAeropuertoOrigen")
    private String idAeropuertoOrigen;

    @Column(name = "idAeropuertoDestino")
    private String idAeropuertoDestino;

    @Column(name = "horaSalida")
    private LocalTime horaSalida;

    @Column(name = "horaLlegada")
    private LocalTime horaLlegada;

    @Column(name = "capacidad")
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "ENUM('SCHEDULED','DELAYED','CANCELLED','COMPLETED')")
    private FlightStatus estado;

    // Campos transient para algoritmos (no en BD)
    private transient Airport origin;
    private transient Airport destination;

    public Flight() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdAeropuertoOrigen() { return idAeropuertoOrigen; }
    public void setIdAeropuertoOrigen(String idAeropuertoOrigen) { this.idAeropuertoOrigen = idAeropuertoOrigen; }

    public String getIdAeropuertoDestino() { return idAeropuertoDestino; }
    public void setIdAeropuertoDestino(String idAeropuertoDestino) { this.idAeropuertoDestino = idAeropuertoDestino; }

    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }

    public LocalTime getHoraLlegada() { return horaLlegada; }
    public void setHoraLlegada(LocalTime horaLlegada) { this.horaLlegada = horaLlegada; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public FlightStatus getEstado() { return estado; }
    public void setEstado(FlightStatus estado) { this.estado = estado; }

    // Transient getters/setters
    public Airport getOrigin() { return origin; }
    public void setOrigin(Airport origin) { this.origin = origin; }

    public Airport getDestination() { return destination; }
    public void setDestination(Airport destination) { this.destination = destination; }
}
