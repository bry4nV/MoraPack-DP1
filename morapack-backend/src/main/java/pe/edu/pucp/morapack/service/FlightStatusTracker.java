package pe.edu.pucp.morapack.service;

import pe.edu.pucp.morapack.dto.simulation.ItineraryDTO;
import pe.edu.pucp.morapack.dto.simulation.RouteSegmentDTO;
import pe.edu.pucp.morapack.dto.simulation.FlightDTO;
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
     * Ubicación física del vuelo (usado para determinar si es cancelable)
     * Nota: Diferente de FlightStatus (modelo general que usa SCHEDULED/DELAYED/CANCELLED/COMPLETED)
     */
    public enum FlightLocation {
        ON_GROUND_ORIGIN,       // En tierra en origen (cancelable)
        IN_AIR,                 // En vuelo (NO cancelable)
        ON_GROUND_DESTINATION,  // En destino (completado)
        NOT_SCHEDULED           // No programado aún
    }
    
    /**
     * Información de estado de un vuelo
     */
    public static class FlightStatusInfo {
        public final FlightLocation status;
        public final String flightId;
        public final String origin;
        public final String destination;
        public final LocalDateTime scheduledDeparture;
        public final LocalDateTime scheduledArrival;
        public final boolean cancellable;
        public final boolean cancelled;  // ✅ NEW: indica si el vuelo fue cancelado

        public FlightStatusInfo(
                FlightLocation status,
                String flightId,
                String origin,
                String destination,
                LocalDateTime scheduledDeparture,
                LocalDateTime scheduledArrival) {
            this(status, flightId, origin, destination, scheduledDeparture, scheduledArrival, false);
        }

        public FlightStatusInfo(
                FlightLocation status,
                String flightId,
                String origin,
                String destination,
                LocalDateTime scheduledDeparture,
                LocalDateTime scheduledArrival,
                boolean cancelled) {
            this.status = status;
            this.flightId = flightId;
            this.origin = origin;
            this.destination = destination;
            this.scheduledDeparture = scheduledDeparture;
            this.scheduledArrival = scheduledArrival;
            this.cancellable = (status == FlightLocation.ON_GROUND_ORIGIN) && !cancelled;
            this.cancelled = cancelled;
        }

        @Override
        public String toString() {
            String statusStr = cancelled ? "CANCELLED" : status.toString();
            return String.format("[%s | %s→%s @ %s | %s]",
                flightId, origin, destination, scheduledDeparture, statusStr);
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
    public void updateFlightStatuses(List<ItineraryDTO> itineraries, LocalDateTime currentTime) {
        logger.debug("🔍 Actualizando estados de vuelos (tiempo: {})", currentTime);

        // Limpiar cache
        flightStatusCache.clear();

        // Procesar todos los vuelos
        for (ItineraryDTO itinerary : itineraries) {
            for (RouteSegmentDTO segment : itinerary.segments) {
                FlightDTO flight = segment.flight;

                String flightId = getFlightId(flight);
                FlightLocation status = determineFlightStatus(flight, currentTime);

                FlightStatusInfo info = new FlightStatusInfo(
                    status,
                    flightId,
                    flight.origin.code,
                    flight.destination.code,
                    parseToLocalDateTime(flight.scheduledDepartureISO),
                    parseToLocalDateTime(flight.scheduledArrivalISO)
                );

                flightStatusCache.put(flightId, info);
            }
        }

        logger.debug("✅ Estados actualizados: {} vuelos rastreados", flightStatusCache.size());

        // DEBUG: Log todos los vuelos rastreados para identificar cuáles están en uso
        if (flightStatusCache.size() > 0) {
            logger.info("📋 VUELOS EN USO (rastreados en itinerarios):");
            flightStatusCache.values().stream()
                .limit(20)  // Limitar a 20 para no saturar logs
                .forEach(flight -> logger.info("   ✈️ {} | Estado: {} | Cancelable: {}",
                    flight.flightId, flight.status, flight.cancellable));

            if (flightStatusCache.size() > 20) {
                logger.info("   ... y {} vuelos más", flightStatusCache.size() - 20);
            }
        }
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

    /**
     * Obtiene un vuelo específico por su ID.
     * @param flightId ID del vuelo en formato "ORIGIN-DESTINATION-YYYY-MM-DD-HH-MM"
     * @return FlightStatusInfo o null si no existe
     */
    public FlightStatusInfo getFlightById(String flightId) {
        return flightStatusCache.get(flightId);
    }

    /**
     * Registra un vuelo cancelado en el tracker.
     * Los vuelos cancelados no aparecen en itinerarios, pero necesitamos mostrarlos en el frontend.
     *
     * @param origin Código IATA de origen
     * @param destination Código IATA de destino
     * @param scheduledDeparture Hora de salida programada
     * @param scheduledArrival Hora de llegada programada
     */
    public void registerCancelledFlight(
            String origin,
            String destination,
            LocalDateTime scheduledDeparture,
            LocalDateTime scheduledArrival) {

        String scheduledTime = String.format("%02d:%02d",
            scheduledDeparture.getHour(),
            scheduledDeparture.getMinute());

        String flightId = String.format("%s-%s-%s", origin, destination, scheduledTime);

        // Crear FlightStatusInfo marcado como cancelado
        FlightStatusInfo cancelledFlight = new FlightStatusInfo(
            FlightLocation.ON_GROUND_ORIGIN, // Estado antes de cancelación
            flightId,
            origin,
            destination,
            scheduledDeparture,
            scheduledArrival,
            true  // cancelled = true
        );

        flightStatusCache.put(flightId, cancelledFlight);
        logger.info("✈️ Vuelo cancelado registrado: {}", flightId);
    }

    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Determina el estado de un vuelo basado en el tiempo actual.
     */
    private FlightLocation determineFlightStatus(FlightDTO flight, LocalDateTime currentTime) {
        LocalDateTime departure = parseToLocalDateTime(flight.scheduledDepartureISO);
        LocalDateTime arrival = parseToLocalDateTime(flight.scheduledArrivalISO);

        // Antes de la salida → En tierra en origen
        if (currentTime.isBefore(departure)) {
            return FlightLocation.ON_GROUND_ORIGIN;
        }

        // Entre salida y llegada → En vuelo
        if (currentTime.isAfter(departure) && currentTime.isBefore(arrival)) {
            return FlightLocation.IN_AIR;
        }

        // Después de la llegada → En tierra en destino
        if (currentTime.isAfter(arrival) || currentTime.isEqual(arrival)) {
            return FlightLocation.ON_GROUND_DESTINATION;
        }

        // Caso por defecto (no debería llegar aquí)
        return FlightLocation.NOT_SCHEDULED;
    }

    /**
     * Genera un ID único para un vuelo.
     */
    private String getFlightId(FlightDTO flight) {
        String scheduledTime = extractTime(flight.scheduledDepartureISO);
        return String.format("%s-%s-%s",
            flight.origin.code,
            flight.destination.code,
            scheduledTime
        );
    }
    
    /**
     * ✅ FIX: Parsea un timestamp ISO a LocalDateTime, aceptando formato con o sin zona horaria.
     */
    private LocalDateTime parseToLocalDateTime(String isoTimestamp) {
        if (isoTimestamp.contains("Z") || isoTimestamp.matches(".*[+-]\\d{2}:\\d{2}$")) {
            // Si tiene zona horaria, parsearlo como OffsetDateTime y luego convertir a LocalDateTime
            java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse(isoTimestamp);
            return offsetDateTime.toLocalDateTime();
        } else {
            // Si no tiene zona horaria, parsearlo directamente como LocalDateTime
            return LocalDateTime.parse(isoTimestamp);
        }
    }
    
    /**
     * Extrae solo la hora (HH:mm) de un timestamp ISO.
     * ✅ FIX: Ahora acepta fechas con zona horaria (Z)
     */
    private String extractTime(String isoTimestamp) {
        // Si tiene zona horaria (Z o +/-offset), usar OffsetDateTime
        if (isoTimestamp.contains("Z") || isoTimestamp.matches(".*[+-]\\d{2}:\\d{2}$")) {
            java.time.OffsetDateTime dateTime = java.time.OffsetDateTime.parse(isoTimestamp);
            return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
        } else {
            // Si no tiene zona horaria, usar LocalDateTime
            LocalDateTime dateTime = LocalDateTime.parse(isoTimestamp);
            return String.format("%02d:%02d", dateTime.getHour(), dateTime.getMinute());
        }
    }
    
    /**
     * Registra un vuelo cancelado en el tracker (para vuelos que existen en BD pero no en itinerarios).
     * Esto permite que las cancelaciones procesen vuelos que no están siendo usados actualmente.
     *
     * @param origin Código de aeropuerto de origen
     * @param destination Código de aeropuerto de destino
     * @param scheduledTime Hora programada (HH:mm)
     * @param cancellationTime Tiempo de la cancelación
     */
    public void registerCancelledFlight(String origin, String destination, String scheduledTime, LocalDateTime cancellationTime) {
        String flightId = String.format("%s-%s-%s", origin, destination, scheduledTime);

        // Crear un FlightStatusInfo marcado como CANCELADO
        // El vuelo estaba en tierra, pero ahora está cancelado
        FlightStatusInfo info = new FlightStatusInfo(
            FlightLocation.ON_GROUND_ORIGIN,
            flightId,
            origin,
            destination,
            cancellationTime, // Como no tenemos los datos exactos, usamos la hora de cancelación
            cancellationTime.plusHours(2), // Estimación de llegada
            true  // ✅ CANCELLED = true
        );

        flightStatusCache.put(flightId, info);
        logger.info("📝 Vuelo registrado en tracker: {} (estado: ON_GROUND_ORIGIN, CANCELLED)", flightId);
    }

    /**
     * Limpia el cache de estados.
     */
    public void clearCache() {
        flightStatusCache.clear();
        logger.info("🧹 Cache de estados de vuelos limpiado");
    }
    
    /**
     * Obtiene estadísticas de ubicaciones de vuelos.
     */
    public Map<FlightLocation, Long> getStatusStatistics() {
        Map<FlightLocation, Long> stats = new EnumMap<>(FlightLocation.class);

        for (FlightLocation location : FlightLocation.values()) {
            long count = flightStatusCache.values().stream()
                .filter(info -> info.status == location)
                .count();
            stats.put(location, count);
        }

        return stats;
    }
}

