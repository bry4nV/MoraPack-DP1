package pe.edu.pucp.morapack.model;

public class Airport {
	private int id;
	private String code;
	private String name;
	private String city;
	private Country country;
	private double latitude;
	private double longitude;

	public Airport() {}

	public Airport(int id, String code, String name, String city, Country country, double latitude, double longitude) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.city = city;
		this.country = country;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public int getId() { return id; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public String getCity() { return city; }
	public Country getCountry() { return country; }
	public double getLatitude() { return latitude; }
	public double getLongitude() { return longitude; }
}

