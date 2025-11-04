package pe.edu.pucp.morapack.dto.simulation;

/**
 * Aggregate metrics about orders in the simulation.
 */
public class OrderMetricsDTO {
    public int totalPedidos;
    public int pendientes;
    public int enTransito;
    public int completados;
    public int sinAsignar;
    
    // Additional statistics
    public int totalProductos;
    public int productosAsignados;
    public double tasaAsignacionPercent;
    
    public OrderMetricsDTO() {}
    
    public OrderMetricsDTO(int totalPedidos, int pendientes, int enTransito, int completados, int sinAsignar) {
        this.totalPedidos = totalPedidos;
        this.pendientes = pendientes;
        this.enTransito = enTransito;
        this.completados = completados;
        this.sinAsignar = sinAsignar;
    }
}


