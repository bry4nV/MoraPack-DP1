package pe.edu.pucp.morapack.service;

import pe.edu.pucp.morapack.model.DynamicOrder;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para inyectar pedidos dinámicos en la simulación en curso.
 * 
 * Responsabilidades:
 * - Convertir DynamicOrder a PlannerOrder
 * - Inyectar pedidos en el momento correcto
 * - Coordinar con DynamicOrderService
 */
@Service
public class OrderInjectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderInjectionService.class);
    
    private final DynamicOrderService dynamicOrderService;
    
    // Mapa de código IATA → PlannerAirport (se configura externamente)
    private Map<String, PlannerAirport> airportMap = new HashMap<>();
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    
    public OrderInjectionService(DynamicOrderService dynamicOrderService) {
        this.dynamicOrderService = dynamicOrderService;
    }
    
    /**
     * Configura el mapa de aeropuertos disponibles.
     * Este método debe ser llamado antes de inyectar pedidos.
     * 
     * @param airports Lista de aeropuertos disponibles
     */
    public void setAirports(List<PlannerAirport> airports) {
        this.airportMap.clear();
        for (PlannerAirport airport : airports) {
            this.airportMap.put(airport.getCode(), airport);
        }
        logger.info("✅ {} aeropuertos configurados en OrderInjectionService", airports.size());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // INYECCIÓN DE PEDIDOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Procesa pedidos dinámicos que deben inyectarse en el tiempo actual.
     * 
     * @param currentSimulationTime Tiempo actual de la simulación
     * @return Lista de PlannerOrders listos para ser procesados por Tabu Search
     */
    public List<PlannerOrder> processOrderInjections(LocalDateTime currentSimulationTime) {
        List<PlannerOrder> injectedOrders = new ArrayList<>();
        
        // 1. Obtener pedidos que deben inyectarse
        List<DynamicOrder> toInject = dynamicOrderService.getOrdersToInjectAt(currentSimulationTime);
        
        if (toInject.isEmpty()) {
            return injectedOrders;
        }
        
        logger.info("➕ Inyectando {} pedidos dinámicos en tiempo: {}", toInject.size(), currentSimulationTime);
        
        // 2. Convertir y marcar como inyectados
        for (DynamicOrder dynamicOrder : toInject) {
            try {
                // Marcar como inyectado y obtener System ID
                Integer systemOrderId = dynamicOrderService.markOrderAsInjected(
                    dynamicOrder.getId(), 
                    currentSimulationTime
                );
                
                if (systemOrderId == null) {
                    logger.error("❌ No se pudo inyectar pedido: {}", dynamicOrder.getId());
                    continue;
                }
                
                // Convertir a PlannerOrder
                PlannerOrder plannerOrder = convertToPlannerOrder(dynamicOrder, systemOrderId, currentSimulationTime);
                injectedOrders.add(plannerOrder);
                
                logger.info("✅ Pedido inyectado: {} → System ID: {} | {}", 
                    dynamicOrder.getId(), 
                    systemOrderId, 
                    dynamicOrder.getDescription());
                
            } catch (Exception e) {
                logger.error("❌ Error inyectando pedido {}: {}", dynamicOrder.getId(), e.getMessage(), e);
                dynamicOrderService.markOrderAsFailed(dynamicOrder.getId(), e.getMessage());
            }
        }
        
        logger.info("✅ {} pedidos dinámicos inyectados correctamente", injectedOrders.size());
        
        return injectedOrders;
    }
    
    /**
     * Convierte un DynamicOrder a PlannerOrder.
     */
    private PlannerOrder convertToPlannerOrder(
            DynamicOrder dynamicOrder, 
            int systemOrderId, 
            LocalDateTime injectionTime) throws IllegalArgumentException {
        
        // 1. Buscar aeropuertos
        PlannerAirport originAirport = airportMap.get(dynamicOrder.getOrigin());
        PlannerAirport destAirport = airportMap.get(dynamicOrder.getDestination());
        
        if (originAirport == null) {
            throw new IllegalArgumentException("Aeropuerto de origen no encontrado: " + dynamicOrder.getOrigin());
        }
        
        if (destAirport == null) {
            throw new IllegalArgumentException("Aeropuerto de destino no encontrado: " + dynamicOrder.getDestination());
        }
        
        // 2. Crear PlannerOrder usando el constructor correcto
        PlannerOrder plannerOrder = new PlannerOrder(
            systemOrderId,
            dynamicOrder.getQuantity(),
            originAirport,
            destAirport
        );
        
        // 3. Configurar orderTime (el constructor usa LocalDateTime.now() por defecto, lo sobrescribimos)
        plannerOrder.setOrderTime(injectionTime);
        
        logger.debug("🔄 Convertido: DynamicOrder {} → PlannerOrder {} [{} units, {}h deadline, {} → {}]",
            dynamicOrder.getId(),
            systemOrderId,
            dynamicOrder.getQuantity(),
            plannerOrder.getMaxDeliveryHours(),
            dynamicOrder.getOrigin(),
            dynamicOrder.getDestination()
        );
        
        return plannerOrder;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // INYECCIÓN MANUAL (para UI)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Inyecta un pedido manual inmediatamente (desde UI).
     * 
     * @param origin Origen
     * @param destination Destino
     * @param quantity Cantidad
     * @param deadlineHours Deadline (48 o 72)
     * @param currentSimulationTime Tiempo actual
     * @param reason Razón
     * @return El PlannerOrder creado, o null si falló
     */
    public PlannerOrder injectManualOrder(
            String origin,
            String destination,
            int quantity,
            int deadlineHours,
            LocalDateTime currentSimulationTime,
            String reason) {
        
        logger.info("➕ Inyección manual: {}-{} ({} units, {}h)", 
            origin, destination, quantity, deadlineHours);
        
        // 1. Crear pedido dinámico
        DynamicOrder dynamicOrder = dynamicOrderService.createManualOrder(
            origin,
            destination,
            quantity,
            deadlineHours,
            currentSimulationTime,
            reason
        );
        
        if (dynamicOrder == null) {
            logger.error("❌ No se pudo crear pedido dinámico");
            return null;
        }
        
        // 2. Inyectar inmediatamente
        Integer systemOrderId = dynamicOrderService.markOrderAsInjected(
            dynamicOrder.getId(), 
            currentSimulationTime
        );
        
        if (systemOrderId == null) {
            logger.error("❌ No se pudo inyectar pedido: {}", dynamicOrder.getId());
            return null;
        }
        
        // 3. Convertir a PlannerOrder
        PlannerOrder plannerOrder = convertToPlannerOrder(
            dynamicOrder, 
            systemOrderId, 
            currentSimulationTime
        );
        
        logger.info("✅ Pedido manual inyectado: {} → System ID: {}", 
            dynamicOrder.getId(), 
            systemOrderId);
        
        return plannerOrder;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene estadísticas de inyección.
     */
    public InjectionStatistics getStatistics() {
        var dynamicStats = dynamicOrderService.getStatistics();
        
        return new InjectionStatistics(
            (int) dynamicStats.get("total"),
            (int) dynamicStats.get("pending"),
            (int) dynamicStats.get("injected"),
            (int) dynamicStats.get("totalProducts"),
            (int) dynamicStats.get("injectedProducts")
        );
    }
    
    /**
     * DTO para estadísticas de inyección.
     */
    public static class InjectionStatistics {
        public final int totalOrders;
        public final int pendingOrders;
        public final int injectedOrders;
        public final int totalProducts;
        public final int injectedProducts;
        
        public InjectionStatistics(
                int totalOrders, 
                int pendingOrders, 
                int injectedOrders,
                int totalProducts,
                int injectedProducts) {
            this.totalOrders = totalOrders;
            this.pendingOrders = pendingOrders;
            this.injectedOrders = injectedOrders;
            this.totalProducts = totalProducts;
            this.injectedProducts = injectedProducts;
        }
        
        @Override
        public String toString() {
            return String.format("Injection[Total: %d, Pending: %d, Injected: %d, Products: %d/%d]",
                totalOrders, pendingOrders, injectedOrders, injectedProducts, totalProducts);
        }
    }
    
    /**
     * Log de resumen de inyecciones.
     */
    public void logSummary() {
        InjectionStatistics stats = getStatistics();
        logger.info("📊 Inyección - {}", stats);
    }
}

