package pe.edu.pucp.morapack.dto.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary information for a single order in the simulation.
 * Used for both preview (before simulation) and real-time tracking (during simulation).
 */
public class OrderSummaryDTO {
    public int id;
    public String codigo;              // "PED-1234"
    
    // Origen y destino
    public String origenCodigo;        // "SPIM"
    public String origenNombre;        // "Lima"
    public String destinoCodigo;       // "VABB"
    public String destinoNombre;       // "Mumbai"
    
    // Cantidades
    public int cantidadTotal;          // 45
    public int cantidadAsignada;       // 34 (0 in preview mode)
    public double progresoPercent;     // 75.5% (0 in preview mode)
    
    // Tiempos
    public String fechaSolicitudISO;   // "2025-12-01T08:00:00"
    public String fechaETA_ISO;        // Estimado de entrega (null in preview mode)
    
    // Estado
    public OrderStatus estado;         // PENDING, IN_TRANSIT, COMPLETED, UNASSIGNED
    
    // Vuelos asignados (empty in preview mode)
    public List<String> vuelosAsignados = new ArrayList<>(); // ["VU81-001", "VU81-002"]
    
    // Prioridad (opcional, for future use)
    public int prioridad = 3;          // 1-5 (3 = normal)
    
    public enum OrderStatus {
        PENDING,      // En cola, no asignado aún
        IN_TRANSIT,   // Parcial o totalmente asignado, en camino
        COMPLETED,    // 100% entregado
        UNASSIGNED    // No se pudo asignar
    }
}






