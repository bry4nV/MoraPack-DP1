/**
 * Shared types used across multiple domains (CRUD, simulation, etc.)
 */

// ═══════════════════════════════════════════════════════════════
// ORDER STATUS
// ═══════════════════════════════════════════════════════════════

/**
 * Order status for both CRUD and simulation tracking.
 * Matches backend OrderSummaryDTO.OrderStatus
 */
export type OrderStatus = 'PENDING' | 'IN_TRANSIT' | 'COMPLETED' | 'UNASSIGNED';

// ═══════════════════════════════════════════════════════════════
// AIRPORT (Base)
// ═══════════════════════════════════════════════════════════════

/**
 * Base Airport interface used in CRUD operations.
 * Matches backend AirportDto.java
 */
export interface Airport {
  id: number;
  continent: string;
  code: string;
  city: string;
  country: string;
  cityAcronym: string;
  gmt: number;
  capacity: number;
  latitude: string;
  longitude: string;
  status: string;
  isHub: boolean;
}

/**
 * Extended Airport interface with UI tracking fields.
 * Used for simulation and real-time monitoring.
 */
export interface AirportWithTracking extends Airport {
  // Capacity tracking
  capacidadTotal?: number;
  capacidadUsada?: number;
  capacidadDisponible?: number;
  porcentajeUso?: number;

  // Order tracking
  pedidosEnEspera?: number;
  pedidosDestino?: number;
  productosEnEspera?: number;

  // Flight tracking
  vuelosActivosDesde?: number;
  vuelosActivosHacia?: number;
  vuelosEnTierra?: string[];
}
