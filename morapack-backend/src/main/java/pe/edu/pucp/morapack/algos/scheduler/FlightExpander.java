package pe.edu.pucp.morapack.algos.scheduler;

import pe.edu.pucp.morapack.algos.entities.PlannerFlight;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Expands FlightTemplates into specific PlannerFlight instances.
 * 
 * FlightTemplates from CSV only have times (03:11, 10:15), not dates.
 * This class generates actual flights for each day of the simulation.
 * 
 * Example:
 *   Template: SPIM→SCEL, departure 03:11, arrival 10:15
 *   Simulation period: 2025-10-01 to 2025-10-07
 *   
 *   Output: 7 PlannerFlight instances:
 *     - Flight 1: 2025-10-01 03:11 → 2025-10-01 10:15
 *     - Flight 2: 2025-10-02 03:11 → 2025-10-02 10:15
 *     - ...
 *     - Flight 7: 2025-10-07 03:11 → 2025-10-07 10:15
 */
public class FlightExpander {
    
    private int nextFlightId = 1; // Auto-increment for generated flights
    
    /**
     * Expand a single template for a specific date.
     * 
     * @param template The flight template
     * @param date The date to apply
     * @return A PlannerFlight for that specific date
     */
    public PlannerFlight expandForDate(FlightTemplate template, LocalDate date) {
        // Flight times in CSV are in LOCAL time of each airport
        // We need to convert them to UTC for the algorithm
        
        // Convert departure local time to UTC
        LocalDateTime departureLocal = date.atTime(template.getDepartureTime());
        int originGmtOffset = template.getOrigin().getGmt();
        LocalDateTime departureUTC = departureLocal.minusHours(originGmtOffset);
        
        // Convert arrival local time to UTC
        LocalDateTime arrivalLocal = date.atTime(template.getArrivalTime());
        int destGmtOffset = template.getDestination().getGmt();
        LocalDateTime arrivalUTC = arrivalLocal.minusHours(destGmtOffset);
        
        // Handle flights that arrive next day (in local time)
        // After UTC conversion, we might need to adjust if arrival is before departure
        if (template.arrivesNextDay()) {
            arrivalUTC = arrivalUTC.plusDays(1);
        }
        
        // Additional check: if after conversion arrival is still before departure,
        // the flight crosses into the next day
        if (arrivalUTC.isBefore(departureUTC)) {
            arrivalUTC = arrivalUTC.plusDays(1);
        }
        
        return new PlannerFlight(
            nextFlightId++,
            template.getOrigin(),
            template.getDestination(),
            departureUTC,
            arrivalUTC,
            template.getCapacity(),
            template.getCost()
        );
    }
    
    /**
     * Expand a template for a date range (multiple days).
     * 
     * @param template The flight template
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of PlannerFlights, one per day
     */
    public List<PlannerFlight> expandForDateRange(FlightTemplate template, 
                                                  LocalDate startDate, 
                                                  LocalDate endDate) {
        List<PlannerFlight> flights = new ArrayList<>();
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            flights.add(expandForDate(template, current));
            current = current.plusDays(1);
        }
        
        return flights;
    }
    
    /**
     * Expand all templates for a date range.
     * 
     * @param templates List of flight templates
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of all PlannerFlights for all templates across all days
     */
    public List<PlannerFlight> expandAllForDateRange(List<FlightTemplate> templates,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        List<PlannerFlight> allFlights = new ArrayList<>();
        
        for (FlightTemplate template : templates) {
            allFlights.addAll(expandForDateRange(template, startDate, endDate));
        }
        
        return allFlights;
    }
    
    /**
     * Expand templates for a time window (used by DataProvider).
     * Only returns flights whose DEPARTURE is within the window.
     * 
     * @param templates List of flight templates
     * @param startDateTime Start of time window
     * @param endDateTime End of time window
     * @return List of PlannerFlights within the window
     */
    public List<PlannerFlight> expandForTimeWindow(List<FlightTemplate> templates,
                                                   LocalDateTime startDateTime,
                                                   LocalDateTime endDateTime) {
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        
        // Generate flights for the date range
        List<PlannerFlight> allFlights = expandAllForDateRange(templates, startDate, endDate);
        
        // Filter to only include flights departing within the window
        List<PlannerFlight> filtered = new ArrayList<>();
        for (PlannerFlight flight : allFlights) {
            LocalDateTime departure = flight.getDepartureTime();
            if (!departure.isBefore(startDateTime) && !departure.isAfter(endDateTime)) {
                filtered.add(flight);
            }
        }
        
        return filtered;
    }
    
    /**
     * Reset the flight ID counter (useful for testing).
     */
    public void resetIdCounter() {
        nextFlightId = 1;
    }
    
    /**
     * Get the next flight ID that will be assigned.
     */
    public int getNextFlightId() {
        return nextFlightId;
    }
}

