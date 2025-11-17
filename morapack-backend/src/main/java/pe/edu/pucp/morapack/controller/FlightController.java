package pe.edu.pucp.morapack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// --- ¡AÑADE ESTA IMPORTACIÓN! ---
import org.springframework.web.bind.annotation.CrossOrigin; 
import pe.edu.pucp.morapack.dto.FlightDto;
import pe.edu.pucp.morapack.service.FlightService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
// --- ¡AÑADE ESTA LÍNEA! ---
@CrossOrigin(origins = "http://localhost:3000") 
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<FlightDto> list() {
        return flightService.listAll();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<FlightDto> cancelFlight(@PathVariable Long id) {
        FlightDto updatedFlight = flightService.cancelFlight(id);
        return ResponseEntity.ok(updatedFlight); 
    }
}