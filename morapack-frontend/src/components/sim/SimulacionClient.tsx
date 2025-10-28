"use client";

import { useEffect, useMemo, useState } from "react";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { AEROPUERTOS } from "@/data/aeropuertos";
import type { Itinerario } from "@/types/itinerario";
import type { Aeropuerto } from "@/types";
import { Play, Pause, Square, RotateCcw, Plane, Calendar, Clock } from "lucide-react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

type SimulationState = 'stopped' | 'running';

export default function SimulacionClient() {
  const [itinerarios, setItinerarios] = useState<Itinerario[]>([]);
  const [aeropuertos, setAeropuertos] = useState<Aeropuerto[]>(AEROPUERTOS);
  const [connected, setConnected] = useState(false);
  const [running, setRunning] = useState(false);
  const [client, setClient] = useState<Client | null>(null);
  const [simulationState, setSimulationState] = useState<SimulationState>('stopped');
  
  // Estados del formulario (solo visuales)
  const [simulationType, setSimulationType] = useState("semanal");
  const [startDate, setStartDate] = useState("");
  const [startTime, setStartTime] = useState("");

  // Estados funcionales
  const [planesInFlight, setPlanesInFlight] = useState(0);
  const [simulationDateTime, setSimulationDateTime] = useState("");
  const [elapsedTime, setElapsedTime] = useState(0);

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
              setPlanesInFlight(mappedItinerarios.length);
            }

            // Update running state
            if (data.meta) {
              setRunning(data.meta.running || false);
              if (data.meta.running) {
                setSimulationState('running');
                // Simular fecha y hora actual
                const now = new Date();
                setSimulationDateTime(`${now.toLocaleDateString()} - ${now.toLocaleTimeString()}`);
              }
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

  // Timer para mostrar tiempo transcurrido
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (simulationState === 'running') {
      interval = setInterval(() => {
        setElapsedTime(prev => prev + 1);
      }, 1000);
    } else if (simulationState === 'stopped') {
      setElapsedTime(0);
    }
    return () => clearInterval(interval);
  }, [simulationState]);

  const formatTime = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handlePlay = () => {
    if (client && connected) {
      client.publish({
        destination: "/app/tabu/init",
        body: JSON.stringify({
          seed: Date.now(),
          snapshotMs: 1000,
        }),
      });
      console.log("▶️ Simulation started");
      setSimulationState('running');
      setRunning(true);
    }
  };

  const handlePause = () => {
    if (client && connected) {
      client.publish({
        destination: "/app/tabu/stop",
        body: "{}",
      });
      console.log("⏸️ Simulation paused");
      setSimulationState('stopped');
      setRunning(false);
    }
  };

  const handleRestart = () => {
    if (client && connected) {
      client.publish({
        destination: "/app/tabu/stop",
        body: "{}",
      });
      setSimulationState('stopped');
      setRunning(false);
      setItinerarios([]);
      setPlanesInFlight(0);
      setSimulationDateTime("");
      
      // Reiniciar después de un breve delay
      setTimeout(() => handlePlay(), 200);
    }
  };

  return (
    <div className="space-y-4">
      {/* Header con badges */}
      <div className="flex items-center justify-between">
        <h1 className="text-4xl font-bold tracking-tight">Simulación de vuelos</h1>
        
        <div className="flex items-center gap-4">
          <Badge variant="outline" className="flex items-center gap-2 px-3 py-1">
            <Plane className="h-4 w-4" />
            Aviones en vuelo: {planesInFlight}
          </Badge>
          
          <Badge variant="outline" className="flex items-center gap-2 px-3 py-1">
            <Calendar className="h-4 w-4" />
            Fecha y hora de la simulación: {simulationDateTime || "23/10/2025 - 9:06:58 p. m."}
          </Badge>
        </div>
      </div>

      {/* Controles de simulación - Compacto */}
      <div className="bg-muted/30 rounded-lg p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-8">
            <div>
              <Label className="text-sm font-medium mb-2 block">Información de la simulación:</Label>
            </div>

            {/* Tipo de simulación */}
            <div className="flex items-center gap-2">
              <Label className="text-sm text-muted-foreground">Tipo:</Label>
              <RadioGroup 
                value={simulationType} 
                onValueChange={setSimulationType}
                className="flex items-center gap-4"
                disabled
              >
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="semanal" id="semanal" />
                  <Label htmlFor="semanal" className="text-sm">Semanal</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="colapso" id="colapso" />
                  <Label htmlFor="colapso" className="text-sm">Colapso</Label>
                </div>
              </RadioGroup>
            </div>

            {/* Fecha de inicio */}
            <div className="flex items-center gap-2">
              <Label className="text-sm text-muted-foreground">Fecha de inicio:</Label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                placeholder="dd/mm/aaaa"
                className="h-8 w-36 text-sm"
                disabled
              />
            </div>

            {/* Hora de inicio */}
            <div className="flex items-center gap-2">
              <Label className="text-sm text-muted-foreground">Hora de inicio:</Label>
              <Input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                placeholder="--:--"
                className="h-8 w-24 text-sm"
                disabled
              />
            </div>
          </div>

          {/* Controles de reproducción */}
          <div className="flex items-center gap-3">
            {/* Temporizador */}
            <div className="flex items-center gap-2 bg-background rounded-md px-3 py-2 border">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <span className="font-mono text-sm font-medium min-w-[60px]">
                {formatTime(elapsedTime)}
              </span>
            </div>

            <Button
              variant="ghost"
              size="icon"
              onClick={handleRestart}
              disabled={!connected}
              className="h-10 w-10 rounded-full hover:bg-muted"
              title="Reiniciar"
            >
              <RotateCcw className="h-5 w-5" />
            </Button>
            
            <Button
              variant="ghost"
              size="icon"
              onClick={simulationState === 'running' ? handlePause : handlePlay}
              disabled={!connected}
              className="h-10 w-10 rounded-full hover:bg-muted"
              title={simulationState === 'running' ? 'Pausar' : 'Iniciar'}
            >
              {simulationState === 'running' ? (
                <Pause className="h-5 w-5" />
              ) : (
                <Play className="h-5 w-5" />
              )}
            </Button>
          </div>
        </div>
      </div>

      {/* Mapa */}
      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="h-[calc(100vh-14rem)]">
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
