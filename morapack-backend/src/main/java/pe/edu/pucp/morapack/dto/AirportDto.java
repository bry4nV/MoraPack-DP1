package pe.edu.pucp.morapack.dto;

public class AirportDto {
    private Integer id;
    private String continent;
    private String code;
    private String city;
    private String country;
    private String cityAcronym;
    private Integer gmt;
    private Integer capacity;
    private String latitude;
    private String longitude;
    private Boolean isHub;

    public AirportDto() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

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

    public Boolean getIsHub() { return isHub; }
    public void setIsHub(Boolean isHub) { this.isHub = isHub; }
}
