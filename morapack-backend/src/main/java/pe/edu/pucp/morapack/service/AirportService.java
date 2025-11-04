package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.AirportDto;
import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.repository.AirportRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AirportService {

    private final AirportRepository airportRepository;

    public AirportService(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    public List<AirportDto> listAll() {
        List<Airport> entities = airportRepository.findAll();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    private AirportDto toDto(Airport e) {
        AirportDto d = new AirportDto();
        d.setId(e.getId());
        d.setName(e.getName()); // ← CAMBIO
        d.setCountry(e.getCountry()); // ← CAMBIO
        d.setCity(e.getCity()); // ← CAMBIO
        d.setGmt(e.getGmt());
        d.setCapacity(e.getCapacity()); // ← CAMBIO
        d.setContinent(e.getContinent()); // ← CAMBIO
        d.setIsHub(e.isMainHub()); // ← CAMBIO
        return d;
    }
}
