package pe.edu.pucp.morapack.dto.websocket;

import pe.edu.pucp.morapack.dto.simulation.TabuSimulationResponse;

import java.time.LocalDateTime;

/**
 * Status update sent from server to client during simulation
 */
public class SimulationStatusUpdate {
    
    private SimulationState state;
    private String message;
    private LocalDateTime timestamp;
    
    // Progress information
    private Integer currentIteration;
    private Integer totalIterations;
    private Double progressPercentage;
    private LocalDateTime simulatedTime;  // Current simulation time (not real time)
    
    // Speed control
    private Double currentSpeed;  // 1.0 = normal, 2.0 = 2x faster, etc.
    
    // Latest iteration result (if available)
    private TabuSimulationResponse latestResult;
    
    // Error information (if state = ERROR)
    private String errorMessage;
    private String errorDetails;
    
    // Constructors
    public SimulationStatusUpdate() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SimulationStatusUpdate(SimulationState state, String message) {
        this();
        this.state = state;
        this.message = message;
    }
    
    // Static factory methods for common scenarios
    public static SimulationStatusUpdate starting() {
        return new SimulationStatusUpdate(SimulationState.STARTING, "Initializing simulation...");
    }
    
    public static SimulationStatusUpdate running(int current, int total, LocalDateTime simTime) {
        SimulationStatusUpdate update = new SimulationStatusUpdate(
            SimulationState.RUNNING, 
            "Simulation in progress"
        );
        update.setCurrentIteration(current);
        update.setTotalIterations(total);
        update.setProgressPercentage((double) current / total * 100.0);
        update.setSimulatedTime(simTime);
        return update;
    }
    
    public static SimulationStatusUpdate paused(int current, int total) {
        SimulationStatusUpdate update = new SimulationStatusUpdate(
            SimulationState.PAUSED, 
            "Simulation paused by user"
        );
        update.setCurrentIteration(current);
        update.setTotalIterations(total);
        update.setProgressPercentage((double) current / total * 100.0);
        return update;
    }
    
    public static SimulationStatusUpdate completed(int totalIterations) {
        SimulationStatusUpdate update = new SimulationStatusUpdate(
            SimulationState.COMPLETED, 
            "Simulation completed successfully"
        );
        update.setCurrentIteration(totalIterations);
        update.setTotalIterations(totalIterations);
        update.setProgressPercentage(100.0);
        return update;
    }
    
    public static SimulationStatusUpdate error(String errorMessage, String details) {
        SimulationStatusUpdate update = new SimulationStatusUpdate(
            SimulationState.ERROR, 
            errorMessage
        );
        update.setErrorMessage(errorMessage);
        update.setErrorDetails(details);
        return update;
    }
    
    // Getters and Setters
    public SimulationState getState() {
        return state;
    }
    
    public void setState(SimulationState state) {
        this.state = state;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Integer getCurrentIteration() {
        return currentIteration;
    }
    
    public void setCurrentIteration(Integer currentIteration) {
        this.currentIteration = currentIteration;
    }
    
    public Integer getTotalIterations() {
        return totalIterations;
    }
    
    public void setTotalIterations(Integer totalIterations) {
        this.totalIterations = totalIterations;
    }
    
    public Double getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public LocalDateTime getSimulatedTime() {
        return simulatedTime;
    }
    
    public void setSimulatedTime(LocalDateTime simulatedTime) {
        this.simulatedTime = simulatedTime;
    }
    
    public Double getCurrentSpeed() {
        return currentSpeed;
    }
    
    public void setCurrentSpeed(Double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }
    
    public TabuSimulationResponse getLatestResult() {
        return latestResult;
    }
    
    public void setLatestResult(TabuSimulationResponse latestResult) {
        this.latestResult = latestResult;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}






