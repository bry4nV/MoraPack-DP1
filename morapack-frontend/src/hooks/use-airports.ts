"use client";

import { useState, useEffect } from "react";
import { airportsApi } from "@/api/airports/airports";
import { Airport } from "@/types/airport";

export function useAirports() {
  const [airports, setAirports] = useState<Airport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAirports = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await airportsApi.getAirports();
      setAirports(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al cargar aeropuertos");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAirports();
  }, []);

  return {
    airports,
    isLoading,
    error,
    refetch: fetchAirports,
  };
}
