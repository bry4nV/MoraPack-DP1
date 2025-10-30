package pe.edu.pucp.morapack.algos.scheduler;

/**
 * Configuration for different simulation scenarios.
 * 
 * The key parameter is K (minutesToConsume), which determines
 * how much future data the planner processes in each iteration:
 * 
 * - DAILY (Real-time): K = 1 minute
 *   Process orders arriving in the next minute only
 *   Simulates actual day-to-day operations
 * 
 * - WEEKLY (Simulation): K = 60-120 minutes (1-2 hours)
 *   Process larger time chunks to simulate a week in 30-90 minutes
 *   Allows testing with more data without waiting days
 * 
 * - COLLAPSE (Stress test): K = 1440 minutes (24 hours)
 *   Process entire days at once to quickly reach system limits
 *   Used to find breaking points and capacity issues
 * 
 * Example:
 *   If current simulation time is 2025-10-13 08:00:00
 *   And K = 60 minutes
 *   The planner will process orders from 08:00:00 to 09:00:00
 */
public class ScenarioConfig {
    
    /**
     * Predefined scenario types
     */
    public enum ScenarioType {
        DAILY,      // Real-time operations (K=1)
        WEEKLY,     // Weekly simulation (K=60-120)
        COLLAPSE    // Until collapse simulation (K=1440)
    }
    
    private final ScenarioType type;
    
    /**
     * K: Minutes of data to consume per iteration
     * This is the core parameter that changes simulation speed
     */
    private final int minutesToConsume;
    
    /**
     * Total simulation duration (in minutes)
     * - DAILY: Infinite (runs continuously)
     * - WEEKLY: 10,080 minutes (7 days)
     * - COLLAPSE: Depends on when system breaks
     */
    private final int totalDurationMinutes;
    
    /**
     * Whether to persist results to database
     * - DAILY: true (need to save for historical tracking)
     * - WEEKLY/COLLAPSE: false (only visual results needed)
     */
    private final boolean persistToDatabase;
    
    /**
     * How often to emit status updates (in simulation minutes)
     * - DAILY: Every 60 minutes (hourly updates)
     * - WEEKLY: Every 1440 minutes (daily updates)
     * - COLLAPSE: Every 1440 minutes (daily updates)
     */
    private final int updateIntervalMinutes;
    
    // Private constructor - use factory methods below
    private ScenarioConfig(ScenarioType type, 
                          int minutesToConsume,
                          int totalDurationMinutes,
                          boolean persistToDatabase,
                          int updateIntervalMinutes) {
        this.type = type;
        this.minutesToConsume = minutesToConsume;
        this.totalDurationMinutes = totalDurationMinutes;
        this.persistToDatabase = persistToDatabase;
        this.updateIntervalMinutes = updateIntervalMinutes;
    }
    
    /**
     * Factory method: Daily operations configuration
     */
    public static ScenarioConfig daily() {
        return new ScenarioConfig(
            ScenarioType.DAILY,
            1,              // K = 1 minute (real-time)
            Integer.MAX_VALUE,  // Runs indefinitely
            true,           // Persist to database
            60              // Update every hour
        );
    }
    
    /**
     * Factory method: Weekly simulation configuration
     */
    public static ScenarioConfig weekly() {
        return new ScenarioConfig(
            ScenarioType.WEEKLY,
            60,             // K = 60 minutes (1 hour chunks)
            10080,          // 7 days = 7 * 24 * 60 = 10,080 minutes
            false,          // Don't persist (only visual)
            1440            // Update every day (1440 min)
        );
    }
    
    /**
     * Factory method: Collapse simulation configuration
     */
    public static ScenarioConfig collapse() {
        return new ScenarioConfig(
            ScenarioType.COLLAPSE,
            1440,           // K = 1440 minutes (entire days)
            Integer.MAX_VALUE,  // Until system collapses
            false,          // Don't persist (only visual)
            1440            // Update every day
        );
    }
    
    /**
     * Custom configuration (for testing)
     */
    public static ScenarioConfig custom(int minutesToConsume, 
                                       int totalDurationMinutes,
                                       boolean persistToDatabase) {
        return new ScenarioConfig(
            ScenarioType.WEEKLY, // Default to weekly
            minutesToConsume,
            totalDurationMinutes,
            persistToDatabase,
            Math.max(60, minutesToConsume) // Update interval = K or 1 hour minimum
        );
    }
    
    // Getters
    
    public ScenarioType getType() {
        return type;
    }
    
    /**
     * Get K: How many minutes to consume per iteration
     */
    public int getMinutesToConsume() {
        return minutesToConsume;
    }
    
    public int getTotalDurationMinutes() {
        return totalDurationMinutes;
    }
    
    public boolean shouldPersistToDatabase() {
        return persistToDatabase;
    }
    
    public int getUpdateIntervalMinutes() {
        return updateIntervalMinutes;
    }
    
    public boolean isRealTime() {
        return type == ScenarioType.DAILY;
    }
    
    public boolean isSimulation() {
        return type == ScenarioType.WEEKLY || type == ScenarioType.COLLAPSE;
    }
    
    @Override
    public String toString() {
        return String.format("ScenarioConfig[type=%s, K=%d min, duration=%d min, persist=%s]",
            type, minutesToConsume, totalDurationMinutes, persistToDatabase);
    }
}

