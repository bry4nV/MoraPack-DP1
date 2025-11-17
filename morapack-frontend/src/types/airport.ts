export enum AirportStatus {
  ACTIVE = "ACTIVE",
  INACTIVE = "INACTIVE"
}

export enum Continent {
  AMERICA_DEL_SUR = "America del Sur.",
  EUROPA = "Europa",
  ASIA = "Asia"
}

export interface Airport {
  id: number; // Cambié de string a number
  continent: Continent;
  code: string;
  city: string;
  country: string;
  cityAcronym: string;
  gmt: number;
  capacity: number;
  latitude: string;
  longitude: string;
  status: AirportStatus;
  isHub: boolean;
}

// Payload para crear - debe coincidir con lo que espera el backend
export interface CreateAirportPayload {
  continent: string;
  code: string;
  city: string;
  country: string;
  cityAcronym: string; // Cambié a camelCase para coincidir con lo que devuelve el backend
  gmt: number;
  capacity: number;
  latitude: string;
  longitude: string;
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
