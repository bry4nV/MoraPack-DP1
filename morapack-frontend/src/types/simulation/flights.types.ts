/**
 * Tipos relacionados con el trackeo de vuelos en la simulación
 */

export type FlightStatus =
  | "ON_GROUND_ORIGIN"       // En tierra en origen (cancelable)
  | "IN_AIR"                  // En vuelo (NO cancelable)
  | "ON_GROUND_DESTINATION"   // En tierra en destino (completado)
  | "NOT_SCHEDULED";          // No programado aún

export interface FlightInfo {
  flightId: string;                // "SPIM-SCEL-0800"
  origin: string;                  // "SPIM"
  destination: string;             // "SCEL"
  scheduledDeparture: string;      // ISO timestamp
  scheduledArrival: string;        // ISO timestamp
  status: FlightStatus;            // Estado actual
  cancellable: boolean;            // Si puede ser cancelado (solo en tierra origen)
  cancelled: boolean;              // Si fue cancelado
}

export interface FlightsResponse {
  success: boolean;
  count: number;
  flights: FlightInfo[];
}
