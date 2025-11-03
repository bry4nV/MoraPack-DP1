package pe.edu.pucp.morapack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.scheduler.FlightExpander;
import pe.edu.pucp.morapack.algos.scheduler.FlightTemplate;
import pe.edu.pucp.morapack.model.Country;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Tests for FlightExpander - expanding flight templates to specific dates
 */
class FlightExpanderTest {
    
    private FlightExpander expander;
    private PlannerAirport lima;
    private PlannerAirport santiago;
    
    @BeforeEach
    void setUp() {
        expander = new FlightExpander();
        lima = createAirport("LIM", "Lima");
        santiago = createAirport("SCL", "Santiago");
    }
    
    @Test
    void testExpandSingleFlightForOneDay() {
        // Template: LIM→SCL departing 08:30 local, arriving 12:45 local
        // Lima is GMT-5, Santiago is GMT-3
        FlightTemplate template = new FlightTemplate(
            1,
            lima,
            santiago,
            LocalTime.of(8, 30),   // 08:30 Lima local
            LocalTime.of(12, 45),  // 12:45 Santiago local
            300,
            1500.0
        );
        
        LocalDate date = LocalDate.of(2025, 10, 15);
        PlannerFlight flight = expander.expandForDate(template, date);
        
        assertNotNull(flight);
        assertEquals(lima, flight.getOrigin());
        assertEquals(santiago, flight.getDestination());
        
        // Departure: 08:30 Lima local (GMT-5) → 08:30 + 5 = 13:30 UTC
        assertEquals(LocalDateTime.of(2025, 10, 15, 13, 30), flight.getDepartureTime());
        
        // Arrival: 12:45 Santiago local (GMT-3) → 12:45 + 3 = 15:45 UTC
        assertEquals(LocalDateTime.of(2025, 10, 15, 15, 45), flight.getArrivalTime());
        
        assertEquals(300, flight.getCapacity());
        assertEquals(1500.0, flight.getCost());
    }
    
    @Test
    void testExpandFlightArrivingNextDay() {
        // Template: LIM→SCL departing 23:30 local, arriving 03:45 local (next day)
        // Lima GMT-5, Santiago GMT-3
        FlightTemplate template = new FlightTemplate(
            1,
            lima,
            santiago,
            LocalTime.of(23, 30),  // 23:30 Lima local
            LocalTime.of(3, 45),   // 03:45 Santiago local (earlier = next day)
            300,
            1500.0
        );
        
        assertTrue(template.arrivesNextDay(), "Should detect next-day arrival");
        
        LocalDate date = LocalDate.of(2025, 10, 15);
        PlannerFlight flight = expander.expandForDate(template, date);
        
        // Departure: 23:30 Lima local (GMT-5) → 23:30 + 5 = 28:30 = 04:30 UTC next day
        assertEquals(LocalDateTime.of(2025, 10, 16, 4, 30), flight.getDepartureTime());
        
        // Arrival: 03:45 Santiago local (GMT-3) next day → 03:45 + 3 = 06:45 UTC next day
        assertEquals(LocalDateTime.of(2025, 10, 16, 6, 45), flight.getArrivalTime());
    }
    
    @Test
    void testExpandFlightForDateRange() {
        FlightTemplate template = new FlightTemplate(
            1,
            lima,
            santiago,
            LocalTime.of(8, 30),  // 08:30 Lima local
            LocalTime.of(12, 45), // 12:45 Santiago local
            300,
            1500.0
        );
        
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 7); // 7 days
        
        List<PlannerFlight> flights = expander.expandForDateRange(template, startDate, endDate);
        
        assertEquals(7, flights.size(), "Should generate 7 flights for 7 days");
        
        // Check first flight: 08:30 Lima local (GMT-5) = 13:30 UTC
        PlannerFlight first = flights.get(0);
        assertEquals(LocalDateTime.of(2025, 10, 1, 13, 30), first.getDepartureTime());
        
        // Check last flight: 08:30 Lima local (GMT-5) = 13:30 UTC
        PlannerFlight last = flights.get(6);
        assertEquals(LocalDateTime.of(2025, 10, 7, 13, 30), last.getDepartureTime());
    }
    
    @Test
    void testExpandMultipleTemplatesForDateRange() {
        FlightTemplate template1 = new FlightTemplate(
            1, lima, santiago,
            LocalTime.of(8, 0), LocalTime.of(12, 0),
            300, 1500.0
        );
        
        FlightTemplate template2 = new FlightTemplate(
            2, lima, santiago,
            LocalTime.of(16, 0), LocalTime.of(20, 0),
            250, 1300.0
        );
        
        List<FlightTemplate> templates = List.of(template1, template2);
        
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 3); // 3 days
        
        List<PlannerFlight> flights = expander.expandAllForDateRange(templates, startDate, endDate);
        
        // 2 templates × 3 days = 6 flights
        assertEquals(6, flights.size());
    }
    
    @Test
    void testExpandForTimeWindow() {
        FlightTemplate template = new FlightTemplate(
            1,
            lima,
            santiago,
            LocalTime.of(10, 0),  // 10:00 Lima local
            LocalTime.of(14, 0),  // 14:00 Santiago local
            300,
            1500.0
        );
        
        List<FlightTemplate> templates = List.of(template);
        
        // Window: Oct 1, 14:00 UTC to Oct 1, 18:00 UTC
        // Flight departs at 10:00 Lima local = 15:00 UTC (should be included)
        LocalDateTime windowStart = LocalDateTime.of(2025, 10, 1, 14, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2025, 10, 1, 18, 0);
        
        List<PlannerFlight> flights = expander.expandForTimeWindow(templates, windowStart, windowEnd);
        
        // Flight departs at 10:00 Lima (GMT-5) = 15:00 UTC, within [14:00, 18:00] UTC
        assertEquals(1, flights.size());
        assertEquals(LocalDateTime.of(2025, 10, 1, 15, 0), flights.get(0).getDepartureTime());
    }
    
    @Test
    void testExpandForTimeWindowExcludesFlightsOutsideWindow() {
        FlightTemplate template = new FlightTemplate(
            1,
            lima,
            santiago,
            LocalTime.of(15, 0),  // 15:00 Lima local = 20:00 UTC
            LocalTime.of(19, 0),  // 19:00 Santiago local
            300,
            1500.0
        );
        
        List<FlightTemplate> templates = List.of(template);
        
        // Window: Oct 1, 08:00 UTC to Oct 1, 12:00 UTC
        LocalDateTime windowStart = LocalDateTime.of(2025, 10, 1, 8, 0);
        LocalDateTime windowEnd = LocalDateTime.of(2025, 10, 1, 12, 0);
        
        List<PlannerFlight> flights = expander.expandForTimeWindow(templates, windowStart, windowEnd);
        
        // Flight departs at 15:00 Lima (GMT-5) = 20:00 UTC, which is OUTSIDE [08:00, 12:00] UTC
        assertEquals(0, flights.size(), "Flight departing outside window should be excluded");
    }
    
    @Test
    void testFlightCodeGeneration() {
        FlightTemplate template = new FlightTemplate(
            1, lima, santiago,
            LocalTime.of(8, 0), LocalTime.of(12, 0),
            300, 1500.0
        );
        
        LocalDate date = LocalDate.of(2025, 10, 1);
        
        PlannerFlight flight1 = expander.expandForDate(template, date);
        PlannerFlight flight2 = expander.expandForDate(template, date);
        PlannerFlight flight3 = expander.expandForDate(template, date);
        
        // Flights should have unique codes
        assertNotNull(flight1.getCode());
        assertNotNull(flight2.getCode());
        assertNotNull(flight3.getCode());
        assertNotEquals(flight1.getCode(), flight2.getCode());
    }
    
    @Test
    void testResetIdCounter() {
        FlightTemplate template = new FlightTemplate(
            1, lima, santiago,
            LocalTime.of(8, 0), LocalTime.of(12, 0),
            300, 1500.0
        );
        
        LocalDate date = LocalDate.of(2025, 10, 1);
        
        int idBefore = expander.getNextFlightId();
        expander.expandForDate(template, date);
        int idAfter = expander.getNextFlightId();
        
        assertTrue(idAfter > idBefore, "Flight ID should increment");
        
        expander.resetIdCounter();
        
        assertEquals(1, expander.getNextFlightId(), "ID should reset to 1");
    }
    
    // Helper methods
    
    private PlannerAirport createAirport(String code, String name) {
        int id = code.equals("LIM") ? 1 : 2;
        int gmt = code.equals("LIM") ? -5 : -3; // Lima GMT-5, Santiago GMT-3
        int capacity = 1000;
        
        // Create a dummy country (not important for flight expander tests)
        Country country = null; // Can be null for these tests
        
        return new PlannerAirport(id, code, name, name, country, capacity, gmt);
    }
}

