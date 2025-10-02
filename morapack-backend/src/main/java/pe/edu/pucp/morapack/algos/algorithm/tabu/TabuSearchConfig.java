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

    // Constructor con valores por defecto
    public TabuSearchConfig() {
        this(15,     // tabuListSize - Aumentado para mejor memoria
             150,    // maxIterations - Más iteraciones para explorar más
             30,     // maxIterationsWithoutImprovement - Más paciencia
             50,     // directRouteProbability (50%) - Reducido para considerar más escalas
             90,     // oneStopRouteProbability (90%) - Aumentado para favorecer escalas
             200,    // bottleneckCapacity
             20000,  // capacityViolationPenalty
             40000,  // emptyRoutePenalty - Reducido para permitir más rutas
             10000,  // delayBasePenalty
             100,    // delayHourPenalty
             50,     // stopoverPenalty - Reducido para favorecer escalas
             10000,  // invalidStopoverTimePenalty - Reducido pero manteniendo validez
             25000,  // cancellationPenalty
             5000); // replanificationPenalty
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