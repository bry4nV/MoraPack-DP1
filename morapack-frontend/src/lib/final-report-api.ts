import type { FinalReport } from "@/types/simulation/final-report.types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

/**
 * Obtiene el reporte final de una sesión de simulación
 */
export async function getFinalReport(userId: string): Promise<FinalReport> {
  try {
    const url = `${API_BASE_URL}/api/simulation/${userId}/final-report`;
    console.log(`[final-report-api] Fetching report from: ${url}`);

    const response = await fetch(url);

    if (!response.ok) {
      const statusText = response.statusText || 'Unknown error';
      console.error(`[final-report-api] HTTP ${response.status}: ${statusText}`);
      throw new Error(`${response.status} ${statusText}`);
    }

    const data = await response.json();
    console.log(`[final-report-api] Successfully fetched final report`);
    return data;
  } catch (error) {
    console.error('[final-report-api] Error fetching final report:', error);
    throw error;
  }
}
