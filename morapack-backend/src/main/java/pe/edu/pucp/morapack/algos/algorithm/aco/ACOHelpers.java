package MoraTravel;

import java.util.*;

public class ACOHelpers {
    private final double[][] tau;        // Matriz de feromonas
    private final ACOparams p;          // Parámetros del algoritmo
    private final int numCiudades;      // Número total de ciudades
    
    public ACOHelpers(int numCiudades, ACOparams params) {
        this.numCiudades = numCiudades;
        this.p = params;
        this.tau = new double[numCiudades][numCiudades];
        
        // Inicializar feromonas
        for(int i = 0; i < numCiudades; i++) {
            for(int j = 0; j < numCiudades; j++) {
                tau[i][j] = p.tau0;
            }
        }
    }
    
    // Métodos auxiliares para la construcción de soluciones
    public int maxIndex(double[] arr) {
        int best = 0;
        for(int i = 1; i < arr.length; i++)
            if(arr[i] > arr[best]) best = i;
        return best;
    }
    
    public int ruleta(double[] scores, Random rnd) {
        double total = 0;
        for(double s : scores) total += s;
        
        double r = rnd.nextDouble() * total;
        double sum = 0;
        for(int i = 0; i < scores.length; i++) {
            sum += scores[i];
            if(sum >= r) return i;
        }
        return scores.length - 1;
    }
    
    public void evaporarLocal(flight vuelo) {
        int i = vuelo.origen.id;
        int j = vuelo.destino.id;
        tau[i][j] = (1.0 - p.xi) * tau[i][j] + p.xi * p.tau0;
    }
    
    public void actualizarFeromonaGlobal(solution mejor) {
        // Evaporación global
        for(int i = 0; i < numCiudades; i++) {
            for(int j = 0; j < numCiudades; j++) {
                tau[i][j] = (1.0 - p.rho) * tau[i][j];
            }
        }
        
        // Depósito de feromona en las aristas de la mejor solución
        if(mejor != null) {
            double deltaTau = 1.0 / mejor.cost();
            for(orderplan plan : mejor.planes.values()) {
                for(legassign leg : plan.legs) {
                    int i = leg.slot.flight.origen.id;
                    int j = leg.slot.flight.destino.id;
                    tau[i][j] += p.rho * deltaTau;
                }
            }
        }
    }
    
    public List<flightslot> filtrarCandidatos(List<flightslot> slots, order pedido, Aeropuerto actual, double tActual) {
        List<flightslot> filtrados = new ArrayList<>();
        for(flightslot s : slots) {
            // Verificar política de continentes para sedes
            if(actual == pedido.sedeOrigen && 
               !actual.continente.equals(pedido.destino.continente)) {
                continue;
            }
            
            // Verificar capacidad mínima y tiempo límite
            if(s.capacidadRestante <= 0 || s.llegadaDia > pedido.deadlineDias) {
                continue;
            }
            
            filtrados.add(s);
        }
        
        // Ordenar por heurística
        filtrados.sort((a, b) -> 
            Double.compare(calcularHeuristica(b, pedido, tActual),
                         calcularHeuristica(a, pedido, tActual)));
                         
        return filtrados;
    }
    
    public double calcularScore(flightslot s, order pedido, Aeropuerto actual, double tActual) {
        int i = actual.id;
        int j = s.flight.destino.id;
        
        // Validar índices
        if (i >= numCiudades || j >= numCiudades || i < 0 || j < 0) {
            return 0.0; // Retornar score 0 para rutas inválidas
        }
        
        double t = tau[i][j];
        double n = calcularHeuristica(s, pedido, tActual);
        return Math.pow(t, p.alpha) * Math.pow(n, p.beta);
    }
    
    public double calcularHeuristica(flightslot s, order pedido, double tActual) {
        // Preferir vuelos que:
        // 1. Lleguen más cerca al destino (distancia al objetivo)
        // 2. Tengan más capacidad disponible
        // 3. Salgan más pronto (pero después de tActual)
        // 4. Lleguen antes del deadline
        
        double distScore = 1.0; // Por ahora ignoramos distancias geográficas
        if(s.flight.destino.id == pedido.destino.id) distScore = 2.0; // Bonus por llegar al destino
        
        double capScore = (double)s.capacidadRestante / s.flight.capacidadMax;
        
        double timeScore = 1.0;
        if(s.llegadaDia <= pedido.deadlineDias) {
            timeScore = 1.0 + (pedido.deadlineDias - s.llegadaDia) / pedido.deadlineDias;
        } else {
            timeScore = 0.1; // Penalizar fuertemente llegadas tardías
        }
        
        return distScore * capScore * timeScore;
    }
}