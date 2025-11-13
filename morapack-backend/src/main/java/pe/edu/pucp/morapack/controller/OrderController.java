package pe.edu.pucp.morapack.controller;

// --- IMPORTACIONES AÑADIDAS ---
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.CrossOrigin; // ¡Importante!
import pe.edu.pucp.morapack.dto.CreateOrderDto; // El DTO de creación
// ---

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000") // <-- ¡AÑADIDO! Da permiso al frontend
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderDto> list() {
        return orderService.listAll();
    }

    // --- ¡NUEVO MÉTODO AÑADIDO! ---
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Devuelve un código 201 (Creado)
    public OrderDto create(@RequestBody CreateOrderDto createOrderDto) {
        // Llama al método 'createOrder' que hicimos en el servicio
        return orderService.createOrder(createOrderDto);
    }
}