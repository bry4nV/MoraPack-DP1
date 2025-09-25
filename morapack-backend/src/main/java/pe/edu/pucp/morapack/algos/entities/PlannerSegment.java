package pe.edu.pucp.morapack.algos.entities;

import java.util.Objects;
import pe.edu.pucp.morapack.model.Flight;

public class PlannerSegment {
    private Flight flight;
    
    public PlannerSegment(Flight flight) {
        this.flight = flight;
    }
    
    public PlannerSegment(PlannerSegment other) {
        this.flight = other.flight;
    }
    
    public Flight getFlight() {
        return flight;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(flight, ((PlannerSegment) o).flight);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(flight);
    }
}