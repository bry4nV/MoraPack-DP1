package pe.edu.pucp.morapack.algos.algorithm.aco;

import java.util.*;

public class ACOPedidos {
    private Grafo grafo;
    private List<Aeropuerto> sedes;
    private int numHormigas;
    private int numIteraciones;
    private double evaporacion;
    private double incrementoFeromona;

    public ACOPedidos(Grafo grafo, List<Aeropuerto> sedes, int numHormigas,
                      int numIteraciones, double evaporacion, double incrementoFeromona) {
        this.grafo = grafo;
        this.sedes = sedes;
        this.numHormigas = numHormigas;
        this.numIteraciones = numIteraciones;
        this.evaporacion = evaporacion;
        this.incrementoFeromona = incrementoFeromona;
    }

    /**
     * Ejecuta ACO para una lista de pedidos
     */
    public Map<Pedido, Hormiga> solucionar(List<Pedido> pedidos, Map<String, Aeropuerto> aeropuertoPorCodigo) {
        Map<Pedido, Hormiga> mejorRutaPorPedido = new HashMap<>();

        for (Pedido pedido : pedidos) {
            Hormiga mejorHormiga = null;

            // Definir plazo máximo para el pedido
            double plazoMax = calcularPlazoMaximo(sedes.get(0), aeropuertoPorCodigo.get(pedido.getDestino()));

            for (int iter = 0; iter < numIteraciones; iter++) {
                List<Hormiga> hormigas = new ArrayList<>();

                for (int h = 0; h < numHormigas; h++) {
                    Aeropuerto sedeInicial = sedes.get(new Random().nextInt(sedes.size()));
                    Hormiga hormiga = new Hormiga(
                            sedeInicial,
                            aeropuertoPorCodigo.get(pedido.getDestino().toUpperCase().trim()),
                            pedido.getCantidadPaquetes(),
                            plazoMax
                    );
                    hormigas.add(hormiga);
                }

                for (Hormiga h : hormigas) {
                    h.construirRuta(grafo);
                }

                actualizarFeromonas(hormigas);

                for (Hormiga h : hormigas) {
                    if (mejorHormiga == null || (h.ruta.size() > 0 && h.tiempoTotal < mejorHormiga.tiempoTotal)) {
                        mejorHormiga = h;
                    }
                }
            }

            mejorRutaPorPedido.put(pedido, mejorHormiga);

            // Mostrar ruta final del pedido
            System.out.println("\nPedido " + pedido.getIdCliente() +
                    " | destino " + pedido.getDestino() +
                    " | cantidad " + pedido.getCantidadPaquetes());

            if (mejorHormiga != null && !mejorHormiga.ruta.isEmpty()) {
                for (Arista a : mejorHormiga.ruta) {
                    System.out.println(a.origen.codigo + " -> " + a.destino.codigo +
                            " | Tiempo: " + a.tiempo + "h");
                }
                System.out.println("Tiempo total de la ruta: " + mejorHormiga.tiempoTotal + "h");
            } else {
                System.out.println("No se pudo generar ruta para este pedido.");
            }
        }

        return mejorRutaPorPedido;
    }

    /**
     * Evaporación y refuerzo de feromonas
     */
    private void actualizarFeromonas(List<Hormiga> hormigas) {
        // Evaporación
        for (List<Arista> aristas : grafo.adyacencias.values()) {
            for (Arista a : aristas) {
                a.feromona *= (1 - evaporacion);
                if (a.feromona < 0.1) a.feromona = 0.1;
            }
        }

        // Incremento según calidad de rutas
        for (Hormiga h : hormigas) {
            if (h.ruta.isEmpty()) continue;
            double valor = incrementoFeromona / (1 + h.tiempoTotal);
            for (Arista a : h.ruta) {
                a.feromona += valor;
            }
        }
    }

    /**
     * Ejemplo de cálculo de plazo máximo: 24h mismo continente, 48h distinto
     */
    private double calcularPlazoMaximo(Aeropuerto origen, Aeropuerto destino) {
        if (origen == null || destino == null) return 48.0;
        return origen.continente.equals(destino.continente) ? 72.0 : 120.0;
    }
}
