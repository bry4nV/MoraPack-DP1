package pe.edu.pucp.morapack.dto;

public class AirportDto {
    private String id;
    private String nombre;
    private String pais;
    private String ciudad;
    private String gmt; // ← CAMBIO: de Integer a String
    private Integer capacidad;
    private String continente;
    private Boolean esSede;

    public AirportDto() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getGmt() { return gmt; } // ← CAMBIO: de Integer a String
    public void setGmt(String gmt) { this.gmt = gmt; } // ← CAMBIO: de Integer a String

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public String getContinente() { return continente; }
    public void setContinente(String continente) { this.continente = continente; }

    public Boolean getEsSede() { return esSede; }
    public void setEsSede(Boolean esSede) { this.esSede = esSede; }
}
