package pe.edu.pucp.morapack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.Airport;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {
    boolean existsByCode(String code);
}
