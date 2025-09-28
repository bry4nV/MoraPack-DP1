package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.*;

public class Grafo {
    public Map<Aeropuerto, List<Arista>> adyacencias;

    public Grafo() {
        adyacencias = new HashMap<>();
    }

    public void agregarAeropuerto(Aeropuerto a) {
        adyacencias.putIfAbsent(a, new ArrayList<>());
    }

    public void agregarArista(Aeropuerto origen, Aeropuerto destino, int capacidad, double tiempo) {
        Arista arista = new Arista(origen, destino, capacidad, tiempo);
        adyacencias.get(origen).add(arista);
    }

    public List<Arista> obtenerAristasDesde(Aeropuerto a) {
        return adyacencias.getOrDefault(a, new ArrayList<>());
    }
}
