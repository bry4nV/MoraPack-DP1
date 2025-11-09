package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.OrderDto;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.repository.OrderRepository;

import java.util.List;
import java.util.stream.Collectors;
// Importamos formateadores para convertir la fecha y hora a String
import java.time.format.DateTimeFormatter; 

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

    // --- MÉTODO ACTUALIZADO ---
    // Convierte la Entidad (Order) al DTO (OrderDto)
    private OrderDto toDto(Order e) { // 'e' es la Entidad Order
        OrderDto d = new OrderDto(); // 'd' es el DTO

        // Mapeo de los campos nuevos
        d.setId(e.getId());
        d.setOrderNumber(e.getOrderNumber());
        d.setAirportDestinationCode(e.getAirportDestinationCode());
        d.setQuantity(e.getQuantity());
        d.setClientCode(e.getClientCode());
        d.setStatus(e.getStatus());

        // Conversión de LocalDate/LocalTime a String
        // Añadimos chequeos de nulidad para evitar errores si un dato falta en la BD
        if (e.getOrderDate() != null) {
            // Convierte la fecha (ej: 2025-11-06) a un String
            d.setOrderDate(e.getOrderDate().toString());
        }

        if (e.getPersistedOrderTime() != null) {
            // Convierte la hora (ej: 14:30:00) a un String
            d.setOrderTime(e.getPersistedOrderTime().format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
        
        return d;
    }
}