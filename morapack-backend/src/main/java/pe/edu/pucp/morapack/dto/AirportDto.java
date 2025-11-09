package pe.edu.pucp.morapack.dto;

public class AirportDto {
    
    // Tipos de datos actualizados para coincidir con la entidad
    private Long id;
    private String continent;
    private String code;        // <-- Añadido
    private String city;
    private String country;
    private String cityAcronym; // <-- Añadido
    private Integer gmt;
    private Integer capacity;
    private String latitude;    // <-- Añadido
    private String longitude;   // <-- Añadido
    private String status;      // <-- Añadido
    private boolean isHub;      // <-- Tipo corregido

    public AirportDto() {}

    // --- Getters y Setters para TODOS los campos ---

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

    
    public boolean getIsHub() {
        return isHub;
    }


    public void setHub(boolean hub) {
        isHub = hub;
    }
}