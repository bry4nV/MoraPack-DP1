package pe.edu.pucp.morapack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.dto.AirportDto;
import pe.edu.pucp.morapack.service.AirportService;

import java.util.List;

@RestController
@RequestMapping("/api/airports")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Aeropuertos", description = "API para gestión de aeropuertos")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los aeropuertos", description = "Obtiene la lista completa de aeropuertos")
    public ResponseEntity<List<AirportDto>> listAll() {
        try {
            List<AirportDto> airports = airportService.listAll();
            return ResponseEntity.ok(airports);
        } catch (Exception e) {
            e.printStackTrace(); // Ver el error en consola
            throw e;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener aeropuerto por ID", description = "Obtiene un aeropuerto específico por su ID")
    public ResponseEntity<AirportDto> getById(@PathVariable Long id) {
        AirportDto airport = airportService.getById(id);
        return ResponseEntity.ok(airport);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo aeropuerto", description = "Crea un nuevo aeropuerto en el sistema")
    public ResponseEntity<AirportDto> create(@RequestBody AirportDto airportDto) {
        AirportDto created = airportService.create(airportDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar aeropuerto", description = "Actualiza un aeropuerto existente")
    public ResponseEntity<AirportDto> update(@PathVariable Long id, @RequestBody AirportDto airportDto) {
        AirportDto updated = airportService.update(id, airportDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar aeropuerto", description = "Elimina un aeropuerto del sistema")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        airportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
