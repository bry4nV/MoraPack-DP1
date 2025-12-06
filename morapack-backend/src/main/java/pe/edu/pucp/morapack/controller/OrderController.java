package pe.edu.pucp.morapack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.service.OrderService;
import pe.edu.pucp.morapack.service.TabuSimulationService;
import pe.edu.pucp.morapack.model.Order;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos", description = "API para gestión de pedidos")
@CrossOrigin(origins = "http://localhost:3000") 
public class OrderController {

    private final OrderService orderService;
    private final TabuSimulationService tabuService;

    public OrderController(OrderService orderService, TabuSimulationService tabuService) {
        this.orderService = orderService;
        this.tabuService = tabuService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listAll() {
        return ResponseEntity.ok(orderService.listAll());
    }

    // Método auxiliar para guardar en BD respetando CHAR(9) y CHAR(7)
    private Order procesarGuardado(String client, String dest, int qty) {
        Order newOrder = new Order();
        
        // 🔥 CORRECCIÓN CRÍTICA: Ajustar a CHAR(9)
        // "O-" (2 chars) + 7 chars UUID = 9 chars exactos.
        String codigoGenerado = "O-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase();
        newOrder.setOrderNumber(codigoGenerado);

        // Validar longitud de cliente (Cortar si es necesario para evitar error)
        if (client.length() > 7) client = client.substring(0, 7);
        newOrder.setClientCode(client); 
        
        newOrder.setAirportDestinationCode(dest); 
        newOrder.setQuantity(qty);
        newOrder.setStatus("PENDING");
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setPersistedOrderTime(LocalTime.now()); 
        
        return orderService.save(newOrder);
    }

    private void lanzarSimulacion(Order savedOrder) {
        new Thread(() -> {
            try {
                System.out.println("🚀 [OrderController] Lanzando simulación para " + savedOrder.getOrderNumber());
                tabuService.iniciarSimulacionVuelo(savedOrder);
            } catch (Exception e) {
                System.err.println("❌ [OrderController] Error fatal en hilo: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody OrderRequest request) {
        try {
            Order saved = procesarGuardado(request.getClientCode(), request.getAirportDestinationCode(), request.getQuantity());
            lanzarSimulacion(saved);
            return ResponseEntity.status(HttpStatus.CREATED).body("{\"message\": \"Creado\"}");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("Archivo vacío");

        int procesados = 0;
        int errores = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.toLowerCase().startsWith("client")) continue;

                String[] data = line.split("[,;]");
                if (data.length >= 3) {
                    try {
                        String client = data[0].trim();
                        String dest = data[1].trim().toUpperCase();
                        int qty = Integer.parseInt(data[2].trim());

                        Order saved = procesarGuardado(client, dest, qty);
                        lanzarSimulacion(saved);
                        
                        procesados++;
                        System.out.println("✅ CSV Procesado: " + client + " -> " + dest);
                    } catch (Exception e) {
                        errores++;
                        System.err.println("❌ Error CSV línea: " + line + " -> " + e.getMessage());
                    }
                }
            }
            return ResponseEntity.ok(String.format("{\"message\": \"Procesados: %d, Errores: %d\"}", procesados, errores));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error lectura: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAll() {
        orderService.deleteAll(); 
        return ResponseEntity.noContent().build();
    }
}

class OrderRequest {
    private String clientCode;
    private String airportDestinationCode;
    private int quantity;
    // Getters y Setters...
    public String getClientCode() { return clientCode; }
    public void setClientCode(String c) { this.clientCode = c; }
    public String getAirportDestinationCode() { return airportDestinationCode; }
    public void setAirportDestinationCode(String a) { this.airportDestinationCode = a; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
}