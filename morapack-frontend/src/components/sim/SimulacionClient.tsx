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
import type { Itinerario } from "@/types/simulation/itinerary.types";
import type { Aeropuerto } from "@/types/aeropuerto";
import type { OrderSummary, OrderMetrics } from "@/types/simulation/order-summary.types";
import type { SimulationPreview } from "@/types/simulation/preview.types";
import { Play, Pause, Square, RotateCcw, Plane, Calendar, Clock, Gauge, PackageCheck, TrendingUp, ChevronLeft, ChevronRight, Timer, Menu, ChevronDown, ChevronUp } from "lucide-react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { PedidosPanel } from "./PedidosPanel";
import { EventosPanel } from "./EventosPanel";
import { getCancellations, getDynamicOrders } from "@/lib/dynamic-events-api";
import type { FlightCancellation, DynamicOrder } from "@/types/simulation/events.types";

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
  const [startDate, setStartDate] = useState<string>("2025-01-02"); // Default start date (primera fecha con datos)
  const [startTime, setStartTime] = useState<string>("00:00"); // Default start time

  // UI state
  const [isPanelOpen, setIsPanelOpen] = useState<boolean>(true);
  const [isHeaderOpen, setIsHeaderOpen] = useState<boolean>(true); // Controls panel
  const [elapsedSeconds, setElapsedSeconds] = useState<number>(0);
  
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
  const K_MINUTES = 24; // K=24 → cada iteración avanza 120 minutos (WEEKLY scenario)
  const ITERATION_DELAY_MS = 10000; // 10 segundos por iteración
  
  // ✅ Display time (actualizado cada segundo para evitar "palpitar")
  const [displayTime, setDisplayTime] = useState("");
  
  // Métricas finales
  const [finalMetrics, setFinalMetrics] = useState<any>(null);
  
  // Pedidos (preview + realtime)
  const [previewData, setPreviewData] = useState<SimulationPreview | null>(null);
  const [pedidos, setPedidos] = useState<OrderSummary[]>([]);
  const [metricasPedidos, setMetricasPedidos] = useState<OrderMetrics | null>(null);
  
  // Dynamic events
  const [cancellations, setCancellations] = useState<FlightCancellation[]>([]);
  const [dynamicOrders, setDynamicOrders] = useState<DynamicOrder[]>([]);

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
                  
                  // 🔍 DEBUG: Ver estructura completa de itinerarios
                  if (update.latestResult?.itinerarios && update.latestResult.itinerarios.length > 0) {
                    const firstItin = update.latestResult.itinerarios[0];
                    console.log("🔍 [SimulacionClient] Primer itinerario RAW del JSON:", {
                      id: firstItin.id,
                      orderId: firstItin.orderId,
                      numSegmentos: firstItin.segmentos?.length || 0,
                      firstSegment: firstItin.segmentos?.[0],
                      vueloKeys: firstItin.segmentos?.[0]?.vuelo ? Object.keys(firstItin.segmentos[0].vuelo) : [],
                      fullVueloJSON: JSON.stringify(firstItin.segmentos?.[0]?.vuelo),
                    });
                  }

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
                    if (result.airports && result.airports.length > 0) {
                      const mappedAeropuertos = result.airports.map((a: any) => ({
                        id: a.id,
                        nombre: a.name || a.code,
                        codigo: a.code,
                        ciudad: a.city || a.code,
                        latitud: a.latitude,
                        longitud: a.longitude,
                        gmt: a.gmt,
                        esSede: a.isHub || false,
                        capacidadAlmacen: a.totalCapacity || 1000,
                        // ✅ Capacity information
                        capacidadTotal: a.totalCapacity || 1000,
                        capacidadUsada: a.usedCapacity || 0,
                        capacidadDisponible: a.availableCapacity || 1000,
                        porcentajeUso: a.usagePercentage || 0,
                        // Dynamic info
                        pedidosEnEspera: a.pendingOrders || 0,
                        productosEnEspera: a.pendingProducts || 0,
                        vuelosActivosDesde: a.activeFlightsFrom || 0,
                        vuelosActivosHacia: a.activeFlightsTo || 0,
                        pais: { id: a.id, nombre: "Unknown", continente: "AMERICA" as const },
                      }));
                      setAeropuertos(mappedAeropuertos);
                    }

                    //Update orders
                    if (result.orders) {
                      setPedidos(result.orders);
                    }
                    if (result.metrics) {
                      setMetricasPedidos(result.metrics);
                    }

                    // Update itinerarios
                    if (result.itineraries) {
                      const mappedItinerarios = result.itineraries.map((itin: any) => ({
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
                            // ✅ FIX: Agregar las fechas que estaban faltando
                            salidaProgramadaISO: seg.vuelo.salidaProgramadaISO,
                            llegadaProgramadaISO: seg.vuelo.llegadaProgramadaISO,
                            capacidad: seg.vuelo.capacidad,
                            preplanificado: seg.vuelo.preplanificado,
                            estado: seg.vuelo.estado,
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

  // ✅ Elapsed time counter: track real-world time spent in simulation
  useEffect(() => {
    let intervalId: NodeJS.Timeout | null = null;

    if (simulationState === 'RUNNING') {
      intervalId = setInterval(() => {
        setElapsedSeconds(prev => prev + 1);
      }, 1000);
    } else if (simulationState === 'IDLE' || simulationState === 'STOPPED') {
      setElapsedSeconds(0);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [simulationState]);

  // Format elapsed time as HH:MM:SS
  const formatElapsedTime = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // ✅ Time interpolation: smooth time progression between backend updates
  useEffect(() => {
    if (simulationState !== 'RUNNING') {
      return;
    }

    console.log('[SimulacionClient] 🚀 Interpolación ACTIVADA');
    
    let animationFrameId: number;
    let frameCount = 0;
    
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
        // ✅ FIX: Formatear como hora local (sin Z) para coincidir con el backend
        const isoLocal = interpolatedDate.getFullYear() + '-' +
          String(interpolatedDate.getMonth() + 1).padStart(2, '0') + '-' +
          String(interpolatedDate.getDate()).padStart(2, '0') + 'T' +
          String(interpolatedDate.getHours()).padStart(2, '0') + ':' +
          String(interpolatedDate.getMinutes()).padStart(2, '0') + ':' +
          String(interpolatedDate.getSeconds()).padStart(2, '0');
        
        // 🔍 DEBUG CRÍTICO: Log cada 60 frames
        if (frameCount % 60 === 0) {
          console.log('[SimulacionClient] 🔍 INTERPOLACIÓN:', {
            backendTime: lastBackendTimeRef.current,
            elapsedMs: elapsedMs.toFixed(0),
            progress: (progress * 100).toFixed(1) + '%',
            minutesElapsed: simulatedMinutesElapsed.toFixed(2),
            interpolatedTime: isoLocal,
            willSetTo: isoLocal,
          });
        }
        frameCount++;
        
        setInterpolatedTime(isoLocal);
      }
      
      animationFrameId = requestAnimationFrame(interpolateTime);
    };
    
    animationFrameId = requestAnimationFrame(interpolateTime);
    
    return () => {
      console.log('[SimulacionClient] 🛑 Interpolación DESACTIVADA');
      cancelAnimationFrame(animationFrameId);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
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

  // Load dynamic events when connected
  useEffect(() => {
    if (connected) {
      loadDynamicEvents();
    }
  }, [connected]);

  const loadDynamicEvents = async () => {
    try {
      const [cancellationsData, ordersData] = await Promise.all([
        getCancellations(),
        getDynamicOrders(),
      ]);
      setCancellations(cancellationsData);
      setDynamicOrders(ordersData);
    } catch (error) {
      console.error('Error loading dynamic events:', error);
    }
  };

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
          setPedidos(preview.orders);

          // Update airports from preview
          if (preview.airports && preview.airports.length > 0) {
            const mappedAeropuertos = preview.airports.map((a: any) => ({
              id: a.id,
              nombre: a.name || a.code,
              codigo: a.code,
              ciudad: a.city || a.code,
              latitud: a.latitude,
              longitud: a.longitude,
              gmt: a.gmt,
              esSede: a.isHub || false,
              capacidadAlmacen: a.totalCapacity || 1000,
              // ✅ Capacity information
              capacidadTotal: a.totalCapacity || 1000,
              capacidadUsada: a.usedCapacity || 0,
              capacidadDisponible: a.availableCapacity || (a.totalCapacity || 1000),
              porcentajeUso: a.usagePercentage || 0,
              // Dynamic info
              pedidosEnEspera: a.pendingOrders || 0,
              productosEnEspera: a.pendingProducts || 0,
              vuelosActivosDesde: a.activeFlightsFrom || 0,
              vuelosActivosHacia: a.activeFlightsTo || 0,
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
            totalOrders: preview.totalOrders,
            pending: preview.totalOrders, // All pending in preview
            inTransit: 0,
            completed: 0,
            unassigned: 0,
            totalProducts: preview.totalProducts,
            assignedProducts: 0,
            assignmentRatePercent: 0
          };
          setMetricasPedidos(metrics);

          // Update message
          setMessage(`Vista previa: ${preview.totalOrders} pedidos, ${preview.totalProducts} productos`);
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
      // Combine date and time for START action
      const startDateTime = action === 'START'
        ? `${startDate}T${startTime}:00`
        : undefined;

      client.publish({
        destination: "/app/simulation/control",
        body: JSON.stringify({
          action,
          scenarioType,
          customK: scenarioType === 'WEEKLY' ? 24 : undefined, // K=24 (Sc=120 min)
          speed,
          startDate: startDateTime, // Send combined date+time as ISO string
          ...extras,
        }),
      });
      console.log(`Sent control: ${action}`, { startDateTime, ...extras });
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
    setPedidos(previewData?.orders || []);
    setMetricasPedidos(previewData ? {
      totalOrders: previewData.totalOrders,
      pending: previewData.totalOrders,
      inTransit: 0,
      completed: 0,
      unassigned: 0,
      totalProducts: previewData.totalProducts,
      assignedProducts: 0,
      assignmentRatePercent: 0
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
    <div className="space-y-2">
      {/* Header mínimo - SIEMPRE VISIBLE */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">Simulación</h1>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setIsHeaderOpen(!isHeaderOpen)}
            className="h-7 gap-1.5"
          >
            <Menu className="h-3.5 w-3.5" />
            <span className="text-xs">Controles</span>
            {isHeaderOpen ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
          </Button>
        </div>

        {/* Badges siempre visibles (info crítica) */}
        <div className="flex items-center gap-2">
          <Badge variant={
            simulationState === 'RUNNING' ? 'default' :
            simulationState === 'COMPLETED' ? 'outline' :
            simulationState === 'ERROR' ? 'destructive' : 'secondary'
          } className="text-[10px] px-2 py-0.5">
            {simulationState}
          </Badge>
          <Badge variant="outline" className="text-[10px] px-2 py-0.5">
            {currentIteration}/{totalIterations}
          </Badge>
        </div>
      </div>

      {/* Panel de controles - COLAPSABLE */}
      {isHeaderOpen && (
        <Card>
        <CardContent className="pt-4 pb-4">
          <div className="flex items-center justify-between gap-3">
            {/* Configuración izquierda - MÁS COMPACTA */}
            <div className="flex items-center gap-3">
              {/* Escenario - Radio buttons */}
              <div className="flex items-center gap-2">
                <Label className="text-xs text-muted-foreground">Tipo:</Label>
                <RadioGroup
                  value={scenarioType}
                  onValueChange={(value: ScenarioType) => setScenarioType(value)}
                  disabled={simulationState === 'RUNNING' || simulationState === 'STARTING'}
                  className="flex items-center gap-3"
                >
                  <div className="flex items-center space-x-1.5">
                    <RadioGroupItem value="WEEKLY" id="weekly" className="h-3.5 w-3.5" />
                    <Label htmlFor="weekly" className="text-xs font-normal cursor-pointer">
                      Semanal
                    </Label>
                  </div>
                  <div className="flex items-center space-x-1.5">
                    <RadioGroupItem value="COLLAPSE" id="collapse" className="h-3.5 w-3.5" />
                    <Label htmlFor="collapse" className="text-xs font-normal cursor-pointer">
                      Colapso
                    </Label>
                  </div>
                </RadioGroup>
              </div>

              {/* Fecha y hora de inicio */}
              <div className="flex items-center gap-2">
                <Label className="text-xs text-muted-foreground flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" />
                  Inicio:
                </Label>
                <div className="flex items-center gap-1">
                  <Input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    min="2025-01-02"
                    max="2025-01-31"
                    disabled={simulationState === 'RUNNING' || simulationState === 'STARTING'}
                    className="w-32 h-8 text-xs"
                  />
                  <Input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    disabled={simulationState === 'RUNNING' || simulationState === 'STARTING'}
                    className="w-24 h-8 text-xs"
                  />
                </div>
              </div>

              {/* Velocidad */}
              <div className="flex items-center gap-2">
                <Label className="text-xs text-muted-foreground flex items-center gap-1">
                  <Gauge className="h-3.5 w-3.5" />
                  Vel:
                </Label>
                <Select
                  value={speed.toString()}
                  onValueChange={handleSpeedChange}
                  disabled={simulationState !== 'RUNNING'}
                >
                  <SelectTrigger className="w-16 h-8 text-xs">
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
              </div>
            </div>

            {/* Controles derecha - MÁS COMPACTO */}
            <div className="flex items-center gap-2">
              {/* Progreso */}
              <div className="flex flex-col items-end min-w-[100px]">
                <span className="text-xs font-medium">
                  {currentIteration}/{totalIterations}
                </span>
                <span className="text-[10px] text-muted-foreground">
                  {Math.round(progress * 100)}%
                </span>
              </div>

              {/* Botones de control - MÁS PEQUEÑOS */}
              <Button
                variant="outline"
                size="sm"
                onClick={handleReset}
                disabled={!connected || simulationState === 'STARTING'}
                title="Reset"
                className="h-8 w-8 p-0"
              >
                <RotateCcw className="h-3.5 w-3.5" />
              </Button>

              <Button
                variant="outline"
                size="sm"
                onClick={handleStop}
                disabled={!connected || simulationState === 'IDLE' || simulationState === 'STOPPED'}
                title="Stop"
                className="h-8 w-8 p-0"
              >
                <Square className="h-3.5 w-3.5" />
              </Button>

              {simulationState === 'PAUSED' ? (
                <Button
                  variant="default"
                  size="sm"
                  onClick={handleResume}
                  disabled={!connected}
                  title="Resume"
                  className="h-8 w-8 p-0"
                >
                  <Play className="h-3.5 w-3.5" />
                </Button>
              ) : simulationState === 'RUNNING' ? (
                <Button
                  variant="default"
                  size="sm"
                  onClick={handlePause}
                  disabled={!connected}
                  title="Pause"
                  className="h-8 w-8 p-0"
                >
                  <Pause className="h-3.5 w-3.5" />
                </Button>
              ) : (
                <Button
                  variant="default"
                  size="sm"
                  onClick={handleStart}
                  disabled={!connected || simulationState === 'STARTING'}
                  title="Start"
                  className="h-8 w-8 p-0"
                >
                  <Play className="h-3.5 w-3.5" />
                </Button>
              )}
            </div>
          </div>

          {/* Mensaje de estado */}
          {message && (
            <div className="mt-2 text-xs text-muted-foreground text-center">
              {message}
            </div>
          )}
        </CardContent>
      </Card>
      )}

      {/* Layout: Mapa + Panel de Pedidos/Eventos COLAPSABLE */}
      <div className="relative flex gap-2">
        {/* Mapa (principal) - OCUPA CASI TODA LA PANTALLA */}
        <div className={`transition-all duration-300 ${isPanelOpen ? 'flex-1' : 'w-full'}`}>
          <Card className="overflow-hidden h-[calc(100vh-8rem)]">
            <CardContent className="p-0 h-full relative">
              <AnimatedFlights
                itinerarios={itinerarios}
                aeropuertos={aeropuertos}
                speedKmh={800 * speed}
                simulatedTime={interpolatedTime || simulatedTime}
                center={[-60, -15]}
                zoom={3}
                loop={false}
              />

              {/* INFO FLOTANTE SOBRE EL MAPA - SIEMPRE VISIBLE */}
              <div className="absolute top-3 left-3 z-10 flex flex-col gap-2">
                {/* Tiempo simulado - GRANDE Y VISIBLE */}
                <Badge variant="secondary" className="text-sm px-3 py-1.5 shadow-lg bg-white/95 backdrop-blur">
                  <Calendar className="h-4 w-4 mr-1.5" />
                  {displayTime || "Cargando..."}
                </Badge>

                {/* Tiempo transcurrido (real) */}
                {simulationState === 'RUNNING' && (
                  <Badge variant="secondary" className="text-xs px-2.5 py-1 shadow-lg bg-white/95 backdrop-blur">
                    <Timer className="h-3.5 w-3.5 mr-1" />
                    {formatElapsedTime(elapsedSeconds)}
                  </Badge>
                )}

                {/* Itinerarios activos */}
                {itinerarios.length > 0 && (
                  <Badge variant="outline" className="text-xs px-2.5 py-1 shadow-lg bg-white/95 backdrop-blur">
                    <Plane className="h-3.5 w-3.5 mr-1" />
                    {itinerarios.length} vuelos
                  </Badge>
                )}
              </div>

              {/* Botón para toggle panel lateral - DERECHA */}
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setIsPanelOpen(!isPanelOpen)}
                className="absolute top-3 right-3 z-10 h-8 w-8 p-0 shadow-lg"
                title={isPanelOpen ? "Ocultar panel" : "Mostrar panel"}
              >
                {isPanelOpen ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
              </Button>
            </CardContent>
          </Card>
        </div>

        {/* Panel lateral con tabs (Pedidos + Eventos) - COLAPSABLE */}
        {isPanelOpen && (
          <div className="w-80 transition-all duration-300">
            <Card className="overflow-hidden h-[calc(100vh-8rem)]">
              <Tabs defaultValue="pedidos" className="h-full flex flex-col">
                <TabsList className="grid grid-cols-2 m-2 mb-0">
                  <TabsTrigger value="pedidos" className="text-xs">
                    Pedidos
                  </TabsTrigger>
                  <TabsTrigger value="eventos" className="text-xs">
                    Eventos
                  </TabsTrigger>
                </TabsList>

              <TabsContent value="pedidos" className="flex-1 m-0 overflow-hidden">
                <PedidosPanel
                  pedidos={pedidos.filter(p => {
                    // En preview mode, mostrar todos
                    if (simulationState === 'IDLE') return true;
                    
                    // En modo realtime, solo mostrar pedidos que ya "llegaron" según el reloj
                    const currentSimTime = new Date(simulatedTime || interpolatedTime);
                    const orderTime = new Date(p.requestDateISO);
                    return orderTime <= currentSimTime;
                  })}
                  metricas={metricasPedidos}
                  mode={simulationState === 'IDLE' ? 'preview' : 'realtime'}
                  onSelectPedido={(id) => {
                    console.log("Selected pedido:", id);
                  }}
                />
              </TabsContent>

                <TabsContent value="eventos" className="flex-1 m-0 overflow-hidden">
                  <EventosPanel
                    aeropuertos={aeropuertos}
                    cancellations={cancellations}
                    dynamicOrders={dynamicOrders}
                    onCancellationCreated={(c) => setCancellations([...cancellations, c])}
                    onOrderCreated={(o) => setDynamicOrders([...dynamicOrders, o])}
                    onRefresh={loadDynamicEvents}
                  />
                </TabsContent>
              </Tabs>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}
