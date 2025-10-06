package pe.edu.pucp.morapack.algos.algorithm.aco;

import java.time.LocalTime;
import java.util.*;

public class Hormiga {
    private Aeropuerto sedeInicial;
    private Aeropuerto destinoPedido;
    private int cantidadPaquetes;
    private int plazoMaximo; // en minutos
    double tiempoTotal = 0.0; // en minutos
    private Set<Aeropuerto> visitados = new HashSet<>();
    List<Arista> ruta = new ArrayList<>();
    private Random random;

    // Hora actual de la hormiga en minutos desde medianoche
    private int horaActual;

    public Hormiga(Aeropuerto sedeInicial, Aeropuerto destinoPedido, int cantidadPaquetes, int plazoMaximo, Random random,int horaRegistro) {
        this.sedeInicial = sedeInicial;
        this.destinoPedido = destinoPedido;
        this.cantidadPaquetes = cantidadPaquetes;
        this.plazoMaximo = plazoMaximo;
        this.random = random;
        this.horaActual = horaRegistro; 
        visitados.add(sedeInicial);
        
    }

    // Construir la ruta del pedido
    public void construirRuta(Grafo grafo) {
        Aeropuerto actual = sedeInicial;

        while (!actual.equals(destinoPedido)) {
            Arista siguiente = elegirSiguienteArista(grafo, actual);
            if (siguiente == null) break; // No hay ruta válida

            // Calcular tiempo de espera si llega antes del vuelo
            int horaSalidaVuelo = siguiente.horaSalida; // en minutos
            int tiempoEspera = horaSalidaVuelo - horaActual;
            if (tiempoEspera < 0) tiempoEspera = 0;

            // Calcular duración del vuelo
            int tiempoVuelo = siguiente.getDuracionMinutos();

            // Actualizar hora actual y tiempo total
            horaActual = horaSalidaVuelo + tiempoVuelo;
            tiempoTotal += tiempoEspera + tiempoVuelo;

            // Agregar arista a la ruta
            ruta.add(siguiente);
            visitados.add(siguiente.destino);
            actual = siguiente.destino;
        }
    }

    // Seleccionar la siguiente arista según probabilidad (feromonas + heurística)
    private Arista elegirSiguienteArista(Grafo grafo, Aeropuerto actual) {
        List<Arista> posibles = grafo.obtenerAristasDesde(actual);
        List<Arista> candidatas = new ArrayList<>();

        for (Arista a : posibles) {
            // No visitar nodos repetidos y respetar plazo máximo
            int tiempoVuelo = a.getDuracionMinutos();
            int tiempoEspera = Math.max(a.horaSalida - horaActual, 0);
            int tiempoTotalEstimado = (int) tiempoTotal + tiempoVuelo + tiempoEspera;

            if (!visitados.contains(a.destino) && tiempoTotalEstimado <= plazoMaximo) {
                candidatas.add(a);
            }
        }

        if (candidatas.isEmpty()) return null;

        // Calcular probabilidades
        double sumaProb = 0.0;
        Map<Arista, Double> probabilidades = new HashMap<>();
        for (Arista a : candidatas) {
            int tiempoVuelo = a.getDuracionMinutos();
            int tiempoEspera = Math.max(a.horaSalida - horaActual, 0);
            double heuristica = 1.0 / (tiempoVuelo + tiempoEspera); // preferir vuelos cortos
            double prob = Math.pow(a.feromona, 1) * Math.pow(heuristica, 2);
            probabilidades.put(a, prob);
            sumaProb += prob;
        }

        // Elegir arista aleatoriamente según probabilidades
        double rand = random.nextDouble() * sumaProb;
        double acum = 0.0;
        for (Arista a : candidatas) {
            acum += probabilidades.get(a);
            if (rand <= acum) return a;
        }

        return candidatas.get(candidatas.size() - 1);
    }
}
