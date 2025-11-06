package pe.edu.pucp.morapack.service;

import pe.edu.pucp.morapack.model.FlightCancellation;
import pe.edu.pucp.morapack.model.ReplanificationTask;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de replanificación de pedidos afectados por cancelaciones de vuelos.
 * 
 * Flujo principal:
 * 1. Identificar productos afectados por vuelo cancelado
 * 2. Extraer pedidos completos de esos productos
 * 3. Filtrar: NO reasignar productos que ya están en destino final
 * 4. Ejecutar TabuSearch solo con pedidos afectados
 * 5. Aplicar nueva solución
 * 6. Registrar métricas y resultados
 */
@Service
public class ReplanificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReplanificationService.class);
    
    private final CancellationService cancellationService;
    private final TabuSearchPlanner tabuSearchPlanner;
    
    // Historial de replanificaciones
    private final Map<String, ReplanificationTask> replanificationHistory = new LinkedHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    
    public ReplanificationService(
            CancellationService cancellationService,
            TabuSearchPlanner tabuSearchPlanner) {
        this.cancellationService = cancellationService;
        this.tabuSearchPlanner = tabuSearchPlanner;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // REPLANIFICACIÓN PRINCIPAL
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Dispara la replanificación tras una cancelación de vuelo.
     * 
     * @param cancellation Cancelación que dispara la replanificación
     * @param currentSolution Solución actual de TabuSearch (para identificar afectados)
     * @param allOrders Todos los pedidos del sistema
     * @param availableFlights Vuelos disponibles (sin el cancelado)
     * @param airports Aeropuertos
     * @param currentTime Tiempo actual de la simulación
     * @return Tarea de replanificación con resultados
     */
    public ReplanificationTask triggerReplanification(
            FlightCancellation cancellation,
            TabuSolution currentSolution,
            List<PlannerOrder> allOrders,
            List<PlannerFlight> availableFlights,
            List<PlannerAirport> airports,
            LocalDateTime currentTime) {
        
        logger.info("🔄 Disparando replanificación por cancelación: {}", 
            cancellation.getFlightIdentifier());
        
        // 1. Crear tarea de replanificación
        ReplanificationTask task = new ReplanificationTask(
            cancellation.getId(),
            cancellation.getFlightIdentifier(),
            currentTime
        );
        
        task.markAsStarted(currentTime);
        
        try {
            // 2. Identificar pedidos afectados
            Set<Integer> affectedOrderIds = identifyAffectedOrders(
                cancellation,
                currentSolution
            );
            
            if (affectedOrderIds.isEmpty()) {
                logger.info("ℹ️ No hay pedidos afectados por la cancelación");
                task.markAsCompleted(currentTime, 0, 0, 0);
                replanificationHistory.put(task.getId(), task);
                return task;
            }
            
            task.addAffectedOrders(new ArrayList<>(affectedOrderIds));
            
            logger.info("📊 Pedidos afectados: {}", affectedOrderIds.size());
            
            // 3. Extraer pedidos completos
            List<PlannerOrder> affectedOrders = allOrders.stream()
                .filter(order -> affectedOrderIds.contains(order.getId()))
                .collect(Collectors.toList());
            
            // 4. Filtrar vuelos (excluir el cancelado)
            List<PlannerFlight> filteredFlights = filterCancelledFlight(
                availableFlights,
                cancellation
            );
            
            logger.info("✈️ Vuelos disponibles para replanificación: {}", 
                filteredFlights.size());
            
            // 5. Ejecutar TabuSearch con pedidos afectados
            logger.info("🔍 Ejecutando TabuSearch para replanificación...");
            
            Solution solution = tabuSearchPlanner.optimize(
                affectedOrders,
                filteredFlights,
                airports
            );
            
            // Cast a TabuSolution para acceder a shipments
            TabuSolution newSolution = (solution instanceof TabuSolution) 
                ? (TabuSolution) solution 
                : new TabuSolution(solution);
            
            // 6. Registrar resultados
            int newShipments = newSolution.getPlannerShipments().size();
            int totalProducts = affectedOrders.stream()
                .mapToInt(PlannerOrder::getTotalQuantity)
                .sum();
            
            task.markAsCompleted(
                LocalDateTime.now(),
                affectedOrderIds.size(),  // cancelledShipments (aprox)
                newShipments,
                totalProducts
            );
            
            logger.info("✅ Replanificación completada: {}", task.getSummary());
            
            // 7. Actualizar contador de productos afectados en la cancelación
            cancellationService.updateAffectedProducts(
                cancellation.getId(), 
                totalProducts
            );
            cancellationService.markReplanificationTriggered(cancellation.getId());
            
            // 8. Guardar en historial
            replanificationHistory.put(task.getId(), task);
            
            return task;
            
        } catch (Exception e) {
            logger.error("❌ Error en replanificación: {}", e.getMessage(), e);
            task.markAsFailed(LocalDateTime.now(), e.getMessage());
            replanificationHistory.put(task.getId(), task);
            return task;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // IDENTIFICACIÓN DE PEDIDOS AFECTADOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Identifica qué pedidos están afectados por un vuelo cancelado.
     * 
     * Lógica:
     * - Buscar en la solución actual de TabuSearch
     * - Encontrar shipments que usan el vuelo cancelado
     * - Extraer IDs de pedidos de esos shipments
     * 
     * @param cancellation Cancelación del vuelo
     * @param currentSolution Solución actual de TabuSearch
     * @return Set de IDs de pedidos afectados
     */
    private Set<Integer> identifyAffectedOrders(
            FlightCancellation cancellation,
            TabuSolution currentSolution) {
        
        Set<Integer> affectedOrderIds = new HashSet<>();
        
        logger.debug("🔍 Buscando envíos que usan vuelo: {}-{}-{}",
            cancellation.getFlightOrigin(),
            cancellation.getFlightDestination(),
            cancellation.getScheduledDepartureTime()
        );
        
        // Iterar sobre todos los shipments en la solución actual
        for (PlannerShipment shipment : currentSolution.getPlannerShipments()) {
            
            // Verificar si este shipment usa el vuelo cancelado
            boolean usesCancelledFlight = false;
            
            for (PlannerFlight flight : shipment.getFlights()) {
                if (matchesCancellation(flight, cancellation)) {
                    usesCancelledFlight = true;
                    break;
                }
            }
            
            // Si usa el vuelo cancelado, agregar el pedido a la lista
            if (usesCancelledFlight) {
                int orderId = shipment.getOrder().getId();
                affectedOrderIds.add(orderId);
                logger.debug("  ✓ Pedido {} afectado (shipment {})", orderId, shipment.getId());
            }
        }
        
        return affectedOrderIds;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // FILTRADO DE VUELOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Filtra vuelos para excluir el cancelado.
     * 
     * @param allFlights Todos los vuelos disponibles
     * @param cancellation Cancelación a excluir
     * @return Lista de vuelos sin el cancelado
     */
    private List<PlannerFlight> filterCancelledFlight(
            List<PlannerFlight> allFlights,
            FlightCancellation cancellation) {
        
        return allFlights.stream()
            .filter(flight -> !matchesCancellation(flight, cancellation))
            .collect(Collectors.toList());
    }
    
    /**
     * Verifica si un vuelo coincide con una cancelación.
     */
    private boolean matchesCancellation(
            PlannerFlight flight, 
            FlightCancellation cancellation) {
        
        String flightTime = String.format("%02d:%02d",
            flight.getDepartureTime().getHour(),
            flight.getDepartureTime().getMinute()
        );
        
        return flight.getOrigin().getCode().equals(cancellation.getFlightOrigin()) &&
               flight.getDestination().getCode().equals(cancellation.getFlightDestination()) &&
               flightTime.equals(cancellation.getScheduledDepartureTime());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene todas las replanificaciones ejecutadas.
     */
    public Collection<ReplanificationTask> getAllReplanifications() {
        return new ArrayList<>(replanificationHistory.values());
    }
    
    /**
     * Obtiene una replanificación por ID.
     */
    public ReplanificationTask getReplanificationById(String id) {
        return replanificationHistory.get(id);
    }
    
    /**
     * Obtiene replanificaciones exitosas.
     */
    public List<ReplanificationTask> getSuccessfulReplanifications() {
        return replanificationHistory.values().stream()
            .filter(ReplanificationTask::isSuccessful)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene replanificaciones fallidas.
     */
    public List<ReplanificationTask> getFailedReplanifications() {
        return replanificationHistory.values().stream()
            .filter(task -> !task.isSuccessful())
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estadísticas de replanificación.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", replanificationHistory.size());
        stats.put("successful", getSuccessfulReplanifications().size());
        stats.put("failed", getFailedReplanifications().size());
        
        // Estadísticas agregadas
        int totalAffectedOrders = replanificationHistory.values().stream()
            .mapToInt(t -> t.getAffectedOrderIds().size())
            .sum();
        
        int totalAffectedProducts = replanificationHistory.values().stream()
            .mapToInt(ReplanificationTask::getTotalAffectedProducts)
            .sum();
        
        int totalNewShipments = replanificationHistory.values().stream()
            .mapToInt(ReplanificationTask::getNewShipmentsCount)
            .sum();
        
        double avgExecutionTime = replanificationHistory.values().stream()
            .mapToLong(ReplanificationTask::getExecutionTimeMs)
            .average()
            .orElse(0.0);
        
        stats.put("totalAffectedOrders", totalAffectedOrders);
        stats.put("totalAffectedProducts", totalAffectedProducts);
        stats.put("totalNewShipments", totalNewShipments);
        stats.put("avgExecutionTimeMs", avgExecutionTime);
        
        return stats;
    }
    
    /**
     * Log de resumen de replanificaciones.
     */
    public void logSummary() {
        Map<String, Object> stats = getStatistics();
        logger.info("📊 Replanificaciones - Total: {}, Exitosas: {}, Fallidas: {}, " +
                "Pedidos afectados: {}, Productos: {}, Tiempo promedio: {:.0f}ms",
            stats.get("total"),
            stats.get("successful"),
            stats.get("failed"),
            stats.get("totalAffectedOrders"),
            stats.get("totalAffectedProducts"),
            stats.get("avgExecutionTimeMs")
        );
    }
    
    /**
     * Limpia el historial de replanificaciones.
     */
    public void clear() {
        replanificationHistory.clear();
        logger.info("🧹 Historial de replanificaciones limpiado");
    }
}

