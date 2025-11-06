/**
 * Types for dynamic events (flight cancellations and dynamic orders)
 */

export interface FlightCancellation {
  id: string;
  flightOrigin: string;
  flightDestination: string;
  scheduledDepartureTime: string;
  cancellationTime: string | null;
  reason: string;
  status: 'PENDING' | 'EXECUTED';
}

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

export interface CancelFlightRequest {
  flightOrigin: string;
  flightDestination: string;
  scheduledDepartureTime: string;
  reason?: string;
}

export interface DynamicOrderRequest {
  origin: string;
  destination: string;
  quantity: number;
  deadlineHours: number;
  reason?: string;
}

export interface CancellationApiResponse {
  success: boolean;
  message: string;
  cancellation?: FlightCancellation;
}

export interface DynamicOrderApiResponse {
  success: boolean;
  message: string;
  order?: DynamicOrder;
}

export interface CancellationsListResponse {
  success: boolean;
  count: number;
  cancellations: FlightCancellation[];
}

export interface DynamicOrdersListResponse {
  success: boolean;
  count: number;
  orders: DynamicOrder[];
}

