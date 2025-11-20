/**
 * Types for the final simulation report
 */

export type SimulationRating = "EXCELLENT" | "GOOD" | "MODERATE" | "POOR" | "CRITICAL";

export interface FinalReport {
  // Información general
  scenarioType: string;
  k: number;
  scMinutes: number;
  totalIterations: number;
  startTime: string;
  endTime: string;

  // Métricas de pedidos
  totalOrders: number;
  fullyCompleted: number;
  partiallyCompleted: number;
  notCompleted: number;
  completionRate: number;

  // Métricas de productos
  totalProductsRequested: number;
  totalProductsAssigned: number;
  productAssignmentRate: number;

  // Métricas de shipments
  totalShipments: number;

  // Métricas de entregas a tiempo (timezone-aware)
  deliveredOrders: number;
  onTimeDeliveries: number;
  lateDeliveries: number;
  onTimeRate: number;
  avgDelayHours: number;
  maxDelayHours: number;
  totalDelayHours: number;

  // Calificación general
  rating: SimulationRating;

  // Métricas de colapso (solo para escenario COLLAPSE)
  collapseDetected: boolean;
  collapseReason: string | null;
}
