package pe.edu.pucp.morapack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig.ScenarioType;

/**
 * Tests for ScenarioConfig and the K parameter concept
 */
class ScenarioConfigTest {
    
    @Test
    void testDailyScenarioHasCorrectK() {
        ScenarioConfig config = ScenarioConfig.daily();
        
        assertEquals(ScenarioType.DAILY, config.getType());
        assertEquals(1, config.getMinutesToConsume(), "Daily operations should have K=1 (real-time)");
        assertTrue(config.shouldPersistToDatabase(), "Daily operations should persist to database");
        assertTrue(config.isRealTime(), "Daily should be marked as real-time");
        assertFalse(config.isSimulation(), "Daily should not be a simulation");
    }
    
    @Test
    void testWeeklyScenarioHasCorrectK() {
        ScenarioConfig config = ScenarioConfig.weekly();
        
        assertEquals(ScenarioType.WEEKLY, config.getType());
        assertEquals(60, config.getMinutesToConsume(), "Weekly simulation should have K=60 minutes");
        assertEquals(10080, config.getTotalDurationMinutes(), "Weekly should last 7 days (10,080 minutes)");
        assertFalse(config.shouldPersistToDatabase(), "Simulations should not persist to database");
        assertFalse(config.isRealTime(), "Weekly should not be real-time");
        assertTrue(config.isSimulation(), "Weekly should be a simulation");
    }
    
    @Test
    void testCollapseScenarioHasCorrectK() {
        ScenarioConfig config = ScenarioConfig.collapse();
        
        assertEquals(ScenarioType.COLLAPSE, config.getType());
        assertEquals(1440, config.getMinutesToConsume(), "Collapse simulation should have K=1440 (full days)");
        assertFalse(config.shouldPersistToDatabase(), "Collapse simulation should not persist");
        assertTrue(config.isSimulation(), "Collapse should be a simulation");
    }
    
    @Test
    void testCustomScenario() {
        int customK = 120; // 2 hours
        int customDuration = 5040; // 3.5 days
        
        ScenarioConfig config = ScenarioConfig.custom(customK, customDuration, false);
        
        assertEquals(customK, config.getMinutesToConsume());
        assertEquals(customDuration, config.getTotalDurationMinutes());
        assertFalse(config.shouldPersistToDatabase());
    }
    
    @Test
    void testKParameterDeterminesSimulationSpeed() {
        // The K parameter is THE key difference between scenarios
        
        ScenarioConfig daily = ScenarioConfig.daily();
        ScenarioConfig weekly = ScenarioConfig.weekly();
        ScenarioConfig collapse = ScenarioConfig.collapse();
        
        // K increases from daily → weekly → collapse
        assertTrue(daily.getMinutesToConsume() < weekly.getMinutesToConsume(),
            "Weekly should consume more minutes per iteration than daily");
        assertTrue(weekly.getMinutesToConsume() < collapse.getMinutesToConsume(),
            "Collapse should consume more minutes per iteration than weekly");
        
        // Ratio comparison
        int dailyK = daily.getMinutesToConsume();
        int weeklyK = weekly.getMinutesToConsume();
        int collapseK = collapse.getMinutesToConsume();
        
        System.out.println("\n📊 K Parameter Comparison:");
        System.out.println(String.format("   Daily (Real-time):    K = %4d minute  (1x)", dailyK));
        System.out.println(String.format("   Weekly (Simulation):  K = %4d minutes (%dx faster)", weeklyK, weeklyK / dailyK));
        System.out.println(String.format("   Collapse (Stress):    K = %4d minutes (%dx faster)", collapseK, collapseK / dailyK));
        System.out.println("\n   K controls how fast the simulation advances.");
        System.out.println("   Higher K = More data consumed per iteration = Faster simulation");
    }
    
    @Test
    void testScenarioConfigToString() {
        ScenarioConfig config = ScenarioConfig.weekly();
        String str = config.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("WEEKLY"));
        assertTrue(str.contains("60")); // K value
    }
}

