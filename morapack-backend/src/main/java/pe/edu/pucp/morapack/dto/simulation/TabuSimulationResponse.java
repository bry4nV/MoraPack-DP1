package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TabuSimulationResponse {
    @JsonProperty("meta")
    public TabuSearchMeta meta;

    @JsonProperty("airports")
    public AirportDTO[] airports;

    @JsonProperty("itineraries")
    public ItineraryDTO[] itineraries;

    @JsonProperty("orders")
    public OrderSummaryDTO[] orders;

    @JsonProperty("metrics")
    public OrderMetricsDTO metrics;
}


