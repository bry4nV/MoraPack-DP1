package pe.edu.pucp.morapack.algos.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PlannerRoute {
    private List<PlannerSegment> segments = new ArrayList<>();
    
    public PlannerRoute() {}
    
    public PlannerRoute(PlannerRoute other) {
        this.segments = other.segments.stream()
            .map(PlannerSegment::new)
            .collect(Collectors.toList());
    }
    
    public List<PlannerSegment> getSegments() {
        return segments;
    }
    
    public LocalDateTime getInitialDepartureTime() {
        return segments.isEmpty() ? null : segments.get(0).getFlight().getDepartureTime();
    }
    
    public LocalDateTime getFinalArrivalTime() {
        return segments.isEmpty() ? null : segments.get(segments.size() - 1).getFlight().getArrivalTime();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(segments, ((PlannerRoute) o).segments);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }
}