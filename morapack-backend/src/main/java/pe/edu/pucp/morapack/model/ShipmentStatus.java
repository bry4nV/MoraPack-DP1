package pe.edu.pucp.morapack.model;

public enum ShipmentStatus {
    PENDING,        // Asignado a vuelo(s) pero primer vuelo aún no ha despegado
    IN_TRANSIT,     // En vuelo actualmente
    IN_WAREHOUSE,   // En tierra esperando conexión en hub
    DELIVERED,      // Llegó a destino final
    CANCELLED       // Cancelado
}
