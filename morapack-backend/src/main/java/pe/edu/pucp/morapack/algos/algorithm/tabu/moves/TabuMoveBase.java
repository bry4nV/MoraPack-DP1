package pe.edu.pucp.morapack.algos.algorithm.tabu.moves;

import pe.edu.pucp.morapack.algos.algorithm.tabu.TabuSolution;

/**
 * Clase base abstracta para todos los movimientos del Tabu Search.
 */
public abstract class TabuMoveBase {
    protected String moveType;
    
    public TabuMoveBase(String moveType) {
        this.moveType = moveType;
    }
    
    /**
     * Aplicar el movimiento a la solución (modifica la solución)
     */
    public abstract void apply(TabuSolution solution);
    
    /**
     * Tipo de movimiento (para lista tabú)
     */
    public String getMoveType() {
        return moveType;
    }
    
    /**
     * Clave única del movimiento (para lista tabú)
     */
    public abstract String getMoveKey();
    
    @Override
    public abstract String toString();
}

