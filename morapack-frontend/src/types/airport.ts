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
  isHub: boolean;
}

export interface CreateAirportPayload {
  continent: string;
  code: string;
  city: string;
  country: string;
  cityAcronym: string;
  gmt: number;
  capacity: number;
  latitude: string;
  longitude: string;
  isHub: boolean;
}

export type UpdateAirportPayload = Partial<CreateAirportPayload>;

export interface BulkCreateAirportPayload {
  airports: CreateAirportPayload[];
}

export interface BulkDeleteAirportPayload {
  airports: number[];
}
