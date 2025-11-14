package pe.edu.pucp.morapack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.dto.CreateOrderDto;
import pe.edu.pucp.morapack.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos", description = "API para gestión de pedidos")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los pedidos", description = "Obtiene la lista completa de pedidos")
    public ResponseEntity<List<OrderDto>> listAll() {
        List<OrderDto> orders = orderService.listAll();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido por ID", description = "Obtiene un pedido específico por su ID")
    public ResponseEntity<OrderDto> getById(@PathVariable Long id) {
        OrderDto order = orderService.getById(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo pedido", description = "Crea un nuevo pedido en el sistema")
    public ResponseEntity<OrderDto> create(@RequestBody CreateOrderDto orderDto) {
        OrderDto created = orderService.createOrder(orderDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar pedido", description = "Actualiza un pedido existente")
    public ResponseEntity<OrderDto> update(@PathVariable Long id, @RequestBody OrderDto orderDto) {
        OrderDto updated = orderService.update(id, orderDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar pedido", description = "Elimina un pedido del sistema")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}