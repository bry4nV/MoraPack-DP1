package pe.edu.pucp.morapack.algos.entities;

import java.util.HashMap;
import java.util.Map;

import pe.edu.pucp.morapack.model.Shipment;

public class Solution {
    private Map<Shipment, PlannerRoute> routeMap;
    
    public Solution() {
        this.routeMap = new HashMap<>();
    }
    
    public Solution(Solution other) {
        this.routeMap = new HashMap<>();
        other.routeMap.forEach((shipment, route) -> 
            this.routeMap.put(shipment, new PlannerRoute(route)));
    }
    
    public Solution(PlannerRoute route) {
        this.routeMap = new HashMap<>();
        // This constructor is used for temporary validation solutions
        // No need to map to a specific shipment as it's just for validation
    }
    
    public Map<Shipment, PlannerRoute> getRouteMap() {
        return routeMap;
    }
}