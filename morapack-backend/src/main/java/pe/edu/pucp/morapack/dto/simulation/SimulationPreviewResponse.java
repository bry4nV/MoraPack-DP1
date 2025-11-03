package pe.edu.pucp.morapack.dto.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Response for preview endpoint - shows what will be in the simulation
 * BEFORE it starts.
 */
public class SimulationPreviewResponse {
    public int totalPedidos;
    public int totalProductos;
    public int totalVuelos;
    
    public DateRangeDTO dateRange;
    public List<OrderSummaryDTO> pedidos = new ArrayList<>();
    public List<String> aeropuertosInvolucrados = new ArrayList<>();
    public AeropuertoDTO[] aeropuertos;  // ✅ Full airport data for map display
    public EstadisticasDTO estadisticas;
    
    public static class DateRangeDTO {
        public String start;  // ISO format
        public String end;    // ISO format
        
        public DateRangeDTO() {}
        
        public DateRangeDTO(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
    
    public static class EstadisticasDTO {
        public int[] pedidosPorDia;        // Array de 7 elementos (para WEEKLY)
        public double productosPromedioPorPedido;
        public int pedidoMaxProductos;
        public int pedidoMinProductos;
        
        public EstadisticasDTO() {}
    }
}

