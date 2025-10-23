package pe.edu.pucp.morapack.algos.planner;

/**
 * No-op planner used when messaging is disabled. Keeps the same interface
 * but doesn't attempt to send updates.
 */
public class NoOpWeeklyPlanner implements WeeklyPlanner {
    private volatile boolean running = false;

    @Override
    public void start(String payload) {
        running = true;
        // Immediately stop; this planner is a no-op to allow REST endpoints to work
        running = false;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public void updateFailures(String payload) {
        // no-op
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
