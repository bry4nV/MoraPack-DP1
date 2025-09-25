package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.entities.PlannerRoute;
import pe.edu.pucp.morapack.algos.entities.PlannerSegment;
import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Order;
import pe.edu.pucp.morapack.model.Shipment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;

public class TabuSearchConstraints {
    private final TabuSearchConfig config;

    public TabuSearchConstraints(TabuSearchConfig config) {
        this.config = config;
    }

    public double evaluateConstraints(Solution solution) {
        double penaltySum = 0;
        
        // Evaluar restricciones de capacidad
        penaltySum += evaluateCapacityConstraints(solution);
        
        // Evaluar rutas vacías
        penaltySum += evaluateEmptyRoutes(solution);
        
        // Evaluar retrasos en entregas
        penaltySum += evaluateDeliveryDelays(solution);
        
        // Evaluar restricciones de escalas
        penaltySum += evaluateStopoverConstraints(solution);
        
        // Evaluar penalizaciones por replanificación
        penaltySum += evaluateReplanificationPenalties(solution);
        
        return penaltySum;
    }

    private double evaluateCapacityConstraints(Solution solution) {
        double penalty = 0;
        Map<Flight, Integer> loadPerFlight = new HashMap<>();
        
        // Calcular la carga por vuelo
        for (Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute route = entry.getValue();
            
            for (PlannerSegment segment : route.getSegments()) {
                Flight flight = segment.getFlight();
                loadPerFlight.merge(flight, shipment.getQuantity(), Integer::sum);
            }
        }
        
        // Evaluar violaciones de capacidad
        for (Entry<Flight, Integer> entry : loadPerFlight.entrySet()) {
            if (entry.getValue() > entry.getKey().getCapacity()) {
                penalty += config.getCapacityViolationPenalty() * 
                          (entry.getValue() - entry.getKey().getCapacity());
            }
        }
        
        return penalty;
    }

    private double evaluateEmptyRoutes(Solution solution) {
        double penalty = 0;
        
        for (Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            if (entry.getValue().getSegments().isEmpty()) {
                penalty += config.getEmptyRoutePenalty();
            }
        }
        
        return penalty;
    }

    private double evaluateDeliveryDelays(Solution solution) {
        double penalty = 0;
        
        for (Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            Shipment shipment = entry.getKey();
            PlannerRoute route = entry.getValue();
            
            if (route.getSegments().isEmpty()) {
                continue;
            }
            
            LocalDateTime orderTime = shipment.getParentOrder().getOrderTime();
            LocalDateTime deliveryTime = route.getFinalArrivalTime();
            
            // Considerar tiempo de procesamiento (2 horas) y zona horaria del destino
            deliveryTime = deliveryTime.plusHours(2);
            
            long totalHours = ChronoUnit.HOURS.between(orderTime, deliveryTime);
            
            // Ajustar por zona horaria del destino
            totalHours += shipment.getDestination().getGmt();
            
            // Verificar si excede el tiempo máximo de entrega
            long maxHours = shipment.isInterContinental() ? 72 : 48;
            if (totalHours > maxHours) {
                long delayHours = totalHours - maxHours;
                penalty += config.getDelayBasePenalty() + 
                          (delayHours * config.getDelayHourPenalty());
            }
        }
        
        return penalty;
    }

    private double evaluateStopoverConstraints(Solution solution) {
        double penalty = 0;
        
        for (Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            PlannerRoute route = entry.getValue();
            List<PlannerSegment> segments = route.getSegments();
            
            // Penalizar por número de escalas
            if (segments.size() > 1) {
                penalty += (segments.size() - 1) * config.getStopoverPenalty();
            }
            
            // Verificar tiempos entre escalas
            for (int i = 0; i < segments.size() - 1; i++) {
                Flight current = segments.get(i).getFlight();
                Flight next = segments.get(i + 1).getFlight();
                
                if (!isValidStopoverTime(current.getArrivalTime(), next.getDepartureTime())) {
                    penalty += config.getInvalidStopoverTimePenalty();
                }
            }
        }
        
        return penalty;
    }

    private boolean isValidStopoverTime(LocalDateTime arrival, LocalDateTime departure) {
        // Debe haber al menos 1 hora entre vuelos y no más de 24 horas
        long stopoverHours = ChronoUnit.HOURS.between(arrival, departure);
        return stopoverHours >= 1 && stopoverHours <= 24;
    }

    public boolean isSolutionFeasible(Solution solution) {
        // Una solución es factible si no tiene penalizaciones críticas
        double penalties = evaluateConstraints(solution);
        return penalties < (config.getCapacityViolationPenalty() + 
                          config.getEmptyRoutePenalty() + 
                          config.getDelayBasePenalty());
    }
    
    public boolean isPreplannedRouteRespected(Solution solution) {
        // Asegurar que las rutas preplaneadas se mantienen
        for (Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            PlannerRoute route = entry.getValue();
            
            for (PlannerSegment segment : route.getSegments()) {
                Flight flight = segment.getFlight();
                if (flight != null && !route.getSegments().isEmpty()) {
                    // Si es un vuelo preplaneado, verificar que se mantiene la ruta
                    if (flight.isPreplanned()) {
                        PlannerRoute originalRoute = entry.getValue();
                        if (!originalRoute.getSegments().contains(segment)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private double evaluateReplanificationPenalties(Solution solution) {
        double penalty = 0;
        
        for (Entry<Shipment, PlannerRoute> entry : solution.getRouteMap().entrySet()) {
            PlannerRoute route = entry.getValue();
            
            // Penalizar vuelos cancelados en la ruta
            penalty += route.getSegments().stream()
                .filter(s -> s.getFlight().getStatus() == Flight.Status.CANCELLED)
                .count() * config.getCancellationPenalty();
            
            // Penalizar rutas que han sido replanificadas
            // (Múltiples segmentos después de un vuelo cancelado)
            long replanifiedSegments = route.getSegments().stream()
                .filter(s -> s.getFlight().getStatus() != Flight.Status.CANCELLED)
                .count();
                
            if (replanifiedSegments > 0) {
                penalty += replanifiedSegments * config.getReplanificationPenalty();
            }
        }
        
        return penalty;
    }
}