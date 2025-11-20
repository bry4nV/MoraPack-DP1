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
    private final pe.edu.pucp.morapack.service.FlightStatusTracker flightStatusTracker;

    public DynamicEventsController(
            CancellationService cancellationService,
            DynamicOrderService dynamicOrderService,
            pe.edu.pucp.morapack.service.FlightStatusTracker flightStatusTracker) {

        this.cancellationService = cancellationService;
        this.dynamicOrderService = dynamicOrderService;
        this.flightStatusTracker = flightStatusTracker;
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

    /**
     * Obtener vuelos en tierra (cancelables).
     * Solo retorna vuelos que no han despegado.
     */
    @GetMapping("/flights/grounded")
    public ResponseEntity<Map<String, Object>> getGroundedFlights() {
        List<pe.edu.pucp.morapack.service.FlightStatusTracker.FlightStatusInfo> groundedFlights =
            flightStatusTracker.getCancellableFlights();

        // Convertir a DTOs simples
        List<Map<String, Object>> flightDtos = groundedFlights.stream()
            .map(flight -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("code", flight.flightId);
                dto.put("origin", flight.origin);
                dto.put("destination", flight.destination);
                dto.put("scheduledDepartureTime", flight.scheduledDeparture.toString());
                dto.put("scheduledArrivalTime", flight.scheduledArrival.toString());
                dto.put("status", flight.status.name());
                dto.put("cancellable", flight.cancellable);
                return dto;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", flightDtos.size());
        response.put("flights", flightDtos);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════
    // PEDIDOS DINÁMICOS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Crear un pedido dinámico manualmente durante la simulación.
     * 
     * El origen y deadline se determinan automáticamente según restricciones del caso:
     * - Origen: SPIM (América), EBCI (Europa), o UBBB (Asia/Medio Oriente)
     * - Deadline: 48h mismo continente, 72h intercontinental
     * 
     * Formato del request:
     * {
     *   "destination": "SUAA",
     *   "quantity": 250,
     *   "reason": "Pedido urgente"
     * }
     */
    @PostMapping("/add-order")
    public ResponseEntity<Map<String, Object>> addDynamicOrder(
            @RequestBody DynamicOrderDTO orderDto) {
        
        try {
            // Validar destino
            if (orderDto.getDestination() == null || orderDto.getDestination().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Destination is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Determinar origen automáticamente según restricciones del caso
            String origin = determineOptimalOrigin(orderDto.getDestination());
            
            // Obtener tiempo actual de simulación (se inyecta INMEDIATAMENTE)
            LocalDateTime currentTime = LocalDateTime.now(); // TODO: Obtener de SimulationSession
            
            // Deadline se calcula automáticamente según restricciones:
            // - 48h si origen y destino están en el mismo continente
            // - 72h si es envío intercontinental
            // (Ver PlannerOrder constructor línea 25)
            int deadlineHours = 48; // Placeholder, se recalcula en PlannerOrder
            
            DynamicOrder order = dynamicOrderService.createManualOrder(
                origin,
                orderDto.getDestination(),
                orderDto.getQuantity(),
                deadlineHours, // Se recalcula automáticamente
                currentTime,   // Se inyecta inmediatamente
                orderDto.getReason() != null ? orderDto.getReason() : "Manual order"
            );
            
            // Respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully (Origin: " + origin + ", automatically determined)");
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
     * Determina el origen óptimo basado en el destino.
     * Sigue las mismas reglas que DataLoader.determineOptimalOrigin().
     * 
     * Hubs de distribución:
     * - SPIM (Lima, Perú) → América del Sur
     * - EBCI (Bruselas, Bélgica) → Europa
     * - UBBB (Baku, Azerbaiyán) → Asia/Medio Oriente
     */
    private String determineOptimalOrigin(String destinationCode) {
        // Simplificación: usar primera letra del código ICAO
        // S = Sudamérica → SPIM
        // L, E, U, O = Europa/África/Asia → EBCI o UBBB
        
        char firstChar = destinationCode.charAt(0);
        
        if (firstChar == 'S') {
            // Sudamérica (ICAO codes starting with 'S')
            return "SPIM";
        } else if (firstChar == 'L' || firstChar == 'E') {
            // Europa (ICAO codes starting with 'L' or 'E')
            return "EBCI";
        } else if (firstChar == 'U' || firstChar == 'O' || firstChar == 'V') {
            // Asia/Medio Oriente/Oceanía
            return "UBBB";
        } else {
            // Default: Europa (más conexiones)
            return "EBCI";
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

    /**
     * Cargar cancelaciones en masa desde el frontend.
     *
     * Formato del request:
     * {
     *   "cancellations": [
     *     {"day": 1, "origin": "SPIM", "destination": "SCEL", "departureTime": "0800"},
     *     {"day": 5, "origin": "SKBO", "destination": "SEQM", "departureTime": "1430"}
     *   ],
     *   "startDate": "2025-12-01"
     * }
     */
    @PostMapping("/bulk-cancel-flights")
    public ResponseEntity<Map<String, Object>> bulkCancelFlights(
            @RequestBody Map<String, Object> request) {

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cancellationsData =
                (List<Map<String, Object>>) request.get("cancellations");
            String startDateStr = (String) request.get("startDate");

            if (cancellationsData == null || cancellationsData.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "No cancellations provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            int successCount = 0;
            java.util.List<FlightCancellation> bulkCancellations = new java.util.ArrayList<>();

            for (Map<String, Object> cancData : cancellationsData) {
                int day = ((Number) cancData.get("day")).intValue();
                String origin = (String) cancData.get("origin");
                String destination = (String) cancData.get("destination");
                String departureTime = (String) cancData.get("departureTime"); // HHmm format

                // Parse start date and add the day
                LocalDateTime scheduledDate = LocalDateTime.parse(startDateStr + "T00:00:00")
                    .plusDays(day - 1); // day is 1-based

                // Add hours and minutes from departureTime (HHmm)
                int hours = Integer.parseInt(departureTime.substring(0, 2));
                int minutes = Integer.parseInt(departureTime.substring(2, 4));
                scheduledDate = scheduledDate.withHour(hours).withMinute(minutes);

                // Format time as HH:mm
                String scheduledTimeStr = String.format("%02d:%02d", hours, minutes);

                // Create scheduled cancellation using constructor
                FlightCancellation cancellation = new FlightCancellation(
                    origin,
                    destination,
                    scheduledTimeStr,
                    scheduledDate,
                    "Bulk cancellation upload"
                );

                bulkCancellations.add(cancellation);
                successCount++;
            }

            // Add all cancellations to the service
            cancellationService.addBulkCancellations(bulkCancellations);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", successCount + " cancellations loaded successfully");
            response.put("count", successCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load cancellations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}

