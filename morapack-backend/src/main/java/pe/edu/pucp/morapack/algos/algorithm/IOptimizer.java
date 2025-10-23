package pe.edu.pucp.morapack.algos.algorithm;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.entities.PlannerFlight;
import pe.edu.pucp.morapack.algos.entities.PlannerOrder;
import pe.edu.pucp.morapack.algos.entities.Solution;

import java.util.List;

public interface IOptimizer {
    Solution optimize(List<PlannerOrder> orders, List<PlannerFlight> flights, List<PlannerAirport> airports);
}