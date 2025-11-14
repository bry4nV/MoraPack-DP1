import { API_CONFIG, API_ENDPOINTS } from '@/config/api';
import type { 
  Airport, 
  CreateAirportPayload, 
  UpdateAirportPayload,
  BulkCreateAirportPayload,
  BulkDeleteAirportPayload
} from "@/types/airport";

const getFullUrl = (path: string) => `${API_CONFIG.BASE_URL}${path}`;

export const airportsApi = {
  async getAllAirports(): Promise<Airport[]> {
    try {
      const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BASE), {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });
      
      if (!response.ok) {
        throw new Error(`Error ${response.status}: ${response.statusText}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error("API Error:", error);
      throw error;
    }
  },

  async getAirportById(id: number): Promise<Airport> {
    const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BY_ID(id)));
    if (!response.ok) throw new Error("Error al obtener aeropuerto");
    return response.json();
  },

  async createAirport(payload: CreateAirportPayload): Promise<Airport> {
    try {
      const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BASE), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error("API Error creating airport:", error);
      throw error;
    }
  },

  async updateAirport(id: number, payload: UpdateAirportPayload): Promise<Airport> {
    const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BY_ID(id)), {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error("Error al actualizar aeropuerto");
    return response.json();
  },

  async deleteAirport(id: number): Promise<void> {
    const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BY_ID(id)), {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
    });
    if (!response.ok) throw new Error("Error al eliminar aeropuerto");
  },

  async bulkCreateAirports(payload: BulkCreateAirportPayload): Promise<Airport[]> {
    const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BULK_CREATE), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error("Error en carga masiva de aeropuertos");
    return response.json();
  },

  async bulkDeleteAirports(payload: BulkDeleteAirportPayload): Promise<void> {
    const response = await fetch(getFullUrl(API_ENDPOINTS.AIRPORTS.BULK_DELETE), {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error("Error en eliminación masiva de aeropuertos");
  },
};
