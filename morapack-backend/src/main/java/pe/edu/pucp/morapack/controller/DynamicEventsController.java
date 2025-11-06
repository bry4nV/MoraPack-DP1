package pe.edu.pucp.morapack.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.dto.FlightCancellationDTO;
import pe.edu.pucp.morapack.dto.DynamicOrderDTO;
import pe.edu.pucp.morapack.model.FlightCancellation;
import pe.edu.pucp.morapack.model.DynamicOrder;
import pe.edu.pucp.morapack.service.CancellationService;
import pe.edu.pucp.morapack.service.DynamicOrderService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API para gestionar eventos dinámicos durante la simulación.
 * 
 * Endpoints:
 * - Cancelación manual de vuelos
 * - Creación manual de pedidos dinámicos
 * - Carga de archivos de eventos programados
 * - Consulta de estado de eventos
 */
@RestController
@RequestMapping("/api/simulation/events")
public class DynamicEventsController {
    
    private final CancellationService cancellationService;
    private final DynamicOrderService dynamicOrderService;
    
    public DynamicEventsController(
            CancellationService cancellationService,
            DynamicOrderService dynamicOrderService) {
        
        this.cancellationService = cancellationService;
        this.dynamicOrderService = dynamicOrderService;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CANCELACIONES DE VUELOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Cancelar un vuelo manualmente durante la simulación.
     * 
     * Formato del request:
     * {
     *   "flightOrigin": "SPIM",
     *   "flightDestination": "SEQM",
     *   "scheduledDepartureTime": "2025-12-01T06:00:00",
     *   "reason": "Maintenance issue"
     * }
     */
    @PostMapping("/cancel-flight")
    public ResponseEntity<Map<String, Object>> cancelFlight(
            @RequestBody FlightCancellationDTO cancellationDto) {
        
        try {
            // Obtener tiempo actual de simulación (si está corriendo)
            // Si no hay simulación activa, usar tiempo actual del sistema
            LocalDateTime currentTime = LocalDateTime.now(); // Simplificado por ahora
            
            // Crear cancelación manual
            FlightCancellation cancellation = cancellationService.requestManualCancellation(
                cancellationDto.getFlightOrigin(),
                cancellationDto.getFlightDestination(),
                cancellationDto.getScheduledDepartureTime(),
                currentTime,
                cancellationDto.getReason() != null ? cancellationDto.getReason() : "Manual cancellation"
            );
            
            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Flight cancelled successfully");
            response.put("cancellation", toCancellationDTO(cancellation));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to cancel flight: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Cargar cancelaciones programadas desde un archivo.
     * 
     * Formato del request:
     * {
     *   "filePath": "data/cancellations/cancellations_2025_12.txt",
     *   "startDate": "2025-12-01"
     * }
     */
    @PostMapping("/load-cancellations")
    public ResponseEntity<Map<String, Object>> loadCancellations(
            @RequestBody Map<String, String> request) {
        
        try {
            String filePath = request.get("filePath");
            String startDateStr = request.get("startDate");
            
            if (filePath == null || startDateStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Missing required fields: filePath, startDate");
                return ResponseEntity.badRequest().body(error);
            }
            
            java.time.LocalDate startDate = java.time.LocalDate.parse(startDateStr);
            int loadedCount = cancellationService.loadScheduledCancellations(filePath, startDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cancellations loaded successfully");
            response.put("count", loadedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load cancellations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtener todas las cancelaciones (ejecutadas y pendientes).
     */
    @GetMapping("/cancellations")
    public ResponseEntity<Map<String, Object>> getAllCancellations() {
        List<FlightCancellation> all = new java.util.ArrayList<>(cancellationService.getAllCancellations());
        List<FlightCancellationDTO> dtos = all.stream()
            .map(this::toCancellationDTO)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", dtos.size());
        response.put("cancellations", dtos);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener cancelaciones ejecutadas.
     */
    @GetMapping("/cancellations/executed")
    public ResponseEntity<Map<String, Object>> getExecutedCancellations() {
        List<FlightCancellation> executed = cancellationService.getExecutedCancellations();
        List<FlightCancellationDTO> dtos = executed.stream()
            .map(this::toCancellationDTO)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", dtos.size());
        response.put("cancellations", dtos);
        
        return ResponseEntity.ok(response);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PEDIDOS DINÁMICOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Crear un pedido dinámico manualmente durante la simulación.
     * 
     * Formato del request:
     * {
     *   "origin": "SPIM",
     *   "destination": "EBCI",
     *   "quantity": 250,
     *   "deadlineHours": 48
     * }
     */
    @PostMapping("/add-order")
    public ResponseEntity<Map<String, Object>> addDynamicOrder(
            @RequestBody DynamicOrderDTO orderDto) {
        
        try {
            // Obtener tiempo actual de simulación
            LocalDateTime currentTime = LocalDateTime.now(); // Simplificado por ahora
            
            // Crear pedido manual
            int deadlineHours = orderDto.getDeadlineHours();
            if (deadlineHours <= 0) {
                deadlineHours = 48; // Default
            }
            
            DynamicOrder order = dynamicOrderService.createManualOrder(
                orderDto.getOrigin(),
                orderDto.getDestination(),
                orderDto.getQuantity(),
                deadlineHours,
                currentTime,
                orderDto.getReason() != null ? orderDto.getReason() : "Manual order"
            );
            
            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("order", toOrderDTO(order));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Cargar pedidos programados desde un archivo.
     * 
     * Formato del request:
     * {
     *   "filePath": "data/dynamic_orders/dynamic_orders_2025_12.txt",
     *   "startDate": "2025-12-01"
     * }
     */
    @PostMapping("/load-orders")
    public ResponseEntity<Map<String, Object>> loadDynamicOrders(
            @RequestBody Map<String, String> request) {
        
        try {
            String filePath = request.get("filePath");
            String startDateStr = request.get("startDate");
            
            if (filePath == null || startDateStr == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Missing required fields: filePath, startDate");
                return ResponseEntity.badRequest().body(error);
            }
            
            java.time.LocalDate startDate = java.time.LocalDate.parse(startDateStr);
            int loadedCount = dynamicOrderService.loadScheduledOrders(filePath, startDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Orders loaded successfully");
            response.put("count", loadedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Obtener todos los pedidos dinámicos (inyectados y pendientes).
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllDynamicOrders() {
        List<DynamicOrder> all = new java.util.ArrayList<>(dynamicOrderService.getAllOrders());
        List<DynamicOrderDTO> dtos = all.stream()
            .map(this::toOrderDTO)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", dtos.size());
        response.put("orders", dtos);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtener pedidos inyectados.
     */
    @GetMapping("/orders/injected")
    public ResponseEntity<Map<String, Object>> getInjectedOrders() {
        List<DynamicOrder> injected = dynamicOrderService.getInjectedOrders();
        List<DynamicOrderDTO> dtos = injected.stream()
            .map(this::toOrderDTO)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", dtos.size());
        response.put("orders", dtos);
        
        return ResponseEntity.ok(response);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CONVERSIÓN A DTO
    // ═══════════════════════════════════════════════════════════════
    
    private FlightCancellationDTO toCancellationDTO(FlightCancellation c) {
        FlightCancellationDTO dto = new FlightCancellationDTO();
        dto.setId(c.getId());
        dto.setFlightOrigin(c.getFlightOrigin());
        dto.setFlightDestination(c.getFlightDestination());
        dto.setScheduledDepartureTime(c.getScheduledDepartureTime());
        dto.setCancellationTime(c.getCancellationTime() != null ? c.getCancellationTime().toString() : null);
        dto.setReason(c.getReason());
        dto.setStatus(c.getStatus().name());
        return dto;
    }
    
    private DynamicOrderDTO toOrderDTO(DynamicOrder o) {
        DynamicOrderDTO dto = new DynamicOrderDTO();
        dto.setId(o.getId());
        dto.setOrigin(o.getOrigin());
        dto.setDestination(o.getDestination());
        dto.setQuantity(o.getQuantity());
        dto.setDeadlineHours(o.getDeadlineHours());
        dto.setInjectionTime(o.getInjectionTime() != null ? o.getInjectionTime().toString() : null);
        dto.setReason(o.getReason());
        dto.setStatus(o.getStatus().name());
        return dto;
    }
}

