package pe.edu.pucp.morapack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AirportDto {
    private Long id;
    private String continent;
    private String code;
    private String city;
    private String country;
    private String cityAcronym;
    private Integer gmt;
    private Integer capacity;
    private String latitude;
    private String longitude;
    private String status;
    
    @JsonProperty("isHub") // Importante: esto hace que JSON use "isHub"
    private boolean isHub;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContinent() { return continent; }
    public void setContinent(String continent) { this.continent = continent; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCityAcronym() { return cityAcronym; }
    public void setCityAcronym(String cityAcronym) { this.cityAcronym = cityAcronym; }

    public Integer getGmt() { return gmt; }
    public void setGmt(Integer gmt) { this.gmt = gmt; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Importante: usar isHub() no getHub()
    public boolean isHub() { return isHub; }
    public void setHub(boolean hub) { isHub = hub; }
}