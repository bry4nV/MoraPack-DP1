package pe.edu.pucp.morapack.algos.algorithm.tabu;

import java.time.Instant;

/**
 * Listener interface used by TabuSearchPlanner to emit snapshots and query stop requests.
 * Lightweight primitive-style contract to avoid cross-package DTO dependencies.
 */
public interface TabuSearchListener {
    /**
     * Called by the planner to publish a snapshot of the current solution.
     * Implementations should be fast (convert and forward) to avoid blocking the planner.
     *
     * @param solution current (possibly best) solution
     * @param iteration current iteration number
     * @param bestCost current best cost
     * @param snapshotId monotonic snapshot id
     * @param snapshotTime instant when snapshot was taken
     */
    void onSnapshot(TabuSolution solution, int iteration, double bestCost, long snapshotId, Instant snapshotTime);

    /**
     * Planner will check this flag and stop when true.
     */
    boolean isStopRequested();
}
