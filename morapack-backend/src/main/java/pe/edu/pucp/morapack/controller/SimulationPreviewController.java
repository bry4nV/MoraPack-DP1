package pe.edu.pucp.morapack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.simulation.SimulationPreviewResponse;
import pe.edu.pucp.morapack.service.SimulationDataService;

import java.time.LocalDate;

/**
 * REST controller for simulation preview functionality.
 * Allows users to see what orders/flights will be in the simulation BEFORE starting it.
 * CORS is configured globally in CorsConfig.java
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationPreviewController {
    
    private final SimulationDataService dataService;
    
    public SimulationPreviewController(SimulationDataService dataService) {
        this.dataService = dataService;
    }
    
    /**
     * Simple test endpoint to verify CORS is working
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("{\"message\": \"CORS is working!\"}");
    }
    
    /**
     * Get a preview of the simulation data.
     * 
     * GET /api/simulation/preview?startDate=2025-12-01&scenarioType=WEEKLY&customK=24
     * 
     * Returns:
     * - Total orders, products, flights
     * - List of orders (first 200)
     * - Statistics (orders per day, etc.)
     * - Airports involved
     */
    @GetMapping("/preview")
    public ResponseEntity<SimulationPreviewResponse> getSimulationPreview(
            @RequestParam(defaultValue = "2025-12-01") String startDate,
            @RequestParam(defaultValue = "WEEKLY") String scenarioType,
            @RequestParam(required = false) Integer customK) {
        
        try {
            System.out.println("[SimulationPreviewController] Preview request:");
            System.out.println("   Start date: " + startDate);
            System.out.println("   Scenario: " + scenarioType);
            System.out.println("   Custom K: " + customK);
            
            // Parse start date
            LocalDate start = LocalDate.parse(startDate);
            
            // Create scenario config
            ScenarioConfig scenario = createScenarioConfig(scenarioType, customK);
            
            // Build preview
            SimulationPreviewResponse preview = dataService.buildPreview(start, scenario);
            
            System.out.println("[SimulationPreviewController] Preview generated:");
            System.out.println("   Total orders: " + preview.totalPedidos);
            System.out.println("   Total products: " + preview.totalProductos);
            System.out.println("   Total flights: " + preview.totalVuelos);
            
            return ResponseEntity.ok(preview);
            
        } catch (Exception e) {
            System.err.println("[SimulationPreviewController] Error generating preview: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Clear the data cache (useful for development/testing).
     * 
     * POST /api/simulation/clear-cache
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        try {
            dataService.clearCache();
            return ResponseEntity.ok("{\"message\": \"Cache cleared successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Helper to create ScenarioConfig from request parameters.
     */
    private ScenarioConfig createScenarioConfig(String scenarioType, Integer customK) {
        return switch (scenarioType.toUpperCase()) {
            case "WEEKLY" -> customK != null ? 
                ScenarioConfig.weekly(customK) : 
                ScenarioConfig.weekly(); // Default K=24
            case "COLLAPSE" -> ScenarioConfig.collapse();
            case "DAILY" -> ScenarioConfig.daily();
            default -> ScenarioConfig.weekly();
        };
    }
}

