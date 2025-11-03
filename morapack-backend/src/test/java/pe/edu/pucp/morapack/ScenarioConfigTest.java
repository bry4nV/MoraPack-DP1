package pe.edu.pucp.morapack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig.ScenarioType;

/**
 * Tests for ScenarioConfig with Ta, Sa, K, and Sc parameters
 */
class ScenarioConfigTest {
    
    @Test
    void testDailyScenarioHasCorrectParameters() {
        ScenarioConfig config = ScenarioConfig.daily();
        
        assertEquals(ScenarioType.DAILY, config.getType());
        assertEquals(1, config.getTaMinutes(), "Ta should be 1 minute (fixed)");
        assertEquals(5, config.getSaMinutes(), "Sa should be 5 minutes (fixed)");
        assertEquals(1, config.getK(), "Daily operations should have K=1");
        assertEquals(5, config.getScMinutes(), "Sc = K × Sa = 1 × 5 = 5 minutes");
        assertTrue(config.shouldPersistToDatabase(), "Daily operations should persist to database");
        assertTrue(config.isRealTime(), "Daily should be marked as real-time");
        assertFalse(config.isSimulation(), "Daily should not be a simulation");
    }
    
    @Test
    void testWeeklyScenarioHasCorrectParameters() {
        ScenarioConfig config = ScenarioConfig.weekly();
        
        assertEquals(ScenarioType.WEEKLY, config.getType());
        assertEquals(1, config.getTaMinutes(), "Ta should be 1 minute (fixed)");
        assertEquals(5, config.getSaMinutes(), "Sa should be 5 minutes (fixed)");
        assertEquals(14, config.getK(), "Weekly simulation default should have K=14");
        assertEquals(70, config.getScMinutes(), "Sc = K × Sa = 14 × 5 = 70 minutes");
        assertEquals(10080, config.getTotalDurationMinutes(), "Weekly should last 7 days (10,080 minutes)");
        assertFalse(config.shouldPersistToDatabase(), "Simulations should not persist to database");
        assertFalse(config.isRealTime(), "Weekly should not be real-time");
        assertTrue(config.isSimulation(), "Weekly should be a simulation");
    }
    
    @Test
    void testCollapseScenarioHasCorrectParameters() {
        ScenarioConfig config = ScenarioConfig.collapse();
        
        assertEquals(ScenarioType.COLLAPSE, config.getType());
        assertEquals(1, config.getTaMinutes(), "Ta should be 1 minute (fixed)");
        assertEquals(5, config.getSaMinutes(), "Sa should be 5 minutes (fixed)");
        assertEquals(75, config.getK(), "Collapse simulation should have K=75");
        assertEquals(375, config.getScMinutes(), "Sc = K × Sa = 75 × 5 = 375 minutes (6.25 hours)");
        assertFalse(config.shouldPersistToDatabase(), "Collapse simulation should not persist");
        assertTrue(config.isSimulation(), "Collapse should be a simulation");
    }
    
    @Test
    void testCustomScenario() {
        int customK = 24; // Similar to PDDS-VRP
        int customDuration = 10080; // 7 days
        
        ScenarioConfig config = ScenarioConfig.custom(customK, customDuration, false);
        
        assertEquals(customK, config.getK());
        assertEquals(120, config.getScMinutes(), "Sc = 24 × 5 = 120 minutes");
        assertEquals(customDuration, config.getTotalDurationMinutes());
        assertFalse(config.shouldPersistToDatabase());
    }
    
    @Test
    void testWeeklyWithCustomK() {
        // Test different K values for weekly simulation
        ScenarioConfig k14 = ScenarioConfig.weekly(14); // Default
        ScenarioConfig k20 = ScenarioConfig.weekly(20); // Faster
        ScenarioConfig k24 = ScenarioConfig.weekly(24); // Similar to PDDS-VRP
        
        assertEquals(14, k14.getK());
        assertEquals(70, k14.getScMinutes());
        
        assertEquals(20, k20.getK());
        assertEquals(100, k20.getScMinutes());
        
        assertEquals(24, k24.getK());
        assertEquals(120, k24.getScMinutes(), "K=24 gives Sc=120, similar to PDDS-VRP");
    }
    
    @Test
    void testKParameterDeterminesSimulationSpeed() {
        // The K parameter is THE key difference between scenarios
        
        ScenarioConfig daily = ScenarioConfig.daily();
        ScenarioConfig weekly = ScenarioConfig.weekly();
        ScenarioConfig collapse = ScenarioConfig.collapse();
        
        // K increases from daily → weekly → collapse
        assertTrue(daily.getK() < weekly.getK(),
            "Weekly should have higher K than daily");
        assertTrue(weekly.getK() < collapse.getK(),
            "Collapse should have higher K than weekly");
        
        // Sc (consumption) also increases
        assertTrue(daily.getScMinutes() < weekly.getScMinutes(),
            "Weekly should consume more minutes per iteration than daily");
        assertTrue(weekly.getScMinutes() < collapse.getScMinutes(),
            "Collapse should consume more minutes per iteration than weekly");
        
        // Display comparison
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║               K PARAMETER COMPARISON                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println(String.format("   Daily (Real-time):    K=%2d  →  Sc=%3d min  (1x)", 
            daily.getK(), daily.getScMinutes()));
        System.out.println(String.format("   Weekly (Simulation):  K=%2d  →  Sc=%3d min  (%dx faster)", 
            weekly.getK(), weekly.getScMinutes(), weekly.getK() / daily.getK()));
        System.out.println(String.format("   Collapse (Stress):    K=%2d  →  Sc=%3d min  (%dx faster)", 
            collapse.getK(), collapse.getScMinutes(), collapse.getK() / daily.getK()));
        System.out.println("\n   Formula: Sc = K × Sa  (Sa=5 minutes fixed)");
        System.out.println("   Higher K = Fewer iterations = Faster simulation");
    }
    
    @Test
    void testWeeklyIterationCalculations() {
        // Test how many iterations different K values produce for 7 days
        int sevenDaysMinutes = 10080;
        
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║         WEEKLY SIMULATION: K VALUE COMPARISON (7 days)        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        int[] testKValues = {10, 14, 20, 24, 30};
        for (int k : testKValues) {
            ScenarioConfig config = ScenarioConfig.weekly(k);
            int sc = config.getScMinutes();
            int iterations = sevenDaysMinutes / sc;
            
            System.out.println(String.format("   K=%2d  →  Sc=%3d min  →  %3d iterations", k, sc, iterations));
        }
        System.out.println("\n   PDDS-VRP Reference: 120 min per iteration → 84 iterations");
        System.out.println("   (Equivalent to K=24 in our model)");
    }
    
    @Test
    void testScenarioConfigToString() {
        ScenarioConfig config = ScenarioConfig.weekly();
        String str = config.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("WEEKLY"));
        assertTrue(str.contains("K=14"), "Should display K value");
        assertTrue(str.contains("Sc=70"), "Should display Sc value");
    }
}

