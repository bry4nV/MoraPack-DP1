export interface Flight {
  id: number;
  originCode: string;
  destinationCode: string;
  departureTime: string; // "HH:mm:ss"
  arrivalTime: string; // "HH:mm:ss"
  capacity: number;
}

export interface CreateFlightPayload {
  originCode: string;
  destinationCode: string;
  departureTime: string;
  arrivalTime: string;
  capacity: number;
}

export type UpdateFlightPayload = Partial<CreateFlightPayload>;

export interface BulkCreateFlightPayload {
  flights: CreateFlightPayload[];
}

export interface BulkDeleteFlightPayload {
  flights: number[];
}
