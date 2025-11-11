package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "airport") // Coincide con tu tabla: "airport"
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // SQL: id bigint UN AI PK
    private Long id;

    @Column(name = "continent") // SQL: continent enum(...)
    private String continent;

    @Column(name = "code") // SQL: code char(4)
    private String code;

    @Column(name = "city") // SQL: city varchar(100)
    private String city;

    @Column(name = "country") // SQL: country varchar(100)
    private String country;

    @Column(name = "city_acronym") // SQL: city_acronym varchar(10)
    private String cityAcronym; // Java usa camelCase

    @Column(name = "gmt") // SQL: gmt int
    private Integer gmt;

    @Column(name = "capacity") // SQL: capacity int
    private Integer capacity;

    @Column(name = "latitude") // SQL: latitude varchar(20)
    private String latitude; // Es un varchar en tu BD, así que lo ponemos String

    @Column(name = "longitude") // SQL: longitude varchar(20)
    private String longitude; // Es un varchar en tu BD, así que lo ponemos String

    @Column(name = "status") // SQL: status enum(...)
    private String status;

    @Column(name = "is_hub") // SQL: is_hub tinyint(1)
    private boolean isHub; // tinyint(1) se mapea a boolean

    // --- Constructor, Getters y Setters ---
    // JPA necesita un constructor vacío
    public Airport() {
    }

    // --- GETTERS Y SETTERS ---
    // (Asegúrate de tenerlos para TODOS los campos)

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

    public boolean isHub() {
        return isHub;
    }

    public void setHub(boolean hub) {
        isHub = hub;
    }

}