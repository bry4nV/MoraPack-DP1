package pe.edu.pucp.morapack.algos.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Route {
    private List<Segment> segments = new ArrayList<>();
    
    public Route() {}
    
    public Route(Route other) {
        this.segments = other.segments.stream()
            .map(Segment::new)
            .collect(Collectors.toList());
    }
    
    public List<Segment> getSegments() {
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
        return Objects.equals(segments, ((Route) o).segments);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }
}