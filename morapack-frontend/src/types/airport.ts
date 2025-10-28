export interface Airport {
  id: string;
  name: string;
  country: string;
  city: string;
  gmt: string;
  capacity: number;
  continent: string;
  isHub: boolean;
}

// Tipos para los payloads de creación y actualización
export interface CreateAirportPayload {
  id: string;
  name: string;
  country: string;
  city: string;
  gmt: string;
  capacity: number;
  continent: string;
  isHub: boolean;
}

export type UpdateAirportPayload = Partial<CreateAirportPayload>;

// Carga masiva
export interface BulkCreateAirportPayload {
  airports: CreateAirportPayload[];
}

export interface BulkDeleteAirportPayload {
  airports: string[]; // array of airport IDs
}
