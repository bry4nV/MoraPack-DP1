"use client";

import { useMemo, useState } from "react";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AEROPUERTOS } from "@/data/aeropuertos";
import { buildItinerariosSemana, buildItinerariosColapso } from "@/data/simulacion";

type Modo = "semanal" | "colapso";

export default function SimulacionClient() {
  const [modo, setModo] = useState<Modo>("semanal");
  const [seed, setSeed] = useState(0);

  const itinerarios = useMemo(
    () => (modo === "semanal" ? buildItinerariosSemana(12) : buildItinerariosColapso(14)),
    [modo, seed]
  );
  const loop = modo === "colapso";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-4xl font-bold tracking-tight">Simulación</h1>
        <div className="flex items-center gap-3">
          <div className="inline-flex rounded-xl overflow-hidden border">
            <button
              className={`px-4 py-2 text-sm font-medium ${modo === "semanal" ? "bg-gray-900 text-white" : "bg-white text-gray-700"}`}
              onClick={() => setModo("semanal")}
            >
              Semanal
            </button>
            <button
              className={`px-4 py-2 text-sm font-medium border-l ${modo === "colapso" ? "bg-gray-900 text-white" : "bg-white text-gray-700"}`}
              onClick={() => setModo("colapso")}
            >
              Colapso
            </button>
          </div>
          <Button variant="outline" onClick={() => setSeed(s => s + 1)}>Regenerar vuelos</Button>
        </div>
      </div>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="h-[calc(100dvh-12rem)]">
            <AnimatedFlights
              itinerarios={itinerarios}
              aeropuertos={AEROPUERTOS}
              center={[-60, -15]}
              zoom={3}
              loop={loop}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
