package pe.edu.pucp.morapack.algos.main;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.algos.data.DataLoader;
import pe.edu.pucp.morapack.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class MorapackPlanner {
    private static final String DEFAULT_AIRPORTS_FILE = "data/airports.txt";
    private static final String DEFAULT_FLIGHTS_FILE  = "data/flights.csv";
    private static final String DEFAULT_ORDERS_FILE   = "data/pedidos.csv";

    public static void main(String[] args) {
        String airportsFile = args.length > 0 ? args[0] : DEFAULT_AIRPORTS_FILE;
        String flightsFile  = args.length > 1 ? args[1] : DEFAULT_FLIGHTS_FILE;
        String ordersFile   = args.length > 2 ? args[2] : DEFAULT_ORDERS_FILE;

        System.out.println("--- Iniciando Prueba del Planificador MoraPack ---");
        System.out.println("Usando archivos:");
        System.out.println("- Aeropuertos: " + airportsFile);
        System.out.println("- Vuelos: " + flightsFile);
        System.out.println("- Órdenes: " + ordersFile);

        try {
            // Load airports
            List<PlannerAirport> airports = DataLoader.loadAirports(airportsFile);
            System.out.println("Loaded " + airports.size() + " airports");

            // Create map for quick airport lookup
            Map<String, PlannerAirport> airportMap = airports.stream()
                    .collect(Collectors.toMap(PlannerAirport::getCode, a -> a));

            // Load flights (generar para Diciembre 2025, 31 días)
            List<PlannerFlight> availableFlights = DataLoader.loadFlights(flightsFile, airportMap, 2025, 12, 31);
            System.out.println("Loaded " + availableFlights.size() + " flights");

            // Load orders (usando Diciembre 2025 como referencia)
            List<PlannerOrder> pendingOrders = DataLoader.loadOrders(ordersFile, airportMap, 2025, 12);
            System.out.println("Loaded " + pendingOrders.size() + " orders");

            System.out.println("\n[INITIAL INPUT DATA]");
            pendingOrders.forEach(p ->
                    System.out.println("  - Order #" + p.getId() + ": " + p.getTotalQuantity() +
                            " products to " + p.getDestination().getCity() +
                            " (Deadline: " + p.getMaxDeliveryHours() + "h)")
                            
            );

           
            System.out.println("\n=== TESTING: ProductAssignment-first Implementation ===");
            System.out.println("Running TabuSearch algorithm ONCE to test:");
            Solution solution;
            long t0 = System.nanoTime();
            TabuSearchPlanner plannerOnce = new TabuSearchPlanner();
            solution = plannerOnce.optimize(pendingOrders, availableFlights, airports);
            long t1 = System.nanoTime();
            double executionTimeMs = (t1 - t0) / 1_000_000.0;
            double avgDeliveryTimeMinutes = plannerOnce.getAverageDeliveryTimeMinutes();
            System.out.printf("Execution completed - Time: %.3f ms, Avg Delivery: %.2f min%n",
                    executionTimeMs, avgDeliveryTimeMinutes);

            System.out.println("\n=== FINAL STATISTICS ===");
            printDetailedStatistics(solution, pendingOrders);

            
            LocalDate start = availableFlights.stream()
                    .map(f -> f.getDepartureTime().toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());

            // 5% de cancelación por vuelo y día, semilla fija para reproducibilidad
            simulateWeek(airports, availableFlights, pendingOrders, 0.05, start, 42L);

        } catch (IOException e) {
            System.err.println("Error loading data files: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("\n--- Test Finished ---");
    }

    private static void printDetailedStatistics(Solution solution, List<PlannerOrder> originalOrders) {
        if (solution == null) {
            System.out.println("No solution to analyze.");
            return;
        }
        if (!(solution instanceof pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution)) {
            System.out.println("Solution is not a TabuSolution.");
            return;
        }

        pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution tabuSolution =
                (pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution) solution;

        int totalShipments = tabuSolution.getPlannerShipments().size();

        int assignedProducts = tabuSolution.getPlannerShipments().stream()
                .mapToInt(s -> s.getQuantity())
                .sum();

        int totalProducts = originalOrders.stream()
                .mapToInt(PlannerOrder::getTotalQuantity)
                .sum();

        int completedOrders = 0;
        int totalOrders = originalOrders.size();

        for (PlannerOrder order : originalOrders) {
            List<pe.edu.pucp.morapack.algos.entities.PlannerShipment> orderShipments =
                    tabuSolution.getPlannerShipments().stream()
                            .filter(s -> s.getOrder().getId() == order.getId())
                            .collect(Collectors.toList());

            int assignedQty = orderShipments.stream().mapToInt(s -> s.getQuantity()).sum();
            boolean fullQuantity = (assignedQty == order.getTotalQuantity());
            boolean allOnTime = orderShipments.stream().allMatch(s -> s.meetsDeadline());

            if (fullQuantity && allOnTime) completedOrders++;
        }

        long assignedRoutes = tabuSolution.getPlannerShipments().stream()
                .filter(s -> s.getFlights() != null && !s.getFlights().isEmpty())
                .count();

        long emptyRoutes = tabuSolution.getPlannerShipments().stream()
                .filter(s -> s.getFlights() == null || s.getFlights().isEmpty())
                .count();

        System.out.println("Total Orders: " + totalOrders);
        System.out.println("Completed Orders: " + completedOrders + " (" +
                String.format("%.1f%%", (double) completedOrders / totalOrders * 100) + ")");
        System.out.println("Incomplete Orders: " + (totalOrders - completedOrders));

        System.out.println("\nProduct Assignment:");
        System.out.println("Total Products: " + totalProducts);
        System.out.println("Assigned Products: " + assignedProducts + " (" +
                String.format("%.1f%%", (double) assignedProducts / totalProducts * 100) + ")");
        System.out.println("Unassigned Products: " + (totalProducts - assignedProducts));

        System.out.println("\nRoute Statistics:");
        System.out.println("Assigned Routes: " + assignedRoutes);
        System.out.println("Empty Routes: " + emptyRoutes);
        System.out.println("Total Shipments: " + totalShipments);

        if (assignedProducts > 0 && totalShipments > 0) {
            System.out.printf("Average Products per Shipment: %.1f%n",
                    (double) assignedProducts / totalShipments);
        }
    }

    private static void printSolution(Solution solution) {
        if (solution != null && solution instanceof pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution) {
            pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution tabuSolution =
                    (pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution) solution;

            if (!tabuSolution.getPlannerShipments().isEmpty()) {
                System.out.println("Solution found!");
                System.out.println("Total PlannerShipments: " + tabuSolution.getPlannerShipments().size());

                tabuSolution.getPlannerShipments().forEach(shipment -> {
                    System.out.println("\n  > PlannerShipment #" + shipment.getId() +
                            " (from Order #" + shipment.getOrder().getId() +
                            " with " + shipment.getQuantity() + " products)");
                    if (shipment.getFlights() == null || shipment.getFlights().isEmpty()) {
                        System.out.println("    Route: Could not assign a route!");
                    } else {
                        String routeStr = shipment.getFlights().stream()
                                .map(f -> f.getCode() + " (" +
                                        f.getOrigin().getCity() + " -> " +
                                        f.getDestination().getCity() + ")")
                                .collect(Collectors.joining(" | "));
                        System.out.println("    Route: " + routeStr);
                        System.out.println("    Type: " + (shipment.isDirect() ? "DIRECT" :
                                "WITH CONNECTIONS (" + shipment.getNumberOfStops() + " stops)"));
                        System.out.println("    Estimated arrival: " + shipment.getFinalArrivalTime());
                        System.out.println("    On time: " + (shipment.meetsDeadline() ? "YES" : "NO"));
                    }
                });
            } else {
                System.out.println("Could not find a solution.");
            }
        } else {
            System.out.println("Could not find a solution.");
        }
    }

    // ===================== Simulación semanal  =====================

    /** DTO interno: backlog sin mutar Order. */
    private static class PendingOrder {
        private final int id;
        private final PlannerAirport origin;
        private final PlannerAirport destination;
        private final long maxDeliveryHours;
        private final LocalDateTime orderTime;
        private int remainingQuantity;

        PendingOrder(PlannerOrder o) {
            this.id = o.getId();
            this.origin = o.getOrigin();
            this.destination = o.getDestination();
            this.maxDeliveryHours = o.getMaxDeliveryHours();
            this.orderTime = o.getOrderTime();
            this.remainingQuantity = o.getTotalQuantity();
        }

        int getId() { return id; }
        PlannerAirport getOrigin() { return origin; }
        PlannerAirport getDestination() { return destination; }
        long getMaxDeliveryHours() { return maxDeliveryHours; }
        LocalDateTime getOrderTime() { return orderTime; }
        int getRemainingQuantity() { return remainingQuantity; }
        void subtractDelivered(int qty) { remainingQuantity = Math.max(0, remainingQuantity - Math.max(0, qty)); }
        boolean isComplete() { return remainingQuantity == 0; }
    }

    /**
     * Simula 7 días: por día filtra vuelos, cancela con probabilidad,
     * crea órdenes efímeras con cantidad pendiente, ejecuta Tabu y descuenta lo entregado.
     */
    public static void simulateWeek(
            List<PlannerAirport> airports,
            List<PlannerFlight> allFlights,
            List<PlannerOrder> allOrders,
            double dailyCancelProb,
            LocalDate startDate,
            long seed) {

        Random rnd = new Random(seed);
        List<PendingOrder> backlog = allOrders.stream()
                .map(PendingOrder::new)
                .collect(Collectors.toCollection(ArrayList::new));

        System.out.println("\n=== SIMULACIÓN SEMANAL (7 días) ===");
        double weeklyDelivered = 0.0;
        double weeklyAvgMinutesSum = 0.0;
        int daysWithDeliveries = 0;

        for (int d = 0; d < 7; d++) {
            LocalDate day = startDate.plusDays(d);
            System.out.println("\n--- DÍA " + (d + 1) + " (" + day + ") ---");

            List<PlannerFlight> todaysFlights = flightsForDay(allFlights, day);
            if (todaysFlights.isEmpty()) {
                System.out.println("No hay vuelos hoy. Se mantiene backlog.");
                continue;
            }
            cancelSomeFlights(todaysFlights, dailyCancelProb, rnd);

            List<PlannerOrder> dailyOrders = buildDailyOrdersFromPending(backlog);
            if (dailyOrders.isEmpty()) {
                System.out.println("No hay pedidos pendientes. Día sin optimización.");
                continue;
            }

            TabuSearchPlanner planner = new TabuSearchPlanner();
            Solution dailySolution = planner.optimize(dailyOrders, todaysFlights, airports);

            double avgMin = 0.0;
            try { avgMin = planner.getAverageDeliveryTimeMinutes(); } catch (Throwable ignored) {}

            Map<Integer, Integer> deliveredPerOrderId = computeDeliveredPerOrderId(dailySolution);
            int deliveredQtyToday = deliveredPerOrderId.values().stream().mapToInt(Integer::intValue).sum();
            weeklyDelivered += deliveredQtyToday;
            if (deliveredQtyToday > 0) {
                weeklyAvgMinutesSum += avgMin;
                daysWithDeliveries++;
            }
            System.out.printf(Locale.US,
                    "Entregado hoy: %d unidades | Tiempo promedio entrega: %.2f min%n",
                    deliveredQtyToday, avgMin);

            updateBacklog(backlog, deliveredPerOrderId);
        }

        System.out.println("\n=== RESUMEN SEMANAL ===");
        System.out.printf(Locale.US, "Total entregado: %.0f unidades%n", weeklyDelivered);
        if (daysWithDeliveries > 0) {
            System.out.printf(Locale.US,
                    "Promedio diario (días con entrega): %.2f min%n",
                    weeklyAvgMinutesSum / daysWithDeliveries);
        }
        int remaining = backlog.stream().mapToInt(PendingOrder::getRemainingQuantity).sum();
        System.out.println("Unidades pendientes al finalizar la semana: " + remaining);
    }

    // --------------------------- Helpers ---------------------------

    private static boolean isSameDay(LocalDateTime dt, LocalDate date) {
        return dt != null && dt.toLocalDate().isEqual(date);
    }

    private static List<PlannerFlight> flightsForDay(List<PlannerFlight> all, LocalDate day) {
        return all.stream()
                .filter(f -> isSameDay(f.getDepartureTime(), day))
                .collect(Collectors.toList());
    }

    private static void cancelSomeFlights(List<PlannerFlight> flights, double pCancel, Random rnd) {
        for (PlannerFlight f : flights) {
            if (rnd.nextDouble() < pCancel) f.setStatus(PlannerFlight.Status.CANCELLED);
            else if (f.getStatus() == PlannerFlight.Status.CANCELLED) f.setStatus(PlannerFlight.Status.SCHEDULED);
        }
    }

    /** Construye órdenes efímeras del día (solo la cantidad pendiente). */
    private static List<PlannerOrder> buildDailyOrdersFromPending(List<PendingOrder> backlog) {
        List<PlannerOrder> daily = new ArrayList<>();
        for (PendingOrder p : backlog) {
            if (p.getRemainingQuantity() <= 0) continue;
            PlannerOrder o = new PlannerOrder(p.getId(), p.getRemainingQuantity(), p.getOrigin(), p.getDestination());
            o.setOrderTime(p.getOrderTime());
            daily.add(o);
        }
        return daily;
    }

    /** Lee del Solution lo entregado por id de Order. */
    private static Map<Integer, Integer> computeDeliveredPerOrderId(Solution solution) {
        Map<Integer, Integer> delivered = new HashMap<>();
        if (!(solution instanceof pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution)) return delivered;

        pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution ts =
                (pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution) solution;

        ts.getPlannerShipments().forEach(s -> {
            if (s.getFlights() != null && !s.getFlights().isEmpty()) {
                int orderId = s.getOrder().getId();
                int qty = s.getQuantity();
                delivered.merge(orderId, qty, Integer::sum);
            }
        });
        return delivered;
    }

    /** Descuenta entregas del backlog. */
    private static void updateBacklog(List<PendingOrder> backlog, Map<Integer, Integer> deliveredPerOrderId) {
        for (PendingOrder p : backlog) {
            int delivered = deliveredPerOrderId.getOrDefault(p.getId(), 0);
            if (delivered > 0) p.subtractDelivered(delivered);
        }
        backlog.removeIf(PendingOrder::isComplete);
    }
}
