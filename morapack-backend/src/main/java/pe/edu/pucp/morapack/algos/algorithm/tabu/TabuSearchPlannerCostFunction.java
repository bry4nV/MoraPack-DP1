package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.LocalDateTime;

/**
 * Función de costo para Tabu Search basada en PlannerShipments.
 * Calcula penalizaciones por violaciones de restricciones y preferencias.
 */
public class TabuSearchPlannerCostFunction {
    // Penalty constants
    private static final double CAPACITY_VIOLATION_PENALTY = 25000;
    private static final double DELAY_BASE_PENALTY = 10000;
    private static final double DELAY_HOUR_PENALTY = 300;
    private static final double STOPOVER_PENALTY = 600;
    private static final double INVALID_STOPOVER_TIME_PENALTY = 22000;
    private static final double AIRPORT_CAPACITY_VIOLATION_PENALTY = 20000;
    private static final double AIRPORT_CAPACITY_UNIT_PENALTY = 150;
    private static final double INCOMPLETE_ORDER_PENALTY = 50000;  // Orden no completada
    private static final double INVALID_SEQUENCE_PENALTY = 30000;  // Secuencia de vuelos inválida

    /**
     * Calcular costo total de una solución
     */
    public static double calculateCost(TabuSolution solution, List<PlannerFlight> flights, 
                                       List<PlannerAirport> airports, int currentIteration, int maxIterations) {
        double totalCost = 0.0;
        List<PlannerShipment> shipments = solution.getPlannerShipments();

        // 1. Penalización por violación de capacidad de vuelos
        totalCost += calculateFlightCapacityPenalty(shipments, solution);

        // 2. Penalización por retrasos en entregas
        totalCost += calculateDeliveryDelayPenalty(shipments);

        // 3. Penalización por escalas
        totalCost += calculateStopoverPenalty(shipments);

        // 4. Penalización por secuencias inválidas
        totalCost += calculateInvalidSequencePenalty(shipments);

        // 5. Penalización por capacidad de almacenes
        totalCost += calculateAirportCapacityPenalty(shipments, airports);

        // 6. Penalización por órdenes incompletas
        totalCost += calculateIncompleteOrderPenalty(shipments, solution);

        return totalCost;
    }

    /**
     * 1. Violación de capacidad de vuelos
     */
    private static double calculateFlightCapacityPenalty(List<PlannerShipment> shipments, 
                                                          TabuSolution solution) {
        double penalty = 0.0;
        Map<PlannerFlight, Integer> flightLoads = new HashMap<>();

        // Calcular carga de cada vuelo
        for (PlannerShipment shipment : shipments) {
            for (PlannerFlight flight : shipment.getFlights()) {
                flightLoads.merge(flight, shipment.getQuantity(), Integer::sum);
            }
        }

        // Penalizar excesos de capacidad
        for (Map.Entry<PlannerFlight, Integer> entry : flightLoads.entrySet()) {
            PlannerFlight flight = entry.getKey();
            int load = entry.getValue();
            if (load > flight.getCapacity()) {
                int excess = load - flight.getCapacity();
                penalty += CAPACITY_VIOLATION_PENALTY * excess;
            }
        }

        return penalty;
    }

    /**
     * 2. Retrasos en entregas (por Order)
     */
    private static double calculateDeliveryDelayPenalty(List<PlannerShipment> shipments) {
        double penalty = 0.0;
        
        // Agrupar shipments por Order
        Map<PlannerOrder, List<PlannerShipment>> byOrder = new HashMap<>();
        for (PlannerShipment shipment : shipments) {
            byOrder.computeIfAbsent(shipment.getOrder(), k -> new ArrayList<>()).add(shipment);
        }

        // Evaluar cada Order
        for (Map.Entry<PlannerOrder, List<PlannerShipment>> entry : byOrder.entrySet()) {
            PlannerOrder order = entry.getKey();
            List<PlannerShipment> orderShipments = entry.getValue();

            // El plazo se cumple cuando el ÚLTIMO shipment llega
            LocalDateTime latestArrival = orderShipments.stream()
                .map(PlannerShipment::getFinalArrivalTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(order.getOrderTime());

            // Calcular tiempo de entrega
            long deliveryHours = ChronoUnit.HOURS.between(
                order.getOrderTime(), 
                latestArrival
            );

            // All timestamps are in UTC, no timezone conversion needed
            
            // Obtener plazo máximo
            long maxHours = order.getMaxDeliveryHours();

            // Penalizar si excede el plazo
            if (deliveryHours > maxHours) {
                long delayHours = deliveryHours - maxHours;
                penalty += DELAY_BASE_PENALTY + (delayHours * DELAY_HOUR_PENALTY);
            }
        }

        return penalty;
    }

    /**
     * 3. Penalización por escalas
     */
    private static double calculateStopoverPenalty(List<PlannerShipment> shipments) {
        double penalty = 0.0;

        for (PlannerShipment shipment : shipments) {
            int stops = shipment.getNumberOfStops();
            
            // Penalizar cada escala
            penalty += stops * STOPOVER_PENALTY;

            // Validar tiempos de conexión
            if (stops > 0) {
                List<PlannerFlight> flights = shipment.getFlights();
                for (int i = 0; i < flights.size() - 1; i++) {
                    PlannerFlight current = flights.get(i);
                    PlannerFlight next = flights.get(i + 1);

                    long connectionHours = ChronoUnit.HOURS.between(
                        current.getArrivalTime(),
                        next.getDepartureTime()
                    );

                    // Penalizar conexiones demasiado cortas o largas
                    if (connectionHours < 1 || connectionHours > 24) {
                        penalty += INVALID_STOPOVER_TIME_PENALTY;
                    }
                }
            }
        }

        return penalty;
    }

    /**
     * 4. Penalización por secuencias inválidas
     */
    private static double calculateInvalidSequencePenalty(List<PlannerShipment> shipments) {
        double penalty = 0.0;

        for (PlannerShipment shipment : shipments) {
            if (!shipment.isValidSequence()) {
                penalty += INVALID_SEQUENCE_PENALTY;
            }
        }

        return penalty;
    }

    /**
     * 5. Penalización por capacidad de almacenes en aeropuertos
     */
    private static double calculateAirportCapacityPenalty(List<PlannerShipment> shipments, 
                                                           List<PlannerAirport> airports) {
        double penalty = 0.0;
        Map<PlannerAirport, Integer> airportLoads = new HashMap<>();

        // Calcular productos en tránsito por aeropuerto (escalas)
        for (PlannerShipment shipment : shipments) {
            List<PlannerFlight> flights = shipment.getFlights();
            
            // Solo contar aeropuertos intermedios (escalas)
            for (int i = 0; i < flights.size() - 1; i++) {
                PlannerAirport stopover = flights.get(i).getDestination();
                airportLoads.merge(stopover, shipment.getQuantity(), Integer::sum);
            }
        }

        // Penalizar excesos de capacidad
        // ⚠️ NOTA: Esto NO debería ocurrir nunca si los hard constraints funcionan correctamente.
        // Si se detecta una violación aquí, es un BUG en isValidSolution() o en el greedy allocation.
        for (Map.Entry<PlannerAirport, Integer> entry : airportLoads.entrySet()) {
            PlannerAirport airport = entry.getKey();
            int load = entry.getValue();
            int capacity = airport.getStorageCapacity();

            if (load > capacity) {
                int excess = load - capacity;
                
                // ⚠️ LOGGING CRÍTICO: Esto indica un problema serio
                System.err.println(String.format(
                    "⚠️ CRITICAL: Airport %s OVERLOADED! Load=%d, Capacity=%d, Excess=%d",
                    airport.getCode(), load, capacity, excess
                ));
                System.err.println("   This should NOT happen! Hard constraints should prevent this.");
                
                // Penalización EXTREMADAMENTE alta (x1000 del original)
                // para asegurar que estas soluciones nunca sean aceptadas
                penalty += AIRPORT_CAPACITY_VIOLATION_PENALTY * 1000;
                penalty += excess * AIRPORT_CAPACITY_UNIT_PENALTY * 1000;
            }
        }

        return penalty;
    }

    /**
     * 6. Penalización por órdenes incompletas
     */
    private static double calculateIncompleteOrderPenalty(List<PlannerShipment> shipments, 
                                                           TabuSolution solution) {
        double penalty = 0.0;
        
        // Obtener todas las órdenes únicas
        Set<PlannerOrder> allOrders = new HashSet<>();
        for (PlannerShipment shipment : shipments) {
            allOrders.add(shipment.getOrder());
        }

        // Verificar si cada orden está completamente asignada
        for (PlannerOrder order : allOrders) {
            int assigned = solution.getAssignedQuantityForOrder(order);
            int required = order.getTotalQuantity();

            if (assigned < required) {
                int missing = required - assigned;
                penalty += INCOMPLETE_ORDER_PENALTY * missing;
            }
        }

        return penalty;
    }
}

