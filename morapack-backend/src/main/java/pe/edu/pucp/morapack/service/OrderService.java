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
        d.setPackageCount(e.getPackageCount());
        d.setAirportDestinationId(e.getAirportDestinationId());
        d.setPriority(e.getPriority());
        d.setClientId(e.getClientId());
        d.setStatus(e.getStatus());
        d.setDay(e.getDay());
        d.setHour(e.getHour());
        d.setMinute(e.getMinute());
        return d;
    }
}
