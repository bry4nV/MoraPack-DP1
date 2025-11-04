package pe.edu.pucp.morapack.controller;

import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.dto.ShipmentDto;
import pe.edu.pucp.morapack.service.ShipmentService;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<ShipmentDto> list() {
        return shipmentService.listAll();
    }

    @GetMapping("/order/{idPedido}")
    public List<ShipmentDto> listByOrder(@PathVariable Long idPedido) {
        return shipmentService.listByOrderId(idPedido);
    }
}
