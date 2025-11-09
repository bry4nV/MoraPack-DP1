// Contenido para tu archivo types/flight.ts

export interface Flight {
  // Campos que SÍ vienen del backend
  id: number;
  airportOriginCode: string;    // <-- CAMBIADO
  airportDestinationCode: string; // <-- CAMBIADO
  flightDate: string;           // <-- ¡AÑADIDO!
  departureTime: string;
  arrivalTime: string;
  capacity: number;
  status: FlightStatus; // Esto ya estaba bien
}

export enum FlightStatus {
  SCHEDULED = 'SCHEDULED',
  DELAYED = 'DELAYED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED',
}

// Payloads actualizados
export interface CreateFlightPayload {
  airportOriginCode: string;
  airportDestinationCode: string;
  flightDate: string; // "YYYY-MM-DD"
  departureTime: string; // "HH:MM:SS"
  arrivalTime: string; // "HH:MM:SS"
  capacity: number;
  status: FlightStatus;
}

export type UpdateFlightPayload = Partial<CreateFlightPayload>;