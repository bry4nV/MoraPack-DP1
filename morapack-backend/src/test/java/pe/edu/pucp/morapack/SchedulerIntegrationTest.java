package pe.edu.pucp.morapack;

import org.junit.jupiter.api.*;
import pe.edu.pucp.morapack.algos.entities.PlannerShipment;
import pe.edu.pucp.morapack.algos.scheduler.FileDataProvider;
import pe.edu.pucp.morapack.algos.scheduler.PlannerScheduler;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PlannerScheduler with FileDataProvider.
 * 
 * Tests the complete workflow:
 * 1. FileDataProvider loads data from CSVs
 * 2. PlannerScheduler orchestrates iterations with concept K
 * 3. TabuSearchPlanner assigns orders to flights
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchedulerIntegrationTest {
    
    private static final String DATA_DIR = "data/";
    private FileDataProvider dataProvider;
    
    @BeforeAll
    static void setup() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     SCHEDULER INTEGRATION TEST - FILE DATA PROVIDER           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        // Verify data files exist
        File airportsFile = new File(DATA_DIR + "airports.txt");
        File flightsFile = new File(DATA_DIR + "flights.csv");
        File ordersFile = new File(DATA_DIR + "pedidos_generados.csv");
        
        assertTrue(airportsFile.exists(), "airports.txt not found");
        assertTrue(flightsFile.exists(), "flights.csv not found");
        assertTrue(ordersFile.exists(), "pedidos_generados.csv not found");
        
        System.out.println("✅ All data files found\n");
    }
    
    @BeforeEach
    void loadData() throws Exception {
        System.out.println("\n[LOADING DATA] Creating FileDataProvider...\n");
        
        // Create FileDataProvider for December 2025 (7 days for faster testing)
        dataProvider = new FileDataProvider(
            DATA_DIR + "airports.txt",
            DATA_DIR + "flights.csv",
            DATA_DIR + "pedidos_generados.csv",
            2025,  // Year
            12,    // Month (December)
            7      // Days (1 week for testing)
        );
        
        System.out.println("[DATA LOADED] FileDataProvider ready\n");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: FileDataProvider loads data correctly")
    void testFileDataProviderLoadsData() {
        System.out.println("\n[TEST 1] Verifying FileDataProvider...\n");
        
        // Verify airports
        assertNotNull(dataProvider.getAirports());
        assertFalse(dataProvider.getAirports().isEmpty());
        System.out.println("✅ Airports loaded: " + dataProvider.getAirports().size());
        
        // Verify flights
        assertNotNull(dataProvider.getAllFlights());
        assertFalse(dataProvider.getAllFlights().isEmpty());
        System.out.println("✅ Flights loaded: " + dataProvider.getAllFlights().size());
        
        // Verify orders
        assertNotNull(dataProvider.getAllOrders());
        assertFalse(dataProvider.getAllOrders().isEmpty());
        System.out.println("✅ Orders loaded: " + dataProvider.getAllOrders().size());
        
        System.out.println("\n✅ TEST 1 PASSED: FileDataProvider working correctly\n");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: FileDataProvider filters by time window")
    void testFileDataProviderFiltersTimeWindow() {
        System.out.println("\n[TEST 2] Testing time window filtering...\n");
        
        // Query first day only (December 1, 2025)
        LocalDateTime dayStart = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2025, 12, 2, 0, 0);
        
        var flightsDay1 = dataProvider.getFlights(dayStart, dayEnd);
        var ordersDay1 = dataProvider.getOrders(dayStart, dayEnd);
        
        System.out.println("   Day 1 Flights: " + flightsDay1.size());
        System.out.println("   Day 1 Orders:  " + ordersDay1.size());
        
        // Verify filtering works
        assertTrue(flightsDay1.size() > 0, "Should have flights on day 1");
        assertTrue(ordersDay1.size() > 0, "Should have orders on day 1");
        
        // Verify all flights are within time window
        for (var flight : flightsDay1) {
            assertTrue(!flight.getDepartureTime().isBefore(dayStart), 
                "Flight departure before window start");
            assertTrue(flight.getDepartureTime().isBefore(dayEnd), 
                "Flight departure after window end");
        }
        
        // Verify all orders are within time window
        for (var order : ordersDay1) {
            assertTrue(!order.getOrderTime().isBefore(dayStart), 
                "Order time before window start");
            assertTrue(order.getOrderTime().isBefore(dayEnd), 
                "Order time after window end");
        }
        
        System.out.println("\n✅ TEST 2 PASSED: Time window filtering works correctly\n");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: PlannerScheduler with DAILY scenario (K=1, Sc=5)")
    void testSchedulerDailyScenario() {
        System.out.println("\n[TEST 3] Testing DAILY scenario (K=1, Sc=5 minutes)...\n");
        
        // Run scheduler for first 4 hours of December 1
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 1, 4, 0);  // 4 hours = 240 minutes
        
        PlannerScheduler scheduler = new PlannerScheduler(
            dataProvider,
            ScenarioConfig.daily(),  // K=1, Sc = 1 × 5 = 5 minutes
            start,
            end
        );
        
        List<PlannerShipment> shipments = scheduler.runSimulation();
        
        System.out.println("\n📊 DAILY SCENARIO RESULTS:");
        System.out.println("   Iterations: " + scheduler.getIterationCount());
        System.out.println("   Expected:   48 (240 minutes / 5 min per iteration)");
        System.out.println("   Shipments:  " + shipments.size());
        
        // Verify scheduler ran correct number of iterations
        // 4 hours = 240 minutes / Sc=5 minutes = 48 iterations
        assertEquals(48, scheduler.getIterationCount(), 
            "Should have 48 iterations (240 min / 5 min per iter)");
        
        System.out.println("\n✅ TEST 3 PASSED: DAILY scenario works correctly\n");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: PlannerScheduler with WEEKLY scenario (K=14, Sc=70)")
    void testSchedulerWeeklyScenario() {
        System.out.println("\n[TEST 4] Testing WEEKLY scenario (K=14, Sc=70 minutes)...\n");
        
        // Run scheduler for first 2 days of December
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 3, 0, 0);  // 2 days = 2880 minutes
        
        PlannerScheduler scheduler = new PlannerScheduler(
            dataProvider,
            ScenarioConfig.weekly(),  // K=14, Sc = 14 × 5 = 70 minutes
            start,
            end
        );
        
        List<PlannerShipment> shipments = scheduler.runSimulation();
        
        System.out.println("\n📊 WEEKLY SCENARIO RESULTS:");
        System.out.println("   Iterations: " + scheduler.getIterationCount());
        System.out.println("   Expected:   42 (2880 minutes / 70 min per iteration)");
        System.out.println("   Shipments:  " + shipments.size());
        
        // Verify scheduler ran correct number of iterations
        // 2 days = 2880 minutes / Sc=70 minutes = 41.14 ≈ 42 iterations
        assertEquals(42, scheduler.getIterationCount(), 
            "Should have 42 iterations (2880 min / 70 min per iter)");
        
        // Verify shipments were created
        assertTrue(shipments.size() > 0, "Should have created shipments");
        
        System.out.println("\n✅ TEST 4 PASSED: WEEKLY scenario works correctly\n");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: PlannerScheduler with COLLAPSE scenario (K=75, Sc=375)")
    void testSchedulerCollapseScenario() {
        System.out.println("\n[TEST 5] Testing COLLAPSE scenario (K=75, Sc=375 minutes)...\n");
        
        // Run scheduler for entire week
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 8, 0, 0);  // 7 days = 10080 minutes
        
        PlannerScheduler scheduler = new PlannerScheduler(
            dataProvider,
            ScenarioConfig.collapse(),  // K=75, Sc = 75 × 5 = 375 minutes
            start,
            end
        );
        
        List<PlannerShipment> shipments = scheduler.runSimulation();
        
        System.out.println("\n📊 COLLAPSE SCENARIO RESULTS:");
        System.out.println("   Iterations: " + scheduler.getIterationCount());
        System.out.println("   Expected:   27 (10080 minutes / 375 min per iteration)");
        System.out.println("   Shipments:  " + shipments.size());
        
        // Verify scheduler ran correct number of iterations
        // 7 days = 10080 minutes / Sc=375 minutes = 26.88 ≈ 27 iterations
        assertEquals(27, scheduler.getIterationCount(), 
            "Should have 27 iterations (10080 min / 375 min per iter)");
        
        // Verify shipments were created
        assertTrue(shipments.size() > 0, "Should have created shipments");
        
        System.out.println("\n✅ TEST 5 PASSED: COLLAPSE scenario works correctly\n");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Compare scenarios (same period, different K)")
    void testCompareScenarios() {
        System.out.println("\n[TEST 6] Comparing WEEKLY vs COLLAPSE scenarios...\n");
        
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 3, 0, 0);  // 2 days = 2880 minutes
        
        // WEEKLY: K=14, Sc=70 (42 iterations)
        PlannerScheduler weeklyScheduler = new PlannerScheduler(
            dataProvider,
            ScenarioConfig.weekly(),
            start,
            end
        );
        List<PlannerShipment> weeklyShipments = weeklyScheduler.runSimulation();
        
        // COLLAPSE: K=75, Sc=375 (8 iterations)
        PlannerScheduler collapseScheduler = new PlannerScheduler(
            dataProvider,
            ScenarioConfig.collapse(),
            start,
            end
        );
        List<PlannerShipment> collapseShipments = collapseScheduler.runSimulation();
        
        System.out.println("\n📊 COMPARISON:");
        System.out.println("   WEEKLY   - Iterations: " + weeklyScheduler.getIterationCount() + ", Shipments: " + weeklyShipments.size());
        System.out.println("   COLLAPSE - Iterations: " + collapseScheduler.getIterationCount() + ", Shipments: " + collapseShipments.size());
        
        // Iterations should match Sc values
        // 2880 minutes / 70 (Sc) = 41.14 ≈ 42
        assertEquals(42, weeklyScheduler.getIterationCount());
        // 2880 minutes / 375 (Sc) = 7.68 ≈ 8
        assertEquals(8, collapseScheduler.getIterationCount());
        
        // Note: Shipment counts may differ due to different planning granularity
        System.out.println("\n   ℹ️  Note: Shipment counts may differ due to different planning granularity");
        System.out.println("           (WEEKLY: Sc=70 min chunks, COLLAPSE: Sc=375 min chunks)");
        
        System.out.println("\n✅ TEST 6 PASSED: Scenario comparison successful\n");
    }
}

