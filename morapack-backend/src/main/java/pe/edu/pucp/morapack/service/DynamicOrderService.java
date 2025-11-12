package pe.edu.pucp.morapack.service;

import pe.edu.pucp.morapack.model.DynamicOrder;
import pe.edu.pucp.morapack.algos.data.loaders.OrderFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar pedidos dinámicos (programados y manuales).
 * 
 * Responsabilidades:
 * - Cargar pedidos programados desde archivo
 * - Procesar pedidos manuales desde UI
 * - Determinar cuándo inyectar pedidos en la simulación
 * - Validar datos de pedidos
 */
@Service
public class DynamicOrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicOrderService.class);
    
    // Almacenamiento de pedidos dinámicos
    private final Map<String, DynamicOrder> orders = new ConcurrentHashMap<>();
    
    // Generador de IDs de pedidos del sistema
    private final AtomicInteger nextSystemOrderId = new AtomicInteger(10000);
    
    // ═══════════════════════════════════════════════════════════════
    // CARGA DE PEDIDOS PROGRAMADOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Carga pedidos programados desde un archivo.
     * 
     * @param filePath Ruta del archivo
     * @param startDate Fecha de inicio de la simulación
     * @return Número de pedidos cargados
     */
    public int loadScheduledOrders(String filePath, LocalDate startDate) {
        logger.info("📂 Cargando pedidos dinámicos desde: {}", filePath);
        
        // Validar que el archivo exista
        if (!OrderFileLoader.validateFile(filePath)) {
            logger.warn("⚠️ Archivo de pedidos no disponible, continuando sin pedidos programados");
            return 0;
        }
        
        // Cargar pedidos
        List<DynamicOrder> loaded = OrderFileLoader.loadOrders(filePath, startDate);
        
        // Agregar al mapa
        for (DynamicOrder order : loaded) {
            orders.put(order.getId(), order);
        }
        
        logger.info("✅ {} pedidos dinámicos programados cargados", loaded.size());
        
        return loaded.size();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PEDIDOS MANUALES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Crea un pedido manual (desde UI).
     * 
     * @param origin Origen (IATA code)
     * @param destination Destino (IATA code)
     * @param quantity Cantidad de productos
     * @param deadlineHours Deadline en horas (48 o 72)
     * @param currentSimulationTime Tiempo actual de la simulación
     * @param reason Razón del pedido urgente
     * @return El pedido creado, o null si no se pudo crear
     */
    public DynamicOrder createManualOrder(
            String origin,
            String destination,
            int quantity,
            int deadlineHours,
            LocalDateTime currentSimulationTime,
            String reason) {
        
        logger.info("➕ Creando pedido manual: {}-{} ({} units, {}h)", 
            origin, destination, quantity, deadlineHours);
        
        // 1. Validar datos
        String validationError = validateOrderData(origin, destination, quantity, deadlineHours);
        if (validationError != null) {
            logger.error("❌ Validación fallida: {}", validationError);
            return null;
        }
        
        // 2. Crear pedido dinámico
        DynamicOrder order = DynamicOrder.createManualOrder(
            origin,
            destination,
            quantity,
            deadlineHours,
            currentSimulationTime,
            reason != null ? reason : "Pedido urgente manual"
        );
        
        // 3. Agregar al mapa
        orders.put(order.getId(), order);
        
        logger.info("✅ Pedido manual creado: {} - {}", order.getId(), order.getDescription());
        
        return order;
    }
    
    /**
     * Valida los datos de un pedido.
     */
    private String validateOrderData(String origin, String destination, int quantity, int deadlineHours) {
        if (origin == null || origin.trim().isEmpty()) {
            return "Origen no puede estar vacío";
        }
        
        if (destination == null || destination.trim().isEmpty()) {
            return "Destino no puede estar vacío";
        }
        
        if (origin.equals(destination)) {
            return "Origen y destino deben ser diferentes";
        }
        
        if (quantity <= 0) {
            return "Cantidad debe ser mayor a 0";
        }
        
        if (deadlineHours != 48 && deadlineHours != 72) {
            return "Deadline debe ser 48 o 72 horas";
        }
        
        return null; // Validación exitosa
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PROCESAMIENTO DE PEDIDOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene pedidos que deben inyectarse en el tiempo actual.
     * 
     * @param currentSimulationTime Tiempo actual de la simulación
     * @return Lista de pedidos a inyectar
     */
    public List<DynamicOrder> getOrdersToInjectAt(LocalDateTime currentSimulationTime) {
        return orders.values().stream()
            .filter(order -> order.shouldInjectAt(currentSimulationTime))
            .collect(Collectors.toList());
    }
    
    /**
     * Marca un pedido como inyectado en la simulación.
     * 
     * @param orderId ID del pedido dinámico
     * @param injectedTime Tiempo en que se inyectó
     * @return El ID del pedido en el sistema, o null si falló
     */
    public Integer markOrderAsInjected(String orderId, LocalDateTime injectedTime) {
        DynamicOrder order = orders.get(orderId);
        
        if (order == null) {
            logger.error("❌ Pedido no encontrado: {}", orderId);
            return null;
        }
        
        // Generar ID del sistema
        Integer systemOrderId = nextSystemOrderId.getAndIncrement();
        
        // Marcar como inyectado
        order.markAsInjected(injectedTime, systemOrderId);
        
        logger.info("✅ Pedido inyectado: {} → System ID: {}", orderId, systemOrderId);
        
        return systemOrderId;
    }
    
    /**
     * Marca un pedido como asignado (tras Tabu Search).
     */
    public void markOrderAsAssigned(String orderId) {
        DynamicOrder order = orders.get(orderId);
        
        if (order != null) {
            order.markAsAssigned();
            logger.debug("✅ Pedido asignado: {}", orderId);
        }
    }
    
    /**
     * Marca un pedido como fallido.
     */
    public void markOrderAsFailed(String orderId, String errorMessage) {
        DynamicOrder order = orders.get(orderId);
        
        if (order != null) {
            order.markAsFailed(errorMessage);
            logger.error("❌ Pedido fallido: {} - {}", orderId, errorMessage);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene todos los pedidos dinámicos.
     */
    public Collection<DynamicOrder> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    /**
     * Obtiene pedidos por estado.
     */
    public List<DynamicOrder> getOrdersByStatus(DynamicOrder.OrderStatus status) {
        return orders.values().stream()
            .filter(o -> o.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene un pedido por ID.
     */
    public DynamicOrder getOrderById(String id) {
        return orders.get(id);
    }
    
    /**
     * Obtiene pedidos por ID del sistema.
     */
    public DynamicOrder getOrderBySystemId(Integer systemId) {
        return orders.values().stream()
            .filter(o -> systemId.equals(o.getSystemOrderId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Obtiene pedidos pendientes.
     */
    public List<DynamicOrder> getPendingOrders() {
        return getOrdersByStatus(DynamicOrder.OrderStatus.PENDING);
    }
    
    /**
     * Obtiene pedidos inyectados.
     */
    public List<DynamicOrder> getInjectedOrders() {
        return getOrdersByStatus(DynamicOrder.OrderStatus.INJECTED);
    }
    
    /**
     * Obtiene el total de productos en pedidos dinámicos.
     */
    public int getTotalProducts() {
        return orders.values().stream()
            .mapToInt(DynamicOrder::getQuantity)
            .sum();
    }
    
    /**
     * Obtiene el total de productos inyectados.
     */
    public int getInjectedProducts() {
        return orders.values().stream()
            .filter(o -> o.getStatus() != DynamicOrder.OrderStatus.PENDING)
            .mapToInt(DynamicOrder::getQuantity)
            .sum();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Limpia todos los pedidos.
     */
    public void clear() {
        orders.clear();
        nextSystemOrderId.set(10000);
        logger.info("🧹 Todos los pedidos dinámicos limpiados");
    }
    
    /**
     * Reinicia el generador de IDs.
     */
    public void resetSystemIdGenerator(int startId) {
        nextSystemOrderId.set(startId);
        logger.info("🔄 Generador de IDs reiniciado desde: {}", startId);
    }
    
    /**
     * Obtiene estadísticas de pedidos.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", orders.size());
        stats.put("pending", getOrdersByStatus(DynamicOrder.OrderStatus.PENDING).size());
        stats.put("injected", getOrdersByStatus(DynamicOrder.OrderStatus.INJECTED).size());
        stats.put("assigned", getOrdersByStatus(DynamicOrder.OrderStatus.ASSIGNED).size());
        stats.put("inTransit", getOrdersByStatus(DynamicOrder.OrderStatus.IN_TRANSIT).size());
        stats.put("delivered", getOrdersByStatus(DynamicOrder.OrderStatus.DELIVERED).size());
        stats.put("failed", getOrdersByStatus(DynamicOrder.OrderStatus.FAILED).size());
        
        long scheduled = orders.values().stream()
            .filter(o -> o.getType() == DynamicOrder.OrderType.SCHEDULED)
            .count();
        long manual = orders.values().stream()
            .filter(o -> o.getType() == DynamicOrder.OrderType.MANUAL)
            .count();
        
        stats.put("scheduled", scheduled);
        stats.put("manual", manual);
        stats.put("totalProducts", getTotalProducts());
        stats.put("injectedProducts", getInjectedProducts());
        
        return stats;
    }
    
    /**
     * Log de resumen de pedidos.
     */
    public void logSummary() {
        Map<String, Object> stats = getStatistics();
        logger.info("📊 Pedidos Dinámicos - Total: {}, Pendientes: {}, Inyectados: {}, Asignados: {}, Productos: {}",
            stats.get("total"),
            stats.get("pending"),
            stats.get("injected"),
            stats.get("assigned"),
            stats.get("totalProducts")
        );
    }
}


