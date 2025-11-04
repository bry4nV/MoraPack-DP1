package pe.edu.pucp.morapack;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.morapack.algos.data.DataLoader;
import pe.edu.pucp.morapack.algos.entities.PlannerAirport;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that airport coordinates are being parsed correctly from DMS format
 */
public class CoordinateParseTest {

    @Test
    public void testLimaCoordinates() throws IOException {
        // Load airports from airports_real.txt
        List<PlannerAirport> airports = DataLoader.loadAirports("data/airports_real.txt");
        
        // Find Lima (SPIM)
        PlannerAirport lima = airports.stream()
            .filter(a -> a.getCode().equals("SPIM"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(lima, "Lima airport (SPIM) should be found");
        
        System.out.println("\n=== LIMA AIRPORT (SPIM) ===");
        System.out.println("Name: " + lima.getName());
        System.out.println("Latitude: " + lima.getLatitude());
        System.out.println("Longitude: " + lima.getLongitude());
        System.out.println("GMT: " + lima.getGmt());
        
        // Expected coordinates (from airports_real.txt):
        // Latitude: 12° 01' 19" S = -12.0219
        // Longitude: 77° 06' 52" W = -77.1144
        
        // Verify latitude is approximately -12.0219 (South)
        assertTrue(lima.getLatitude() < 0, "Lima latitude should be negative (South)");
        assertTrue(Math.abs(lima.getLatitude() + 12.0219) < 0.01, 
            "Lima latitude should be approximately -12.0219, got: " + lima.getLatitude());
        
        // Verify longitude is approximately -77.1144 (West)
        assertTrue(lima.getLongitude() < 0, "Lima longitude should be negative (West)");
        assertTrue(Math.abs(lima.getLongitude() + 77.1144) < 0.01,
            "Lima longitude should be approximately -77.1144, got: " + lima.getLongitude());
        
        System.out.println("✅ Lima coordinates are CORRECT!");
    }
    
    @Test
    public void testBrusselsCoordinates() throws IOException {
        List<PlannerAirport> airports = DataLoader.loadAirports("data/airports_real.txt");
        
        PlannerAirport brussels = airports.stream()
            .filter(a -> a.getCode().equals("EBCI"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(brussels, "Brussels airport (EBCI) should be found");
        
        System.out.println("\n=== BRUSSELS AIRPORT (EBCI) ===");
        System.out.println("Name: " + brussels.getName());
        System.out.println("Latitude: " + brussels.getLatitude());
        System.out.println("Longitude: " + brussels.getLongitude());
        
        // Expected: Latitude: 50° 54' 07" N = 50.9019
        //           Longitude: 04° 29' 09" E = 4.4858
        
        assertTrue(brussels.getLatitude() > 0, "Brussels latitude should be positive (North)");
        assertTrue(Math.abs(brussels.getLatitude() - 50.9019) < 0.01,
            "Brussels latitude should be approximately 50.9019, got: " + brussels.getLatitude());
        
        assertTrue(brussels.getLongitude() > 0, "Brussels longitude should be positive (East)");
        assertTrue(Math.abs(brussels.getLongitude() - 4.4858) < 0.01,
            "Brussels longitude should be approximately 4.4858, got: " + brussels.getLongitude());
        
        System.out.println("✅ Brussels coordinates are CORRECT!");
    }
    
    @Test
    public void testAllAirportsHaveValidCoordinates() throws IOException {
        List<PlannerAirport> airports = DataLoader.loadAirports("data/airports_real.txt");
        
        System.out.println("\n=== ALL AIRPORTS COORDINATES ===");
        
        int parseErrors = 0;
        
        for (PlannerAirport airport : airports) {
            System.out.printf("%s (%s): lat=%.4f, lon=%.4f%n", 
                airport.getCode(), 
                airport.getName(),
                airport.getLatitude(),
                airport.getLongitude());
            
            // Check if coordinates are valid (not 0,0 which would indicate parse failure)
            if (airport.getLatitude() == 0.0 && airport.getLongitude() == 0.0) {
                System.out.println("  ⚠️  WARNING: Coordinates are 0,0 (possible parse error)");
                parseErrors++;
            }
            
            // Check if coordinates are in valid range
            assertTrue(airport.getLatitude() >= -90 && airport.getLatitude() <= 90,
                "Invalid latitude for " + airport.getCode() + ": " + airport.getLatitude());
            assertTrue(airport.getLongitude() >= -180 && airport.getLongitude() <= 180,
                "Invalid longitude for " + airport.getCode() + ": " + airport.getLongitude());
        }
        
        System.out.println("\n✅ All airports: " + airports.size());
        System.out.println("⚠️  Parse errors: " + parseErrors);
        
        assertEquals(0, parseErrors, "Some airports failed to parse coordinates");
    }
}



