package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "airport")
public class Airport {
    @Id
    @Column(name = "idAeropuerto")
    private String id;

    @Column(name = "nombre")
    private String name; // ← CAMBIO: de nombre a name

    @Column(name = "pais")
    private String country; // ← CAMBIO: de pais a country

    @Column(name = "ciudad")
    private String city; // ← CAMBIO: de ciudad a city

    @Column(name = "GMT")
    private String gmt;

    @Column(name = "capacidad")
    private Integer capacity; // ← CAMBIO: de capacidad a capacity

    @Column(name = "continente")
    private String continent; // ← CAMBIO: de continente a continent

    @Column(name = "esSede")
    private Integer isHub; // ← CAMBIO: de esSede a isHub

    // domain fields kept for compatibility with algos domain; not persisted here
    private transient Country countryObj; // ← CAMBIO: evitar conflicto con country field
    private transient double latitude;
    private transient double longitude;

    public Airport() {}

    // convenience constructor for domain usage
    public Airport(String id, String name, String city, String country, String continent,
                   int capacity, String latitude, String longitude, String gmt) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.country = country;
        this.continent = continent;
        this.capacity = capacity;
        this.gmt = gmt;
        // Parse coordinates for algorithms
        try {
            this.latitude = Double.parseDouble(latitude);
            this.longitude = Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            this.latitude = 0.0;
            this.longitude = 0.0;
        }
    }

    // JPA-friendly getters/setters for persisted columns
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getGmt() { return gmt; }
    public void setGmt(String gmt) { this.gmt = gmt; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getContinent() { return continent; }
    public void setContinent(String continent) { this.continent = continent; }

    public Integer getIsHub() { return isHub; }
    public void setIsHub(Integer isHub) { this.isHub = isHub; }

    // Domain accessors (transient)
    public Country getCountryObj() { return countryObj; }
    public void setCountryObj(Country countryObj) { this.countryObj = countryObj; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Helper methods
    public boolean isMainHub() { // ← CAMBIO: nombre más claro
        return isHub != null && isHub == 1;
    }
}

