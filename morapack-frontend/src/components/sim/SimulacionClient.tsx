"use client";

import { useEffect, useMemo, useState, useRef } from "react";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { Itinerario } from "@/types/itinerario";
import type { Aeropuerto, OrderSummary, OrderMetrics, SimulationPreview } from "@/types";
import { Play, Pause, Square, RotateCcw, Plane, Calendar, Clock, Gauge, PackageCheck, TrendingUp } from "lucide-react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { PedidosPanel } from "./PedidosPanel";

type SimulationState = 'IDLE' | 'STARTING' | 'RUNNING' | 'PAUSED' | 'STOPPED' | 'COMPLETED' | 'ERROR';
type ScenarioType = 'WEEKLY' | 'DAILY' | 'COLLAPSE';

export default function SimulacionClient() {
  const [itinerarios, setItinerarios] = useState<Itinerario[]>([]);
  const [aeropuertos, setAeropuertos] = useState<Aeropuerto[]>([]);
  const [connected, setConnected] = useState(false);
  const [client, setClient] = useState<Client | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  
  // Estados de simulación
  const [simulationState, setSimulationState] = useState<SimulationState>('IDLE');
  const [scenarioType, setScenarioType] = useState<ScenarioType>('WEEKLY');
  const [speed, setSpeed] = useState<number>(1);
  const [startDate, setStartDate] = useState<string>("2025-12-01"); // Default start date
  
  // Métricas en tiempo real
  const [currentIteration, setCurrentIteration] = useState(0);
  const [totalIterations, setTotalIterations] = useState(0);
  const [progress, setProgress] = useState(0);
  const [simulatedTime, setSimulatedTime] = useState("");
  const [interpolatedTime, setInterpolatedTime] = useState(""); // ✅ Tiempo interpolado
  const [message, setMessage] = useState("Cargando vista previa...");
  
  // ✅ Referencias para interpolación de tiempo
  const lastBackendTimeRef = useRef<string>("");
  const lastUpdateTimestampRef = useRef<number>(0);
  const K_MINUTES = 12; // K=12 → cada iteración avanza 60 minutos
  const ITERATION_DELAY_MS = 10000; // 10 segundos por iteración
  
  // ✅ Display time (actualizado cada segundo para evitar "palpitar")
  const [displayTime, setDisplayTime] = useState("");
  
  // Métricas finales
  const [finalMetrics, setFinalMetrics] = useState<any>(null);
  
  // Pedidos (preview + realtime)
  const [previewData, setPreviewData] = useState<SimulationPreview | null>(null);
  const [pedidos, setPedidos] = useState<OrderSummary[]>([]);
  const [metricasPedidos, setMetricasPedidos] = useState<OrderMetrics | null>(null);

  // Connect to WebSocket on mount
  useEffect(() => {
    const wsUrl = process.env.NEXT_PUBLIC_WS_URL || "http://localhost:8080/ws";
    const sock = new SockJS(wsUrl);
    const stompClient = new Client({
      webSocketFactory: () => sock as any,
      debug: (str) => console.log(str),
      onConnect: () => {
        console.log("Connected to WebSocket");
        setConnected(true);

        // Subscribe to control topic (to receive session ID)
        stompClient.subscribe("/topic/simulation-control", (message) => {
          try {
            const data = JSON.parse(message.body);
            console.log("Control message:", data);

            if (data.type === "SESSION_ID") {
              setSessionId(data.sessionId);
              console.log("Session ID received:", data.sessionId);

              // Subscribe to session-specific topic
              stompClient.subscribe(`/topic/simulation/${data.sessionId}`, (sessionMessage) => {
                try {
                  const update = JSON.parse(sessionMessage.body);
                  console.log("Simulation update:", update);

                  // Update state
                  setSimulationState(update.state);
                  setMessage(update.message || "");
                  setCurrentIteration(update.currentIteration || 0);
                  setTotalIterations(update.totalIterations || 0);
                  setProgress(update.progress || 0);
                  setSpeed(update.currentSpeed || 1);
                  
                  // ✅ Update simulated time with interpolation tracking
                  if (update.simulatedTime) {
                    setSimulatedTime(update.simulatedTime);
                    lastBackendTimeRef.current = update.simulatedTime;
                    lastUpdateTimestampRef.current = Date.now();
                    setInterpolatedTime(update.simulatedTime);
                  }

                  // Update map with latest results
                  if (update.latestResult) {
                    const result = update.latestResult;

                    // Update airports
                    if (result.aeropuertos && result.aeropuertos.length > 0) {
                      const mappedAeropuertos = result.aeropuertos.map((a: any) => ({
                        id: a.id,
                        nombre: a.nombre || a.codigo,
                        codigo: a.codigo,
                        ciudad: a.ciudad || a.codigo,
                        latitud: a.latitud,
                        longitud: a.longitud,
                        gmt: a.gmt,
                        esSede: a.esSede || false,
                        capacidadAlmacen: a.capacidadTotal || 1000,
                        // ✅ Capacity information
                        capacidadTotal: a.capacidadTotal || 1000,
                        capacidadUsada: a.capacidadUsada || 0,
                        capacidadDisponible: a.capacidadDisponible || 1000,
                        porcentajeUso: a.porcentajeUso || 0,
                        // Dynamic info
                        pedidosEnEspera: a.pedidosEnEspera || 0,
                        productosEnEspera: a.productosEnEspera || 0,
                        vuelosActivosDesde: a.vuelosActivosDesde || 0,
                        vuelosActivosHacia: a.vuelosActivosHacia || 0,
                        pais: { id: a.id, nombre: "Unknown", continente: "AMERICA" as const },
                      }));
                      setAeropuertos(mappedAeropuertos);
                    }

                    //Update pedidos
                    if (result.pedidos) {
                      setPedidos(result.pedidos);
                    }
                    if (result.metricas) {
                      setMetricasPedidos(result.metricas);
                    }

                    // Update itinerarios
                    if (result.itinerarios) {
                      const mappedItinerarios = result.itinerarios.map((itin: any) => ({
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
                              pais: { id: 0, nombre: "Unknown", continente: "Unknown" },
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
                              pais: { id: 0, nombre: "Unknown", continente: "Unknown" },
                            },
                          },
                        })),
                      }));
                      setItinerarios(mappedItinerarios);
                    }
                  }

                  // Store final metrics when completed
                  if (update.state === "COMPLETED" && update.latestResult) {
                    setFinalMetrics(update.latestResult);
                  }
                } catch (e) {
                  console.error("Error parsing simulation update:", e);
                }
              });
            }
          } catch (e) {
            console.error("Error parsing control message:", e);
          }
        });
      },
      onDisconnect: () => {
        console.log("Disconnected from WebSocket");
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error("STOMP error:", frame);
        setConnected(false);
      },
    });

    stompClient.activate();
    setClient(stompClient);

    return () => {
      stompClient.deactivate();
    };
  }, []);

  // ✅ Time interpolation: smooth time progression between backend updates
  useEffect(() => {
    if (simulationState !== 'RUNNING' || !lastBackendTimeRef.current) {
      return;
    }

    let animationFrameId: number;
    
    const interpolateTime = () => {
      const now = Date.now();
      const elapsedMs = now - lastUpdateTimestampRef.current;
      const totalIterationMs = ITERATION_DELAY_MS;
      const scMinutes = K_MINUTES * 5; // K * Sa (5 minutes)
      
      // Calculate how much simulated time should have passed
      const progress = Math.min(1, elapsedMs / totalIterationMs);
      const simulatedMinutesElapsed = progress * scMinutes;
      
      // Add elapsed time to last known backend time
      if (lastBackendTimeRef.current) {
        const backendDate = new Date(lastBackendTimeRef.current);
        const interpolatedDate = new Date(backendDate.getTime() + simulatedMinutesElapsed * 60 * 1000);
        setInterpolatedTime(interpolatedDate.toISOString());
      }
      
      animationFrameId = requestAnimationFrame(interpolateTime);
    };
    
    animationFrameId = requestAnimationFrame(interpolateTime);
    
    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [simulationState, lastBackendTimeRef.current]);

  // ✅ Update display time only once per second (reduce visual "palpitation")
  useEffect(() => {
    if (!interpolatedTime) {
      setDisplayTime(simulatedTime || "Esperando...");
      return;
    }

    // Format: "2025-12-01 09:13" (sin segundos para evitar palpitación)
    const formatTime = (isoString: string) => {
      const date = new Date(isoString);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    };

    setDisplayTime(formatTime(interpolatedTime));

    // Update display every second
    const intervalId = setInterval(() => {
      if (interpolatedTime) {
        setDisplayTime(formatTime(interpolatedTime));
      }
    }, 1000);

    return () => clearInterval(intervalId);
  }, [interpolatedTime, simulatedTime]);

  // Fetch preview data when date or scenario changes
  useEffect(() => {
    if (startDate && scenarioType && simulationState === 'IDLE') {
      const apiUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
      const customK = scenarioType === 'WEEKLY' ? 24 : undefined;
      const url = `${apiUrl}/api/simulation/preview?startDate=${startDate}&scenarioType=${scenarioType}${customK ? `&customK=${customK}` : ''}`;
      
      console.log("📡 Fetching preview:", url);
      
      fetch(url)
        .then(r => {
          if (!r.ok) {
            throw new Error(`HTTP ${r.status}: ${r.statusText}`);
          }
          return r.json();
        })
        .then((preview: SimulationPreview) => {
          console.log("✅ Preview received:", preview);
          setPreviewData(preview);
          setPedidos(preview.pedidos);
          
          // Update airports from preview
          if (preview.aeropuertos && preview.aeropuertos.length > 0) {
            const mappedAeropuertos = preview.aeropuertos.map((a: any) => ({
              id: a.id,
              nombre: a.nombre || a.codigo,
              codigo: a.codigo,
              ciudad: a.ciudad || a.codigo,
              latitud: a.latitud,
              longitud: a.longitud,
              gmt: a.gmt,
              esSede: a.esSede || false,
              capacidadAlmacen: a.capacidadTotal || 1000,
              // ✅ Capacity information
              capacidadTotal: a.capacidadTotal || 1000,
              capacidadUsada: a.capacidadUsada || 0,
              capacidadDisponible: a.capacidadDisponible || (a.capacidadTotal || 1000),
              porcentajeUso: a.porcentajeUso || 0,
              // Dynamic info
              pedidosEnEspera: a.pedidosEnEspera || 0,
              productosEnEspera: a.productosEnEspera || 0,
              vuelosActivosDesde: a.vuelosActivosDesde || 0,
              vuelosActivosHacia: a.vuelosActivosHacia || 0,
              pais: { 
                id: a.id, 
                nombre: "Unknown", 
                continente: "AMERICA" as const
              },
            }));
            setAeropuertos(mappedAeropuertos);
            console.log(`✅ Loaded ${mappedAeropuertos.length} airports from backend`);
          }
          
          // Calculate preview metrics
          const metrics: OrderMetrics = {
            totalPedidos: preview.totalPedidos,
            pendientes: preview.totalPedidos, // All pending in preview
            enTransito: 0,
            completados: 0,
            sinAsignar: 0,
            totalProductos: preview.totalProductos,
            productosAsignados: 0,
            tasaAsignacionPercent: 0
          };
          setMetricasPedidos(metrics);
          
          // Update message
          setMessage(`Vista previa: ${preview.totalPedidos} pedidos, ${preview.totalProductos} productos`);
        })
        .catch(err => {
          console.error("❌ Error fetching preview:", err);
          const apiUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
          console.warn(`Asegúrate de que el backend esté corriendo en ${apiUrl}`);
          
          // Set empty state so UI doesn't break
          setPedidos([]);
          setAeropuertos([]); // Clear airports on error
          setMetricasPedidos({
            totalPedidos: 0,
            pendientes: 0,
            enTransito: 0,
            completados: 0,
            sinAsignar: 0,
            totalProductos: 0,
            productosAsignados: 0,
            tasaAsignacionPercent: 0
          });
          setMessage(`❌ Error: No se pudo conectar con el backend`);
        });
    }
  }, [startDate, scenarioType, simulationState]);

  // Control handlers
  const sendControlMessage = (action: string, extras = {}) => {
    if (client && connected) {
      client.publish({
        destination: "/app/simulation/control",
        body: JSON.stringify({
          action,
          scenarioType,
          customK: scenarioType === 'WEEKLY' ? 24 : undefined, // K=24 (Sc=120 min)
          speed,
          startDate: action === 'START' ? startDate : undefined, // Only send startDate on START
          ...extras,
        }),
      });
      console.log(`Sent control: ${action}`, extras);
    }
  };

  const handleStart = () => sendControlMessage("START");
  const handlePause = () => sendControlMessage("PAUSE");
  const handleResume = () => sendControlMessage("RESUME");
  const handleStop = () => {
    sendControlMessage("STOP");
    setFinalMetrics(null);
    setItinerarios([]);
  };
  const handleReset = () => {
    sendControlMessage("RESET");
    setFinalMetrics(null);
    setItinerarios([]);
    setCurrentIteration(0);
    setProgress(0);
    // ✅ FIX: Limpiar pedidos y métricas al resetear
    setPedidos(previewData?.pedidos || []);
    setMetricasPedidos(previewData ? {
      totalPedidos: previewData.totalPedidos,
      pendientes: previewData.totalPedidos,
      enTransito: 0,
      completados: 0,
      sinAsignar: 0,
      totalProductos: previewData.totalProductos,
      productosAsignados: 0,
      tasaAsignacionPercent: 0
    } : null);
    setSimulatedTime("");
    setInterpolatedTime("");
    setDisplayTime("");
    lastBackendTimeRef.current = "";
    lastUpdateTimestampRef.current = 0;
  };
  const handleSpeedChange = (newSpeed: string) => {
    const speedValue = parseFloat(newSpeed);
    setSpeed(speedValue);
    sendControlMessage("SPEED", { speed: speedValue });
  };

  return (
    <div className="space-y-4">
      {/* Header con badges */}
      <div className="flex items-center justify-between">
        <h1 className="text-4xl font-bold tracking-tight">Simulación de vuelos</h1>
        
        <div className="flex items-center gap-4">
          {/* Estado */}
          <Badge variant={
            simulationState === 'RUNNING' ? 'default' :
            simulationState === 'COMPLETED' ? 'outline' :
            simulationState === 'ERROR' ? 'destructive' : 'secondary'
          } className="flex items-center gap-2 px-3 py-1">
            {simulationState}
          </Badge>
          
          {/* Itinerarios */}
          <Badge variant="outline" className="flex items-center gap-2 px-3 py-1">
            <Plane className="h-4 w-4" />
            Itinerarios: {itinerarios.length}
          </Badge>
          
          {/* Tiempo simulado */}
          <Badge variant="outline" className="flex items-center gap-2 px-3 py-1">
            <Calendar className="h-4 w-4" />
            {displayTime}
          </Badge>
        </div>
      </div>

      {/* Controles de simulación */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center justify-between gap-4">
            {/* Configuración izquierda */}
            <div className="flex items-center gap-6">
              {/* Escenario - Radio buttons */}
              <div className="flex items-center gap-3">
                <Label className="text-sm">Tipo:</Label>
                <RadioGroup 
                  value={scenarioType} 
                  onValueChange={(value: ScenarioType) => setScenarioType(value)}
                  disabled={simulationState === 'RUNNING' || simulationState === 'STARTING'}
                  className="flex items-center gap-4"
                >
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="WEEKLY" id="weekly" />
                    <Label htmlFor="weekly" className="font-normal cursor-pointer">
                      Semanal
                    </Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <RadioGroupItem value="COLLAPSE" id="collapse" />
                    <Label htmlFor="collapse" className="font-normal cursor-pointer">
                      Colapso
                    </Label>
                  </div>
                </RadioGroup>
              </div>

              {/* Fecha de inicio */}
              <div className="flex items-center gap-2">
                <Label className="text-sm flex items-center gap-1">
                  <Calendar className="h-4 w-4" />
                  Fecha inicio:
                </Label>
                <Input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  min="2025-12-01"
                  max="2025-12-31"
                  disabled={simulationState === 'RUNNING' || simulationState === 'STARTING'}
                  className="w-40"
                />
              </div>

              {/* Velocidad */}
              <div className="flex items-center gap-2">
                <Label className="text-sm flex items-center gap-1">
                  <Gauge className="h-4 w-4" />
                  Velocidad:
                </Label>
                <div className="flex items-center gap-2">
                  <Select 
                    value={speed.toString()} 
                    onValueChange={handleSpeedChange}
                    disabled={simulationState !== 'RUNNING'}
                  >
                    <SelectTrigger className="w-20">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="0.5">0.5x</SelectItem>
                      <SelectItem value="1">1x</SelectItem>
                      <SelectItem value="2">2x</SelectItem>
                      <SelectItem value="5">5x</SelectItem>
                      <SelectItem value="10">10x</SelectItem>
                    </SelectContent>
                  </Select>
                  {simulationState === 'RUNNING' && (
                    <span className="text-xs text-muted-foreground" title="Velocidad visual de aviones">
                      ({(800 * speed).toLocaleString()} km/h visual)
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Controles derecha */}
            <div className="flex items-center gap-3">
              {/* Progreso */}
              <div className="flex flex-col items-end min-w-[120px]">
                <span className="text-sm font-medium">
                  Iter: {currentIteration}/{totalIterations}
                </span>
                <span className="text-xs text-muted-foreground">
                  {Math.round(progress * 100)}%
                </span>
              </div>

              {/* Botones de control */}
              <Button
                variant="outline"
                size="icon"
                onClick={handleReset}
                disabled={!connected || simulationState === 'STARTING'}
                title="Reset"
              >
                <RotateCcw className="h-4 w-4" />
              </Button>

              <Button
                variant="outline"
                size="icon"
                onClick={handleStop}
                disabled={!connected || simulationState === 'IDLE' || simulationState === 'STOPPED'}
                title="Stop"
              >
                <Square className="h-4 w-4" />
              </Button>

              {simulationState === 'PAUSED' ? (
                <Button
                  variant="default"
                  size="icon"
                  onClick={handleResume}
                  disabled={!connected}
                  title="Resume"
                >
                  <Play className="h-4 w-4" />
                </Button>
              ) : simulationState === 'RUNNING' ? (
                <Button
                  variant="default"
                  size="icon"
                  onClick={handlePause}
                  disabled={!connected}
                  title="Pause"
                >
                  <Pause className="h-4 w-4" />
                </Button>
              ) : (
                <Button
                  variant="default"
                  size="icon"
                  onClick={handleStart}
                  disabled={!connected || simulationState === 'STARTING'}
                  title="Start"
                >
                  <Play className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>

          {/* Mensaje de estado */}
          {message && (
            <div className="mt-3 text-sm text-muted-foreground text-center">
              {message}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Layout: Mapa + Panel de Pedidos */}
      <div className="flex gap-4">
        {/* Mapa (principal) */}
        <div className="flex-1">
          <Card className="overflow-hidden h-[calc(100vh-14rem)]">
            <CardContent className="p-0 h-full">
                <AnimatedFlights
                  itinerarios={itinerarios}
                  aeropuertos={aeropuertos}
                  speedKmh={800 * speed}
                  simulatedTime={interpolatedTime || simulatedTime}
                  center={[-60, -15]}
                  zoom={3}
                  loop={false}
                />
            </CardContent>
          </Card>
        </div>
        
        {/* Panel de pedidos (sidebar) */}
        <div className="w-96">
          <Card className="overflow-hidden h-[calc(100vh-14rem)]">
            <PedidosPanel
              pedidos={pedidos}
              metricas={metricasPedidos}
              mode={simulationState === 'IDLE' ? 'preview' : 'realtime'}
              onSelectPedido={(id) => {
                console.log("Selected pedido:", id);
                // TODO: Resaltar ruta en el mapa
              }}
            />
          </Card>
        </div>
      </div>
    </div>
  );
}
