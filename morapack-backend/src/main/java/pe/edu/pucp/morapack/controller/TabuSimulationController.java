package pe.edu.pucp.morapack.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import pe.edu.pucp.morapack.service.TabuSimulationService;

import java.util.Map;

@Controller
public class TabuSimulationController {
    private final TabuSimulationService service;

    public TabuSimulationController(TabuSimulationService service) {
        this.service = service;
    }

    @MessageMapping("/tabu/init")
    public void init(@Payload Map<String, Object> payload) {
        long seed = payload.getOrDefault("seed", System.currentTimeMillis()) instanceof Number
            ? ((Number)payload.getOrDefault("seed", System.currentTimeMillis())).longValue() : System.currentTimeMillis();
        long snapshotMs = payload.getOrDefault("snapshotMs", 1000) instanceof Number
            ? ((Number)payload.getOrDefault("snapshotMs", 1000)).longValue() : 1000;
        service.startSimulation(seed, snapshotMs);
    }

    @MessageMapping("/tabu/stop")
    public void stop() {
        service.stopSimulation();
    }
}

