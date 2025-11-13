/**
 * Itinerary types for simulation.
 * Represents planned routes for shipments.
 */

import type { Vuelo } from '../vuelo';

// ═══════════════════════════════════════════════════════════════
// ITINERARY
// ═══════════════════════════════════════════════════════════════

export interface SegmentoPlan {
  orden: number;
  vuelo: Vuelo;
}

export interface Itinerario {
  id: string;
  segmentos: SegmentoPlan[];
}
