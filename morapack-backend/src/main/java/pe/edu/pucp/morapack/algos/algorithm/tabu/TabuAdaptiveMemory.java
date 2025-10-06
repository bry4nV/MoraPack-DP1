package pe.edu.pucp.morapack.algos.algorithm.tabu;

import java.util.*;

/**
 * Adaptive Memory System para TabuSearch
 * Maneja la memoria adaptativa que controla la intensificación y diversificación
 * durante el proceso de búsqueda tabú
 */
public class TabuAdaptiveMemory {
    private Map<String, Integer> moveFrequency = new HashMap<>();
    private List<Double> improvementHistory = new ArrayList<>();
    private List<Double> costHistory = new ArrayList<>();
    private int baseTabuTenure;
    private int currentTabuTenure;
    private double avgImprovement = 0.0;
    private int consecutiveNoImprovement = 0;
    private boolean inIntensificationPhase = false;
    
    public TabuAdaptiveMemory(int baseTabuTenure) {
        this.baseTabuTenure = baseTabuTenure;
        this.currentTabuTenure = baseTabuTenure;
    }
    
    public void recordMove(String moveSignature) {
        moveFrequency.put(moveSignature, moveFrequency.getOrDefault(moveSignature, 0) + 1);
    }
    
    public void recordImprovement(double improvementPercentage, double currentCost) {
        improvementHistory.add(improvementPercentage);
        costHistory.add(currentCost);
        
        // Keep only last 20 improvements for adaptive calculation
        if (improvementHistory.size() > 20) {
            improvementHistory.remove(0);
            costHistory.remove(0);
        }
        
        // Calculate running average improvement
        avgImprovement = improvementHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        if (improvementPercentage > 0.01) { // Significant improvement (>0.01%)
            consecutiveNoImprovement = 0;
            inIntensificationPhase = true;
        } else {
            consecutiveNoImprovement++;
            if (consecutiveNoImprovement > 8) {
                inIntensificationPhase = false;
            }
        }
    }
    
    public int adaptTabuTenure() {
        if (inIntensificationPhase) {
            // Intensification: shorter tenure to explore local area
            currentTabuTenure = Math.max(baseTabuTenure - 3, 5);
        } else if (consecutiveNoImprovement > 15) {
            // Strong diversification: much longer tenure
            currentTabuTenure = Math.min(baseTabuTenure + 8, 35);
        } else if (consecutiveNoImprovement > 8) {
            // Moderate diversification
            currentTabuTenure = baseTabuTenure + 3;
        } else {
            // Normal operation
            currentTabuTenure = baseTabuTenure;
        }
        
        return currentTabuTenure;
    }
    
    public double getMoveFrequencyPenalty(String moveSignature) {
        int frequency = moveFrequency.getOrDefault(moveSignature, 0);
        // Higher penalty for frequently used moves to encourage diversification
        return 1.0 + (frequency * 0.02); // 2% penalty per previous use
    }
    
    public boolean shouldDiversify() {
        return consecutiveNoImprovement > 12 && avgImprovement < 0.001;
    }
    
    public String getPhaseInfo() {
        return String.format("Phase: %s, Tenure: %d, NoImprove: %d, AvgImpr: %.4f%%",
            inIntensificationPhase ? "INTENSIFY" : "DIVERSIFY",
            currentTabuTenure, consecutiveNoImprovement, avgImprovement * 100);
    }
}