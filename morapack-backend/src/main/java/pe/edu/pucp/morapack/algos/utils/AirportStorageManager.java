package pe.edu.pucp.morapack.algos.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;

/**
 * Manages storage capacity reservations for airports.
 * Implements HARD CONSTRAINTS to prevent physically impossible overloads.
 * 
 * This class tracks:
 * - Current occupancy (products physically present)
 * - Reserved capacity (products planned to arrive)
 * - Maximum capacities per airport
 */
public class AirportStorageManager {
    private Map<String, Integer> currentOccupancy = new HashMap<>();
    private Map<String, Integer> reservedCapacity = new HashMap<>();
    private Map<String, Integer> maxCapacities = new HashMap<>();
    
    public AirportStorageManager() {}
    
    /**
     * Constructor con inicialización de aeropuertos
     */
    public AirportStorageManager(List<PlannerAirport> airports) {
        for (PlannerAirport airport : airports) {
            String code = airport.getCode();
            currentOccupancy.put(code, 0);
            reservedCapacity.put(code, 0);
            maxCapacities.put(code, airport.getStorageCapacity());
        }
    }
    
    /**
     * Check if airport has available capacity for the given quantity
     */
    public boolean hasAvailableCapacity(PlannerAirport airport, int quantity) {
        int current = currentOccupancy.getOrDefault(airport.getCode(), 0);
        int reserved = reservedCapacity.getOrDefault(airport.getCode(), 0);
        int totalUsed = current + reserved;
        
        return (totalUsed + quantity) <= airport.getStorageCapacity();
    }
    
    /**
     * Reserve capacity at an airport
     */
    public boolean reserveCapacity(PlannerAirport airport, int quantity) {
        if (!hasAvailableCapacity(airport, quantity)) {
            return false;
        }
        
        String code = airport.getCode();
        int current = reservedCapacity.getOrDefault(code, 0);
        reservedCapacity.put(code, current + quantity);
        return true;
    }
    
    /**
     * Release reserved capacity (when products are actually placed or removed)
     */
    public void releaseReservedCapacity(PlannerAirport airport, int quantity) {
        String code = airport.getCode();
        int current = reservedCapacity.getOrDefault(code, 0);
        reservedCapacity.put(code, Math.max(0, current - quantity));
    }
    
    /**
     * Move products from reserved to current occupancy
     */
    public void confirmOccupancy(PlannerAirport airport, int quantity) {
        String code = airport.getCode();
        
        // Release from reserved
        releaseReservedCapacity(airport, quantity);
        
        // Add to current occupancy
        int current = currentOccupancy.getOrDefault(code, 0);
        currentOccupancy.put(code, current + quantity);
    }
    
    /**
     * Remove products from current occupancy (when they are shipped out)
     */
    public void removeFromOccupancy(PlannerAirport airport, int quantity) {
        String code = airport.getCode();
        int current = currentOccupancy.getOrDefault(code, 0);
        currentOccupancy.put(code, Math.max(0, current - quantity));
    }
    
    /**
     * Get current utilization percentage of an airport
     */
    public double getUtilizationPercentage(PlannerAirport airport) {
        String code = airport.getCode();
        int current = currentOccupancy.getOrDefault(code, 0);
        int reserved = reservedCapacity.getOrDefault(code, 0);
        int totalUsed = current + reserved;
        
        if (airport.getStorageCapacity() == 0) return 0.0;
        return (double) totalUsed / airport.getStorageCapacity() * 100.0;
    }
    
    /**
     * Get available capacity at an airport
     */
    public int getAvailableCapacity(PlannerAirport airport) {
        String code = airport.getCode();
        int current = currentOccupancy.getOrDefault(code, 0);
        int reserved = reservedCapacity.getOrDefault(code, 0);
        int totalUsed = current + reserved;
        
        return Math.max(0, airport.getStorageCapacity() - totalUsed);
    }
    
    /**
     * Reset all reservations and occupancy (for algorithm restarts)
     */
    public void reset() {
        currentOccupancy.clear();
        reservedCapacity.clear();
    }
    
    /**
     * Get storage info for debugging
     */
    public String getStorageInfo(PlannerAirport airport) {
        return String.format("Airport %s: Capacity=%d, Current=%d, Reserved=%d, Available=%d, Utilization=%.1f%%",
                airport.getCode(),
                airport.getStorageCapacity(),
                currentOccupancy.getOrDefault(airport.getCode(), 0),
                reservedCapacity.getOrDefault(airport.getCode(), 0),
                getAvailableCapacity(airport),
                getUtilizationPercentage(airport));
    }
    
    /**
     * Añade productos a un aeropuerto (HARD CONSTRAINT).
     * Lanza excepción si excede capacidad.
     * 
     * @throws IllegalStateException si excede capacidad
     */
    public void add(PlannerAirport airport, int quantity) {
        if (!hasAvailableCapacity(airport, quantity)) {
            throw new IllegalStateException(
                String.format("Cannot add %d products to %s: exceeds capacity (%d/%d used)",
                    quantity, airport.getCode(),
                    getCurrentLoad(airport), airport.getStorageCapacity())
            );
        }
        
        String code = airport.getCode();
        // Añadir a ocupación actual
        int current = currentOccupancy.getOrDefault(code, 0);
        currentOccupancy.put(code, current + quantity);
    }
    
    /**
     * Añade productos usando solo el código del aeropuerto
     */
    public void add(String airportCode, int quantity) {
        int current = currentOccupancy.getOrDefault(airportCode, 0);
        int max = maxCapacities.getOrDefault(airportCode, Integer.MAX_VALUE);
        
        if (current + quantity > max) {
            throw new IllegalStateException(
                String.format("Cannot add %d products to %s: exceeds capacity (%d/%d used)",
                    quantity, airportCode, current, max)
            );
        }
        
        currentOccupancy.put(airportCode, current + quantity);
    }
    
    /**
     * Remueve productos de un aeropuerto (cuando despegan o se consumen).
     * 
     * @throws IllegalStateException si se intenta remover más de lo que hay
     */
    public void remove(String airportCode, int quantity) {
        int current = currentOccupancy.getOrDefault(airportCode, 0);
        
        if (quantity > current) {
            throw new IllegalStateException(
                String.format("Cannot remove %d products from %s: only %d present",
                    quantity, airportCode, current)
            );
        }
        
        currentOccupancy.put(airportCode, current - quantity);
    }
    
    /**
     * Obtiene la carga actual total (ocupación + reservas)
     */
    public int getCurrentLoad(PlannerAirport airport) {
        String code = airport.getCode();
        int current = currentOccupancy.getOrDefault(code, 0);
        int reserved = reservedCapacity.getOrDefault(code, 0);
        return current + reserved;
    }
    
    /**
     * Clona el manager (útil para evaluar movimientos sin commitear)
     */
    public AirportStorageManager clone() {
        AirportStorageManager copy = new AirportStorageManager();
        copy.currentOccupancy.putAll(this.currentOccupancy);
        copy.reservedCapacity.putAll(this.reservedCapacity);
        copy.maxCapacities.putAll(this.maxCapacities);
        return copy;
    }
    
    /**
     * Obtiene snapshot del estado actual (para debugging y logging)
     */
    public Map<String, String> getSnapshot() {
        Map<String, String> snapshot = new HashMap<>();
        for (String code : maxCapacities.keySet()) {
            int current = currentOccupancy.getOrDefault(code, 0);
            int reserved = reservedCapacity.getOrDefault(code, 0);
            int max = maxCapacities.get(code);
            snapshot.put(code, String.format("%d+%d/%d", current, reserved, max));
        }
        return snapshot;
    }
}