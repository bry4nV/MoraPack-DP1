package pe.edu.pucp.morapack.model;

import java.util.Objects;

public class Airport {
    private int id;
    private String name;
    private String code;
    private String city;
    private Country country;
    private int storageCapacity;
    private int gmt; // timezone offset

    private double latitude;
    private double longitude;

    public Airport(int id, String code, String name, String city, Country country, int capacity, int gmt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.city = city;
        this.country = country;
        this.storageCapacity = capacity;
        this.gmt = gmt;
    }

    public Airport(int id, String code, String name, String city, Country country, int capacity, int gmt, double latitude, double longitude) {
        this(id, code, name, city, country, capacity, gmt);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public Country getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getCode() {
        return code;
    }

    public int getGmt() {
        return gmt;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getStorageCapacity() {
        return storageCapacity;
    }
    
    /**
     * Obtiene el continente del aeropuerto
     */
    public Continent getContinent() {
        return country != null ? country.getContinent() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Airport) o).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}