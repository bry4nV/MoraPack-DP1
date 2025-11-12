package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VueloDTO {
    @JsonProperty("codigo")
    public String codigo;
    
    @JsonProperty("origen")
    public AeropuertoDTO origen;
    
    @JsonProperty("destino")
    public AeropuertoDTO destino;
    
    @JsonProperty("salidaProgramadaISO")
    public String salidaProgramadaISO;
    
    @JsonProperty("llegadaProgramadaISO")
    public String llegadaProgramadaISO;
    
    @JsonProperty("capacidad")
    public int capacidad;
    
    @JsonProperty("preplanificado")
    public boolean preplanificado;
    
    @JsonProperty("estado")
    public String estado;
    
    // Getters y Setters
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public AeropuertoDTO getOrigen() { return origen; }
    public void setOrigen(AeropuertoDTO origen) { this.origen = origen; }
    
    public AeropuertoDTO getDestino() { return destino; }
    public void setDestino(AeropuertoDTO destino) { this.destino = destino; }
    
    public String getSalidaProgramadaISO() { return salidaProgramadaISO; }
    public void setSalidaProgramadaISO(String salidaProgramadaISO) { this.salidaProgramadaISO = salidaProgramadaISO; }
    
    public String getLlegadaProgramadaISO() { return llegadaProgramadaISO; }
    public void setLlegadaProgramadaISO(String llegadaProgramadaISO) { this.llegadaProgramadaISO = llegadaProgramadaISO; }
    
    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }
    
    public boolean isPreplanificado() { return preplanificado; }
    public void setPreplanificado(boolean preplanificado) { this.preplanificado = preplanificado; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}







