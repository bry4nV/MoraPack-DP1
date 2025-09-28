
package pe.edu.pucp.morapack.algos.algorithm.aco;

public class flightslot{
    public final flight flight;
    public final double salidaDia;
    public final double llegadaDia;
    public int capacidadRestante;

    public flightslot(flight flight, double salidaDia) {
        this.flight = flight;
        this.salidaDia = salidaDia;
        this.llegadaDia = salidaDia + flight.duracionDias;
        this.capacidadRestante = flight.capacidadMax;
    }
}
