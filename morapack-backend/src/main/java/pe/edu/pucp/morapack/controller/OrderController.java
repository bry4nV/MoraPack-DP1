package pe.edu.pucp.morapack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.service.OrderService;
import pe.edu.pucp.morapack.model.Order;

import java.time.LocalDate;
import java.time.LocalTime; // <--- Usamos LocalTime (Solo hora)
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos", description = "API para gestión de pedidos")
@CrossOrigin(origins = "http://localhost:3000") 
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos")
    public ResponseEntity<List<OrderDto>> listAll() {
        return ResponseEntity.ok(orderService.listAll());
    }

    @PostMapping("/create")
    @Operation(summary = "Crear nuevo pedido")
    public ResponseEntity<?> create(@RequestBody OrderRequest request) {
        try {
            Order newOrder = new Order();
            
            // 1. Generar código único
            String codigoGenerado = "ORD-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            newOrder.setOrderNumber(codigoGenerado);

            // 2. Mapear datos
            newOrder.setClientCode(request.getClientCode()); 
            newOrder.setAirportDestinationCode(request.getAirportDestinationCode()); 
            newOrder.setQuantity(request.getQuantity());
            newOrder.setStatus("PENDING");
            
            // 3. Fechas
            newOrder.setOrderDate(LocalDate.now());
            
            // 🔥 CORRECCIÓN CRÍTICA AQUÍ 🔥
            // Usamos el nombre que vi en tu Service anterior: 'setPersistedOrderTime'
            // Y usamos LocalTime porque la BD solo pide hora, no fecha+hora.
            newOrder.setPersistedOrderTime(LocalTime.now()); 

            // 4. Guardar
            orderService.save(newOrder); 
            
            return ResponseEntity.status(HttpStatus.CREATED).body("{\"message\": \"Pedido creado: " + codigoGenerado + "\"}");

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"message\": \"Error Backend: " + e.getMessage() + "\"}");
        }
    }
}

// DTO Auxiliar
class OrderRequest {
    private String clientCode;
    private String airportDestinationCode;
    private int quantity;

    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }
    
    public String getAirportDestinationCode() { return airportDestinationCode; }
    public void setAirportDestinationCode(String airportDestinationCode) { this.airportDestinationCode = airportDestinationCode; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}