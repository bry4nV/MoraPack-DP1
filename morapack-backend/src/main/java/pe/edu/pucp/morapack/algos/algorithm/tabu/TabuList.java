package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementa la estructura de memoria del algoritmo Tabú Search.
 * Incluye tanto memoria a corto plazo (lista tabú) como memoria a largo plazo
 * para mejorar la diversificación e intensificación de la búsqueda.
 */
public class TabuList {
    // Memoria a corto plazo
    private final Queue<TabuMove> shortTermMemory;
    private final int maxSize;
    
    // Memoria a largo plazo - Frecuencia
    private final Map<String, Integer> frequencyMemory;
    // Memoria a largo plazo - Recencia
    private final Map<String, Integer> recencyMemory;
    // Memoria a largo plazo - Calidad
    private final Map<String, Double> qualityMemory;
    
    private int currentIteration;

    public TabuList(int maxSize) {
        this.maxSize = maxSize;
        this.shortTermMemory = new LinkedList<>();
        this.frequencyMemory = new HashMap<>();
        this.recencyMemory = new HashMap<>();
        this.qualityMemory = new HashMap<>();
        this.currentIteration = 0;
    }

    /**
     * Agrega un movimiento a la lista tabú y actualiza las memorias de largo plazo
     */
    public void add(TabuMove move, double moveQuality) {
        // Actualizar memoria a corto plazo
        shortTermMemory.offer(move);
        if (shortTermMemory.size() > maxSize) {
            shortTermMemory.poll();
        }

        // Actualizar memorias a largo plazo
        String moveKey = getMoveKey(move);
        
        // Frecuencia: número de veces que se ha realizado un movimiento
        frequencyMemory.merge(moveKey, 1, Integer::sum);
        
        // Recencia: última iteración en que se realizó el movimiento
        recencyMemory.put(moveKey, currentIteration);
        
        // Calidad: promedio de la calidad de los movimientos
        double currentQuality = qualityMemory.getOrDefault(moveKey, 0.0);
        int frequency = frequencyMemory.get(moveKey);
        qualityMemory.put(moveKey, (currentQuality * (frequency - 1) + moveQuality) / frequency);
        
        currentIteration++;
    }

    /**
     * Verifica si un movimiento está en la lista tabú
     */
    public boolean contains(TabuMove move) {
        return shortTermMemory.stream().anyMatch(m -> m.equals(move));
    }

    /**
     * Calcula un factor de diversificación basado en la frecuencia de los movimientos
     */
    public double getDiversificationFactor(TabuMove move) {
        String moveKey = getMoveKey(move);
        int frequency = frequencyMemory.getOrDefault(moveKey, 0);
        return 1.0 / (1 + frequency); // Mayor frecuencia = menor factor
    }

    /**
     * Calcula un factor de intensificación basado en la calidad histórica de los movimientos
     */
    public double getIntensificationFactor(TabuMove move) {
        String moveKey = getMoveKey(move);
        return qualityMemory.getOrDefault(moveKey, 0.0);
    }

    /**
     * Identifica regiones prometedoras basadas en la calidad histórica
     */
    public List<String> getPromisingRegions() {
        return qualityMemory.entrySet().stream()
            .filter(e -> e.getValue() > getAverageQuality())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Identifica regiones poco exploradas basadas en la frecuencia
     */
    public List<String> getUnexploredRegions() {
        double avgFrequency = getAverageFrequency();
        return frequencyMemory.entrySet().stream()
            .filter(e -> e.getValue() < avgFrequency)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private String getMoveKey(TabuMove move) {
        Flight[] route = move.getNewRoute();
        if (route == null || route.length == 0) {
            return "unassign-" + move.getShipment().getId();
        }
        
        StringBuilder key = new StringBuilder();
        key.append(move.getShipment().getId());
        for (Flight flight : route) {
            key.append("-").append(flight.getCode());
        }
        return key.toString();
    }

    private double getAverageQuality() {
        return qualityMemory.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    private double getAverageFrequency() {
        return frequencyMemory.values().stream()
            .mapToDouble(Integer::doubleValue)
            .average()
            .orElse(0.0);
    }

    public void clear() {
        shortTermMemory.clear();
        frequencyMemory.clear();
        recencyMemory.clear();
        qualityMemory.clear();
        currentIteration = 0;
    }

    public int getCurrentIteration() {
        return currentIteration;
    }

    public Map<String, Integer> getFrequencyMemory() {
        return new HashMap<>(frequencyMemory);
    }

    public Map<String, Integer> getRecencyMemory() {
        return new HashMap<>(recencyMemory);
    }

    public Map<String, Double> getQualityMemory() {
        return new HashMap<>(qualityMemory);
    }
}