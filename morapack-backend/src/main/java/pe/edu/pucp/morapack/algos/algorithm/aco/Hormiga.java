package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.*;

public class Hormiga {
    private Aeropuerto sedeInicial;
    private Aeropuerto destinoPedido;
    private int cantidadPaquetes;
    private double plazoMaximo;
    double tiempoTotal = 0.0;
    private Set<Aeropuerto> visitados = new HashSet<>();
    List<Arista> ruta = new ArrayList<>();
    private Random random;  // Para el control de aleatoriedad
    private int horaactual;
    // Constructor de la hormiga
    public Hormiga(Aeropuerto sedeInicial, Aeropuerto destinoPedido, int cantidadPaquetes, double plazoMaximo, Random random) {
        this.sedeInicial = sedeInicial;
        this.destinoPedido = destinoPedido;
        this.cantidadPaquetes = cantidadPaquetes;
        this.plazoMaximo = plazoMaximo;
        this.random = random;  // Usar el random con semilla fija
        //this.horaactual=sedeInicial.husoHorario;
        visitados.add(sedeInicial);  // Agregar la sede inicial a los visitados
    }

    // Método para construir la ruta
    public void construirRuta(Grafo grafo) {
        Aeropuerto actual = sedeInicial;  // La sede de inicio es el punto inicial de la ruta

        // Mientras no haya llegado al destino, la hormiga continúa buscando aristas
        while (!actual.equals(destinoPedido)) {
            // Elegir la siguiente arista basada en la probabilidad
            Arista siguienteArista = elegirSiguienteArista(grafo, actual);

            if (siguienteArista == null) {
                break;  // Si no hay aristas válidas, detener el proceso
            }

            //int tiempoDeEspera = calcularTiempoEspera(actual, siguienteArista.destino);
            //horaactual += tiempoDeEspera;

            // Actualizar tiempo total y agregar la arista a la ruta
            tiempoTotal += siguienteArista.tiempo;
            ruta.add(siguienteArista);
            visitados.add(siguienteArista.destino);
            actual = siguienteArista.destino;  // Mover al siguiente aeropuerto
        }
    }

    // Método para elegir la siguiente arista con base en la probabilidad
    public Arista elegirSiguienteArista(Grafo grafo, Aeropuerto actual) {
        List<Arista> posibles = grafo.obtenerAristasDesde(actual);  // Obtener las aristas desde el aeropuerto actual
        List<Arista> candidatas = new ArrayList<>();
        
        // Filtrar aristas válidas que no hayan sido visitadas y que respeten el plazo máximo
        for (Arista arista : posibles) {
            if (!visitados.contains(arista.destino) && (tiempoTotal + arista.tiempo <= plazoMaximo)) {
                candidatas.add(arista);
            }
        }

        if (candidatas.isEmpty()) {
            return null;  // Si no hay aristas válidas, retornar null
        }

        // Calcular la probabilidad de cada arista basada en las feromonas y el tiempo de vuelo
        double sumaProbabilidades = 0.0;
        for (Arista arista : candidatas) {
            double heuristica = 1.0 / arista.tiempo;  // Preferir las aristas con menos tiempo de vuelo
            double probabilidad = Math.pow(arista.feromona, 1) * Math.pow(heuristica, 2);  // Ajustar los exponente para feromonas y heurística
            sumaProbabilidades += probabilidad;
        }

        // Elegir aleatoriamente una arista basada en las probabilidades
        double rand = random.nextDouble() * sumaProbabilidades;
        double acumulado = 0.0;
        for (Arista arista : candidatas) {
            double probabilidad = Math.pow(arista.feromona, 1) * Math.pow(1.0 / arista.tiempo, 2) / sumaProbabilidades;
            acumulado += probabilidad;
            if (rand <= acumulado) {
                return arista;
            }
        }

        // En caso de que haya un problema, devolver la última opción (esto debería ser muy raro)
        return candidatas.get(candidatas.size() - 1);
    }
}
