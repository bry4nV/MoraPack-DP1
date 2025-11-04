package pe.edu.pucp.morapack.algos.scheduler;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;

import java.time.LocalTime;

/**
 * Template for a recurring flight (without a specific date).
 * 
 * In the CSV files, flights don't have specific dates, only times.
 * This class represents that template, which will be expanded
 * into actual PlannerFlight instances for each day of the simulation.
 * 
 * Example:
 *   SPIM,SCEL,03:11,10:15,340
 *   Origin: SPIM, Destination: SCEL
 *   Departure time: 03:11, Arrival time: 10:15
 *   Capacity: 340
 *   
 *   This flight occurs EVERY DAY at the same time.
 */
public class FlightTemplate {
    
    private final int templateId;
    private final PlannerAirport origin;
    private final PlannerAirport destination;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final int capacity;
    private final double cost;
    
    /**
     * Whether arrival is next day (if arrivalTime < departureTime)
     */
    private final boolean arrivesNextDay;
    
    public FlightTemplate(int templateId, 
                         PlannerAirport origin, 
                         PlannerAirport destination,
                         LocalTime departureTime, 
                         LocalTime arrivalTime, 
                         int capacity,
                         double cost) {
        this.templateId = templateId;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.capacity = capacity;
        this.cost = cost;
        
        // Detect if flight arrives next day
        this.arrivesNextDay = arrivalTime.isBefore(departureTime);
    }
    
    public int getTemplateId() {
        return templateId;
    }
    
    public PlannerAirport getOrigin() {
        return origin;
    }
    
    public PlannerAirport getDestination() {
        return destination;
    }
    
    public LocalTime getDepartureTime() {
        return departureTime;
    }
    
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public double getCost() {
        return cost;
    }
    
    public boolean arrivesNextDay() {
        return arrivesNextDay;
    }
    
    @Override
    public String toString() {
        return String.format("FlightTemplate[%d: %s→%s %s-%s, cap=%d%s]",
            templateId,
            origin.getCode(),
            destination.getCode(),
            departureTime,
            arrivalTime,
            capacity,
            arrivesNextDay ? " +1d" : "");
    }
}

