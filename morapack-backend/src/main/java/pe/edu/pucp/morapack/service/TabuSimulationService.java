package pe.edu.pucp.morapack.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchListener;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSearchPlanner;
import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;
import pe.edu.pucp.morapack.algos.data.DataLoader;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.dto.simulation.TabuSimulationResponse;
import pe.edu.pucp.morapack.dto.simulation.ItinerarioDTO;
import pe.edu.pucp.morapack.dto.simulation.TabuSearchMeta;
import pe.edu.pucp.morapack.utils.TabuSolutionToDtoConverter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TabuSimulationService {
    private final SimpMessagingTemplate messaging;

    private Thread workerThread = null;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    public TabuSimulationService(ObjectProvider<SimpMessagingTemplate> messagingProvider) {
        // Accept SimpMessagingTemplate if present; otherwise allow null and use logging fallback.
        this.messaging = messagingProvider.getIfAvailable();
    }

    public synchronized boolean startSimulation(long seed, long snapshotMs) {
        if (running.get()) return false;
        stopRequested.set(false);
        
        // Load CSV data using DataLoader
        try {
            var airports = DataLoader.loadAirports("data/airports.txt");
            
            // Create airport map for lookups
            Map<String, PlannerAirport> airportMap = new HashMap<>();
            for (PlannerAirport a : airports) {
                airportMap.put(a.getCode(), a);
            }
            
            var flights = DataLoader.loadFlights("data/flights.csv", airportMap, 2025, 12, 31);
            var orders = DataLoader.loadOrders("data/pedidos.csv", airportMap, 2025, 12);
            
            // Log counts for debugging and send an initial snapshot to the client so the UI
            // sees what was loaded (helps explain empty-result cases).
            System.out.println(String.format("[tabu-sim] loaded: airports=%d flights=%d orders=%d",
                    airports.size(), flights.size(), orders.size()));

            // Send an initial 'starting' snapshot so the client knows the sim started and the
            // sizes of the loaded data. This is helpful when the planner exits immediately.
            TabuSimulationResponse init = new TabuSimulationResponse();
            init.meta = new TabuSearchMeta(0, 0.0, true, -1);
            init.aeropuertos = TabuSolutionToDtoConverter.toAirportDtos(airports);
            init.itinerarios = new ItinerarioDTO[0];
            if (messaging != null) {
                messaging.convertAndSend("/topic/tabu-simulation", init);
            } else {
                System.out.println("[tabu-sim] initial snapshot (no STOMP): airports=" + airports.size() + " flights=" + flights.size() + " orders=" + orders.size());
            }

            TabuSearchPlanner planner = new TabuSearchPlanner(seed);

            // Keep the last solution received so we can publish a meaningful final snapshot
            AtomicReference<TabuSolution> lastSolution = new AtomicReference<>(null);

            planner.setListener(new TabuSearchListener() {
                @Override
                public void onSnapshot(TabuSolution solution, int iteration, double bestCost, long snapshotId, Instant snapshotTime) {
                    TabuSimulationResponse resp = new TabuSimulationResponse();
                    resp.meta = new TabuSearchMeta(iteration, bestCost, true, snapshotId);
                    resp.aeropuertos = TabuSolutionToDtoConverter.toAirportDtos(airports);
                    // remember last solution for final snapshot
                    lastSolution.set(solution);
                    resp.itinerarios = TabuSolutionToDtoConverter.toItinerarioDtos(solution, snapshotTime);
                    if (messaging != null) {
                        messaging.convertAndSend("/topic/tabu-simulation", resp);
                    } else {
                        System.out.println("[tabu-sim] snapshot (no STOMP): iter=" + iteration + " bestCost=" + bestCost + " id=" + snapshotId);
                    }
                    // Debug: print how many shipments the planner produced in this snapshot.
                    int shipments = (solution == null || solution.getPlannerShipments() == null) ? 0 : solution.getPlannerShipments().size();
                    int flightsInFirst = 0;
                    if (shipments > 0) {
                        var first = solution.getPlannerShipments().get(0);
                        flightsInFirst = (first.getFlights() == null) ? 0 : first.getFlights().size();
                    }
                    System.out.println(String.format("[tabu-sim] snapshot debug: iter=%d snapshotId=%d bestCost=%.2f shipments=%d flightsInFirst=%d",
                            iteration, snapshotId, bestCost, shipments, flightsInFirst));
                }

                @Override
                public boolean isStopRequested() {
                    return stopRequested.get();
                }
            }, snapshotMs);

            workerThread = new Thread(() -> {
                running.set(true);
                    try {
                    // Run planner with loaded inputs
                    planner.optimize(orders, flights, airports);
                } finally {
                    running.set(false);
                    // Prepare final snapshot. Include airports and the last known itineraries so the
                    // client doesn't receive an empty payload that erases previously-displayed data.
                    TabuSimulationResponse end = new TabuSimulationResponse();
                    end.meta = new TabuSearchMeta(0, 0.0, false, 0);
                    end.aeropuertos = TabuSolutionToDtoConverter.toAirportDtos(airports);
                    TabuSolution last = lastSolution.get();
                    java.time.Instant now = java.time.Instant.now();
                    end.itinerarios = (last != null) ? TabuSolutionToDtoConverter.toItinerarioDtos(last, now) : new ItinerarioDTO[0];
                    if (messaging != null) {
                        messaging.convertAndSend("/topic/tabu-simulation", end);
                    } else {
                        System.out.println("[tabu-sim] ended (no STOMP): airports=" + airports.size() + " lastItinerarios=" + (last != null ? "present" : "none"));
                    }
                }
            }, "tabu-sim-worker");
            workerThread.setDaemon(true);
            workerThread.start();
            return true;
        } catch (Exception e) {
            System.err.println("[tabu-sim] ERROR: Failed to start simulation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void stopSimulation() {
        if (!running.get()) return;
        stopRequested.set(true);
        if (workerThread != null) {
            try { workerThread.join(2000); } catch (InterruptedException ignored) {}
        }
    }

    public boolean isRunning() { return running.get(); }
}

