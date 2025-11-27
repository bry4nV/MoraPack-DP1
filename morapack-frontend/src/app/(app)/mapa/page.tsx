"use client";

import { useMemo } from "react";
import { Card, CardContent } from "@/components/ui/card";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { useAirports } from "@/hooks/use-airports";
import type { Aeropuerto } from "@/types/aeropuerto";

export default function MapaPage() {
  const { airports, isLoading } = useAirports();

  // Airport y Aeropuerto son el mismo tipo, solo hacemos el cast
  const aeropuertos = useMemo(() => airports as Aeropuerto[], [airports]);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-4xl font-bold tracking-tight">Operaciones de día a día</h1>
        <Card>
          <CardContent className="p-8 text-center">
            Cargando aeropuertos...
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-4xl font-bold tracking-tight">Operaciones de día a día</h1>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="h-[calc(100dvh-12rem)]">
            <AnimatedFlights
              itinerarios={[]}
              aeropuertos={aeropuertos}
              speedKmh={900}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
