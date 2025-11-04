package pe.edu.pucp.morapack.model;

public class Country {
    private int id;
    private String name;
    private Continent continent;

    public Country(int id, String name, Continent continent) {
        this.id = id;
        this.name = name;
        this.continent = continent;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Continent getContinent() { return continent; }
}