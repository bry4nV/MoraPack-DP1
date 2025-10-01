package pe.edu.pucp.morapack.algos.algorithm.aco;

import java.util.*;

public class ACOPedidos {
    private Grafo grafo;
    private List<Aeropuerto> sedes;
    private int numHormigas;
    private int numIteraciones;
    private double evaporacion;
    private double incrementoFeromona;
    private Random random;  // Añadimos la variable de Random con semilla fija

    public ACOPedidos(Grafo grafo, List<Aeropuerto> sedes, int numHormigas,
                      int numIteraciones, double evaporacion, double incrementoFeromona,Random seed) {
        this.grafo = grafo;
        this.sedes = sedes;
        this.numHormigas = numHormigas;
        this.numIteraciones = numIteraciones;
        this.evaporacion = evaporacion;
        this.incrementoFeromona = incrementoFeromona;
        this.random = seed;  // Usamos la semilla fija para el generador de números aleatorios
    }

    /**
     * Ejecuta ACO para una lista de pedidos
     */
    public Map<Pedido, Hormiga> solucionar(List<Pedido> pedidos, Map<String, Aeropuerto> aeropuertoPorCodigo) {
        Map<Pedido, Hormiga> mejorRutaPorPedido = new HashMap<>();
        int cant;
        for (Pedido pedido : pedidos) {
            Hormiga mejorHormiga = null;
            double plazoMax = calcularPlazoMaximo(sedes.get(0), aeropuertoPorCodigo.get(pedido.getDestino()));
                cant = pedido.getCantidadPaquetes();
            for (int iter = 0; iter < numIteraciones; iter++) {
                List<Hormiga> hormigas = new ArrayList<>();

                // Crear varias hormigas, cada una explorando rutas desde cada sede
                for (int h = 0; h < numHormigas; h++) {
                    for (Aeropuerto sede : sedes) {
                        Hormiga hormiga = new Hormiga(
                                sede,
                                aeropuertoPorCodigo.get(pedido.getDestino()),   
                                cant,
                                plazoMax,
                                random  // Pasamos el random con la semilla fija
                        );
                        hormigas.add(hormiga);
                    }
                }

                for (Hormiga h : hormigas) {
                    h.construirRuta(grafo);
                }

                actualizarFeromonas(hormigas);

                // Seleccionar la mejor hormiga de esta iteración
                for (Hormiga h : hormigas) {
                    if (mejorHormiga == null || (h.ruta.size() > 0 && h.tiempoTotal < mejorHormiga.tiempoTotal)) {
                        mejorHormiga = h;
                    }
                }
            }

            mejorRutaPorPedido.put(pedido, mejorHormiga);

            // Imprimir las mejores rutas generadas
          /*System.out.println("\nPedido " + pedido.getIdCliente() +
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
            }*/
        }

        return mejorRutaPorPedido;
    }

    /**
     * Evaporación y refuerzo de feromonas
     */
    private void actualizarFeromonas(List<Hormiga> hormigas) {
        // Evaporación de feromonas
        for (List<Arista> aristas : grafo.adyacencias.values()) {
            for (Arista a : aristas) {
                a.feromona *= (1 - evaporacion);
                if (a.feromona < 0.1) a.feromona = 0.1;
            }
        }

        // Incremento de feromonas según calidad de las rutas
        for (Hormiga h : hormigas) {
            if (h.ruta.isEmpty()) continue;
            double valor = incrementoFeromona / (1 + h.tiempoTotal); // Preferir rutas más rápidas
            for (Arista a : h.ruta) {
                a.feromona += valor;
            }
        }
    }

    /**
     * Cálculo de plazo máximo de entrega
     */
    private double calcularPlazoMaximo(Aeropuerto origen, Aeropuerto destino) {
        double plazoBase = origen.continente.equals(destino.continente) ? 48.0 : 72.0; //para vuelos dentro del mismo continente
        return plazoBase;
    }
}
