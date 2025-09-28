
package pe.edu.pucp.morapack.algos.algorithm.aco;

public class order {
    public final int id;
    public final Aeropuerto destino;
    public final int cantidad;
    public final double deadlineDias;
    public final Aeropuerto sedeOrigen;

    public order(int id, Aeropuerto sedeOrigen, Aeropuerto destino, int cantidad) {
        this.id = id;
        this.sedeOrigen = sedeOrigen;
        this.destino = destino;
        this.cantidad = cantidad;
        boolean inter = !sedeOrigen.continente.equals(destino.continente);
        this.deadlineDias = inter ? 3.0 : 2.0;
    }
}
