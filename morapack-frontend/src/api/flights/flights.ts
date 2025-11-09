// Contenido para tu nuevo archivo (ej: src/lib/api/flightsApi.ts)

import { apiClient } from '@/lib/api-client'; // (Asumiendo que esta ruta es correcta)
import { API_ENDPOINTS } from '@/config/api';
import { Flight, CreateFlightPayload, UpdateFlightPayload } from '@/types/flight'; // Importamos los tipos que creamos

export const flightsApi = {
  getFlights: async (): Promise<Flight[]> => {
    return await apiClient.get<Flight[]>(API_ENDPOINTS.FLIGHTS.BASE);
  },

  getFlightById: async (id: string): Promise<Flight> => {
    return await apiClient.get<Flight>(API_ENDPOINTS.FLIGHTS.BY_ID(id));
  },

  createFlight: async (payload: CreateFlightPayload): Promise<Flight> => {
    return await apiClient.post<Flight>(API_ENDPOINTS.FLIGHTS.BASE, payload);
  },

  updateFlight: async (id: string, payload: UpdateFlightPayload): Promise<Flight> => {
    return await apiClient.patch<Flight>(API_ENDPOINTS.FLIGHTS.BY_ID(id), payload);
  },

  deleteFlight: async (id: string): Promise<void> => {
    return await apiClient.delete<void>(API_ENDPOINTS.FLIGHTS.BY_ID(id));
  },
};