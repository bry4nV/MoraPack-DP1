package pe.edu.pucp.morapack.service;

import pe.edu.pucp.morapack.model.FlightCancellation;
import pe.edu.pucp.morapack.algos.data.loaders.CancellationFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar cancelaciones de vuelos (programadas y manuales).
 * 
 * Responsabilidades:
 * - Cargar cancelaciones programadas desde archivo
 * - Procesar cancelaciones manuales desde UI
 * - Ejecutar cancelaciones en el momento correcto
 * - Validar que los vuelos puedan ser cancelados
 */
@Service
public class CancellationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CancellationService.class);
    
    private final FlightStatusTracker flightStatusTracker;
    
    // Almacenamiento de cancelaciones
    private final Map<String, FlightCancellation> cancellations = new ConcurrentHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    
    public CancellationService(FlightStatusTracker flightStatusTracker) {
        this.flightStatusTracker = flightStatusTracker;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CARGA DE CANCELACIONES PROGRAMADAS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Carga cancelaciones programadas desde un archivo.
     * 
     * @param filePath Ruta del archivo
     * @param startDate Fecha de inicio de la simulación
     * @return Número de cancelaciones cargadas
     */
    public int loadScheduledCancellations(String filePath, LocalDate startDate) {
        logger.info("📂 Cargando cancelaciones programadas desde: {}", filePath);
        
        // Validar que el archivo exista
        if (!CancellationFileLoader.validateFile(filePath)) {
            logger.warn("⚠️ Archivo de cancelaciones no disponible, continuando sin cancelaciones programadas");
            return 0;
        }
        
        // Cargar cancelaciones
        List<FlightCancellation> loaded = CancellationFileLoader.loadCancellations(filePath, startDate);
        
        // Agregar al mapa
        for (FlightCancellation cancellation : loaded) {
            cancellations.put(cancellation.getId(), cancellation);
        }
        
        logger.info("✅ {} cancelaciones programadas cargadas", loaded.size());
        
        return loaded.size();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CANCELACIÓN MANUAL
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Solicita la cancelación manual de un vuelo (desde UI).
     * 
     * @param origin Origen del vuelo
     * @param destination Destino del vuelo
     * @param scheduledTime Hora programada (HH:mm)
     * @param currentSimulationTime Tiempo actual de la simulación
     * @param reason Razón de la cancelación
     * @return La cancelación creada, o null si no se pudo crear
     */
    public FlightCancellation requestManualCancellation(
            String origin,
            String destination,
            String scheduledTime,
            LocalDateTime currentSimulationTime,
            String reason) {
        
        logger.info("🔴 Solicitud de cancelación manual: {}-{}-{}", origin, destination, scheduledTime);
        
        // 1. Verificar que el vuelo pueda ser cancelado
        if (!flightStatusTracker.canCancelFlight(origin, destination, scheduledTime)) {
            logger.error("❌ No se puede cancelar: vuelo no disponible o ya en aire");
            return null;
        }
        
        // 2. Crear cancelación manual
        FlightCancellation cancellation = FlightCancellation.createManualCancellation(
            origin,
            destination,
            scheduledTime,
            currentSimulationTime,
            reason != null ? reason : "Cancelación manual desde UI"
        );
        
        // 3. Agregar al mapa
        cancellations.put(cancellation.getId(), cancellation);
        
        logger.info("✅ Cancelación manual creada: {}", cancellation.getId());
        
        return cancellation;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PROCESAMIENTO DE CANCELACIONES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Procesa cancelaciones que deben ejecutarse en el tiempo actual.
     * 
     * @param currentSimulationTime Tiempo actual de la simulación
     * @return Lista de cancelaciones ejecutadas en esta iteración
     */
    public List<FlightCancellation> processCancellationsAt(LocalDateTime currentSimulationTime) {
        List<FlightCancellation> executed = new ArrayList<>();

        // DEBUG: Log total cancellations in memory
        int totalCancellations = cancellations.size();
        long pendingCount = cancellations.values().stream()
            .filter(c -> c.getStatus() == FlightCancellation.CancellationStatus.PENDING)
            .count();

        if (totalCancellations > 0) {
            logger.info("🔍 [CANCEL DEBUG] Total cancellations: {}, Pending: {}, CurrentTime: {}",
                totalCancellations, pendingCount, currentSimulationTime);

            // DEBUG: Log first 5 pending cancellations for debugging
            cancellations.values().stream()
                .filter(c -> c.getStatus() == FlightCancellation.CancellationStatus.PENDING)
                .limit(5)
                .forEach(c -> {
                    logger.info("  📅 Pending: {} @ {}", c.getFlightIdentifier(), c.getCancellationTime());
                    logger.info("     shouldExecute: {}", c.shouldExecuteAt(currentSimulationTime));
                    logger.info("     reason: cancellationTime ({}) <= currentTime ({})",
                        c.getCancellationTime(), currentSimulationTime);
                });
        }

        // Filtrar cancelaciones pendientes que deben ejecutarse
        List<FlightCancellation> toExecute = cancellations.values().stream()
            .filter(c -> c.shouldExecuteAt(currentSimulationTime))
            .collect(Collectors.toList());

        if (toExecute.isEmpty()) {
            return executed;
        }

        logger.info("🔴 Procesando {} cancelaciones en tiempo: {}", toExecute.size(), currentSimulationTime);
        logger.info("   📋 Lista de cancelaciones a ejecutar:");
        toExecute.forEach(c -> logger.info("      - {} @ {}",
            c.getFlightIdentifier(), c.getCancellationTime()));
        
        for (FlightCancellation cancellation : toExecute) {
            boolean success = executeCancellation(cancellation, currentSimulationTime);
            
            if (success) {
                executed.add(cancellation);
            }
        }
        
        logger.info("✅ {} cancelaciones ejecutadas correctamente", executed.size());
        
        return executed;
    }
    
    /**
     * Ejecuta una cancelación específica.
     *
     * @param cancellation Cancelación a ejecutar
     * @param executionTime Tiempo de ejecución
     * @return true si se ejecutó correctamente
     */
    private boolean executeCancellation(FlightCancellation cancellation, LocalDateTime executionTime) {
        logger.info("🔴 Ejecutando cancelación: {}", cancellation.getFlightIdentifier());

        try {
            // DEBUG: Log cancellation details
            logger.info("   📅 Detalles:");
            logger.info("      Origin: {}", cancellation.getFlightOrigin());
            logger.info("      Destination: {}", cancellation.getFlightDestination());
            logger.info("      Scheduled time: {}", cancellation.getScheduledDepartureTime());
            logger.info("      Cancellation time: {}", cancellation.getCancellationTime());
            logger.info("      Execution time: {}", executionTime);

            // DEBUG: Log all tracked flights
            var allFlights = flightStatusTracker.getAllFlights();
            logger.info("   🔍 FlightStatusTracker tiene {} vuelos rastreados", allFlights.size());

            // DEBUG: Log flights with matching origin
            var matchingOrigin = allFlights.stream()
                .filter(f -> f.origin.equals(cancellation.getFlightOrigin()))
                .toList();
            logger.info("   🔍 Vuelos con origen {}: {}", cancellation.getFlightOrigin(), matchingOrigin.size());

            // DEBUG: Log flights with matching origin AND destination
            var matchingRoute = allFlights.stream()
                .filter(f -> f.origin.equals(cancellation.getFlightOrigin())
                          && f.destination.equals(cancellation.getFlightDestination()))
                .toList();
            logger.info("   🔍 Vuelos con ruta {}-{}: {}",
                cancellation.getFlightOrigin(),
                cancellation.getFlightDestination(),
                matchingRoute.size());

            if (!matchingRoute.isEmpty()) {
                logger.info("   📋 Vuelos disponibles en esa ruta:");
                matchingRoute.forEach(f -> logger.info("      - {}", f.toString()));
            }

            // Verificar estado del vuelo
            FlightStatusTracker.FlightStatusInfo flightInfo = flightStatusTracker.getFlightStatus(
                cancellation.getFlightOrigin(),
                cancellation.getFlightDestination(),
                cancellation.getScheduledDepartureTime()
            );

            if (flightInfo == null) {
                String error = "Vuelo no encontrado en FlightStatusTracker";
                cancellation.markAsFailed(error);
                logger.error("❌ {}: {}", error, cancellation.getFlightIdentifier());
                logger.error("   ⚠️ Posibles causas:");
                logger.error("      1. El vuelo no está en los itinerarios actuales");
                logger.error("      2. El tiempo programado no coincide (buscando: {})", cancellation.getScheduledDepartureTime());
                logger.error("      3. El vuelo ya despegó o completó");
                return false;
            }
            
            if (!flightInfo.cancellable) {
                String error = String.format("Vuelo no cancelable (estado: %s)", flightInfo.status);
                cancellation.markAsFailed(error);
                logger.error("❌ {}: {}", error, cancellation.getFlightIdentifier());
                return false;
            }
            
            // Ejecutar cancelación
            // Nota: El contador de productos afectados se actualizará desde ReplanificationService
            cancellation.markAsExecuted(executionTime, 0);
            
            logger.info("✅ Cancelación ejecutada: {} en {}", 
                cancellation.getFlightIdentifier(), executionTime);
            
            return true;
            
        } catch (Exception e) {
            String error = "Error al ejecutar cancelación: " + e.getMessage();
            cancellation.markAsFailed(error);
            logger.error("❌ {}", error, e);
            return false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene todas las cancelaciones.
     */
    public Collection<FlightCancellation> getAllCancellations() {
        return new ArrayList<>(cancellations.values());
    }
    
    /**
     * Obtiene cancelaciones por estado.
     */
    public List<FlightCancellation> getCancellationsByStatus(FlightCancellation.CancellationStatus status) {
        return cancellations.values().stream()
            .filter(c -> c.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene una cancelación por ID.
     */
    public FlightCancellation getCancellationById(String id) {
        return cancellations.get(id);
    }
    
    /**
     * Obtiene cancelaciones ejecutadas exitosamente.
     */
    public List<FlightCancellation> getExecutedCancellations() {
        return getCancellationsByStatus(FlightCancellation.CancellationStatus.EXECUTED);
    }
    
    /**
     * Obtiene cancelaciones pendientes.
     */
    public List<FlightCancellation> getPendingCancellations() {
        return getCancellationsByStatus(FlightCancellation.CancellationStatus.PENDING);
    }
    
    /**
     * Verifica si un vuelo específico ha sido cancelado.
     */
    public boolean isFlightCancelled(String origin, String destination, String scheduledTime) {
        String flightId = String.format("%s-%s-%s", origin, destination, scheduledTime);
        
        return cancellations.values().stream()
            .anyMatch(c -> 
                c.getStatus() == FlightCancellation.CancellationStatus.EXECUTED &&
                c.getFlightIdentifier().equals(flightId)
            );
    }
    
    /**
     * Actualiza el contador de productos afectados en una cancelación.
     */
    public void updateAffectedProducts(String cancellationId, int affectedCount) {
        FlightCancellation cancellation = cancellations.get(cancellationId);
        if (cancellation != null) {
            cancellation.setAffectedProductsCount(affectedCount);
            logger.debug("📊 Actualizado productos afectados: {} → {}", cancellationId, affectedCount);
        }
    }
    
    /**
     * Marca que se disparó la replanificación para una cancelación.
     */
    public void markReplanificationTriggered(String cancellationId) {
        FlightCancellation cancellation = cancellations.get(cancellationId);
        if (cancellation != null) {
            cancellation.triggerReplanification();
            logger.debug("🔄 Replanificación marcada: {}", cancellationId);
        }
    }

    /**
     * Agrega cancelaciones en masa (bulk upload desde UI).
     * No valida existencia de vuelos - las cancelaciones se programan directamente.
     *
     * @param bulkCancellations Lista de cancelaciones a agregar
     * @return Número de cancelaciones agregadas
     */
    public int addBulkCancellations(List<FlightCancellation> bulkCancellations) {
        int added = 0;
        for (FlightCancellation cancellation : bulkCancellations) {
            cancellations.put(cancellation.getId(), cancellation);
            added++;
        }

        logger.info("✅ {} cancelaciones agregadas en masa", added);
        return added;
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Limpia todas las cancelaciones.
     */
    public void clear() {
        cancellations.clear();
        logger.info("🧹 Todas las cancelaciones limpiadas");
    }
    
    /**
     * Obtiene estadísticas de cancelaciones.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", cancellations.size());
        stats.put("pending", getCancellationsByStatus(FlightCancellation.CancellationStatus.PENDING).size());
        stats.put("executed", getCancellationsByStatus(FlightCancellation.CancellationStatus.EXECUTED).size());
        stats.put("failed", getCancellationsByStatus(FlightCancellation.CancellationStatus.FAILED).size());
        
        long scheduled = cancellations.values().stream()
            .filter(c -> c.getType() == FlightCancellation.CancellationType.SCHEDULED)
            .count();
        long manual = cancellations.values().stream()
            .filter(c -> c.getType() == FlightCancellation.CancellationType.MANUAL)
            .count();
        
        stats.put("scheduled", scheduled);
        stats.put("manual", manual);
        
        return stats;
    }
    
    /**
     * Log de resumen de cancelaciones.
     */
    public void logSummary() {
        Map<String, Object> stats = getStatistics();
        logger.info("📊 Cancelaciones - Total: {}, Pendientes: {}, Ejecutadas: {}, Fallidas: {}, Programadas: {}, Manuales: {}",
            stats.get("total"),
            stats.get("pending"),
            stats.get("executed"),
            stats.get("failed"),
            stats.get("scheduled"),
            stats.get("manual")
        );
    }
}


