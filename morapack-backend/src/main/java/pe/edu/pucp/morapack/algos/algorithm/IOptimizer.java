package pe.edu.pucp.morapack.algos.algorithm;

import pe.edu.pucp.morapack.algos.entities.Solution;
import pe.edu.pucp.morapack.model.Airport;
import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Order;

import java.util.List;

public interface IOptimizer {
    Solution optimize(List<Order> orders, List<Flight> flights, List<Airport> airports);
}