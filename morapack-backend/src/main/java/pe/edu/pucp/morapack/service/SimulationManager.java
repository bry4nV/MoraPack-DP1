package pe.edu.pucp.morapack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pe.edu.pucp.morapack.algos.data.providers.DataProvider;
import pe.edu.pucp.morapack.algos.data.providers.DatabaseDataProvider;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.websocket.SimulationState;
import pe.edu.pucp.morapack.dto.websocket.SimulationStatusUpdate;
import pe.edu.pucp.morapack.repository.simulation.SimOrderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages multiple concurrent simulation sessions (one per user).
 *
 * Each user can have their own simulation running independently.
 * This service creates, controls, and cleans up SimulationSession instances.
 */
@Service
public class SimulationManager {

    private final SimpMessagingTemplate messagingTemplate;

    // 🆕 Dynamic event services (injected by Spring)
    private final CancellationService cancellationService;
    private final DynamicOrderService dynamicOrderService;
    private final OrderInjectionService orderInjectionService;
    private final ReplanificationService replanificationService;
    private final FlightStatusTracker flightStatusTracker;

    // ✅ DatabaseDataProvider for loading ALL orders from database
    private final DatabaseDataProvider databaseDataProvider;

    // Repository to query data availability
    private final SimOrderRepository simOrderRepository;

    // Map of userId -> SimulationSession
    private final Map<String, SimulationSession> activeSessions = new ConcurrentHashMap<>();

    // Thread pool for running simulations
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Default simulation parameters
    private final int simulationDays = 7;    // 1 week

    // Valid date range for simulation data (both loaded dynamically from database)
    private LocalDate firstDataDate;  // Earliest order date in database
    private LocalDate lastDataDate;   // Latest order date in database

    @Autowired
    public SimulationManager(
            SimpMessagingTemplate messagingTemplate,
            CancellationService cancellationService,
            DynamicOrderService dynamicOrderService,
            OrderInjectionService orderInjectionService,
            ReplanificationService replanificationService,
            FlightStatusTracker flightStatusTracker,
            DatabaseDataProvider databaseDataProvider,
            SimOrderRepository simOrderRepository) {

        this.messagingTemplate = messagingTemplate;
        this.cancellationService = cancellationService;
        this.dynamicOrderService = dynamicOrderService;
        this.orderInjectionService = orderInjectionService;
        this.replanificationService = replanificationService;
        this.flightStatusTracker = flightStatusTracker;
        this.databaseDataProvider = databaseDataProvider;
        this.simOrderRepository = simOrderRepository;

        // Query the database to find the actual date range
        try {
            this.firstDataDate = simOrderRepository.findMinOrderDate();
            this.lastDataDate = simOrderRepository.findMaxOrderDate();

            if (this.firstDataDate == null || this.lastDataDate == null) {
                // Fallback values
                this.firstDataDate = LocalDate.of(2025, 1, 2);
                this.lastDataDate = LocalDate.of(2025, 1, 31);
                System.out.println("[SimulationManager] ⚠️  No orders found in database, using fallback dates: " +
                                 this.firstDataDate + " to " + this.lastDataDate);
            } else {
                System.out.println("[SimulationManager] ✅ Initialized with DatabaseDataProvider");
                System.out.println("[SimulationManager] ✅ Loading ALL orders from database (not files)");
                System.out.println("[SimulationManager] 📅 Data available from " + this.firstDataDate + " to " + this.lastDataDate);
            }
        } catch (Exception e) {
            // Fallback values
            this.firstDataDate = LocalDate.of(2025, 1, 2);
            this.lastDataDate = LocalDate.of(2025, 1, 31);
            System.err.println("[SimulationManager] ⚠️  Error querying order date range: " + e.getMessage());
            System.err.println("[SimulationManager] ⚠️  Using fallback dates: " + this.firstDataDate + " to " + this.lastDataDate);
        }
    }
    
    /**
     * Start a new simulation for a user
     * @param startDate Optional start date (if null, defaults to FIRST_DATA_DATE)
     */
    public void startSimulation(String userId, ScenarioConfig scenario, LocalDate startDate, double initialSpeedMultiplier) {
        // Check if user already has an active simulation
        SimulationSession existingSession = activeSessions.get(userId);
        if (existingSession != null && existingSession.isRunning()) {
            sendError(userId, "A simulation is already running. Please stop it first.");
            return;
        }
        
        try {
            // Clean up old session if exists
            if (existingSession != null) {
                activeSessions.remove(userId);
            }
            
            // Validate and set start date
            if (startDate == null) {
                startDate = firstDataDate;  // Default to first day of data
            }

            // Validate date range
            if (startDate.isBefore(firstDataDate)) {
                sendError(userId, String.format(
                    "Start date %s is before available data (earliest: %s)",
                    startDate, firstDataDate
                ));
                return;
            }
            
            // ✅ Use DatabaseDataProvider to load ALL orders from database
            DataProvider dataProvider = databaseDataProvider;

            // Calculate simulation time range based on scenario
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime;

            // For COLLAPSE and DAILY, use a very far end date (will stop based on other conditions)
            if (scenario.getType() == ScenarioConfig.ScenarioType.COLLAPSE ||
                scenario.getType() == ScenarioConfig.ScenarioType.DAILY) {
                // Use max available data range
                endTime = lastDataDate.atTime(23, 59, 59);
                System.out.println("[SimulationManager] " + scenario.getType() + " scenario: running until " +
                                 (scenario.getType() == ScenarioConfig.ScenarioType.COLLAPSE ? "collapse detected" : "stopped"));
            } else {
                // WEEKLY: use fixed duration
                int durationMinutes = scenario.getTotalDurationMinutes();
                endTime = startTime.plusMinutes(durationMinutes);

                // Validate end date doesn't exceed available data
                LocalDate endDate = endTime.toLocalDate();
                if (endDate.isAfter(lastDataDate)) {
                    sendError(userId, String.format(
                        "Simulation would extend to %s, beyond available data (latest: %s)",
                        endDate, lastDataDate
                    ));
                    return;
                }
            }

            System.out.println("[SimulationManager] ✅ Starting simulation with DatabaseDataProvider");
            System.out.println("[SimulationManager] ✅ This will load ALL orders from ALL destinations (not just SUAA)");
            
            // Create new session (with dynamic events support and initial speed)
            SimulationSession session = new SimulationSession(
                userId,
                dataProvider,
                scenario,
                startTime,
                endTime,
                messagingTemplate,
                cancellationService,
                dynamicOrderService,
                orderInjectionService,
                replanificationService,
                flightStatusTracker,
                initialSpeedMultiplier
            );
            
            // Store and start
            activeSessions.put(userId, session);
            
            // Send session ID to user FIRST so they can subscribe to the correct topic
            sendSessionId(userId, session.getSessionId());
            
            // Then start the simulation
            executorService.submit(session);
            
            System.out.println("[SimulationManager] Started simulation for user: " + userId);
            System.out.println("   Scenario: " + scenario.getType());
            System.out.println("   Session ID: " + session.getSessionId());
            System.out.println("   Start Date: " + startDate);
            System.out.println("   End Date: " + endTime.toLocalDate());

        } catch (Exception e) {
            String errorMsg = "Failed to initialize simulation: " + e.getMessage();
            System.err.println("[SimulationManager] " + errorMsg);
            e.printStackTrace();
            sendError(userId, errorMsg);
        }
    }
    
    /**
     * Pause the user's simulation
     */
    public void pauseSimulation(String userId) {
        SimulationSession session = activeSessions.get(userId);
        if (session == null) {
            sendError(userId, "No active simulation found");
            return;
        }
        
        session.pause();
        System.out.println("[SimulationManager] Paused simulation for user: " + userId);
    }
    
    /**
     * Resume the user's simulation
     */
    public void resumeSimulation(String userId) {
        SimulationSession session = activeSessions.get(userId);
        if (session == null) {
            sendError(userId, "No active simulation found");
            return;
        }
        
        session.resume();
        System.out.println("[SimulationManager] Resumed simulation for user: " + userId);
    }
    
    /**
     * Stop the user's simulation
     */
    public void stopSimulation(String userId) {
        SimulationSession session = activeSessions.get(userId);
        if (session == null) {
            sendError(userId, "No active simulation found");
            return;
        }

        session.stop();

        // Clear all loaded cancellations to avoid them persisting to next simulation
        cancellationService.clearCancellations();

        // Clean up after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);  // Wait 1 second
                activeSessions.remove(userId);
                System.out.println("[SimulationManager] Cleaned up simulation for user: " + userId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        System.out.println("[SimulationManager] Stopped simulation for user: " + userId);
    }
    
    /**
     * Reset: stop current simulation and prepare for new start
     */
    public void resetSimulation(String userId) {
        SimulationSession session = activeSessions.get(userId);
        if (session != null) {
            session.stop();
            activeSessions.remove(userId);
        }

        // Clear all loaded cancellations to avoid them persisting to next simulation
        cancellationService.clearCancellations();

        // Send idle state
        SimulationStatusUpdate update = new SimulationStatusUpdate(
            SimulationState.IDLE,
            "Simulation reset. Ready to start new simulation."
        );
        messagingTemplate.convertAndSendToUser(userId, "/queue/simulation", update);

        System.out.println("[SimulationManager] Reset simulation for user: " + userId);
    }
    
    /**
     * Change simulation speed
     */
    public void setSimulationSpeed(String userId, double speedMultiplier) {
        SimulationSession session = activeSessions.get(userId);
        if (session == null) {
            sendError(userId, "No active simulation found");
            return;
        }
        
        session.setSpeed(speedMultiplier);
        System.out.println("[SimulationManager] Set speed to " + speedMultiplier + "x for user: " + userId);
    }
    
    /**
     * Get current session for a user
     */
    public SimulationSession getSession(String userId) {
        return activeSessions.get(userId);
    }

    /**
     * Get session by sessionId (UUID)
     * This searches through all active sessions to find one with matching sessionId
     */
    public SimulationSession getSessionBySessionId(String sessionId) {
        return activeSessions.values().stream()
            .filter(session -> session.getSessionId().equals(sessionId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Clean up completed/failed sessions
     */
    public void cleanupInactiveSessions() {
        activeSessions.entrySet().removeIf(entry -> {
            SimulationSession session = entry.getValue();
            if (!session.isRunning()) {
                System.out.println("[SimulationManager] Cleaning up inactive session for user: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Send session ID to user so they can subscribe to the correct topic
     */
    private void sendSessionId(String userId, String sessionId) {
        // Create a simple message with the sessionId
        java.util.Map<String, String> message = new java.util.HashMap<>();
        message.put("type", "SESSION_ID");
        message.put("sessionId", sessionId);
        message.put("topic", "/topic/simulation/" + sessionId);
        
        // Send via broadcast topic that the user is already listening to
        messagingTemplate.convertAndSend("/topic/simulation-control", message);
        
        System.out.println("[SimulationManager] Sent session ID to user: " + userId + " -> " + sessionId);
    }
    
    /**
     * Send error message to user
     */
    private void sendError(String userId, String errorMessage) {
        SimulationStatusUpdate update = SimulationStatusUpdate.error(errorMessage, null);
        messagingTemplate.convertAndSend("/topic/simulation-control", update);
    }
    
    /**
     * Shutdown all sessions (called on application shutdown)
     */
    public void shutdown() {
        System.out.println("[SimulationManager] Shutting down...");
        
        // Stop all active sessions
        activeSessions.values().forEach(SimulationSession::stop);
        activeSessions.clear();
        
        // Shutdown executor
        executorService.shutdown();
        
        System.out.println("[SimulationManager] Shutdown complete");
    }
}

