package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.*;

public class Hormiga {
    public List<Arista> ruta = new ArrayList<>();
    public Set<Aeropuerto> visitados = new HashSet<>();
    public double tiempoTotal = 0;
    public int cantidadPaquetes;
    public Aeropuerto destinoPedido;
    public double plazoMaximo; // en horas

    private double alpha = 1.0;
    private double beta = 2.0;

    public Hormiga(Aeropuerto sedeInicial, Aeropuerto destinoPedido, int cantidadPaquetes, double plazoMaximo) {
        this.destinoPedido = destinoPedido;
        this.cantidadPaquetes = cantidadPaquetes;
        this.plazoMaximo = plazoMaximo;
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
            // Filtrar por capacidad y plazo máximo
            if (!visitados.contains(a.destino) &&
                a.capacidad >= cantidadPaquetes &&
                (tiempoTotal + a.tiempo) <= plazoMaximo) {
                candidatas.add(a);
            }
        }

        if (candidatas.isEmpty()) return null;

        double suma = 0.0;
        for (Arista a : candidatas) {
            suma += Math.pow(a.feromona, alpha) * Math.pow(1.0 / a.tiempo, beta);
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

    public void construirRuta(Grafo grafo) {
        while (!obtenerUltimoAeropuerto().equals(destinoPedido)) {
            Arista siguiente = elegirSiguienteArista(grafo);
            if (siguiente == null) break; // no hay más vuelos válidos
            agregarArista(siguiente);
        }
    }

    private void agregarArista(Arista a) {
        ruta.add(a);
        visitados.add(a.destino);
        tiempoTotal += a.tiempo;
        // Reservar capacidad temporalmente
        a.capacidad -= cantidadPaquetes;
    }
}
