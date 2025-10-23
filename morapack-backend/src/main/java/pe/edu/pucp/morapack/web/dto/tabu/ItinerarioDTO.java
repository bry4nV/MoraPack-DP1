package pe.edu.pucp.morapack.web.dto.tabu;

public class ItinerarioDTO {
    public String id;
    public SegmentoDTO[] segmentos;
    public double progressMeters; // optional
    public double positionLon;
    public double positionLat;
}
