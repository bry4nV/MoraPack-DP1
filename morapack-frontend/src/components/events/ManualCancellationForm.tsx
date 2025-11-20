"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AlertCircle, Plane, CheckCircle } from "lucide-react";
import { cancelFlight } from "@/lib/dynamic-events-api";
import type { FlightCancellation } from "@/types/simulation/events.types";

interface ManualCancellationFormProps {
  onCancellationCreated: (c: FlightCancellation) => void;
  onRefresh: () => void;
}

// Tipo para vuelo disponible
interface AvailableFlight {
  code: string;
  origin: string;
  destination: string;
  scheduledDepartureTime: string;
  status: string;
}

export default function ManualCancellationForm({
  onCancellationCreated,
  onRefresh,
}: ManualCancellationFormProps) {
  const [selectedFlight, setSelectedFlight] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  // Lista de vuelos disponibles (en tierra)
  const [availableFlights, setAvailableFlights] = useState<AvailableFlight[]>([]);
  const [loadingFlights, setLoadingFlights] = useState(false);

  // Cargar vuelos disponibles al montar el componente
  useEffect(() => {
    loadAvailableFlights();
  }, []);

  const loadAvailableFlights = async () => {
    setLoadingFlights(true);
    try {
      const response = await fetch('/api/simulation/events/flights/grounded');

      if (!response.ok) {
        console.warn(`Failed to fetch flights: HTTP ${response.status}`);
        setAvailableFlights([]);
        return;
      }

      // Check if response is JSON
      const contentType = response.headers.get('content-type');
      if (!contentType || !contentType.includes('application/json')) {
        console.warn('Flights endpoint returned non-JSON response');
        setAvailableFlights([]);
        return;
      }

      const data = await response.json();

      if (data.success && data.flights) {
        setAvailableFlights(data.flights);
      } else {
        setAvailableFlights([]);
      }
    } catch (err) {
      console.error("Error loading flights:", err);
      setAvailableFlights([]);
    } finally {
      setLoadingFlights(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    setLoading(true);

    try {
      if (!selectedFlight) {
        throw new Error("Debes seleccionar un vuelo");
      }

      // Parsear el vuelo seleccionado (formato: "ORIGIN-DESTINATION-TIME")
      const parts = selectedFlight.split("-");
      if (parts.length < 3) {
        throw new Error("Formato de vuelo inválido");
      }

      const origin = parts[0];
      const destination = parts[1];
      const departureTime = parts.slice(2).join("-"); // Rejoin in case time has dashes

      const result = await cancelFlight({
        flightOrigin: origin,
        flightDestination: destination,
        scheduledDepartureTime: departureTime,
        reason: reason || "Cancelación manual",
      });

      if (result.success && result.cancellation) {
        onCancellationCreated(result.cancellation);
        setSuccess(true);
        // Reset form
        setSelectedFlight("");
        setReason("");
        // Refresh lists
        setTimeout(() => {
          onRefresh();
          loadAvailableFlights();
        }, 500);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error desconocido");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="space-y-1.5">
        <Label htmlFor="flight" className="text-xs">Vuelo a Cancelar</Label>
        <Select value={selectedFlight} onValueChange={setSelectedFlight} disabled={loadingFlights || loading}>
          <SelectTrigger id="flight" className="h-9 text-sm">
            <SelectValue placeholder={
              loadingFlights
                ? "Cargando vuelos..."
                : availableFlights.length === 0
                  ? "No hay vuelos disponibles"
                  : "Seleccionar vuelo en tierra"
            } />
          </SelectTrigger>
          <SelectContent>
            {availableFlights.length === 0 && (
              <div className="p-4 text-center text-xs text-muted-foreground">
                <Plane className="h-8 w-8 mx-auto mb-2 opacity-30" />
                <p>No hay vuelos en tierra disponibles</p>
                <p className="mt-1 text-[10px]">Solo se pueden cancelar vuelos que aún no han despegado</p>
              </div>
            )}
            {availableFlights.map((flight) => (
              <SelectItem
                key={flight.code}
                value={`${flight.origin}-${flight.destination}-${flight.scheduledDepartureTime}`}
              >
                <div className="flex items-center gap-2">
                  <span className="font-medium">{flight.origin} → {flight.destination}</span>
                  <span className="text-xs text-muted-foreground">
                    {new Date(flight.scheduledDepartureTime).toLocaleTimeString('es-ES', {
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </span>
                </div>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="text-[10px] text-muted-foreground">
          Solo se muestran vuelos en tierra. Los vuelos en aire no pueden ser cancelados.
        </p>
      </div>
      {error && (
        <div className="text-xs text-red-600 bg-red-50 p-2.5 rounded border border-red-200 flex items-start gap-2">
          <AlertCircle className="h-3.5 w-3.5 mt-0.5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="text-xs text-green-600 bg-green-50 p-2.5 rounded border border-green-200 flex items-center gap-2">
          <CheckCircle className="h-3.5 w-3.5" />
          <span>Vuelo cancelado exitosamente. La carga será reasignada automáticamente.</span>
        </div>
      )}

      <Button
        type="submit"
        disabled={loading || !selectedFlight || loadingFlights}
        className="w-full h-9 text-sm"
      >
        {loading ? "Cancelando..." : "Cancelar Vuelo"}
      </Button>
    </form>
  );
}
