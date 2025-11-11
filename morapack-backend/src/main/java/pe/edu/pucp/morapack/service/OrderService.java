package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderDto> listAll() {
        List<Order> entities = orderRepository.findAll();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private OrderDto toDto(Order e) {
        OrderDto d = new OrderDto();
        d.setId(e.getId());
        d.setOrderNumber(e.getOrderNumber());
        d.setOrderDate(e.getOrderDate() != null ? e.getOrderDate().toString() : null);
        d.setOrderTime(e.getOrderTime() != null ? e.getOrderTime().toString() : null);
        d.setAirportDestinationCode(e.getAirportDestinationCode());
        d.setQuantity(e.getQuantity());
        d.setClientCode(e.getClientCode());
        d.setStatus(e.getStatus());
        return d;
    }
}