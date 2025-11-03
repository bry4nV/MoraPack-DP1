package pe.edu.pucp.morapack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import pe.edu.pucp.morapack.algos.entities.PlannerAirport;
import pe.edu.pucp.morapack.algos.utils.AirportStorageManager;
import pe.edu.pucp.morapack.model.Country;
import pe.edu.pucp.morapack.model.Continent;

import java.util.Arrays;
import java.util.List;

/**
 * Tests unitarios para AirportStorageManager
 */
class AirportStorageManagerTest {
    
    private AirportStorageManager manager;
    private PlannerAirport airport1;
    private PlannerAirport airport2;
    
    @BeforeEach
    void setUp() {
        // Crear países de prueba
        Country peru = new Country(1, "Peru", Continent.AMERICA);
        Country belgium = new Country(2, "Belgium", Continent.EUROPE);
        
        airport1 = new PlannerAirport(1, "TEST1", "Test Airport 1", "City1", peru, 500, -5);
        airport2 = new PlannerAirport(2, "TEST2", "Test Airport 2", "City2", belgium, 300, +2);
        
        List<PlannerAirport> airports = Arrays.asList(airport1, airport2);
        manager = new AirportStorageManager(airports);
    }
    
    @Test
    void testInitialCapacityIsZero() {
        // Al inicio, todos los aeropuertos deben estar vacíos
        assertTrue(manager.hasAvailableCapacity(airport1, 500));
        assertEquals(500, manager.getAvailableCapacity(airport1));
        assertEquals(0.0, manager.getUtilizationPercentage(airport1), 0.01);
    }
    
    @Test
    void testAddProductsSuccessfully() {
        // Añadir productos dentro de capacidad
        manager.add(airport1, 200);
        
        assertEquals(300, manager.getAvailableCapacity(airport1));
        assertEquals(40.0, manager.getUtilizationPercentage(airport1), 0.01);
    }
    
    @Test
    void testCannotExceedCapacity() {
        // Intentar añadir más de la capacidad debe fallar
        assertThrows(IllegalStateException.class, () -> {
            manager.add(airport1, 600);  // Capacidad es 500
        });
    }
    
    @Test
    void testCannotExceedCapacityGradually() {
        // Añadir gradualmente hasta llenar
        manager.add(airport1, 300);
        manager.add(airport1, 200);
        
        // Ahora está lleno (500/500)
        assertFalse(manager.hasAvailableCapacity(airport1, 1));
        
        // Intentar añadir 1 más debe fallar
        assertThrows(IllegalStateException.class, () -> {
            manager.add(airport1, 1);
        });
    }
    
    @Test
    void testRemoveProducts() {
        manager.add(airport1, 300);
        manager.remove("TEST1", 100);
        
        assertEquals(300, manager.getAvailableCapacity(airport1));
    }
    
    @Test
    void testCannotRemoveMoreThanPresent() {
        manager.add(airport1, 100);
        
        // Intentar remover más de lo que hay debe fallar
        assertThrows(IllegalStateException.class, () -> {
            manager.remove("TEST1", 200);
        });
    }
    
    @Test
    void testReserveCapacity() {
        // Reservar capacidad
        assertTrue(manager.reserveCapacity(airport1, 200));
        
        // La capacidad reservada reduce la disponible
        assertEquals(300, manager.getAvailableCapacity(airport1));
        
        // No se puede reservar más allá de la capacidad total
        assertFalse(manager.reserveCapacity(airport1, 400));
    }
    
    @Test
    void testClone() {
        manager.add(airport1, 200);
        manager.reserveCapacity(airport2, 100);
        
        // Clonar el manager
        AirportStorageManager clone = manager.clone();
        
        // El clon debe tener el mismo estado
        assertEquals(300, clone.getAvailableCapacity(airport1));
        assertEquals(200, clone.getAvailableCapacity(airport2));
        
        // Modificar el clon NO debe afectar al original
        clone.add(airport1, 100);
        assertEquals(300, manager.getAvailableCapacity(airport1));  // Original sin cambios
        assertEquals(200, clone.getAvailableCapacity(airport1));     // Clon modificado
    }
    
    @Test
    void testReset() {
        manager.add(airport1, 200);
        manager.reserveCapacity(airport2, 100);
        
        manager.reset();
        
        // Después de reset, todo debe estar vacío
        assertEquals(500, manager.getAvailableCapacity(airport1));
        assertEquals(300, manager.getAvailableCapacity(airport2));
    }
    
    @Test
    void testMultipleAirports() {
        // Verificar que múltiples aeropuertos se manejan independientemente
        manager.add(airport1, 200);
        manager.add(airport2, 150);
        
        assertEquals(300, manager.getAvailableCapacity(airport1));
        assertEquals(150, manager.getAvailableCapacity(airport2));
    }
    
    @Test
    void testGetSnapshot() {
        manager.add(airport1, 200);
        manager.reserveCapacity(airport1, 100);
        
        var snapshot = manager.getSnapshot();
        
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey("TEST1"));
        
        // Snapshot debería mostrar formato: current+reserved/max
        String info = snapshot.get("TEST1");
        assertTrue(info.contains("200")); // current
        assertTrue(info.contains("100")); // reserved
        assertTrue(info.contains("500")); // max
    }
}

