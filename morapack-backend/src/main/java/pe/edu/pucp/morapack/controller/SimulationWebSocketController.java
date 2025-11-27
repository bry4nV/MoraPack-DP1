package pe.edu.pucp.morapack.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.websocket.SimulationControlMessage;
import pe.edu.pucp.morapack.service.SimulationManager;

import java.security.Principal;

/**
 * WebSocket controller for simulation control.
 * 
 * Handles messages from clients to control their simulations:
 * - START: Begin new simulation
 * - PAUSE: Temporarily pause
 * - RESUME: Continue from pause
 * - STOP: Terminate simulation
 * - RESET: Stop and prepare for new start
 * - SPEED: Change execution speed
 * 
 * Each user gets their own simulation session.
 */
@Controller
public class SimulationWebSocketController {
    
    private final SimulationManager simulationManager;
    
    public SimulationWebSocketController(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }
    
    /**
     * Handle simulation control messages from client
     * 
     * Client sends to: /app/simulation/control
     * Server responds to: /user/queue/simulation
     */
    @MessageMapping("/simulation/control")
    public void handleControl(
            @Payload SimulationControlMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        // Get user ID from session
        // In production, use proper authentication
        String userId = getUserId(headerAccessor);
        
        System.out.println("[WebSocket] User " + userId + " sent control: " + message.getAction());
        
        // Route to appropriate handler
        switch (message.getAction()) {
            case START -> handleStart(userId, message);
            case PAUSE -> handlePause(userId);
            case RESUME -> handleResume(userId);
            case STOP -> handleStop(userId);
            case RESET -> handleReset(userId);
            case SPEED -> handleSpeed(userId, message);
            default -> System.err.println("[WebSocket] Unknown action: " + message.getAction());
        }
    }
    
    private void handleStart(String userId, SimulationControlMessage message) {
        // Create ScenarioConfig from message
        ScenarioConfig scenario = message.toScenarioConfig();

        // Extract start date (optional)
        java.time.LocalDate startDate = message.getStartDateAsLocalDate();

        // Extract speed multiplier (optional, defaults to 1.0)
        Double speedMultiplier = message.getSpeedMultiplier();
        if (speedMultiplier == null) {
            speedMultiplier = 1.0;
        }

        System.out.println("[WebSocket] Starting simulation for user: " + userId);
        System.out.println("   Scenario: " + scenario.getType());
        System.out.println("   K: " + scenario.getK());
        System.out.println("   Sc: " + scenario.getScMinutes() + " minutes");
        System.out.println("   Start Date: " + (startDate != null ? startDate : "default"));
        System.out.println("   Initial Speed: " + speedMultiplier + "x");

        // Pass initial speed directly to startSimulation
        simulationManager.startSimulation(userId, scenario, startDate, speedMultiplier);
    }
    
    private void handlePause(String userId) {
        System.out.println("[WebSocket] Pausing simulation for user: " + userId);
        simulationManager.pauseSimulation(userId);
    }
    
    private void handleResume(String userId) {
        System.out.println("[WebSocket] Resuming simulation for user: " + userId);
        simulationManager.resumeSimulation(userId);
    }
    
    private void handleStop(String userId) {
        System.out.println("[WebSocket] Stopping simulation for user: " + userId);
        simulationManager.stopSimulation(userId);
    }
    
    private void handleReset(String userId) {
        System.out.println("[WebSocket] Resetting simulation for user: " + userId);
        simulationManager.resetSimulation(userId);
    }
    
    private void handleSpeed(String userId, SimulationControlMessage message) {
        if (message.getSpeedMultiplier() == null) {
            System.err.println("[WebSocket] Speed command missing speedMultiplier");
            return;
        }
        
        System.out.println("[WebSocket] Setting speed to " + message.getSpeedMultiplier() + "x for user: " + userId);
        simulationManager.setSimulationSpeed(userId, message.getSpeedMultiplier());
    }
    
    /**
     * Get user ID from WebSocket session
     * 
     * In production, this should use proper Spring Security authentication.
     * For now, we use the session ID as a unique identifier.
     */
    private String getUserId(SimpMessageHeaderAccessor headerAccessor) {
        // Try to get authenticated user
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            return principal.getName();
        }
        
        // Fallback: use session ID
        String sessionId = headerAccessor.getSessionId();
        return sessionId != null ? sessionId : "anonymous";
    }
}


