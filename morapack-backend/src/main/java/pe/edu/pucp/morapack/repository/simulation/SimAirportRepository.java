package pe.edu.pucp.morapack.repository.simulation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.simulation.SimAirport;

import java.util.List;

/**
 * Repository for accessing simulation airport data from moraTravelSimulation schema.
 */
@Repository
public interface SimAirportRepository extends JpaRepository<SimAirport, Long> {

    /**
     * Get all active airports for simulation.
     */
    @Query("SELECT a FROM SimAirport a WHERE a.status = 'ACTIVE' ORDER BY a.code")
    List<SimAirport> findActiveAirports();

    /**
     * Find airport by code.
     */
    @Query("SELECT a FROM SimAirport a WHERE a.code = :code")
    SimAirport findByCode(String code);
}
