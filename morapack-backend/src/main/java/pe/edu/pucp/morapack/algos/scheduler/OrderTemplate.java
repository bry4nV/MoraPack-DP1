package pe.edu.pucp.morapack.algos.scheduler;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;

/**
 * Template for an order with relative time (day, hour, minute).
 * 
 * In the CSV files, orders don't have absolute dates, only:
 * - Day (1-31): Relative day of the month
 * - Hour (0-23): Hour of the day
 * - Minute (0-59): Minute of the hour
 * 
 * This class represents that template, which will be converted
 * to actual PlannerOrder instances with absolute timestamps
 * based on the simulation start date.
 * 
 * Example:
 *   13,04,21,SCEL,600,0001234
 *   Day 13, Hour 04, Minute 21
 *   Destination: SCEL, Quantity: 600
 *   Client ID: 0001234
 *   
 *   If simulation starts on 2025-10-01, this becomes:
 *   Order time: 2025-10-13T04:21:00
 */
public class OrderTemplate {
    
    private final int templateId;
    private final int relativeDay;       // 1-31
    private final int hour;              // 0-23
    private final int minute;            // 0-59
    private final PlannerAirport destination;
    private final int quantity;
    private final String clientId;
    
    public OrderTemplate(int templateId,
                        int relativeDay,
                        int hour,
                        int minute,
                        PlannerAirport destination,
                        int quantity,
                        String clientId) {
        this.templateId = templateId;
        this.relativeDay = relativeDay;
        this.hour = hour;
        this.minute = minute;
        this.destination = destination;
        this.quantity = quantity;
        this.clientId = clientId;
    }
    
    public int getTemplateId() {
        return templateId;
    }
    
    public int getRelativeDay() {
        return relativeDay;
    }
    
    public int getHour() {
        return hour;
    }
    
    public int getMinute() {
        return minute;
    }
    
    public PlannerAirport getDestination() {
        return destination;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    @Override
    public String toString() {
        return String.format("OrderTemplate[%d: day=%d %02d:%02d, dest=%s, qty=%d, client=%s]",
            templateId,
            relativeDay,
            hour,
            minute,
            destination.getCode(),
            quantity,
            clientId);
    }
}

