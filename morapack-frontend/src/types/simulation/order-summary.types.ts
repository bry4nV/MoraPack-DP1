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

  // Assigned flights (DEPRECATED - use shipments instead)
  assignedFlights: FlightSegmentInfo[];

  // 🆕 Shipments: Detailed breakdown of how the order is split
  shipments: ShipmentInfo[];

  // Priority (optional)
  priority?: number;
}

/**
 * Minimal flight segment information for order tracking.
 * Contains just enough data to show the route.
 */
export interface FlightSegmentInfo {
  flightCode: string;
  originCode: string;
  destinationCode: string;
}

/**
 * 🆕 Shipment information (envío).
 * Represents a portion of an order with its own quantity and route.
 *
 * Example:
 *   Order #100: 500 products Lima → Miami
 *
 *   Shipment #1: 200 products, route [LIM→MIA] (direct)
 *   Shipment #2: 150 products, route [LIM→MEX, MEX→MIA] (1 stopover)
 *   Shipment #3: 150 products, route [LIM→PTY, PTY→MIA] (1 stopover)
 */
export interface ShipmentInfo {
  shipmentId: number;
  quantity: number;              // Number of products in THIS shipment
  route: FlightSegmentInfo[];    // Flight segments forming the complete route
  isDirect: boolean;             // true if route has only 1 flight
  numberOfStops: number;         // Number of stopovers (route.length - 1)
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
