import type { FlightsResponse } from "@/types/simulation/flights.types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

/**
 * Obtiene el estado de todos los vuelos de una sesión de simulación
 */
export async function getFlightsStatus(userId: string): Promise<FlightsResponse> {
  try {
    const url = `${API_BASE_URL}/api/simulation/${userId}/flights`;
    console.log(`[flights-api] Fetching flights from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      const statusText = response.statusText || 'Unknown error';
      console.error(`[flights-api] HTTP ${response.status}: ${statusText}`);
      throw new Error(`${response.status} ${statusText}`);
    }

    const data = await response.json();
    console.log(`[flights-api] Successfully fetched ${data.flights?.length || 0} flights`);
    return data;
  } catch (error) {
    console.error('[flights-api] Error fetching flights:', error);
    throw error;
  }
}
