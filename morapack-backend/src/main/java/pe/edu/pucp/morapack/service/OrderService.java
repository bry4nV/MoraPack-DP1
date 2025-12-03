package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.repository.daily.OrderRepository;
import pe.edu.pucp.morapack.dto.CreateOrderDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // 👇 ESTE ES EL MÉTODO QUE TE FALTABA PARA CORREGIR EL ERROR 👇
    public Order save(Order order) {
        return orderRepository.save(order);
    }
    // -------------------------------------------------------------

    public List<OrderDto> listAll() {
        List<Order> entities = orderRepository.findAll();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public OrderDto getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));
        return toDto(order);
    }

    public OrderDto createOrder(CreateOrderDto dto) {
        // Validar que el número de pedido no exista
        if (orderRepository.existsByOrderNumber(dto.getOrderNumber())) {
            throw new RuntimeException("Ya existe un pedido con el número: " + dto.getOrderNumber());
        }

        Order newOrder = new Order();
        newOrder.setOrderNumber(dto.getOrderNumber());
        newOrder.setAirportDestinationCode(dto.getAirportDestinationCode());
        newOrder.setQuantity(dto.getQuantity());
        newOrder.setClientCode(dto.getClientCode());
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setPersistedOrderTime(LocalTime.now());

        Order saved = orderRepository.save(newOrder);
        return toDto(saved);
    }

    public OrderDto update(Long id, OrderDto dto) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con id: " + id));

        // Validar número de pedido único si se está cambiando
        if (dto.getOrderNumber() != null && !existing.getOrderNumber().equals(dto.getOrderNumber()) && 
            orderRepository.existsByOrderNumber(dto.getOrderNumber())) {
            throw new RuntimeException("Ya existe un pedido con el número: " + dto.getOrderNumber());
        }

        updateEntityFromDto(existing, dto);
        Order updated = orderRepository.save(existing);
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Pedido no encontrado con id: " + id);
        }
        orderRepository.deleteById(id);
    }

    // Convierte Entidad -> DTO
    private OrderDto toDto(Order e) {
        OrderDto d = new OrderDto();
        d.setId(e.getId());
        d.setOrderNumber(e.getOrderNumber());
        d.setOrderDate(e.getOrderDate() != null ? e.getOrderDate().toString() : null);
        d.setOrderTime(e.getPersistedOrderTime() != null ? e.getPersistedOrderTime().toString() : null);
        d.setAirportDestinationCode(e.getAirportDestinationCode());
        d.setQuantity(e.getQuantity());
        d.setClientCode(e.getClientCode());
        d.setStatus(e.getStatus());
        return d;
    }

    // Actualiza entidad existente con datos del DTO
    private void updateEntityFromDto(Order e, OrderDto dto) {
        if (dto.getOrderNumber() != null) {
            e.setOrderNumber(dto.getOrderNumber());
        }
        if (dto.getAirportDestinationCode() != null) {
            e.setAirportDestinationCode(dto.getAirportDestinationCode());
        }
        if (dto.getQuantity() != null) {
            e.setQuantity(dto.getQuantity());
        }
        if (dto.getClientCode() != null) {
            e.setClientCode(dto.getClientCode());
        }
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
    }

    public void deleteAll() {
        orderRepository.deleteAll();
    }
}