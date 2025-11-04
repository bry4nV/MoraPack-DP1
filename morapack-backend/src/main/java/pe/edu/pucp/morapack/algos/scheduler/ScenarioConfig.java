package pe.edu.pucp.morapack.algos.scheduler;

/**
 * Configuration for different simulation scenarios.
 * Based on planned programming scheduling as defined in course materials.
 * 
 * KEY CONCEPTS:
 * - Ta (Tiempo de Algoritmo) = 1 minute [FIXED]
 *   Time the algorithm takes to execute
 * 
 * - Sa (Salto del Algoritmo) = 5 minutes [FIXED]
 *   Interval between algorithm executions
 *   Must be Sa > Ta to avoid execution overlap
 * 
 * - K (Constante de Proporcionalidad) = Variable [ADJUSTABLE]
 *   Proportionality constant that determines simulation speed
 *   Higher K = Fewer iterations, faster simulation
 *   Lower K = More iterations, more granular control
 *   
 *   SUGGESTED VALUES:
 *   K = 1     → Day-to-day operations (real-time)
 *   K = 10-30 → Weekly simulation (adjust for desired granularity)
 *             K=14 → 144 iterations for 7 days (more granular)
 *             K=24 → 84 iterations for 7 days (default, balanced)
 *   K = 75    → Collapse simulation (until system breaks)
 * 
 * - Sc (Salto de Consumo) = K × Sa [CALCULATED]
 *   Amount of simulation time data consumed per iteration
 *   Formula: Sc = K × Sa
 *   Examples:
 *     K=1,  Sa=5  → Sc=5 minutes
 *     K=14, Sa=5  → Sc=70 minutes
 *     K=75, Sa=5  → Sc=375 minutes (6.25 hours)
 * 
 * IMPORTANT: Sa must be "reasonably greater" than Ta to avoid solution collapse.
 *            If Sa is too small (Sa < Ta), executions overlap causing instability.
 */
public class ScenarioConfig {
    
    /**
     * Predefined scenario types
     */
    public enum ScenarioType {
        DAILY,      // Real-time operations (K=1)
        WEEKLY,     // Weekly simulation (K=14)
        COLLAPSE    // Until collapse simulation (K=75)
    }
    
    private final ScenarioType type;
    
    /**
     * Ta: Algorithm execution time (FIXED at 1 minute)
     */
    private static final int TA_MINUTES = 1;
    
    /**
     * Sa: Jump between algorithm executions (FIXED at 5 minutes)
     */
    private static final int SA_MINUTES = 5;
    
    /**
     * K: Proportionality constant (varies by scenario)
     * This is the core parameter that changes simulation speed
     */
    private final int K;
    
    /**
     * Total simulation duration (in minutes)
     * - DAILY: Infinite (runs continuously)
     * - WEEKLY: 10,080 minutes (7 days = 7 × 24 × 60)
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
                          int K,
                          int totalDurationMinutes,
                          boolean persistToDatabase,
                          int updateIntervalMinutes) {
        this.type = type;
        this.K = K;
        this.totalDurationMinutes = totalDurationMinutes;
        this.persistToDatabase = persistToDatabase;
        this.updateIntervalMinutes = updateIntervalMinutes;
    }
    
    /**
     * Factory method: Daily operations configuration
     * K = 1 → Sc = 1 × 5 = 5 minutes of data per iteration
     */
    public static ScenarioConfig daily() {
        return new ScenarioConfig(
            ScenarioType.DAILY,
            1,                  // K = 1 (real-time proportionality)
            Integer.MAX_VALUE,  // Runs indefinitely
            true,               // Persist to database
            60                  // Update every hour
        );
    }
    
    /**
     * Factory method: Weekly simulation configuration
     * K = 24 → Sc = 24 × 5 = 120 minutes of data per iteration
     * Simulates 7 days in 84 iterations
     * 
     * NOTE: K is adjustable. Use weekly(K) to customize.
     */
    public static ScenarioConfig weekly() {
        return weekly(24); // Default K=24 (2 hour windows)
    }
    
    /**
     * Factory method: Weekly simulation with custom K
     * 
     * @param K Proportionality constant
     *   K=10 → 201 iterations (more granular, slower)
     *   K=14 → 144 iterations (balanced, default)
     *   K=20 → 100 iterations (less granular, faster)
     *   K=24 → 84 iterations (similar to PDDS-VRP)
     *   K=30 → 67 iterations (rapid simulation)
     * 
     * @return ScenarioConfig for weekly simulation
     */
    public static ScenarioConfig weekly(int K) {
        int scMinutes = K * SA_MINUTES;
        int iterations = 10080 / scMinutes;
        
        System.out.println("   [ScenarioConfig] Creating WEEKLY with K=" + K + 
                         " → Sc=" + scMinutes + " min → ~" + iterations + " iterations for 7 days");
        
        return new ScenarioConfig(
            ScenarioType.WEEKLY,
            K,              // Custom K value
            10080,          // 7 days = 7 * 24 * 60 = 10,080 minutes
            false,          // Don't persist (only visual)
            1440            // Update every day (1440 min)
        );
    }
    
    /**
     * Factory method: Collapse simulation configuration
     * K = 75 → Sc = 75 × 5 = 375 minutes (6.25 hours) of data per iteration
     * Runs until system reaches capacity limits
     */
    public static ScenarioConfig collapse() {
        return new ScenarioConfig(
            ScenarioType.COLLAPSE,
            75,             // K = 75 (collapse proportionality)
            Integer.MAX_VALUE,  // Until system collapses
            false,          // Don't persist (only visual)
            1440            // Update every day
        );
    }
    
    /**
     * Custom configuration (for testing)
     * @param K Proportionality constant
     * @param totalDurationMinutes Total simulation duration
     * @param persistToDatabase Whether to persist results
     */
    public static ScenarioConfig custom(int K, 
                                       int totalDurationMinutes,
                                       boolean persistToDatabase) {
        return new ScenarioConfig(
            ScenarioType.WEEKLY, // Default to weekly
            K,
            totalDurationMinutes,
            persistToDatabase,
            Math.max(60, K * SA_MINUTES) // Update interval = Sc or 1 hour minimum
        );
    }
    
    // Getters
    
    public ScenarioType getType() {
        return type;
    }
    
    /**
     * Get Ta: Algorithm execution time (FIXED at 1 minute)
     */
    public int getTaMinutes() {
        return TA_MINUTES;
    }
    
    /**
     * Get Sa: Jump between algorithm executions (FIXED at 5 minutes)
     */
    public int getSaMinutes() {
        return SA_MINUTES;
    }
    
    /**
     * Get K: Proportionality constant
     */
    public int getK() {
        return K;
    }
    
    /**
     * Get Sc: Data consumption jump per iteration
     * Calculated as: Sc = K × Sa
     * This is the amount of simulation time processed per iteration
     */
    public int getScMinutes() {
        return K * SA_MINUTES;
    }
    
    /**
     * @deprecated Use getScMinutes() instead. This method returns the same value.
     */
    @Deprecated
    public int getMinutesToConsume() {
        return getScMinutes();
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
        return String.format("ScenarioConfig[type=%s, K=%d, Sa=%d min, Sc=%d min, duration=%d min, persist=%s]",
            type, K, SA_MINUTES, getScMinutes(), totalDurationMinutes, persistToDatabase);
    }
}

