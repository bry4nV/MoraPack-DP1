/**
 * Types for dynamic events during simulation.
 * Includes flight cancellations and dynamic order injections.
 */

// ═══════════════════════════════════════════════════════════════
// FLIGHT CANCELLATIONS
// ═══════════════════════════════════════════════════════════════

export interface FlightCancellation {
  id: string;
  flightOrigin: string;
  flightDestination: string;
  scheduledDepartureTime: string;
  cancellationTime: string | null;
  reason: string;
  status: 'PENDING' | 'EXECUTED';
}

export interface CancelFlightRequest {
  flightOrigin: string;
  flightDestination: string;
  scheduledDepartureTime: string;
  reason?: string;
}

export interface CancellationApiResponse {
  success: boolean;
  message: string;
  cancellation?: FlightCancellation;
}

export interface CancellationsListResponse {
  success: boolean;
  count: number;
  cancellations: FlightCancellation[];
}

// ═══════════════════════════════════════════════════════════════
// DYNAMIC ORDERS
// ═══════════════════════════════════════════════════════════════

export interface DynamicOrder {
  id: string;
  origin: string;
  destination: string;
  quantity: number;
  deadlineHours: number;
  injectionTime: string | null;
  reason: string;
  status: 'PENDING' | 'INJECTED';
}

export interface DynamicOrderRequest {
  destination: string;
  quantity: number;
  reason?: string;
  // origin y deadlineHours se determinan automáticamente en el backend
}

export interface DynamicOrderApiResponse {
  success: boolean;
  message: string;
  order?: DynamicOrder;
}

export interface DynamicOrdersListResponse {
  success: boolean;
  count: number;
  orders: DynamicOrder[];
}
