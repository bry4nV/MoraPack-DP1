import type { FinalReport } from "@/types/simulation/final-report.types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

/**
 * Obtiene el reporte final de una sesión de simulación
 */
export async function getFinalReport(userId: string): Promise<FinalReport> {
  const response = await fetch(`${API_BASE_URL}/api/simulation/${userId}/final-report`);

  if (!response.ok) {
    throw new Error(`Failed to fetch final report: ${response.statusText}`);
  }

  return response.json();
}
