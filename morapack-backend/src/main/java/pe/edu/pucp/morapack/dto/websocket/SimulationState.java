package pe.edu.pucp.morapack.dto.websocket;

/**
 * States of a simulation session
 */
public enum SimulationState {
    IDLE,        // Not started yet
    STARTING,    // Initializing data
    RUNNING,     // Actively executing iterations
    PAUSED,      // Temporarily stopped by user
    STOPPED,     // Terminated by user
    COMPLETED,   // Finished successfully
    ERROR        // Failed with error
}

