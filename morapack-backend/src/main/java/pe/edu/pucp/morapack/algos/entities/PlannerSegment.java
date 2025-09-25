package pe.edu.pucp.morapack.algos.common;

import pe.edu.pucp.morapack.model.flight.Flight;
import java.util.Objects;

public class Segment {
    private Flight flight;
    
    public Segment(Flight flight) {
        this.flight = flight;
    }
    
    public Segment(Segment other) {
        this.flight = other.flight;
    }
    
    public Flight getFlight() {
        return flight;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(flight, ((Segment) o).flight);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(flight);
    }
}