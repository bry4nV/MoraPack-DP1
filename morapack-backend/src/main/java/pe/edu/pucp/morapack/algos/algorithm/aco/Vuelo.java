
package pe.edu.pucp.morapack.algos.algorithm.aco;


public class Vuelo {
        public static final double INTRA_CONTINENT_DURATION = 0.5;
        public static final double INTER_CONTINENT_DURATION = 1.0;
    
    
        final int id; 
        final Aeropuerto origen; 
        final Aeropuerto destino; 
        final int capacidadMax; 
        final int frecuenciaPorDia;
        final double duracionDias;

        public Vuelo(int id, Aeropuerto origen, Aeropuerto destino, int capacidadMax, int frecuenciaPorDia) {
            this.id = id;
            this.origen = origen;
            this.destino = destino;
            this.capacidadMax = capacidadMax;
            this.frecuenciaPorDia = Math.max(1, frecuenciaPorDia);
            // Si los continentes son distintos, es intercontinental
            boolean intercontinental = !origen.continente.equals(destino.continente);
            this.duracionDias = intercontinental ? INTER_CONTINENT_DURATION : INTRA_CONTINENT_DURATION;
        }
}
      
    