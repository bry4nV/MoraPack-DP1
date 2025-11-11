"use client";

import { useEffect, useState } from "react";
import { airportsApi } from "@/api/airports/airports";
import type { Airport } from "@/types/airport";

export function useAirports() {
  const [airports, setAirports] = useState<Airport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetchAirports = async () => {
      try {
        setIsLoading(true);
        const data = await airportsApi.getAllAirports();
        setAirports(data || []);
        setError(null);
      } catch (err) {
        setError(err as Error);
        console.error("Error fetching airports:", err);
        setAirports([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAirports();
  }, []);

  return { airports, isLoading, error };
}
