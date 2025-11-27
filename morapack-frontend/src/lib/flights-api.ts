import type { FlightsResponse, FlightCargoResponse } from "@/types/simulation/flights.types";

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
      // Don't log 400/404 errors (expected when session doesn't exist)
      if (response.status !== 400 && response.status !== 404) {
        console.error(`[flights-api] HTTP ${response.status}: ${statusText}`);
      }
      throw new Error(`${response.status} ${statusText}`);
    }

    const data = await response.json();
    console.log(`[flights-api] Successfully fetched ${data.flights?.length || 0} flights`);
    return data;
  } catch (error) {
    // Only log if it's not a 400/404 error
    if (error instanceof Error && !error.message.includes('400') && !error.message.includes('404')) {
      console.error('[flights-api] Error fetching flights:', error);
    }
    throw error;
  }
}

/**
 * Obtiene información detallada de la carga de un vuelo específico
 */
export async function getFlightCargo(userId: string, flightId: string): Promise<FlightCargoResponse> {
  try {
    const url = `${API_BASE_URL}/api/simulation/${userId}/flights/${flightId}/cargo`;
    console.log(`[flights-api] Fetching cargo for flight ${flightId} from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      const statusText = response.statusText || 'Unknown error';
      // Don't log 400/404 errors (expected when session doesn't exist)
      if (response.status !== 400 && response.status !== 404) {
        console.error(`[flights-api] HTTP ${response.status}: ${statusText}`);
      }
      throw new Error(`${response.status} ${statusText}`);
    }

    const data = await response.json();
    console.log(`[flights-api] Successfully fetched cargo: ${data.totalShipments} shipments, ${data.totalQuantity} products`);
    return data;
  } catch (error) {
    // Only log if it's not a 400/404 error
    if (error instanceof Error && !error.message.includes('400') && !error.message.includes('404')) {
      console.error('[flights-api] Error fetching flight cargo:', error);
    }
    throw error;
  }
}
