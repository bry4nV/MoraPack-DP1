package pe.edu.pucp.morapack.model.simulation;

import jakarta.persistence.*;

/**
 * Entidad JPA para la tabla airport del esquema moraTravelSimulation.
 * Este esquema contiene datos históricos (~2 años) para simulaciones.
 */
@Entity
@Table(name = "airport", schema = "moraTravelSimulation")
public class SimAirport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "continent")
    private String continent;

    @Column(name = "code", length = 4, unique = true)
    private String code;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city_acronym", length = 10)
    private String cityAcronym;

    @Column(name = "gmt")
    private Integer gmt;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "latitude", length = 20)
    private String latitude;

    @Column(name = "longitude", length = 20)
    private String longitude;

    @Column(name = "status")
    private String status;

    @Column(name = "is_hub")
    private Boolean isHub;

    // Constructors
    public SimAirport() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCityAcronym() {
        return cityAcronym;
    }

    public void setCityAcronym(String cityAcronym) {
        this.cityAcronym = cityAcronym;
    }

    public Integer getGmt() {
        return gmt;
    }

    public void setGmt(Integer gmt) {
        this.gmt = gmt;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsHub() {
        return isHub;
    }

    public void setIsHub(Boolean isHub) {
        this.isHub = isHub;
    }
}
