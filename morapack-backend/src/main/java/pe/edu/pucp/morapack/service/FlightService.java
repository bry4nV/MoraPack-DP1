package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.FlightDto;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.repository.FlightRepository;

import java.util.List;
import java.util.stream.Collectors;

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

    private FlightDto toDto(Flight e) {
        FlightDto d = new FlightDto();
        d.setId(e.getId());
        d.setIdAeropuertoOrigen(e.getIdAeropuertoOrigen());
        d.setIdAeropuertoDestino(e.getIdAeropuertoDestino());
        d.setHoraSalida(e.getHoraSalida() != null ? e.getHoraSalida().toString() : null);
        d.setHoraLlegada(e.getHoraLlegada() != null ? e.getHoraLlegada().toString() : null);
        d.setCapacidad(e.getCapacidad());
        d.setEstado(e.getEstado());
        return d;
    }
}
