/**
 * Simulation preview types.
 * Used before simulation starts to show what will be simulated.
 */

import type { OrderSummary } from './order-summary.types';

// ═══════════════════════════════════════════════════════════════
// SIMULATION PREVIEW
// ═══════════════════════════════════════════════════════════════

/**
 * Preview response before simulation starts.
 * Matches backend SimulationPreviewResponse.java
 */
export interface SimulationPreview {
  totalOrders: number;
  totalProducts: number;
  totalFlights: number;

  dateRange: {
    start: string;
    end: string;
  };

  orders: OrderSummary[];
  involvedAirports: string[];
  airports?: any[];  // Full airport data for map

  statistics: {
    ordersPerDay: number[];
    avgProductsPerOrder: number;
    maxProductsOrder: number;
    minProductsOrder: number;
  };
}
