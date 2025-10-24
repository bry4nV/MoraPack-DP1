"use client";

import { useEffect, useMemo, useState } from "react";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AEROPUERTOS } from "@/data/aeropuertos";
import type { Itinerario } from "@/types/itinerario";
import type { Aeropuerto } from "@/types";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

export default function SimulacionClient() {
  const [itinerarios, setItinerarios] = useState<Itinerario[]>([]);
  const [aeropuertos, setAeropuertos] = useState<Aeropuerto[]>(AEROPUERTOS);
  const [connected, setConnected] = useState(false);
  const [running, setRunning] = useState(false);
  const [client, setClient] = useState<Client | null>(null);

  // Connect to WebSocket on mount
  useEffect(() => {
    const sock = new SockJS("http://localhost:8080/ws");
    const stompClient = new Client({
      webSocketFactory: () => sock as any,
      debug: (str) => console.log(str),
      onConnect: () => {
        console.log("✅ Connected to WebSocket");
        setConnected(true);

        // Subscribe to tabu simulation updates
        stompClient.subscribe("/topic/tabu-simulation", (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log("📦 Received snapshot:", data);

            // Update airports if provided
            if (data.aeropuertos && data.aeropuertos.length > 0) {
              const mappedAeropuertos = data.aeropuertos.map((a: any) => ({
                id: a.id,
                nombre: a.nombre || a.codigo,
                codigo: a.codigo,
                ciudad: a.ciudad || a.codigo,
                latitud: a.latitud,
                longitud: a.longitud,
                gmt: a.gmt,
                esSede: a.esSede || false,
                capacidadAlmacen: 1000,
                pais: {
                  id: a.id,
                  nombre: "Unknown",
                  continente: "Unknown",
                },
              }));
              setAeropuertos(mappedAeropuertos);
            }

            // Update itinerarios
            if (data.itinerarios) {
              const mappedItinerarios = data.itinerarios.map((itin: any) => ({
                id: itin.id,
                segmentos: (itin.segmentos || []).map((seg: any) => ({
                  id: `${itin.id}-${seg.orden}`,
                  orden: seg.orden,
                  vuelo: {
                    id: seg.vuelo.codigo,
                    codigo: seg.vuelo.codigo,
                    origen: {
                      id: 0,
                      nombre: seg.vuelo.origen.nombre || seg.vuelo.origen.codigo,
                      codigo: seg.vuelo.origen.codigo,
                      ciudad: seg.vuelo.origen.ciudad || seg.vuelo.origen.codigo,
                      latitud: seg.vuelo.origen.latitud,
                      longitud: seg.vuelo.origen.longitud,
                      gmt: seg.vuelo.origen.gmt,
                      esSede: seg.vuelo.origen.esSede,
                      capacidadAlmacen: 1000,
                      pais: {
                        id: 0,
                        nombre: "Unknown",
                        continente: "Unknown",
                      },
                    },
                    destino: {
                      id: 0,
                      nombre: seg.vuelo.destino.nombre || seg.vuelo.destino.codigo,
                      codigo: seg.vuelo.destino.codigo,
                      ciudad: seg.vuelo.destino.ciudad || seg.vuelo.destino.codigo,
                      latitud: seg.vuelo.destino.latitud,
                      longitud: seg.vuelo.destino.longitud,
                      gmt: seg.vuelo.destino.gmt,
                      esSede: seg.vuelo.destino.esSede,
                      capacidadAlmacen: 1000,
                      pais: {
                        id: 0,
                        nombre: "Unknown",
                        continente: "Unknown",
                      },
                    },
                  },
                })),
              }));
              setItinerarios(mappedItinerarios);
            }

            // Update running state
            if (data.meta) {
              setRunning(data.meta.running || false);
            }
          } catch (e) {
            console.error("Error parsing WebSocket message:", e);
          }
        });
      },
      onDisconnect: () => {
        console.log("❌ Disconnected from WebSocket");
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error("❌ STOMP error:", frame);
        setConnected(false);
      },
    });

    stompClient.activate();
    setClient(stompClient);

    return () => {
      stompClient.deactivate();
    };
  }, []);

  const handleStart = () => {
    if (client && connected) {
      client.publish({
        destination: "/app/tabu/init",
        body: JSON.stringify({
          seed: Date.now(),
          snapshotMs: 500,
        }),
      });
      console.log("▶️ Simulation started");
      // Actualizar el estado inmediatamente
      setRunning(true);
    }
  };

  const handleStop = () => {
    if (client && connected) {
      client.publish({
        destination: "/app/tabu/stop",
        body: "{}",
      });
      console.log("⏹️ Simulation stopped");
      // Actualizar el estado inmediatamente
      setRunning(false);
      // Limpiar itinerarios para detener la animación
      setItinerarios([]);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-4xl font-bold tracking-tight">Simulación en Tiempo Real</h1>
        <div className="flex items-center gap-3">
          <div className="text-sm">
            <span className={`inline-block w-3 h-3 rounded-full mr-2 ${connected ? "bg-green-500" : "bg-red-500"}`}></span>
            {connected ? "Conectado" : "Desconectado"}
          </div>
          <div className="text-sm">
            <strong>Vuelos:</strong> {itinerarios.length}
          </div>
          <Button variant="outline" onClick={handleStart} disabled={!connected || running}>
            ▶️ Iniciar Simulación
          </Button>
          <Button variant="outline" onClick={handleStop} disabled={!connected}>
            ⏹️ Detener
          </Button>
        </div>
      </div>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="h-[calc(100dvh-12rem)]">
            <AnimatedFlights
              itinerarios={itinerarios}
              aeropuertos={aeropuertos}
              center={[-60, -15]}
              zoom={3}
              loop={true}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
