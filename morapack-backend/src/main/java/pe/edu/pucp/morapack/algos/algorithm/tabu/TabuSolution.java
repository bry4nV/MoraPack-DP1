package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import pe.edu.pucp.morapack.model.Airport;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class TabuSolution extends Solution {
    private final Map<String, Integer> continentStats;
    private final Map<String, List<Duration>> deliveryTimes;
    private double cost;

    @Override
    public List<PlannerRoute> getRoutes() {
        return super.getRoutes();
    }

    @Override
    public List<Order> getCompletedOrders() {
        return super.getCompletedOrders();
    }

    @Override
    public List<Order> getAllOrders() {
        return super.getAllOrders();
    }

    @Override
    public void setAllOrders(List<Order> orders) {
        super.setAllOrders(orders);
        updateStats();
    }
    
    public List<PlannerRoute> getAssignedRoutes() {
        return getRoutes().stream()
            .filter(r -> !r.getSegments().isEmpty())
            .collect(Collectors.toList());
    }
    
    public List<PlannerRoute> getEmptyRoutes() {
        return getRoutes().stream()
            .filter(r -> r.getSegments().isEmpty())
            .collect(Collectors.toList());
    }
    
    public PlannerRoute findRouteForShipment(Shipment shipment) {
        return getRoutes().stream()
            .filter(r -> r.getShipments().contains(shipment))
            .findFirst()
            .orElse(null);
    }
    
    public boolean isShipmentAssigned(Shipment shipment) {
        return getRoutes().stream()
            .anyMatch(r -> r.getShipments().contains(shipment) && !r.getSegments().isEmpty());
    }
    
    public void addRoute(PlannerRoute route) {
        getRoutes().add(route);
        updateStats();
    }
    
    public void removeRoute(PlannerRoute route) {
        getRoutes().remove(route);
        updateStats();
    }
    public TabuSolution() {
        super();
        this.continentStats = new HashMap<>();
        this.deliveryTimes = new HashMap<>();
        this.cost = Double.MAX_VALUE;
    }

    public TabuSolution(Solution solution) {
        super(solution); // Usar el constructor de copia de Solution
        this.continentStats = new HashMap<>();
        this.deliveryTimes = new HashMap<>();
        this.cost = Double.MAX_VALUE;
        updateStats();
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getCost() {
        return cost;
    }

    public void updateStats() {
        continentStats.clear();
        deliveryTimes.clear();

        // Procesar órdenes completadas
        for (Order order : getCompletedOrders()) {
            String continent = getContinent(order.getDestination());
            continentStats.merge(continent, 1, Integer::sum);
            
            deliveryTimes.computeIfAbsent(continent, k -> new ArrayList<>())
                        .add(order.getTotalDeliveryTime());
        }
    }

    public Map<String, Double> getDeliverySuccessRateByContinent() {
        Map<String, Double> rates = new HashMap<>();
        for (Map.Entry<String, Integer> entry : continentStats.entrySet()) {
            String continent = entry.getKey();
            int completed = entry.getValue();
            int total = (int) getAllOrders().stream()
                .filter(o -> getContinent(o.getDestination()).equals(continent))
                .count();
            rates.put(continent, total > 0 ? (double) completed / total : 0.0);
        }
        return rates;
    }

    public Map<String, Duration> getAverageDeliveryTimeByContinent() {
        Map<String, Duration> avgTimes = new HashMap<>();
        for (Map.Entry<String, List<Duration>> entry : deliveryTimes.entrySet()) {
            String continent = entry.getKey();
            List<Duration> times = entry.getValue();
            if (!times.isEmpty()) {
                long avgMinutes = (long) times.stream()
                    .mapToLong(Duration::toMinutes)
                    .average()
                    .orElse(0.0);
                avgTimes.put(continent, Duration.ofMinutes(avgMinutes));
            }
        }
        return avgTimes;
    }

    public Map<TimeRange, Integer> getDeliveryTimeDistribution() {
        Map<TimeRange, Integer> distribution = new EnumMap<>(TimeRange.class);
        for (TimeRange range : TimeRange.values()) {
            distribution.put(range, 0);
        }

        for (Order order : getCompletedOrders()) {
            Duration time = order.getTotalDeliveryTime();
            TimeRange range = TimeRange.getRange(time);
            distribution.merge(range, 1, Integer::sum);
        }

        return distribution;
    }

    public double getAverageFlightUtilization() {
        return getRoutes().stream()
            .mapToDouble(route -> {
                double capacity = route.getFlights().get(0).getCapacity();
                double used = route.getShipments().stream()
                    .mapToInt(Shipment::getQuantity)
                    .sum();
                return used / capacity;
            })
            .average()
            .orElse(0.0);
    }

    public Map<String, Double> getRouteTypeDistribution() {
        long directRoutes = getRoutes().stream()
            .filter(r -> r.getFlights().size() == 1)
            .count();
        
        long totalRoutes = getRoutes().size();
        Map<String, Double> distribution = new HashMap<>();
        
        distribution.put("direct", totalRoutes > 0 ? (double) directRoutes / totalRoutes : 0);
        distribution.put("connecting", totalRoutes > 0 ? (double) (totalRoutes - directRoutes) / totalRoutes : 0);
        
        return distribution;
    }

    private String getContinent(Airport airport) {
        String code = airport.getCode();
        if (code.startsWith("S")) return "South America";
        if (code.startsWith("E") || code.startsWith("L")) return "Europe";
        if (code.startsWith("O") || code.startsWith("V") || code.startsWith("U")) return "Asia";
        return "Other";
    }

    public enum TimeRange {
        UNDER_24H(Duration.ofHours(0), Duration.ofHours(24)),
        HOURS_24_48(Duration.ofHours(24), Duration.ofHours(48)),
        HOURS_48_72(Duration.ofHours(48), Duration.ofHours(72)),
        OVER_72H(Duration.ofHours(72), Duration.ofDays(365));

        private final Duration min;
        private final Duration max;

        TimeRange(Duration min, Duration max) {
            this.min = min;
            this.max = max;
        }
        
        public Duration getMin() {
            return min;
        }
        
        public Duration getMax() {
            return max;
        }

        public static TimeRange getRange(Duration duration) {
            long hours = duration.toHours();
            if (hours < 24) return UNDER_24H;
            if (hours < 48) return HOURS_24_48;
            if (hours < 72) return HOURS_48_72;
            return OVER_72H;
        }
    }
}