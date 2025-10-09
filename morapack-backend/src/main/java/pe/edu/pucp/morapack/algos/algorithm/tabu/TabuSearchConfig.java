package pe.edu.pucp.morapack.algos.algorithm.tabu;

public class TabuSearchConfig {
    // Parámetros del algoritmo
    private final int tabuListSize;
    private final int maxIterations;
    private final int maxIterationsWithoutImprovement;
    private final int directRouteProbability;
    private final int oneStopRouteProbability;
    private final int bottleneckCapacity;
    
    // Penalizaciones
    private final double capacityViolationPenalty;
    private final double emptyRoutePenalty;
    private final double delayBasePenalty;
    private final double delayHourPenalty;
    private final double stopoverPenalty;
    private final double invalidStopoverTimePenalty;
    private final double cancellationPenalty;
    private final double replanificationPenalty;

    public TabuSearchConfig(
            int tabuListSize,
            int maxIterations,
            int maxIterationsWithoutImprovement,
            int directRouteProbability,
            int oneStopRouteProbability,
            int bottleneckCapacity,
            double capacityViolationPenalty,
            double emptyRoutePenalty,
            double delayBasePenalty,
            double delayHourPenalty,
            double stopoverPenalty,
            double invalidStopoverTimePenalty,
            double cancellationPenalty,
            double replanificationPenalty) {
        this.tabuListSize = tabuListSize;
        this.maxIterations = maxIterations;
        this.maxIterationsWithoutImprovement = maxIterationsWithoutImprovement;
        this.directRouteProbability = directRouteProbability;
        this.oneStopRouteProbability = oneStopRouteProbability;
        this.bottleneckCapacity = bottleneckCapacity;
        this.capacityViolationPenalty = capacityViolationPenalty;
        this.emptyRoutePenalty = emptyRoutePenalty;
        this.delayBasePenalty = delayBasePenalty;
        this.delayHourPenalty = delayHourPenalty;
        this.stopoverPenalty = stopoverPenalty;
        this.invalidStopoverTimePenalty = invalidStopoverTimePenalty;
        this.cancellationPenalty = cancellationPenalty;
        this.replanificationPenalty = replanificationPenalty;
    }

    // Constructor con valores por defecto optimizados para mayor exploración
    public TabuSearchConfig() {
        this(22,     // tabuListSize - Phase 1 optimization - Better memory
             220,    // maxIterations - Phase 1 - More exploration time
             45,     // maxIterationsWithoutImprovement - More patience
             50,     // directRouteProbability (50%) - Reducido para considerar más escalas
             90,     // oneStopRouteProbability (90%) - Aumentado para favorecer escalas
             200,    // bottleneckCapacity
             22000,  // capacityViolationPenalty ↑2k - Maintain capacity discipline
             25000,  // emptyRoutePenalty ↓5k - More flexibility for empty routes
             10000,  // delayBasePenalty
             100,    // delayHourPenalty
             40,     // stopoverPenalty ↓10 - Encourage more stopovers
             6000,   // invalidStopoverTimePenalty ↓2k - Less temporal rigidity
             23000,  // cancellationPenalty ↓2k - More cancellation flexibility
             4500);  // replanificationPenalty ↓500 - Encourage replanning
    }

    // Getters
    public int getTabuListSize() { return tabuListSize; }
    public int getMaxIterations() { return maxIterations; }
    public int getMaxIterationsWithoutImprovement() { return maxIterationsWithoutImprovement; }
    public int getDirectRouteProbability() { return directRouteProbability; }
    public int getOneStopRouteProbability() { return oneStopRouteProbability; }
    public int getBottleneckCapacity() { return bottleneckCapacity; }
    public double getCapacityViolationPenalty() { return capacityViolationPenalty; }
    public double getEmptyRoutePenalty() { return emptyRoutePenalty; }
    public double getDelayBasePenalty() { return delayBasePenalty; }
    public double getDelayHourPenalty() { return delayHourPenalty; }
    public double getStopoverPenalty() { return stopoverPenalty; }
    public double getInvalidStopoverTimePenalty() { return invalidStopoverTimePenalty; }
    public double getCancellationPenalty() { return cancellationPenalty; }
    public double getReplanificationPenalty() { return replanificationPenalty; }
}