import type { FlightsResponse } from "@/types/simulation/flights.types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

/**
 * Obtiene el estado de todos los vuelos de una sesión de simulación
 */
export async function getFlightsStatus(userId: string): Promise<FlightsResponse> {
  const response = await fetch(`${API_BASE_URL}/api/simulation/${userId}/flights`);

  if (!response.ok) {
    throw new Error(`Failed to fetch flights: ${response.statusText}`);
  }

  return response.json();
}
