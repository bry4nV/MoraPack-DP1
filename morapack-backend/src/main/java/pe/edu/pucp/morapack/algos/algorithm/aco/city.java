
package MoraTravel;


public class city {
    final int id; 
    final String nombre; 
    final String continente; 
    final int capacidadAlmacen;
        
        public city(int id, String nombre, String continente, int capacidadAlmacen) {
            this.id = id; 
            this.nombre = nombre; 
            this.continente = continente; 
            this.capacidadAlmacen = capacidadAlmacen;
        }
        
        public boolean mismoContinente(city otra) { 
            return this.continente.equalsIgnoreCase(otra.continente); 
        }
}
