package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Response for preview endpoint - shows what will be in the simulation
 * BEFORE it starts.
 */
public class SimulationPreviewResponse {
    @JsonProperty("totalOrders")
    public int totalOrders;

    @JsonProperty("totalProducts")
    public int totalProducts;

    @JsonProperty("totalFlights")
    public int totalFlights;

    @JsonProperty("dateRange")
    public DateRangeDTO dateRange;

    @JsonProperty("orders")
    public List<OrderSummaryDTO> orders = new ArrayList<>();

    @JsonProperty("involvedAirports")
    public List<String> involvedAirports = new ArrayList<>();

    @JsonProperty("airports")
    public AirportDTO[] airports;

    @JsonProperty("statistics")
    public StatisticsDTO statistics;
    
    public static class DateRangeDTO {
        public String start;  // ISO format
        public String end;    // ISO format
        
        public DateRangeDTO() {}
        
        public DateRangeDTO(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
    
    public static class StatisticsDTO {
        @JsonProperty("ordersPerDay")
        public int[] ordersPerDay;

        @JsonProperty("avgProductsPerOrder")
        public double avgProductsPerOrder;

        @JsonProperty("maxProductsOrder")
        public int maxProductsOrder;

        @JsonProperty("minProductsOrder")
        public int minProductsOrder;

        public StatisticsDTO() {}
    }
}

