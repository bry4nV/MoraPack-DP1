package pe.edu.pucp.morapack.algos.utils;

import pe.edu.pucp.morapack.model.Airport;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages storage capacity reservations for airports
 */
public class AirportStorageManager {
    private Map<String, Integer> currentOccupancy = new HashMap<>();
    private Map<String, Integer> reservedCapacity = new HashMap<>();
    
    public AirportStorageManager() {}
    
    /**
     * Check if airport has available capacity for the given quantity
     */
    public boolean hasAvailableCapacity(Airport airport, int quantity) {
        int current = currentOccupancy.getOrDefault(airport.getCode(), 0);
        int reserved = reservedCapacity.getOrDefault(airport.getCode(), 0);
        int totalUsed = current + reserved;
        
        return (totalUsed + quantity) <= airport.getStorageCapacity();
    }
    
    /**
     * Reserve capacity at an airport
     */
    public boolean reserveCapacity(Airport airport, int quantity) {
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
    public void releaseReservedCapacity(Airport airport, int quantity) {
        String code = airport.getCode();
        int current = reservedCapacity.getOrDefault(code, 0);
        reservedCapacity.put(code, Math.max(0, current - quantity));
    }
    
    /**
     * Move products from reserved to current occupancy
     */
    public void confirmOccupancy(Airport airport, int quantity) {
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
    public void removeFromOccupancy(Airport airport, int quantity) {
        String code = airport.getCode();
        int current = currentOccupancy.getOrDefault(code, 0);
        currentOccupancy.put(code, Math.max(0, current - quantity));
    }
    
    /**
     * Get current utilization percentage of an airport
     */
    public double getUtilizationPercentage(Airport airport) {
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
    public int getAvailableCapacity(Airport airport) {
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
    public String getStorageInfo(Airport airport) {
        return String.format("Airport %s: Capacity=%d, Current=%d, Reserved=%d, Available=%d, Utilization=%.1f%%",
                airport.getCode(),
                airport.getStorageCapacity(),
                currentOccupancy.getOrDefault(airport.getCode(), 0),
                reservedCapacity.getOrDefault(airport.getCode(), 0),
                getAvailableCapacity(airport),
                getUtilizationPercentage(airport));
    }
}