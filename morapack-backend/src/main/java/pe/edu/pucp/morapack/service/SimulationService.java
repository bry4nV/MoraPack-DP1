package pe.edu.pucp.morapack.service;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class SimulationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Future<?> task;

    private final pe.edu.pucp.morapack.planner.WeeklyPlanner weeklyPlanner;

    @Autowired
    public SimulationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    // use stub by default; the stub will emit demo updates to the WebSocket topic
    this.weeklyPlanner = new pe.edu.pucp.morapack.planner.WeeklyPlannerStub(messagingTemplate);
    }

    public void startSimulation(String payload) {
        messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_LOADING","data","Starting planner (stub)"));

        // Prevent concurrent starts
        if (task != null && !task.isDone()) {
            messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_ERROR","data","Simulation already running"));
            return;
        }

        // Submit planner run to executor so it doesn't block the messaging thread
        task = scheduler.submit(() -> {
            try {
                weeklyPlanner.start(payload);
                messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_STARTED","data","Planner started (stub)"));

                // Wait while planner reports it's running. Stub sets running=true but does not emit updates.
                while (weeklyPlanner.isRunning()) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }

                messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_STOPPED","data","Planner finished (stub)"));
            } catch (Exception e) {
                messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_ERROR","data",e.getMessage()));
            }
        });
    }

    public void stopSimulation() {
        try {
            weeklyPlanner.stop();
            if (task != null) task.cancel(true);
            messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_STOPPED","data","Planner stopped (stub)"));
        } catch (Exception e) {
            messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_ERROR","data",e.getMessage()));
        }
    }

    public void updateFailures(String payload) {
        try {
            weeklyPlanner.updateFailures(payload);
            messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","STATE_UPDATED","data","Failure applied to planner (stub)"));
        } catch (Exception e) {
            messagingTemplate.convertAndSend("/topic/simulation", Map.of("type","SIMULATION_ERROR","data",e.getMessage()));
        }
    }
}
