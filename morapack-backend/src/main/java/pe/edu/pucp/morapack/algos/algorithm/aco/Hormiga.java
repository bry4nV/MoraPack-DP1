package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.*;



public class Hormiga {
    public List<Arista> ruta;
    public Set<Aeropuerto> visitados;
    public int tiempoTotal;
    public int cantidadPaquetes;
    public Aeropuerto destinoPedido;

    private double alpha = 1.0;
    private double beta = 2.0;

    public Hormiga(Aeropuerto sedeInicial, Aeropuerto destinoPedido, int cantidadPaquetes) {
        this.ruta = new ArrayList<>();
        this.visitados = new HashSet<>();
        this.tiempoTotal = 0;
        this.cantidadPaquetes = cantidadPaquetes;
        this.destinoPedido = destinoPedido;
        visitados.add(sedeInicial);
    }

    public Aeropuerto obtenerUltimoAeropuerto() {
        if (ruta.isEmpty()) return visitados.iterator().next();
        return ruta.get(ruta.size() - 1).destino;
    }

    public Arista elegirSiguienteArista(Grafo grafo) {
        Aeropuerto actual = obtenerUltimoAeropuerto();
        List<Arista> posibles = grafo.obtenerAristasDesde(actual);

        List<Arista> candidatas = new ArrayList<>();
        for (Arista a : posibles) {
            // Solo considerar vuelos con capacidad suficiente
            if (!visitados.contains(a.destino) && a.capacidad >= cantidadPaquetes) {
                candidatas.add(a);
            }
        }
        if (candidatas.isEmpty()) return null;

        double suma = 0.0;
        for (Arista a : candidatas) {
            double valor = Math.pow(a.feromona, alpha) * Math.pow(1.0 / a.tiempo, beta);
            suma += valor;
        }

        double rand = Math.random();
        double acumulado = 0.0;
        for (Arista a : candidatas) {
            double prob = Math.pow(a.feromona, alpha) * Math.pow(1.0 / a.tiempo, beta) / suma;
            acumulado += prob;
            if (rand <= acumulado) return a;
        }
        return candidatas.get(candidatas.size() - 1);
    }

    // Construye la ruta hasta llegar al destino
    public void construirRuta(Grafo grafo) {
        while (!obtenerUltimoAeropuerto().equals(destinoPedido)) {
            Arista siguiente = elegirSiguienteArista(grafo);
            if (siguiente == null) break; // no hay ruta posible
            agregarArista(siguiente);
        }
    }

    private void agregarArista(Arista a) {
        ruta.add(a);
        visitados.add(a.destino);
        tiempoTotal += a.tiempo;
        a.capacidad -= cantidadPaquetes; // reservar espacio en el vuelo
    }
}
