package pe.edu.pucp.morapack.dto.simulation;

import java.util.ArrayList;
import java.util.List;

public class AeropuertoDTO {
    public int id;
    public String nombre;
    public String codigo;
    public String ciudad;
    public double latitud;
    public double longitud;
    public int gmt;
    public boolean esSede;
    
    // ✅ Storage capacity information
    public int capacidadTotal;            // Total storage capacity
    public int capacidadUsada;            // Current capacity used (products on ground + in transit)
    public int capacidadDisponible;       // Available capacity
    public double porcentajeUso;          // Usage percentage (0-100)
    
    // ✅ Dynamic runtime information
    public int pedidosEnEspera;           // Orders originating from this airport (pending)
    public int pedidosDestino;            // Orders with this airport as destination
    public int productosEnEspera;         // Total products waiting at this airport
    public int vuelosActivosDesde;        // Active flights departing from here
    public int vuelosActivosHacia;        // Active flights arriving here
    public List<String> vuelosEnTierra = new ArrayList<>();   // Flight IDs currently at this airport
}


