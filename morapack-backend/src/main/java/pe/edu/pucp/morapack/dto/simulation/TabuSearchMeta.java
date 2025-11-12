package pe.edu.pucp.morapack.dto.simulation;

public class TabuSearchMeta {
    public int iteration;
    public double bestCost;
    public boolean running;
    public long snapshotId;

    public TabuSearchMeta() {}
    public TabuSearchMeta(int iteration, double bestCost, boolean running, long snapshotId) {
        this.iteration = iteration;
        this.bestCost = bestCost;
        this.running = running;
        this.snapshotId = snapshotId;
    }
}







