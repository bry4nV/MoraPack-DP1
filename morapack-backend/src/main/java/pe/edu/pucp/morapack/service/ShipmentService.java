package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.ShipmentDto;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.repository.ShipmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public List<ShipmentDto> listAll() {
        List<Shipment> entities = shipmentRepository.findAll();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ShipmentDto> listByOrderId(Long idPedido) {
        List<Shipment> entities = shipmentRepository.findByIdPedido(idPedido);
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private ShipmentDto toDto(Shipment e) {
        ShipmentDto d = new ShipmentDto();
        d.setId(e.getId());
        d.setIdPedido(e.getIdPedido());
        d.setIdAeropuertoActual(e.getIdAeropuertoActual());
        d.setIdVueloActual(e.getIdVueloActual());
        d.setEstado(e.getEstado());
        return d;
    }
}
