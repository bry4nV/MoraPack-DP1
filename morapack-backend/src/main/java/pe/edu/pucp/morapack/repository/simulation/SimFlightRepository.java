package pe.edu.pucp.morapack.repository.simulation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.morapack.model.simulation.SimFlight;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for accessing simulation flight data from moraTravelSimulation schema.
 */
@Repository
public interface SimFlightRepository extends JpaRepository<SimFlight, Long> {

    /**
     * For WEEKLY scenario: Get flights within a specific date range.
     */
    @Query("SELECT f FROM SimFlight f WHERE " +
           "f.flightDate >= :startDate AND f.flightDate < :endDate " +
           "ORDER BY f.flightDate, f.departureTime")
    List<SimFlight> findFlightsInDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * For COLLAPSE scenario: Get ALL flights from a starting date onwards.
     */
    @Query("SELECT f FROM SimFlight f WHERE " +
           "f.flightDate >= :startDate " +
           "ORDER BY f.flightDate, f.departureTime")
    List<SimFlight> findAllFlightsFromDate(
        @Param("startDate") LocalDate startDate
    );

    /**
     * Alias for findFlightsInDateRange (used by DatabaseDataProvider).
     */
    @Query("SELECT f FROM SimFlight f WHERE " +
           "f.flightDate >= :startDate AND f.flightDate < :endDate " +
           "ORDER BY f.flightDate, f.departureTime")
    List<SimFlight> findFlightsBetweenDates(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get cancelled flights within a date range.
     */
    @Query("SELECT f FROM SimFlight f WHERE " +
           "f.flightDate >= :startDate AND f.flightDate < :endDate " +
           "AND f.status = 'CANCELLED' " +
           "ORDER BY f.flightDate, f.departureTime")
    List<SimFlight> findCancelledFlights(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find a specific flight by origin, destination, date, and departure time.
     * Used for cancellations to verify if flight exists in database.
     */
    @Query("SELECT f FROM SimFlight f WHERE " +
           "f.airportOriginCode = :origin " +
           "AND f.airportDestinationCode = :destination " +
           "AND f.flightDate = :flightDate " +
           "AND f.departureTime = :departureTime")
    SimFlight findFlightByRouteAndTime(
        @Param("origin") String origin,
        @Param("destination") String destination,
        @Param("flightDate") LocalDate flightDate,
        @Param("departureTime") java.time.LocalTime departureTime
    );
}
