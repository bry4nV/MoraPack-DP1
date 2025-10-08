
package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ACO {
    private final flightnetwork net;
    private final List<order> pedidos;
    private final ACOparams p;
    private final ACOHelpers helpers;
    private final Map<Integer, Double> pedidoDeadlines;  // Cache de deadlines por pedido
    
    public ACO(flightnetwork net, List<order> pedidos, ACOparams p) {
        this.net = net;
        this.p = p;
        
        // Crear un mapa de IDs nuevos para los aeropuertos
        Map<Integer, Integer> nuevoId = new HashMap<>();
        for (int i = 0; i < net.ciudades.size(); i++) {
            Aeropuerto ciudad = net.ciudades.get(i);
            nuevoId.put(ciudad.id, i);
            ciudad.id = i;  // Asignar nuevo ID secuencial
        }
        
        // Ordenar pedidos por deadline y cantidad
        this.pedidos = new ArrayList<>(pedidos);
        Collections.sort(this.pedidos, (a, b) -> {
            // Primero por deadline
            int deadlineComp = Double.compare(getDeadline(a), getDeadline(b));
            if (deadlineComp != 0) return deadlineComp;
            // Luego por cantidad (mayor primero)
            return -Integer.compare(a.cantidad, b.cantidad);
        });
        
        // Inicializar helpers y matriz de feromonas
        this.helpers = new ACOHelpers(net.ciudades.size(), p);
            
        // Calcular y cachear deadlines
        pedidoDeadlines = new HashMap<>();
        for (order pedido : pedidos) {
            pedidoDeadlines.put(pedido.id, getDeadline(pedido));
        }
    }
    
    private double getDeadline(order pedido) {
        return pedido.deadlineDias;  // Ya está calculado en el constructor de order
    }

        public solution run() {
            solution bestGlobal = null;
            Random rnd = ThreadLocalRandom.current();
            
            for(int iter = 0; iter < p.iterMax; iter++) {
                List<solution> soluciones = new ArrayList<>();
                for(int a = 0; a < p.mAnts; a++) {
                    soluciones.add(construirSolucion(rnd));
                }
                
                // Encontrar la mejor solución de la iteración
                solution bestIter = null;
                double bestIterCost = Double.POSITIVE_INFINITY;
                for(solution s : soluciones) {
                    if(s.cost() < bestIterCost) {
                        bestIter = s;
                        bestIterCost = s.cost();
                    }
                }
                
                // Actualizar mejor global
                if(bestGlobal == null || bestIterCost < bestGlobal.cost()) {
                    bestGlobal = bestIter;
                }
                
                // Actualizar feromonas globalmente
                helpers.actualizarFeromonaGlobal(bestGlobal);
            }
            return bestGlobal;
        }
        
        // En ACO.java (dentro de la clase ACO)
private solution construirSolucion(Random rnd) {
    solution sol = new solution();

    for (order o : pedidos) {
        orderplan plan = new orderplan(o);
        sol.planes.put(o.id, plan);

        int restantes = o.cantidad;
        Aeropuerto actual = o.sedeOrigen;  // ¡siempre partimos de la sede!
        double tActual = 0.0;
        int saltosUsados = 0;

        // Para pedidos intracontinentales, todas las patas deben ser intra-continente
        final boolean esIntra = o.sedeOrigen.continente.equalsIgnoreCase(o.destino.continente);
        final String cont = o.sedeOrigen.continente;

        while (restantes > 0 && tActual < o.deadlineDias) {

            // 1) Solo slots que SALEN de la ciudad 'actual'
            List<flightslot> cand = net.outgoingFrom(actual, tActual, o.deadlineDias);

            // 2) Filtros de negocio
            List<flightslot> validos = new ArrayList<>();
            for (flightslot s : cand) {
                // intracontinental → ambas puntas dentro del mismo continente
                if (esIntra) {
                    if (!s.flight.origen.continente.equalsIgnoreCase(cont)) continue;
                    if (!s.flight.destino.continente.equalsIgnoreCase(cont)) continue;
                }

                // límite de saltos: si ya hice 'maxSaltos' conexiones, solo acepto tramo al destino
                if (saltosUsados >= p.maxSaltos) {
                    if (s.flight.destino.id != o.destino.id) continue;
                } else {
                    // si NO es al destino, aseguro que desde el intermedio exista ruta al destino antes del deadline
                    if (s.flight.destino.id != o.destino.id) {
                        if (!net.existeRuta(s.flight.destino, o.destino, s.llegadaDia, o.deadlineDias)) {
                            continue;
                        }
                    }
                }

                // No adelantar el reloj hacia atrás
                if (s.salidaDia < tActual) continue;
                // Límite de deadline
                if (s.llegadaDia > o.deadlineDias) continue;

                validos.add(s);
            }

            if (validos.isEmpty()) break; // nos quedamos sin movimientos válidos

            // 3) Selección ACO: score = tau * (heurística)^beta
            // Heurística simple: cuanto antes llegue mejor (1 / (tiempo de llegada - tActual + ε))
            double[] score = new double[validos.size()];
            double sum = 0.0;
            final double[][] tau;
            int n = net.ciudades.size();
            tau = new double[n][n];
            for (int i = 0; i < n; i++) Arrays.fill(tau[i], p.tau0);



            for (int i = 0; i < validos.size(); i++) {
                flightslot s = validos.get(i);
                int iCity = s.flight.origen.id, jCity = s.flight.destino.id;
                double etaLocal = 1.0 / ( (s.llegadaDia - tActual) + 1e-6 );
                double sc = Math.pow(tau[iCity][jCity], p.alpha) * Math.pow(etaLocal, p.beta);
                score[i] = sc;
                sum += sc;
            }

            // Greedy con prob. q0; si no, ruleta
            flightslot elegido;
            if (rnd.nextDouble() <= p.q0) {
                int kBest = 0;
                for (int i = 1; i < score.length; i++) if (score[i] > score[kBest]) kBest = i;
                elegido = validos.get(kBest);
            } else {
                double r = rnd.nextDouble() * sum, acc = 0.0;
                elegido = validos.get(validos.size() - 1);
                for (int i = 0; i < score.length; i++) {
                    acc += score[i];
                    if (acc >= r) { elegido = validos.get(i); break; }
                }
            }

            // 4) Asignación (posible fraccionamiento)
            int asignar = Math.min(restantes, elegido.capacidadRestante);
            if (asignar <= 0) {
                // Si el slot ya está lleno, descártalo y continúa el bucle
                validos.remove(elegido);
                if (validos.isEmpty()) break;
                continue;
            }

            elegido.capacidadRestante -= asignar;
            plan.legs.add(new legassign(elegido, asignar));
            plan.cantidadTotalAsignada += asignar;

            // avanzar en el tiempo y en la posición
            tActual = Math.max(tActual, elegido.salidaDia) + (elegido.llegadaDia - elegido.salidaDia);
            actual = elegido.flight.destino;

            // si llegué al destino, consumí un envío (asigné esos paquetes completos al destino)
            if (actual.id == o.destino.id) {
                restantes -= asignar;
                // resetear para enviar el resto por otra salida desde la SEDE
                actual = o.sedeOrigen;
                tActual = 0.0;
                saltosUsados = 0;
            } else {
                // sigo en ruta (escala)
                saltosUsados++;
            }
        }
    }

    sol.evaluar();
    // asegurar conteo de vuelos usados (legs)
    sol.vuelosUsados = 0;
    for (orderplan op : sol.planes.values()) sol.vuelosUsados += op.legs.size();
    return sol;
}

}
