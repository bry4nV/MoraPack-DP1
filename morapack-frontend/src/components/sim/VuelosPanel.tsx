"use client";

import { useState, useEffect, useMemo, memo } from "react";
import { Plane, Search, X, MapPin, Clock, Ban, CheckCircle } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { FlightInfo, FlightStatus } from "@/types/simulation/flights.types";
import { getFlightsStatus } from "@/lib/flights-api";

interface VuelosPanelProps {
  userId: string;
}

export const VuelosPanel = memo(function VuelosPanel({ userId }: VuelosPanelProps) {
  const [flights, setFlights] = useState<FlightInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");

  // Cargar vuelos
  const loadFlights = async () => {
    if (!userId) {
      setError("Esperando inicio de simulación...");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await getFlightsStatus(userId);
      if (response.success) {
        setFlights(response.flights);
      } else {
        setError("No se pudo cargar los vuelos");
      }
    } catch (error) {
      console.error("Error loading flights:", error);
      // No mostrar error si es porque la simulación no ha iniciado
      if (error instanceof Error && !error.message.includes("404")) {
        setError("Error al cargar vuelos. Verifica que la simulación esté iniciada.");
      }
    } finally {
      setLoading(false);
    }
  };

  // Cargar al montar y cada 5 segundos
  useEffect(() => {
    loadFlights();
    const interval = setInterval(loadFlights, 5000);
    return () => clearInterval(interval);
  }, [userId]);

  // Filtrar vuelos por búsqueda
  const filteredFlights = useMemo(() => {
    if (!searchTerm.trim()) return flights;

    const term = searchTerm.toLowerCase();
    return flights.filter(
      (flight) =>
        flight.flightId.toLowerCase().includes(term) ||
        flight.origin.toLowerCase().includes(term) ||
        flight.destination.toLowerCase().includes(term)
    );
  }, [flights, searchTerm]);

  // Agrupar por estado
  const groupedFlights = useMemo(() => {
    return {
      inAir: filteredFlights.filter((f) => f.status === "IN_AIR"),
      grounded: filteredFlights.filter((f) => f.status === "ON_GROUND_ORIGIN"),
      arrived: filteredFlights.filter((f) => f.status === "ON_GROUND_DESTINATION"),
      cancelled: filteredFlights.filter((f) => f.cancelled),
    };
  }, [filteredFlights]);

  return (
    <div className="h-full flex flex-col bg-white">
      {/* Header */}
      <div className="p-3 border-b bg-slate-50">
        <div className="flex items-center gap-2 mb-2">
          <Plane className="h-4 w-4" />
          <h3 className="font-bold text-base">Vuelos</h3>
          <Badge variant="outline" className="ml-auto text-xs">
            {flights.length} total
          </Badge>
        </div>
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            placeholder="Buscar por código, origen o destino..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-8 pr-8 h-9 text-sm"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm("")}
              className="absolute right-2.5 top-2.5 text-muted-foreground hover:text-foreground"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-3 space-y-4">
        {error ? (
          <div className="text-center py-8 text-sm text-muted-foreground">
            <Plane className="h-8 w-8 mx-auto mb-2 opacity-30" />
            <p>{error}</p>
          </div>
        ) : loading && flights.length === 0 ? (
          <div className="text-center py-8 text-sm text-muted-foreground">
            <Plane className="h-8 w-8 mx-auto mb-2 opacity-30 animate-pulse" />
            <p>Cargando vuelos...</p>
          </div>
        ) : flights.length === 0 ? (
          <div className="text-center py-8 text-sm text-muted-foreground">
            <Plane className="h-8 w-8 mx-auto mb-2 opacity-30" />
            <p>No hay vuelos activos</p>
            <p className="text-xs mt-1">Inicia una simulación para ver vuelos</p>
          </div>
        ) : (
          <>
            {/* Cancelados */}
            {groupedFlights.cancelled.length > 0 && (
              <FlightGroup
                title="Cancelados"
                icon={Ban}
                color="red"
                flights={groupedFlights.cancelled}
              />
            )}

            {/* En Vuelo */}
            {groupedFlights.inAir.length > 0 && (
              <FlightGroup
                title="En Vuelo"
                icon={Plane}
                color="blue"
                flights={groupedFlights.inAir}
              />
            )}

            {/* En Tierra (Origen) */}
            {groupedFlights.grounded.length > 0 && (
              <FlightGroup
                title="En Tierra (Origen)"
                icon={Clock}
                color="yellow"
                flights={groupedFlights.grounded}
              />
            )}

            {/* Llegaron */}
            {groupedFlights.arrived.length > 0 && (
              <FlightGroup
                title="Llegaron a Destino"
                icon={CheckCircle}
                color="green"
                flights={groupedFlights.arrived}
              />
            )}

            {/* Empty state */}
            {filteredFlights.length === 0 && !loading && (
              <div className="text-center py-12 text-sm text-muted-foreground">
                <Plane className="h-12 w-12 mx-auto mb-3 opacity-30" />
                <p className="font-medium">
                  {searchTerm ? "No se encontraron vuelos" : "No hay vuelos en la simulación"}
                </p>
                {searchTerm && (
                  <p className="text-xs mt-1">Intenta con otro término de búsqueda</p>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
});

// ========== COMPONENTES AUXILIARES ==========

interface FlightGroupProps {
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  color: "red" | "blue" | "yellow" | "green";
  flights: FlightInfo[];
}

function FlightGroup({ title, icon: Icon, color, flights }: FlightGroupProps) {
  const colorClasses = {
    red: "text-red-600",
    blue: "text-blue-600",
    yellow: "text-yellow-600",
    green: "text-green-600",
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h4 className={`text-sm font-semibold flex items-center gap-2 ${colorClasses[color]}`}>
          <Icon className="h-4 w-4" />
          {title} ({flights.length})
        </h4>
      </div>
      <div className="space-y-2">
        {flights.map((flight) => (
          <FlightCard key={flight.flightId} flight={flight} />
        ))}
      </div>
    </div>
  );
}

interface FlightCardProps {
  flight: FlightInfo;
}

function FlightCard({ flight }: FlightCardProps) {
  const statusConfig: Record<FlightStatus, { label: string; color: string; icon: any }> = {
    IN_AIR: {
      label: "En Vuelo",
      color: "bg-blue-100 text-blue-800 border-blue-300",
      icon: Plane,
    },
    ON_GROUND_ORIGIN: {
      label: "En Tierra",
      color: "bg-yellow-100 text-yellow-800 border-yellow-300",
      icon: Clock,
    },
    ON_GROUND_DESTINATION: {
      label: "Llegó",
      color: "bg-green-100 text-green-800 border-green-300",
      icon: CheckCircle,
    },
    NOT_SCHEDULED: {
      label: "No Programado",
      color: "bg-gray-100 text-gray-800 border-gray-300",
      icon: Clock,
    },
  };

  const config = statusConfig[flight.status];
  const StatusIcon = config.icon;

  return (
    <Card className={`border ${flight.cancelled ? "opacity-60 bg-red-50" : ""}`}>
      <CardContent className="p-3 space-y-2">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="font-mono text-xs font-semibold">{flight.flightId}</span>
            {flight.cancelled && (
              <Badge className="bg-red-100 text-red-800 border-red-300 text-[10px] px-2 py-0.5">
                <Ban className="h-3 w-3 mr-1" />
                Cancelado
              </Badge>
            )}
          </div>
          <Badge className={`text-[10px] px-2 py-0.5 flex items-center gap-1 ${config.color}`}>
            <StatusIcon className="h-3 w-3" />
            {config.label}
          </Badge>
        </div>

        {/* Route */}
        <div className="flex items-center gap-2 text-sm">
          <MapPin className="h-3.5 w-3.5 text-muted-foreground" />
          <span className="font-medium">{flight.origin}</span>
          <span className="text-muted-foreground">→</span>
          <span className="font-medium">{flight.destination}</span>
        </div>

        {/* Times */}
        <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
          <div>
            <span className="font-medium">Salida:</span>{" "}
            {new Date(flight.scheduledDeparture).toLocaleTimeString("es-ES", {
              hour: "2-digit",
              minute: "2-digit",
            })}{" "}
            <span className="text-[10px] opacity-60">UTC</span>
          </div>
          <div>
            <span className="font-medium">Llegada:</span>{" "}
            {new Date(flight.scheduledArrival).toLocaleTimeString("es-ES", {
              hour: "2-digit",
              minute: "2-digit",
            })}{" "}
            <span className="text-[10px] opacity-60">UTC</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
