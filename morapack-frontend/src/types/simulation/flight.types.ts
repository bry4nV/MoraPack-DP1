/**
 * Flight types for simulation tracking.
 * Note: Different from CRUD flight.ts (which is for /vuelos management)
 */

import type { Aeropuerto } from '../aeropuerto';

// ═══════════════════════════════════════════════════════════════
// FLIGHT STATUS
// ═══════════════════════════════════════════════════════════════

export type EstadoVuelo = 'SCHEDULED' | 'DELAYED' | 'CANCELLED' | 'COMPLETED';

// ═══════════════════════════════════════════════════════════════
// SIMULATION FLIGHT
// ═══════════════════════════════════════════════════════════════

/**
 * Flight representation in simulation context.
 * Used for tracking flights during simulation.
 */
export interface Vuelo {
  codigo: string;
  origen: Aeropuerto;
  destino: Aeropuerto;
  salidaProgramadaISO: string;
  llegadaProgramadaISO: string;
  capacidad: number;
  preplanificado: boolean;
  estado: EstadoVuelo;
}
