import type { 
  Airport, 
  CreateAirportPayload, 
  UpdateAirportPayload,
  BulkCreateAirportPayload,
  BulkDeleteAirportPayload
} from "@/types/airport";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export const airportsApi = {
  async getAllAirports(): Promise<Airport[]> {
    try {
      console.log("Fetching from:", `${API_BASE_URL}/airports`);
      const response = await fetch(`${API_BASE_URL}/airports`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });
      
      console.log("Response status:", response.status);
      
      if (!response.ok) {
        throw new Error(`Error ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      console.log("Parsed data:", data);
      return data;
    } catch (error) {
      console.error("API Error:", error);
      throw error;
    }
  },

  async getAirportById(id: string): Promise<Airport> {
    const response = await fetch(`${API_BASE_URL}/airports/${id}`);
    if (!response.ok) throw new Error("Error al obtener aeropuerto");
    return response.json();
  },

  async createAirport(payload: CreateAirportPayload): Promise<Airport> {
    try {
      console.log("Creating airport with payload:", payload);
      const response = await fetch(`${API_BASE_URL}/airports`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });
      
      console.log("Create response status:", response.status);
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        console.error("Error response:", errorData);
        throw new Error(errorData.message || `Error ${response.status}: ${response.statusText}`);
      }
      
      const data = await response.json();
      console.log("Airport created successfully:", data);
      return data;
    } catch (error) {
      console.error("API Error creating airport:", error);
      throw error;
    }
  },

  async updateAirport(id: string, payload: UpdateAirportPayload): Promise<Airport> {
    const response = await fetch(`${API_BASE_URL}/airports/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error("Error al actualizar aeropuerto");
    return response.json();
  },

  async deleteAirport(id: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/airports/${id}`, {
      method: "DELETE",
    });
    if (!response.ok) throw new Error("Error al eliminar aeropuerto");
  },

  async bulkCreateAirports(payload: BulkCreateAirportPayload): Promise<Airport[]> {
    const response = await fetch(`${API_BASE_URL}/airports/bulk`, {
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
    const response = await fetch(`${API_BASE_URL}/airports/bulk`, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error("Error en eliminación masiva de aeropuertos");
  },
};
