"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Plane, MapPin, Target, Clock, AlertCircle } from "lucide-react";
import { cancelFlight } from "@/lib/dynamic-events-api";
import type { FlightCancellation } from "@/types/simulation/events.types";
import type { Aeropuerto } from "@/types";

interface CancellationsTabProps {
  aeropuertos: Aeropuerto[];
  cancellations: FlightCancellation[];
  onCancellationCreated: (c: FlightCancellation) => void;
  onRefresh: () => void;
}

export default function CancellationsTab({
  aeropuertos,
  cancellations,
  onCancellationCreated,
  onRefresh,
}: CancellationsTabProps) {
  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");
  const [departureTime, setDepartureTime] = useState("");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    setLoading(true);

    try {
      const result = await cancelFlight({
        flightOrigin: origin,
        flightDestination: destination,
        scheduledDepartureTime: departureTime,
        reason: reason || "Manual cancellation",
      });

      if (result.success && result.cancellation) {
        onCancellationCreated(result.cancellation);
        setSuccess(true);
        // Reset form
        setOrigin("");
        setDestination("");
        setDepartureTime("");
        setReason("");
        // Refresh list
        setTimeout(() => onRefresh(), 500);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Form Section */}
      <div className="p-3 border-b">
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="origin" className="text-xs">Origen</Label>
            <Select value={origin} onValueChange={setOrigin} required>
              <SelectTrigger id="origin" className="h-9 text-sm">
                <SelectValue placeholder="Seleccionar aeropuerto" />
              </SelectTrigger>
              <SelectContent>
                {aeropuertos.map((a) => (
                  <SelectItem key={a.code} value={a.code}>
                    {a.code} - {a.city}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="destination" className="text-xs">Destino</Label>
            <Select value={destination} onValueChange={setDestination} required>
              <SelectTrigger id="destination" className="h-9 text-sm">
                <SelectValue placeholder="Seleccionar aeropuerto" />
              </SelectTrigger>
              <SelectContent>
                {aeropuertos.map((a) => (
                  <SelectItem key={a.code} value={a.code}>
                    {a.code} - {a.city}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="departureTime" className="text-xs">Hora de Salida Programada</Label>
            <Input
              id="departureTime"
              type="datetime-local"
              value={departureTime}
              onChange={(e) => setDepartureTime(e.target.value)}
              className="h-9 text-sm"
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reason" className="text-xs">Razon (opcional)</Label>
            <Textarea
              id="reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Ej: Mantenimiento programado"
              className="text-sm resize-none"
              rows={2}
            />
          </div>

          {error && (
            <div className="text-xs text-red-600 bg-red-50 p-2 rounded border border-red-200">
              {error}
            </div>
          )}

          {success && (
            <div className="text-xs text-green-600 bg-green-50 p-2 rounded border border-green-200">
              Vuelo cancelado exitosamente
            </div>
          )}

          <Button
            type="submit"
            disabled={loading || !origin || !destination || !departureTime}
            className="w-full h-9 text-sm"
          >
            {loading ? "Cancelando..." : "Cancelar Vuelo"}
          </Button>
        </form>
      </div>

      {/* List Section */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        <div className="flex items-center justify-between mb-2">
          <h4 className="text-sm font-semibold">Cancelaciones ({cancellations.length})</h4>
        </div>

        {cancellations.length === 0 && (
          <div className="text-center text-gray-400 py-8 text-sm">
            No hay cancelaciones
          </div>
        )}

        {cancellations.map((cancellation) => (
          <CancellationCard key={cancellation.id} cancellation={cancellation} />
        ))}
      </div>
    </div>
  );
}

function CancellationCard({ cancellation }: { cancellation: FlightCancellation }) {
  const statusConfig = {
    PENDING: {
      color: "bg-yellow-100 text-yellow-800 border-yellow-300",
      label: "Pendiente",
    },
    EXECUTED: {
      color: "bg-red-100 text-red-800 border-red-300",
      label: "Ejecutada",
    },
  };

  const config = statusConfig[cancellation.status];

  return (
    <Card className="border">
      <CardContent className="p-3 space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-semibold">
            <Plane className="h-3 w-3" />
            <span>{cancellation.flightOrigin} → {cancellation.flightDestination}</span>
          </div>
          <Badge className={`text-[10px] px-2 py-0.5 ${config.color}`}>
            {config.label}
          </Badge>
        </div>

        <div className="text-xs text-muted-foreground space-y-1">
          <div className="flex items-center gap-2">
            <Clock className="h-3 w-3" />
            <span>Salida: {new Date(cancellation.scheduledDepartureTime).toLocaleString('es-ES')}</span>
          </div>
          {cancellation.reason && (
            <div className="flex items-start gap-2">
              <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0" />
              <span>{cancellation.reason}</span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}


