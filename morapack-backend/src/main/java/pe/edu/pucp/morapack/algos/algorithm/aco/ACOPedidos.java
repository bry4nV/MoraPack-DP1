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
    public Map<Pedido, Hormiga> Solucionar(List<Pedido> pedidos, Map<String, Aeropuerto> aeropuertoPorCodigo) {
        Map<Pedido, Hormiga> mejorRutaPorPedido = new HashMap<>();

        for (Pedido pedido : pedidos) {
            Hormiga mejorHormiga = null;

            for (int iter = 0; iter < numIteraciones; iter++) {
                List<Hormiga> hormigas = new ArrayList<>();

                // Crear varias hormigas para el pedido
                for (int h = 0; h < numHormigas; h++) {
                    Aeropuerto sedeInicial = sedes.get(new Random().nextInt(sedes.size()));
                    Hormiga hormiga = new Hormiga(
                            sedeInicial,
                            aeropuertoPorCodigo.get(pedido.getDestino()),
                            pedido.getCantidadPaquetes()
                    );
                    hormigas.add(hormiga);
                }

                // Cada hormiga construye su ruta
                for (Hormiga hormiga : hormigas) {
                    hormiga.construirRuta(grafo);
                }

                // Actualizar feromonas según rutas de hormigas
                actualizarFeromonas(hormigas);

                // Elegir la mejor hormiga de esta iteración
                for (Hormiga h : hormigas) {
                    if (mejorHormiga == null || h.tiempoTotal < mejorHormiga.tiempoTotal) {
                        mejorHormiga = h;
                    }
                }
            }

            mejorRutaPorPedido.put(pedido, mejorHormiga);

            // Mostrar ruta final del pedido
            System.out.println("\nPedido " + pedido.getIdCliente() +
                    " | destino " + pedido.getDestino() +
                    " | cantidad " + pedido.getCantidadPaquetes());
            for (Arista a : mejorHormiga.ruta) {
                System.out.println(a.origen.codigo + " -> " + a.destino.codigo +
                        " | Tiempo: " + a.tiempo + "h");
            }
            System.out.println("Tiempo total de la ruta: " + mejorHormiga.tiempoTotal + "h");
        }

        return mejorRutaPorPedido;
    }

    private void actualizarFeromonas(List<Hormiga> hormigas) {
        // Evaporación global
        for (List<Arista> aristas : grafo.adyacencias.values()) {
            for (Arista a : aristas) {
                a.feromona *= (1 - evaporacion);
                if (a.feromona < 0.1) a.feromona = 0.1;
            }
        }

        // Incrementar feromona según la calidad de las rutas
        for (Hormiga h : hormigas) {
            double valor = incrementoFeromona / (1 + h.tiempoTotal);
            for (Arista a : h.ruta) {
                a.feromona += valor;
            }
        }
    }
}
