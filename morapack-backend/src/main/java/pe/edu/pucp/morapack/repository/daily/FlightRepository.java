package pe.edu.pucp.morapack.repository.daily;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.Flight;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    // Query methods si es necesario
}
