package pe.edu.pucp.morapack.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pe.edu.pucp.morapack.algos.data.providers.DataProvider;
import pe.edu.pucp.morapack.algos.data.providers.FileDataProvider;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.websocket.SimulationState;
import pe.edu.pucp.morapack.dto.websocket.SimulationStatusUpdate;

import java.io.IOException;
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
    
    // Map of userId -> SimulationSession
    private final Map<String, SimulationSession> activeSessions = new ConcurrentHashMap<>();
    
    // Thread pool for running simulations
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // Data paths (configurable)
    private final String airportsFile = "data/airports_real.txt";
    private final String flightsFile = "data/flights.csv";
    private final String ordersFile = "data/pedidos_generados.csv";
    private final String cancellationsFile = "data/cancellations.csv";  // Future use
    
    // Default simulation parameters
    private final int simulationYear = 2025;
    private final int simulationMonth = 12;  // December
    private final int simulationDays = 7;    // 1 week
    
    // Valid date range for simulation data
    private final LocalDate FIRST_DATA_DATE = LocalDate.of(2025, 12, 1);
    private final LocalDate LAST_DATA_DATE = LocalDate.of(2025, 12, 31);
    
    public SimulationManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        System.out.println("[SimulationManager] Initialized");
    }
    
    /**
     * Start a new simulation for a user
     * @param startDate Optional start date (if null, defaults to FIRST_DATA_DATE)
     */
    public void startSimulation(String userId, ScenarioConfig scenario, LocalDate startDate) {
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
                startDate = FIRST_DATA_DATE;  // Default to first day of data
            }
            
            // Validate date range
            if (startDate.isBefore(FIRST_DATA_DATE)) {
                sendError(userId, String.format(
                    "Start date %s is before available data (earliest: %s)", 
                    startDate, FIRST_DATA_DATE
                ));
                return;
            }
            
            LocalDate endDate = startDate.plusDays(simulationDays);
            if (endDate.isAfter(LAST_DATA_DATE)) {
                sendError(userId, String.format(
                    "Simulation would extend to %s, beyond available data (latest: %s)", 
                    endDate, LAST_DATA_DATE
                ));
                return;
            }
            
            // Create DataProvider (from CSV files)
            DataProvider dataProvider = new FileDataProvider(
                airportsFile,
                flightsFile,
                ordersFile,
                cancellationsFile,
                simulationYear,
                simulationMonth,
                simulationDays
            );
            
            // Calculate simulation time range
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = startTime.plusDays(simulationDays);
            
            // Create new session
            SimulationSession session = new SimulationSession(
                userId,
                dataProvider,
                scenario,
                startTime,
                endTime,
                messagingTemplate
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
            System.out.println("   End Date: " + endDate);
            
        } catch (IOException e) {
            String errorMsg = "Failed to initialize simulation: " + e.getMessage();
            System.err.println("[SimulationManager] " + errorMsg);
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

