package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.FlightDto;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.repository.FlightRepository;

import java.util.List;
import java.util.stream.Collectors;
// (No se necesitan imports extras, .toString() es suficiente para LocalDate/LocalTime)

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public List<FlightDto> listAll() {
        List<Flight> entities = flightRepository.findAll();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    // --- MÉTODO ACTUALIZADO ---
    // Convierte la Entidad (Flight) al DTO (FlightDto)
    private FlightDto toDto(Flight e) { // 'e' es la Entidad Flight
        FlightDto d = new FlightDto(); // 'd' es el DTO

        // Mapeo de los campos nuevos
        d.setId(e.getId());
        d.setAirportOriginCode(e.getAirportOriginCode());       // <-- CAMBIADO
        d.setAirportDestinationCode(e.getAirportDestinationCode()); // <-- CAMBIADO
        d.setCapacity(e.getCapacity());
        d.setStatus(e.getStatus());

        // Conversión de LocalDate/LocalTime a String con chequeo de nulos
        
        if (e.getFlightDate() != null) {
            d.setFlightDate(e.getFlightDate().toString()); // <-- ¡AÑADIDO!
        }
        
        if (e.getDepartureTime() != null) {
            d.setDepartureTime(e.getDepartureTime().toString());
        }
        
        if (e.getArrivalTime() != null) {
            d.setArrivalTime(e.getArrivalTime().toString());
        }
        
        return d;
    }
}