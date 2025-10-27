package pe.edu.pucp.morapack.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import pe.edu.pucp.morapack.service.SimulationService;

@Controller
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @MessageMapping("/init")
    public void startSimulation(@Payload String payload) {
        simulationService.startSimulation(payload);
    }

    @MessageMapping("/update-failures")
    public void updateFailures(@Payload String payload) {
        simulationService.updateFailures(payload);
    }

    @MessageMapping("/stop")
    public void stopSimulation() {
        simulationService.stopSimulation();
    }
}
