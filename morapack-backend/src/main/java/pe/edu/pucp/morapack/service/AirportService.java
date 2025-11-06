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
        d.setContinent(e.getContinent());
        d.setCode(e.getCode());
        d.setCity(e.getCity());
        d.setCountry(e.getCountry());
        d.setCityAcronym(e.getCityAcronym());
        d.setGmt(e.getGmt());
        d.setCapacity(e.getCapacity());
        d.setLatitude(e.getLatitude());
        d.setLongitude(e.getLongitude());
        d.setIsHub(e.isMainHub());
        return d;
    }
}
