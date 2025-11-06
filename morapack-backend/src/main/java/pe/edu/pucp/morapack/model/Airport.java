package pe.edu.pucp.morapack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "airport")
public class Airport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "continent")
    private String continent;

    @Column(name = "code")
    private String code;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "city_acronym")
    private String cityAcronym;

    @Column(name = "gmt")
    private Integer gmt;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "is_hub")
    private Integer isHub;

    // domain fields kept for compatibility with algos domain; not persisted here
    private transient Country countryObj;

    public Airport() {}

    // convenience constructor for domain usage
    public Airport(Integer id, String continent, String code, String city, String country, 
                   String cityAcronym, Integer gmt, Integer capacity, String latitude, 
                   String longitude, Integer isHub) {
        this.id = id;
        this.continent = continent;
        this.code = code;
        this.city = city;
        this.country = country;
        this.cityAcronym = cityAcronym;
        this.gmt = gmt;
        this.capacity = capacity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isHub = isHub;
    }

    // JPA-friendly getters/setters for persisted columns
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

    public Integer getIsHub() { return isHub; }
    public void setIsHub(Integer isHub) { this.isHub = isHub; }

    // Domain accessors (transient)
    public Country getCountryObj() { return countryObj; }
    public void setCountryObj(Country countryObj) { this.countryObj = countryObj; }

    // Helper methods
    public boolean isMainHub() {
        return isHub != null && isHub == 1;
    }

    // Parse helpers for algorithms
    public double getLatitudeAsDouble() {
        try {
            return Double.parseDouble(latitude);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public double getLongitudeAsDouble() {
        try {
            return Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

