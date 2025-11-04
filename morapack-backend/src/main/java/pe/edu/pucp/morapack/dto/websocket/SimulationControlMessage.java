package pe.edu.pucp.morapack.dto.websocket;

import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import java.time.LocalDate;

/**
 * Message from client to control simulation
 */
public class SimulationControlMessage {
    
    private ControlAction action;
    private String scenarioType;  // "WEEKLY" or "COLLAPSE"
    private Integer customK;      // Optional custom K value
    private Double speedMultiplier; // For SPEED action: 0.5, 1.0, 2.0, 5.0, 10.0
    private String startDate;     // Optional start date (ISO format: "2025-12-01")
    
    public enum ControlAction {
        START,   // Begin new simulation
        PAUSE,   // Temporarily pause
        RESUME,  // Continue from pause
        STOP,    // Terminate
        RESET,   // Stop and prepare for new start
        SPEED    // Change execution speed
    }
    
    // Constructors
    public SimulationControlMessage() {}
    
    public SimulationControlMessage(ControlAction action) {
        this.action = action;
    }
    
    // Getters and Setters
    public ControlAction getAction() {
        return action;
    }
    
    public void setAction(ControlAction action) {
        this.action = action;
    }
    
    public String getScenarioType() {
        return scenarioType;
    }
    
    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }
    
    public Integer getCustomK() {
        return customK;
    }
    
    public void setCustomK(Integer customK) {
        this.customK = customK;
    }
    
    public Double getSpeedMultiplier() {
        return speedMultiplier;
    }
    
    public void setSpeedMultiplier(Double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }
    
    public String getStartDate() {
        return startDate;
    }
    
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    
    /**
     * Parse start date from string to LocalDate
     */
    public LocalDate getStartDateAsLocalDate() {
        if (startDate == null || startDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(startDate);
        } catch (Exception e) {
            System.err.println("[SimulationControlMessage] Invalid start date: " + startDate);
            return null;
        }
    }
    
    /**
     * Helper to create ScenarioConfig from message
     */
    public ScenarioConfig toScenarioConfig() {
        if (scenarioType == null) {
            return ScenarioConfig.weekly(); // Default
        }
        
        return switch (scenarioType.toUpperCase()) {
            case "WEEKLY" -> customK != null ? 
                ScenarioConfig.weekly(customK) : 
                ScenarioConfig.weekly();
            case "COLLAPSE" -> ScenarioConfig.collapse();
            case "DAILY" -> ScenarioConfig.daily();
            default -> ScenarioConfig.weekly();
        };
    }
}


