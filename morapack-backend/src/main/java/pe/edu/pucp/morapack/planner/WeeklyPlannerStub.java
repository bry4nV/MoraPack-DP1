package pe.edu.pucp.morapack.planner;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * WeeklyPlannerStub that emits periodic demo updates over WebSocket by using
 * an injected SimpMessagingTemplate. This follows option (b): the planner
 * sends messages directly to the broker.
 */
public class WeeklyPlannerStub implements WeeklyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyPlannerStub.class);
    private volatile boolean running = false;
    private final SimpMessagingTemplate messagingTemplate;
    private Thread workerThread;

    public WeeklyPlannerStub(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void start(String payload) {
        logger.info("WeeklyPlannerStub.start called with payload: {}", payload);
        if (running) {
            logger.warn("WeeklyPlannerStub.start called while already running");
            return;
        }
        running = true;

        // Simple worker thread that emits a demo SIMULATION_UPDATE every 500ms
        workerThread = new Thread(() -> {
            int minute = 0;
            while (running) {
                try {
                    Map<String, Object> data = Map.of("minute", minute, "payload", payload);
                    Map<String, Object> message = Map.of("type", "SIMULATION_UPDATE", "data", data);
                    messagingTemplate.convertAndSend("/topic/simulation", message);
                    minute++;
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in WeeklyPlannerStub worker", e);
                }
            }
            running = false;
            logger.info("WeeklyPlannerStub worker exiting");
        }, "weekly-planner-stub-thread");

        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public void stop() {
        logger.info("WeeklyPlannerStub.stop called");
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void updateFailures(String payload) {
        logger.info("WeeklyPlannerStub.updateFailures called with: {}", payload);
        // For the stub, just emit an immediate STATE_UPDATED message
        try {
            Map<String, Object> message = Map.of("type", "STATE_UPDATED", "data", Map.of("info", "failures applied", "payload", payload));
            messagingTemplate.convertAndSend("/topic/simulation", message);
        } catch (Exception e) {
            logger.error("Failed to emit STATE_UPDATED from stub", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
