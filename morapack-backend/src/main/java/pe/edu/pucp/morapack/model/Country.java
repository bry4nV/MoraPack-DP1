package pe.edu.pucp.morapack.model;

public class Country {
    private int id;
    private String name;
    private Continent continent;

    public Country(int id, String n, Continent c) {
        this.id = id;
        this.name = n;
        this.continent = c;
    }

    public Continent getContinent() {
        return continent;
    }
}