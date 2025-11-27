package pe.edu.pucp.morapack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.morapack.algos.scheduler.ScenarioConfig;
import pe.edu.pucp.morapack.dto.simulation.SimulationPreviewResponse;
import pe.edu.pucp.morapack.dto.simulation.FinalReportDTO;
import pe.edu.pucp.morapack.service.SimulationDataService;
import pe.edu.pucp.morapack.service.SimulationManager;
import pe.edu.pucp.morapack.service.SimulationSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for simulation preview functionality.
 * Allows users to see what orders/flights will be in the simulation BEFORE starting it.
 * CORS is configured globally in CorsConfig.java
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationPreviewController {

    private final SimulationDataService dataService;
    private final SimulationManager simulationManager;

    public SimulationPreviewController(
            SimulationDataService dataService,
            SimulationManager simulationManager) {
        this.dataService = dataService;
        this.simulationManager = simulationManager;
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
            System.out.println("   Total orders: " + preview.totalOrders);
            System.out.println("   Total products: " + preview.totalProducts);
            System.out.println("   Total flights: " + preview.totalFlights);
            
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
     * Obtener información de todos los vuelos con su estado actual y cancelación.
     *
     * GET /api/simulation/{userId}/flights
     *
     * Returns:
     * - Lista de vuelos con estado (ON_GROUND_ORIGIN, IN_AIR, ON_GROUND_DESTINATION)
     * - Indicador de si el vuelo está cancelado
     * - Información de salida y llegada
     */
    @GetMapping("/{userId}/flights")
    public ResponseEntity<Map<String, Object>> getFlightsStatus(@PathVariable String userId) {
        try {
            // Try to find session by userId first, then by sessionId (for shared sessions)
            SimulationSession session = simulationManager.getSession(userId);

            if (session == null) {
                // If not found by userId, try by sessionId (UUID)
                session = simulationManager.getSessionBySessionId(userId);
            }

            if (session == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "No active simulation found for: " + userId);
                return ResponseEntity.badRequest().body(error);
            }

            List<Map<String, Object>> flights = session.getFlightStatusesWithCancellation();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", flights.size());
            response.put("flights", flights);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error getting flights: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get detailed cargo information for a specific flight.
     * Returns all shipments (packages) being transported on this flight.
     */
    @GetMapping("/{userId}/flights/{flightId}/cargo")
    public ResponseEntity<Map<String, Object>> getFlightCargo(
            @PathVariable String userId,
            @PathVariable String flightId) {
        try {
            SimulationSession session = simulationManager.getSession(userId);

            if (session == null) {
                session = simulationManager.getSessionBySessionId(userId);
            }

            if (session == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "No active simulation found for: " + userId);
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> cargoDetails = session.getFlightCargoDetails(flightId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(cargoDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error getting flight cargo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get final simulation report with all metrics.
     * This endpoint is called when the simulation finishes or when the user requests a summary.
     */
    @GetMapping("/{userId}/final-report")
    public ResponseEntity<FinalReportDTO> getFinalReport(@PathVariable String userId) {
        try {
            // Try to find session by userId first, then by sessionId (for shared sessions)
            SimulationSession session = simulationManager.getSession(userId);

            if (session == null) {
                // If not found by userId, try by sessionId (UUID)
                session = simulationManager.getSessionBySessionId(userId);
            }

            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            FinalReportDTO report = session.getFinalReport();
            return ResponseEntity.ok(report);

        } catch (Exception e) {
            System.err.println("[SimulationPreviewController] Error getting final report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper to create ScenarioConfig from request parameters.
     */
    private ScenarioConfig createScenarioConfig(String scenarioType, Integer customK) {
        return switch (scenarioType.toUpperCase()) {
            case "WEEKLY" -> customK != null ?
                ScenarioConfig.weekly(customK) :
                ScenarioConfig.weekly(); // Default K=5 (defined in ScenarioConfig.java)
            case "COLLAPSE" -> ScenarioConfig.collapse();
            case "DAILY" -> ScenarioConfig.daily();
            default -> ScenarioConfig.weekly();
        };
    }
}

