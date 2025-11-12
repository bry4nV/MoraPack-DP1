package pe.edu.pucp.morapack.dto.simulation;

public class ItinerarioDTO {
    public String id;
    public int orderId; // ✅ ID del pedido al que pertenece este itinerario
    public SegmentoDTO[] segmentos;
    public double progressMeters; // optional
    public double positionLon;
    public double positionLat;
}







