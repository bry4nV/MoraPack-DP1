package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "airport") // ← Cambiar de vuelta a "airport"
public class Airport {
    @Id
    @Column(name = "idAeropuerto")
    private String id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "pais")
    private String pais;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "GMT")
    private String gmt; // ← CAMBIO: de Integer a String

    @Column(name = "capacidad")
    private Integer capacidad;

    @Column(name = "continente")
    private String continente;

    @Column(name = "esSede")
    private Integer esSede;

    // domain fields kept for compatibility with algos domain; not persisted here
    private transient Country country;
    private transient double latitude;
    private transient double longitude;

    public Airport() {}

    // convenience constructor for domain usage
    public Airport(String id, String nombre, String ciudad, String pais, String continente,
                   int capacidad, String latitud, String longitud, String gmt) { // ← CORRECCIÓN: parámetro gmt al final
        this.id = id;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.capacidad = capacidad; // ← CORRECCIÓN: capacidad es int
        this.gmt = gmt; // ← CORRECCIÓN: gmt es String
        // Parse coordinates for algorithms
        try {
            this.latitude = Double.parseDouble(latitud);
            this.longitude = Double.parseDouble(longitud);
        } catch (NumberFormatException e) {
            this.latitude = 0.0;
            this.longitude = 0.0;
        }
    }

    // JPA-friendly getters/setters for persisted columns
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

    public Integer getEsSede() { return esSede; }
    public void setEsSede(Integer esSede) { this.esSede = esSede; }

    // Domain accessors (transient)
    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Helper methods
    public boolean isHub() {
        return esSede != null && esSede == 1;
    }
}

