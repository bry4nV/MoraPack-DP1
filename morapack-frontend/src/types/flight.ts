export interface Flight {
  id: number;
  idAeropuertoOrigen: string;
  idAeropuertoDestino: string;
  horaSalida: string; // "03:34:00"
  horaLlegada: string; // "05:21:00"
  capacidad: number;
  estado: FlightStatus; // ← Cambiado
}

export enum FlightStatus {
  SCHEDULED = 'SCHEDULED',
  DELAYED = 'DELAYED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED',
}

export interface CreateFlightPayload {
  idAeropuertoOrigen: string;
  idAeropuertoDestino: string;
  horaSalida: string;
  horaLlegada: string;
  capacidad: number;
  estado: FlightStatus;
}

export type UpdateFlightPayload = Partial<CreateFlightPayload>;
