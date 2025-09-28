
package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.*;

public class flightnetwork {
        final List<Aeropuerto> ciudades; 
        final List<flight> vuelos; 
        final List<flightslot> slots = new ArrayList<>();
        final Map<Integer,List<flightslot>> slotsFromCity = new HashMap<>(); final double horizonteDias;
        public flightnetwork(List<Aeropuerto> ciudades, List<flight> vuelos, double horizonteDias){
            this.ciudades = ciudades; this.vuelos = vuelos; this.horizonteDias = horizonteDias;
            for(Aeropuerto c : ciudades) slotsFromCity.put(c.id,new ArrayList<>());
            instanciarSlots();
        }
        private void instanciarSlots(){
            for(flight f : vuelos){ int freq = Math.max(1,f.frecuenciaPorDia); double intervalo = 1.0/freq;
                for(int d=0; d<Math.ceil(horizonteDias); d++) for(int k=0;k<freq;k++){
                    double salida = d + k*intervalo; flightslot s = new flightslot(f,salida); 
                    slots.add(s); slotsFromCity.get(f.origen.id).add(s);
                }
            }
            for(List<flightslot> l : slotsFromCity.values()) 
                l.sort(Comparator.comparingDouble(s->s.salidaDia));
        }
         
        // Devuelve los slots DIRECTOS origen->destino con salida >= tMin
        // Obtiene slots que salen desde un aeropuerto después de cierto tiempo
        public List<flightslot> slotsFrom(Aeropuerto origen, double tMin) {
            List<flightslot> res = new ArrayList<>();
            List<flightslot> disp = slotsFromCity.getOrDefault(origen.id, Collections.emptyList());
            for(flightslot s : disp) {
                if(s.salidaDia >= tMin && s.capacidadRestante > 0) {
                    res.add(s);
                }
            }
            return res;
        }
        
        // Devuelve los slots DIRECTOS origen->destino con salida >= tMin
        List<flightslot> slotsDirectos(Aeropuerto origen, Aeropuerto destino, double tMin){
            List<flightslot> res = new ArrayList<>();
            List<flightslot> disp = slotsFromCity.getOrDefault(origen.id, Collections.emptyList());
            for(flightslot s : disp){
                if(s.flight.destino.id == destino.id && s.salidaDia >= tMin){
                    res.add(s);
                }
            }
            return res; // ya están ordenados por salida
        }
        
        
    // Devuelve los slots factibles desde 'origen' con salida >= tActual
        public List<flightslot> vecinosFactibles(Aeropuerto origen, Aeropuerto destino, double tActual) {
        List<flightslot> res = new ArrayList<>();
        List<flightslot> disp = slotsFromCity.getOrDefault(origen.id, Collections.emptyList());
        for (flightslot s : disp) {
            // Puede ir a cualquier destino, pero solo vuelos con salida >= tActual
            if (s.salidaDia >= tActual) {
                res.add(s);
            }
        }
        return res;
    }
    
    // Devuelve todos los slots disponibles desde origen a destino dentro del plazo
    public List<flightslot> getAvailableSlots(Aeropuerto origen, Aeropuerto destino, double tInicio, double plazo) {
        List<flightslot> rutasDisponibles = new ArrayList<>();
        
        // Agregar slots directos como punto de partida
        List<flightslot> directos = slotsDirectos(origen, destino, tInicio);
        for (flightslot slot : directos) {
            if (slot.llegadaDia <= plazo) {
                rutasDisponibles.add(slot);
            }
        }
        
        // Buscar rutas con una escala
        List<flightslot> iniciales = slotsFrom(origen, tInicio);
        for (flightslot primerVuelo : iniciales) {
            if (primerVuelo.llegadaDia < plazo) {
                Aeropuerto escala = primerVuelo.flight.destino;
                if (escala.id != destino.id) {
                    List<flightslot> conexiones = slotsDirectos(escala, destino, primerVuelo.llegadaDia);
                    for (flightslot segundoVuelo : conexiones) {
                        if (segundoVuelo.llegadaDia <= plazo) {
                            rutasDisponibles.add(primerVuelo);
                            rutasDisponibles.add(segundoVuelo);
                        }
                    }
                }
            }
        }
        
        return rutasDisponibles;
    }

    // En flightnetwork.java (agrega dentro de la clase)

public List<flightslot> outgoingFrom(Aeropuerto origen, double tMin, double tMax) {
    List<flightslot> res = new ArrayList<>();
    List<flightslot> lista = slotsFromCity.getOrDefault(origen.id, Collections.emptyList());
    for (flightslot s : lista) {
        if (s.salidaDia >= tMin && s.llegadaDia <= tMax && s.capacidadRestante > 0) {
            res.add(s);
        }
    }
    // ordenados por salida (por prolijidad)
    res.sort(Comparator.comparingDouble(x -> x.salidaDia));
    return res;
}

/** Chequeo rápido de viabilidad: ¿puedo llegar desde 'intermedio' al 'destino'
 *  antes de 'deadline' con 0 o 1 conexiones? (suficiente para tu caso actual).
 */
public boolean existeRuta(Aeropuerto intermedio, Aeropuerto destino, double tActual, double deadline) {
    // Directo
    for (flightslot s1 : outgoingFrom(intermedio, tActual, deadline)) {
        if (s1.flight.destino.id == destino.id && s1.llegadaDia <= deadline) return true;
    }
    // Con una escala
    for (flightslot s1 : outgoingFrom(intermedio, tActual, deadline)) {
        for (flightslot s2 : outgoingFrom(s1.flight.destino, s1.llegadaDia, deadline)) {
            if (s2.flight.destino.id == destino.id && s2.llegadaDia <= deadline) return true;
        }
    }
    return false;
}





}