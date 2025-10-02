package pe.edu.pucp.morapack.algos.algorithm.tabu;

import pe.edu.pucp.morapack.model.Flight;
import pe.edu.pucp.morapack.model.Shipment;
import java.util.Arrays;

/**
 * Representa un movimiento en el algoritmo Tabú Search.
 * Un movimiento consiste en la asignación o reasignación de un envío a una ruta.
 */
public class TabuMove {
    private final Shipment shipment;
    private final Flight[] oldRoute;
    private final Flight[] newRoute;
    private final int iteration;

    public TabuMove(Shipment shipment, Flight[] oldRoute, Flight[] newRoute, int iteration) {
        this.shipment = shipment;
        this.oldRoute = oldRoute != null ? oldRoute.clone() : null;
        this.newRoute = newRoute != null ? newRoute.clone() : null;
        this.iteration = iteration;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public Flight[] getOldRoute() {
        return oldRoute != null ? oldRoute.clone() : null;
    }

    public Flight[] getNewRoute() {
        return newRoute != null ? newRoute.clone() : null;
    }

    public int getIteration() {
        return iteration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TabuMove other = (TabuMove) o;
        return shipment.equals(other.shipment) && 
               areRoutesEqual(oldRoute, other.oldRoute) &&
               areRoutesEqual(newRoute, other.newRoute);
    }

    @Override
    public int hashCode() {
        int result = shipment.hashCode();
        result = 31 * result + (oldRoute != null ? Arrays.hashCode(oldRoute) : 0);
        result = 31 * result + (newRoute != null ? Arrays.hashCode(newRoute) : 0);
        return result;
    }

    private boolean areRoutesEqual(Flight[] route1, Flight[] route2) {
        if (route1 == route2) return true;
        if (route1 == null || route2 == null) return false;
        if (route1.length != route2.length) return false;
        
        for (int i = 0; i < route1.length; i++) {
            if (!route1[i].equals(route2[i])) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TabuMove{shipment=").append(shipment.getId());
        
        sb.append(", oldRoute=[");
        if (oldRoute != null) {
            for (int i = 0; i < oldRoute.length; i++) {
                if (i > 0) sb.append(" -> ");
                sb.append(oldRoute[i].getCode());
            }
        }
        sb.append("]");

        sb.append(", newRoute=[");
        if (newRoute != null) {
            for (int i = 0; i < newRoute.length; i++) {
                if (i > 0) sb.append(" -> ");
                sb.append(newRoute[i].getCode());
            }
        }
        sb.append("]");

        sb.append(", iteration=").append(iteration);
        sb.append("}");
        return sb.toString();
    }
}