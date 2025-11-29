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

            if (affectedOrders.isEmpty()) {
                logger.error("❌ [REPLAN] ERROR: affectedOrders está vacío pero affectedOrderIds tiene {} IDs!",
                    affectedOrderIds.size());
                logger.error("   Esto significa que los pedidos afectados NO están en allOrders");
                task.markAsCompleted(currentTime, 0, 0, 0);
                replanificationHistory.put(task.getId(), task);
                return task;
            }

            // 🆕 4. CRITICAL: Calcular cuántos productos de cada pedido fueron afectados
            // Esto es necesario porque TabuSearch asume que order.getTotalQuantity() = productos pendientes
            // pero en replanificación, solo parte del pedido puede estar afectada
            logger.info("📊 [REPLAN] Calculando productos afectados por pedido...");

            Map<Integer, Integer> productsToReassign = new HashMap<>();
            List<PlannerShipment> obsoleteShipments = new ArrayList<>();

            for (PlannerShipment shipment : currentSolution.getPlannerShipments()) {
                // Verificar si este shipment usa el vuelo cancelado
                boolean usesCancelledFlight = false;
                for (PlannerFlight flight : shipment.getFlights()) {
                    if (matchesCancellation(flight, cancellation)) {
                        usesCancelledFlight = true;
                        break;
                    }
                }

                if (usesCancelledFlight && shipment.getOrder() != null) {
                    int orderId = shipment.getOrder().getId();
                    int qty = shipment.getQuantity();
                    productsToReassign.merge(orderId, qty, Integer::sum);
                    obsoleteShipments.add(shipment);
                    logger.debug("   ❌ Shipment #{} (Order #{}): {} productos a reasignar",
                        shipment.getId(), orderId, qty);
                }
            }

            int totalProductsToReassign = productsToReassign.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

            logger.info("   📦 [REPLAN] Productos a reasignar:");
            logger.info("      Total shipments cancelados: {}", obsoleteShipments.size());
            logger.info("      Total productos afectados: {}", totalProductsToReassign);
            productsToReassign.forEach((orderId, qty) ->
                logger.info("         Order #{}: {} productos", orderId, qty));

            // 🆕 Guardar tracking detallado en task
            task.setProductsToReassign(productsToReassign);

            // 🆕 5. Crear pedidos ajustados con SOLO la cantidad afectada
            // Esto garantiza que TabuSearch no intente asignar productos que ya están en rutas válidas
            List<PlannerOrder> adjustedOrders = new ArrayList<>();

            for (PlannerOrder originalOrder : affectedOrders) {
                int orderId = originalOrder.getId();
                int qtyToReassign = productsToReassign.getOrDefault(orderId, 0);

                if (qtyToReassign == 0) {
                    logger.warn("   ⚠️ [REPLAN] Order #{} marcado como afectado pero sin productos a reasignar?", orderId);
                    continue;
                }

                // Crear orden ajustada con solo los productos afectados
                PlannerOrder adjustedOrder = new PlannerOrder(
                    originalOrder.getId(),
                    qtyToReassign,  // ✅ Solo productos afectados, NO totalQuantity completo
                    originalOrder.getOrigin(),
                    originalOrder.getDestination()
                );
                adjustedOrder.setOrderTime(originalOrder.getOrderTime());
                adjustedOrder.setClientId(originalOrder.getClientId());

                adjustedOrders.add(adjustedOrder);

                logger.info("   ✅ [REPLAN] Order #{}: {} productos de {} totales",
                    orderId, qtyToReassign, originalOrder.getTotalQuantity());
            }

            if (adjustedOrders.isEmpty()) {
                logger.warn("⚠️ [REPLAN] No hay pedidos ajustados para replanificar");
                task.markAsCompleted(currentTime, obsoleteShipments.size(), 0, 0);
                replanificationHistory.put(task.getId(), task);
                return task;
            }

            // 6. Filtrar vuelos (excluir el cancelado)
            List<PlannerFlight> filteredFlights = filterCancelledFlight(
                availableFlights,
                cancellation
            );

            logger.info("✈️ Vuelos disponibles para replanificación: {}",
                filteredFlights.size());

            // 7. Ejecutar TabuSearch con pedidos AJUSTADOS
            logger.info("🔍 Ejecutando TabuSearch para replanificación...");
            logger.info("📋 [REPLAN] Pasando {} pedidos ajustados a TabuSearch (total {} productos)",
                adjustedOrders.size(), totalProductsToReassign);

            Solution solution = tabuSearchPlanner.optimize(
                adjustedOrders,      // ✅ Pedidos con cantidades ajustadas
                filteredFlights,
                airports
            );
            
            // Cast a TabuSolution para acceder a shipments
            TabuSolution newSolution = (solution instanceof TabuSolution)
                ? (TabuSolution) solution
                : new TabuSolution(solution);

            // 🔍 DEBUG: Ver cuántos shipments generó TabuSearch
            logger.info("📦 [REPLAN] TabuSearch generó {} shipments para {} pedidos afectados",
                newSolution.getPlannerShipments().size(),
                affectedOrders.size());

            if (newSolution.getPlannerShipments().isEmpty()) {
                logger.warn("⚠️ [REPLAN] TabuSearch NO generó ningún shipment nuevo!");
                logger.warn("   Posibles causas:");
                logger.warn("   - No hay rutas alternativas disponibles");
                logger.warn("   - Todos los vuelos alternativos están llenos");
                logger.warn("   - Los pedidos no cumplen restricciones de tiempo");
            }

            // 8. 🆕 APLICAR CAMBIOS A LA SOLUCIÓN ACTUAL
            logger.info("🔄 Aplicando replanificación a la solución global...");
            Map<Integer, Integer> reassignedProducts = new HashMap<>();
            int cancelledCount = applyReplanificationToSolution(
                currentSolution,
                obsoleteShipments,  // ✅ Pasar shipments obsoletos ya calculados
                newSolution,
                productsToReassign,
                reassignedProducts  // ✅ Output: productos efectivamente reasignados
            );

            // 🆕 Guardar productos reasignados en task para tracking
            task.setProductsReassigned(reassignedProducts);

            // 🔍 DEBUG: Log detallado de tracking
            logger.info("🔍 [DEBUG] Tracking de replanificación guardado en task:");
            logger.info("   📋 productsToReassign: {}", productsToReassign);
            logger.info("   ✅ productsReassigned: {}", reassignedProducts);
            logger.info("   ⏳ productsPending: {}", task.getProductsPending());
            logger.info("   📊 Total pending: {}", task.getTotalProductsPending());

            // 7. Registrar resultados
            int newShipmentsCount = newSolution.getPlannerShipments().size();
            int totalProducts = affectedOrders.stream()
                .mapToInt(PlannerOrder::getTotalQuantity)
                .sum();

            task.markAsCompleted(
                LocalDateTime.now(),
                cancelledCount,            // Shipments cancelados (mantenidos como historial)
                newShipmentsCount,         // Nuevos shipments creados
                totalProducts
            );

            logger.info("✅ Replanificación completada: {}", task.getSummary());
            logger.info("   ❌ Cancelados: {} shipments obsoletos (mantenidos en historial)", cancelledCount);
            logger.info("   ✨ Agregados: {} shipments nuevos", newShipmentsCount);

            // 8. Actualizar contador de productos afectados en la cancelación
            cancellationService.updateAffectedProducts(
                cancellation.getId(),
                totalProducts
            );
            cancellationService.markReplanificationTriggered(cancellation.getId());

            // 9. Guardar en historial
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
    // APLICACIÓN DE REPLANIFICACIÓN A SOLUCIÓN GLOBAL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Aplica los cambios de replanificación a la solución global.
     *
     * Este método:
     * 1. Marca shipments obsoletos como CANCELLED (manteniéndolos en la solución para historial)
     * 2. Agrega los nuevos shipments generados por la replanificación
     * 3. Verifica que se hayan reasignado TODOS los productos afectados
     *
     * @param currentSolution Solución global actual (será modificada)
     * @param obsoleteShipments Shipments obsoletos que usaban el vuelo cancelado
     * @param newSolution Nueva solución con rutas alternativas
     * @param productsToReassign Mapa de productos a reasignar por pedido
     * @param reassignedProducts Output: Mapa de productos efectivamente reasignados por pedido
     * @return Número de shipments marcados como CANCELLED
     */
    private int applyReplanificationToSolution(
            TabuSolution currentSolution,
            List<PlannerShipment> obsoleteShipments,
            TabuSolution newSolution,
            Map<Integer, Integer> productsToReassign,
            Map<Integer, Integer> reassignedProducts) {

        logger.info("   🔄 [APPLY] Iniciando aplicación de replanificación...");

        logger.info("   📦 [APPLY] Shipments obsoletos a marcar como CANCELLED: {}", obsoleteShipments.size());

        // DEBUG: Log first few obsolete shipments
        if (!obsoleteShipments.isEmpty()) {
            logger.debug("   🔍 [APPLY] Primeros shipments obsoletos:");
            obsoleteShipments.stream()
                .limit(3)
                .forEach(s -> logger.debug("      - Shipment #{} (Order #{}): {} vuelos",
                    s.getId(), s.getOrder().getId(), s.getFlights().size()));
        }

        // 2. ✅ MARCAR como CANCELLED (en lugar de eliminar) para mantener historial
        int cancelledCount = 0;
        for (PlannerShipment obsoleteShipment : obsoleteShipments) {
            obsoleteShipment.setStatus(PlannerShipment.Status.CANCELLED);
            cancelledCount++;
            logger.debug("      ❌ Shipment #{} marcado como CANCELLED", obsoleteShipment.getId());
        }

        logger.info("   ❌ [APPLY] Marcados {} shipments como CANCELLED (mantenidos en historial)", cancelledCount);

        // 3. Agregar nuevos shipments de la replanificación
        List<PlannerShipment> newShipments = newSolution.getPlannerShipments();
        int addedCount = 0;

        for (PlannerShipment newShipment : newShipments) {
            currentSolution.getPlannerShipments().add(newShipment);
            addedCount++;
        }

        logger.info("   ✅ [APPLY] Agregados {} shipments nuevos a la solución", addedCount);

        // DEBUG: Log first few new shipments
        if (!newShipments.isEmpty()) {
            logger.debug("   🔍 [APPLY] Primeros shipments nuevos:");
            newShipments.stream()
                .limit(3)
                .forEach(s -> logger.debug("      + Shipment #{} (Order #{}): {} vuelos",
                    s.getId(), s.getOrder() != null ? s.getOrder().getId() : "?", s.getFlights().size()));
        }

        // 4. 🆕 Verificar consistencia: ¿Se reasignaron TODOS los productos afectados?
        logger.info("   🔍 [APPLY] Verificando consistencia de replanificación...");

        // Calcular productos reasignados por pedido
        reassignedProducts.clear();  // Limpiar el mapa de salida
        for (PlannerShipment newShipment : newShipments) {
            if (newShipment.getOrder() != null) {
                int orderId = newShipment.getOrder().getId();
                reassignedProducts.merge(orderId, newShipment.getQuantity(), Integer::sum);
            }
        }

        // Comparar productos esperados vs reasignados
        int totalExpected = productsToReassign.values().stream().mapToInt(Integer::intValue).sum();
        int totalReassigned = reassignedProducts.values().stream().mapToInt(Integer::intValue).sum();

        logger.info("   ✓ [APPLY] Productos esperados a reasignar: {}", totalExpected);
        logger.info("   ✓ [APPLY] Productos efectivamente reasignados: {}", totalReassigned);

        if (totalReassigned < totalExpected) {
            int missing = totalExpected - totalReassigned;
            logger.warn("   ⚠️ [APPLY] ATENCIÓN: Faltan {} productos por reasignar!", missing);
            logger.warn("      Esto significa que algunos productos NO encontraron rutas alternativas");

            // Detallar pedidos con productos faltantes
            productsToReassign.forEach((orderId, expected) -> {
                int reassigned = reassignedProducts.getOrDefault(orderId, 0);
                if (reassigned < expected) {
                    logger.warn("         Order #{}: esperado={}, reasignado={}, faltante={}",
                        orderId, expected, reassigned, expected - reassigned);
                }
            });
        } else if (totalReassigned > totalExpected) {
            logger.warn("   ⚠️ [APPLY] ATENCIÓN: Se reasignaron {} productos de más!", totalReassigned - totalExpected);
        } else {
            logger.info("   ✅ [APPLY] PERFECTO: Todos los productos fueron reasignados correctamente!");
        }

        logger.info("   ✓ [APPLY] Solución actualizada:");
        logger.info("      Total shipments en solución: {}", currentSolution.getPlannerShipments().size());
        logger.info("      Shipments ACTIVOS: {}", currentSolution.getPlannerShipments().stream()
            .filter(s -> s.getStatus() == PlannerShipment.Status.ACTIVE).count());
        logger.info("      Shipments CANCELADOS: {}", currentSolution.getPlannerShipments().stream()
            .filter(s -> s.getStatus() == PlannerShipment.Status.CANCELLED).count());

        return cancelledCount;
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

