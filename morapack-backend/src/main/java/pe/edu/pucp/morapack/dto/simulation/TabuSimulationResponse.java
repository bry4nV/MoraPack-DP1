package pe.edu.pucp.morapack.dto.simulation;

public class TabuSimulationResponse {
    public TabuSearchMeta meta;
    public AeropuertoDTO[] aeropuertos;
    public ItinerarioDTO[] itinerarios;
    
    // ✅ NEW: Order tracking information
    public OrderSummaryDTO[] pedidos;
    public OrderMetricsDTO metricas;
}


