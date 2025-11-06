package pe.edu.pucp.morapack.service;

import pe.edu.pucp.morapack.dto.simulation.ItinerarioDTO;
import pe.edu.pucp.morapack.dto.simulation.SegmentoDTO;
import pe.edu.pucp.morapack.dto.simulation.VueloDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para rastrear el estado de los vuelos en tiempo real.
 * 
 * Determina si un vuelo está:
 * - ON_GROUND_ORIGIN: En tierra en origen (puede cancelarse)
 * - IN_AIR: En vuelo (NO puede cancelarse)
 * - ON_GROUND_DESTINATION: En tierra en destino (completado)
 */
@Service
public class FlightStatusTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(FlightStatusTracker.class);
    
    /**
     * Estados posibles de un vuelo
     */
    public enum FlightStatus {
        ON_GROUND_ORIGIN,       // En tierra en origen (cancelable)
        IN_AIR,                 // En vuelo (NO cancelable)
        ON_GROUND_DESTINATION,  // En destino (completado)
        NOT_SCHEDULED           // No programado aún
    }
    
    /**
     * Información de estado de un vuelo
     */
    public static class FlightStatusInfo {
        public final FlightStatus status;
        public final String flightId;
        public final String origin;
        public final String destination;
        public final LocalDateTime scheduledDeparture;
        public final LocalDateTime scheduledArrival;
        public final boolean cancellable;
        
        public FlightStatusInfo(
                FlightStatus status,
                String flightId,
                String origin,
                String destination,
                LocalDateTime scheduledDeparture,
                LocalDateTime scheduledArrival) {
            this.status = status;
            this.flightId = flightId;
            this.origin = origin;
            this.destination = destination;
            this.scheduledDeparture = scheduledDeparture;
            this.scheduledArrival = scheduledArrival;
            this.cancellable = (status == FlightStatus.ON_GROUND_ORIGIN);
        }
        
        @Override
        public String toString() {
            return String.format("[%s | %s→%s @ %s | %s]", 
                flightId, origin, destination, scheduledDeparture, status);
        }
    }
    
    // Cache de estados de vuelos (key: flightId, value: último estado conocido)
    private final Map<String, FlightStatusInfo> flightStatusCache = new ConcurrentHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Actualiza el estado de todos los vuelos basado en los itinerarios actuales.
     * 
     * @param itinerarios Lista de itinerarios actuales
     * @param currentTime Tiempo actual de la simulación
     */
    public void updateFlightStatuses(List<ItinerarioDTO> itinerarios, LocalDateTime currentTime) {
        logger.debug("🔍 Actualizando estados de vuelos (tiempo: {})", currentTime);
        
        // Limpiar cache
        flightStatusCache.clear();
        
        // Procesar todos los vuelos
        for (ItinerarioDTO itinerario : itinerarios) {
            for (SegmentoDTO segmento : itinerario.segmentos) {
                VueloDTO vuelo = segmento.vuelo;
                
                String flightId = getFlightId(vuelo);
                FlightStatus status = determineFlightStatus(vuelo, currentTime);
                
                FlightStatusInfo info = new FlightStatusInfo(
                    status,
                    flightId,
                    vuelo.origen.codigo,
                    vuelo.destino.codigo,
                    LocalDateTime.parse(vuelo.salidaProgramadaISO),
                    LocalDateTime.parse(vuelo.llegadaProgramadaISO)
                );
                
                flightStatusCache.put(flightId, info);
            }
        }
        
        logger.debug("✅ Estados actualizados: {} vuelos rastreados", flightStatusCache.size());
    }
    
    /**
     * Obtiene el estado de un vuelo específico.
     * 
     * @param origin Origen (IATA)
     * @param destination Destino (IATA)
     * @param scheduledTime Hora programada (HH:mm)
     * @return Estado del vuelo o null si no se encuentra
     */
    public FlightStatusInfo getFlightStatus(String origin, String destination, String scheduledTime) {
        String flightId = String.format("%s-%s-%s", origin, destination, scheduledTime);
        return flightStatusCache.get(flightId);
    }
    
    /**
     * Verifica si un vuelo puede ser cancelado en el tiempo actual.
     * 
     * @param origin Origen
     * @param destination Destino
     * @param scheduledTime Hora programada
     * @return true si el vuelo puede ser cancelado
     */
    public boolean canCancelFlight(String origin, String destination, String scheduledTime) {
        FlightStatusInfo info = getFlightStatus(origin, destination, scheduledTime);
        
        if (info == null) {
            logger.warn("⚠️ Vuelo no encontrado: {}-{}-{}", origin, destination, scheduledTime);
            return false;
        }
        
        boolean cancellable = info.cancellable;
        
        if (!cancellable) {
            logger.warn("❌ Vuelo NO cancelable: {} (estado: {})", info.flightId, info.status);
        }
        
        return cancellable;
    }
    
    /**
     * Obtiene todos los vuelos que pueden ser cancelados en el momento actual.
     */
    public List<FlightStatusInfo> getCancellableFlights() {
        return flightStatusCache.values().stream()
            .filter(info -> info.cancellable)
            .toList();
    }
    
    /**
     * Obtiene todos los vuelos rastreados.
     */
    public Collection<FlightStatusInfo> getAllFlights() {
        return flightStatusCache.values();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Determina el estado de un vuelo basado en el tiempo actual.
     */
    private FlightStatus determineFlightStatus(VueloDTO vuelo, LocalDateTime currentTime) {
        LocalDateTime departure = LocalDateTime.parse(vuelo.salidaProgramadaISO);
        LocalDateTime arrival = LocalDateTime.parse(vuelo.llegadaProgramadaISO);
        
        // Antes de la salida → En tierra en origen
        if (currentTime.isBefore(departure)) {
            return FlightStatus.ON_GROUND_ORIGIN;
        }
        
        // Entre salida y llegada → En vuelo
        if (currentTime.isAfter(departure) && currentTime.isBefore(arrival)) {
            return FlightStatus.IN_AIR;
        }
        
        // Después de la llegada → En tierra en destino
        if (currentTime.isAfter(arrival) || currentTime.isEqual(arrival)) {
            return FlightStatus.ON_GROUND_DESTINATION;
        }
        
        // Caso por defecto (no debería llegar aquí)
        return FlightStatus.NOT_SCHEDULED;
    }
    
    /**
     * Genera un ID único para un vuelo.
     */
    private String getFlightId(VueloDTO vuelo) {
        String scheduledTime = extractTime(vuelo.salidaProgramadaISO);
        return String.format("%s-%s-%s", 
            vuelo.origen.codigo, 
            vuelo.destino.codigo, 
            scheduledTime
        );
    }
    
    /**
     * Extrae solo la hora (HH:mm) de un timestamp ISO.
     */
    private String extractTime(String isoTimestamp) {
        LocalDateTime dateTime = LocalDateTime.parse(isoTimestamp);
        return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
    }
    
    /**
     * Limpia el cache de estados.
     */
    public void clearCache() {
        flightStatusCache.clear();
        logger.info("🧹 Cache de estados de vuelos limpiado");
    }
    
    /**
     * Obtiene estadísticas de estados de vuelos.
     */
    public Map<FlightStatus, Long> getStatusStatistics() {
        Map<FlightStatus, Long> stats = new EnumMap<>(FlightStatus.class);
        
        for (FlightStatus status : FlightStatus.values()) {
            long count = flightStatusCache.values().stream()
                .filter(info -> info.status == status)
                .count();
            stats.put(status, count);
        }
        
        return stats;
    }
}

