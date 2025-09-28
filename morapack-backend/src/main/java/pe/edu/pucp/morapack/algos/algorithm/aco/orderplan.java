
package pe.edu.pucp.morapack.algos.algorithm.aco;
import java.util.ArrayList;
import java.util.List;


public class orderplan {
    public final order order;
    public final List<legassign> legs = new ArrayList<>();
    public int cantidadTotalAsignada = 0;
    public double etaDias = 0.0;
    public boolean entregadoATiempo = false;

    public orderplan(order order) {
        this.order = order;
    }
}
