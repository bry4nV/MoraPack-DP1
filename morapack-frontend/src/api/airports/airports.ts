import { apiClient } from '@/lib/api-client';
import { API_ENDPOINTS } from '@/config/api';
import { Airport } from '@/types/airport';

export const airportsApi = {
  getAirports: async (): Promise<Airport[]> => {
    return await apiClient.get<Airport[]>(API_ENDPOINTS.AIRPORTS.BASE);
  },

  getAirportById: async (id: string): Promise<Airport> => {
    return await apiClient.get<Airport>(API_ENDPOINTS.AIRPORTS.BY_ID(id));
  },

  createAirport: async (payload: Partial<Airport>): Promise<Airport> => {
    return await apiClient.post<Airport>(API_ENDPOINTS.AIRPORTS.BASE, payload);
  },

  updateAirport: async (id: string, payload: Partial<Airport>): Promise<Airport> => {
    return await apiClient.patch<Airport>(API_ENDPOINTS.AIRPORTS.BY_ID(id), payload);
  },

  deleteAirport: async (id: string): Promise<void> => {
    return await apiClient.delete<void>(API_ENDPOINTS.AIRPORTS.BY_ID(id));
  },
};
