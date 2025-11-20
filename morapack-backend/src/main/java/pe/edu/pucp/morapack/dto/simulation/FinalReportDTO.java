package pe.edu.pucp.morapack.dto.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para el reporte final de la simulación.
 * Contiene todas las métricas finales que se muestran al usuario.
 */
public class FinalReportDTO {

    // Información general
    @JsonProperty("scenarioType")
    public String scenarioType;

    @JsonProperty("k")
    public int k;

    @JsonProperty("scMinutes")
    public int scMinutes;

    @JsonProperty("totalIterations")
    public int totalIterations;

    @JsonProperty("startTime")
    public String startTime;

    @JsonProperty("endTime")
    public String endTime;

    // Métricas de pedidos
    @JsonProperty("totalOrders")
    public int totalOrders;

    @JsonProperty("fullyCompleted")
    public int fullyCompleted;

    @JsonProperty("partiallyCompleted")
    public int partiallyCompleted;

    @JsonProperty("notCompleted")
    public int notCompleted;

    @JsonProperty("completionRate")
    public double completionRate;

    // Métricas de productos
    @JsonProperty("totalProductsRequested")
    public long totalProductsRequested;

    @JsonProperty("totalProductsAssigned")
    public long totalProductsAssigned;

    @JsonProperty("productAssignmentRate")
    public double productAssignmentRate;

    // Métricas de shipments
    @JsonProperty("totalShipments")
    public int totalShipments;

    // Métricas de entregas a tiempo (timezone-aware)
    @JsonProperty("deliveredOrders")
    public int deliveredOrders;

    @JsonProperty("onTimeDeliveries")
    public int onTimeDeliveries;

    @JsonProperty("lateDeliveries")
    public int lateDeliveries;

    @JsonProperty("onTimeRate")
    public double onTimeRate;

    @JsonProperty("avgDelayHours")
    public double avgDelayHours;

    @JsonProperty("maxDelayHours")
    public long maxDelayHours;

    @JsonProperty("totalDelayHours")
    public long totalDelayHours;

    // Calificación general
    @JsonProperty("rating")
    public String rating; // EXCELLENT, GOOD, MODERATE, POOR, CRITICAL

    // Métricas de colapso (solo para escenario COLLAPSE)
    @JsonProperty("collapseDetected")
    public boolean collapseDetected;

    @JsonProperty("collapseReason")
    public String collapseReason;
}
