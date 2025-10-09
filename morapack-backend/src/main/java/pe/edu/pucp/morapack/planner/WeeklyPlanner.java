package pe.edu.pucp.morapack.planner;

/**
 * Minimal interface for a weekly planner / simulation runner.
 * Implementations will run the actual scheduling algorithm (Tabu/ACO/etc.)
 */
public interface WeeklyPlanner {
    /**
     * Start the planner with the given payload (usually JSON with initialTime and params).
     */
    void start(String payload);

    /**
     * Stop the currently running planner.
     */
    void stop();

    /**
     * Apply/update failures or incidents while simulation is running.
     */
    void updateFailures(String payload);

    /**
     * Whether the planner is currently running.
     */
    boolean isRunning();
}
