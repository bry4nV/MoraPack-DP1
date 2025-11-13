/**
 * Order tracking types for simulation.
 * Used for real-time simulation tracking via WebSocket.
 */

import type { OrderStatus } from '../shared';

// ═══════════════════════════════════════════════════════════════
// ORDER SUMMARY
// ═══════════════════════════════════════════════════════════════

/**
 * Extended order view with tracking information for simulation.
 * Matches backend OrderSummaryDTO.java
 */
export interface OrderSummary {
  id: number;
  code: string;

  // Origin and destination
  originCode: string;
  originName: string;
  destinationCode: string;
  destinationName: string;

  // Quantities
  totalQuantity: number;
  assignedQuantity: number;
  progressPercent: number;

  // Times
  requestDateISO: string;
  etaISO?: string | null;

  // Status
  status: OrderStatus;

  // Assigned flights
  assignedFlights: string[];

  // Priority (optional)
  priority?: number;
}

// ═══════════════════════════════════════════════════════════════
// ORDER METRICS
// ═══════════════════════════════════════════════════════════════

/**
 * Metrics about orders in the simulation.
 * Matches backend OrderMetricsDTO.java
 */
export interface OrderMetrics {
  totalOrders: number;
  pending: number;
  inTransit: number;
  completed: number;
  unassigned: number;

  // Additional statistics
  totalProducts: number;
  assignedProducts: number;
  assignmentRatePercent: number;
}
