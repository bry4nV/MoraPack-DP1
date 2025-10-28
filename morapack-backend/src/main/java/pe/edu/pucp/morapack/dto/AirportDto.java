package pe.edu.pucp.morapack.dto;

public class AirportDto {
    private String id;
    private String name; // ← CAMBIO
    private String country; // ← CAMBIO
    private String city; // ← CAMBIO
    private String gmt;
    private Integer capacity; // ← CAMBIO
    private String continent; // ← CAMBIO
    private Boolean isHub; // ← CAMBIO

    public AirportDto() {}

    // Getters y Setters
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

    public Boolean getIsHub() { return isHub; }
    public void setIsHub(Boolean isHub) { this.isHub = isHub; }
}
