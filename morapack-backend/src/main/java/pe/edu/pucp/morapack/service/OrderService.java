package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.repository.OrderRepository;
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

    public List<OrderDto> listAll() {
        List<Order> entities = orderRepository.findAll();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

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

    // ... (después de tus otros métodos) ...

    public OrderDto createOrder(CreateOrderDto dto) {
        // 1. Crea una nueva entidad Order
        Order newOrder = new Order();

        // 2. Setea los datos que vienen del formulario (DTO)
        newOrder.setOrderNumber(dto.getOrderNumber());
        newOrder.setAirportDestinationCode(dto.getAirportDestinationCode());
        newOrder.setQuantity(dto.getQuantity());
        newOrder.setClientCode(dto.getClientCode());

        // 3. Setea los datos automáticos (¡tu requerimiento!)
        newOrder.setOrderDate(LocalDate.now());
        newOrder.setPersistedOrderTime(LocalTime.now());
        // El status se setea a "UNASSIGNED" automáticamente
        // gracias al valor por defecto que pusimos en Order.java

        // 4. Guarda la nueva entidad en la base de datos
        Order savedOrder = orderRepository.save(newOrder);

        // 5. Devuelve el pedido recién creado (convertido a DTO)
        return toDto(savedOrder);
    }
}