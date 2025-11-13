package pe.edu.pucp.morapack.service;

import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.dto.AirportDto;
import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.repository.daily.AirportRepository;

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

    public AirportDto getById(Long id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aeropuerto no encontrado con id: " + id));
        return toDto(airport);
    }

    public AirportDto create(AirportDto dto) {
        // Validar que el código no exista
        if (airportRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Ya existe un aeropuerto con el código: " + dto.getCode());
        }

        // DEBUG: Ver qué llega
        System.out.println("Creating airport - isHub: " + dto.isHub());

        Airport entity = toEntity(dto);

        // DEBUG: Ver qué se guarda
        System.out.println("Entity before save - isHub: " + entity.isHub());

        Airport saved = airportRepository.save(entity);

        // DEBUG: Ver qué se guardó
        System.out.println("Entity after save - isHub: " + saved.isHub());

        return toDto(saved);
    }

    public AirportDto update(Long id, AirportDto dto) {
        Airport existing = airportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aeropuerto no encontrado con id: " + id));

        // Validar código único si se está cambiando
        if (!existing.getCode().equals(dto.getCode()) && airportRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Ya existe un aeropuerto con el código: " + dto.getCode());
        }

        updateEntityFromDto(existing, dto);
        Airport updated = airportRepository.save(existing);
        return toDto(updated);
    }

    public void delete(Long id) {
        if (!airportRepository.existsById(id)) {
            throw new RuntimeException("Aeropuerto no encontrado con id: " + id);
        }
        airportRepository.deleteById(id);
    }

    // Convierte Entidad -> DTO
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
        d.setStatus(e.getStatus());
        d.setHub(e.isHub());
        return d;
    }

    // Convierte DTO -> Entidad (para crear nuevo)
    private Airport toEntity(AirportDto dto) {
        Airport e = new Airport();
        e.setContinent(dto.getContinent());
        e.setCode(dto.getCode());
        e.setCity(dto.getCity());
        e.setCountry(dto.getCountry());
        e.setCityAcronym(dto.getCityAcronym());
        e.setGmt(dto.getGmt());
        e.setCapacity(dto.getCapacity());
        e.setLatitude(dto.getLatitude());
        e.setLongitude(dto.getLongitude());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        e.setHub(dto.isHub()); // Asegúrate de que esté usando el getter correcto

        System.out.println("toEntity - isHub from DTO: " + dto.isHub()); // DEBUG

        return e;
    }

    // Actualiza entidad existente con datos del DTO
    private void updateEntityFromDto(Airport e, AirportDto dto) {
        e.setContinent(dto.getContinent());
        e.setCode(dto.getCode());
        e.setCity(dto.getCity());
        e.setCountry(dto.getCountry());
        e.setCityAcronym(dto.getCityAcronym());
        e.setGmt(dto.getGmt());
        e.setCapacity(dto.getCapacity());
        e.setLatitude(dto.getLatitude());
        e.setLongitude(dto.getLongitude());
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
        e.setHub(dto.isHub());
    }
}
