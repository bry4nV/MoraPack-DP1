"use client";

import { useEffect, useMemo, useState, useRef, useCallback, memo } from "react";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { Itinerario } from "@/types/simulation/itinerary.types";
import type { Aeropuerto } from "@/types/aeropuerto";
import type { OrderSummary, OrderMetrics } from "@/types/simulation/order-summary.types";
import type { SimulationPreview } from "@/types/simulation/preview.types";
import { Play, Pause, Square, RotateCcw, Plane, Calendar, Clock, Gauge, PackageCheck, TrendingUp, ChevronLeft, ChevronRight, Timer, Menu, ChevronDown, ChevronUp, FileText, Share2, Check, AlertCircle } from "lucide-react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { PedidosPanel } from "./PedidosPanel";
import { EventosPanel } from "./EventosPanel";
import { VuelosPanel } from "./VuelosPanel";
import { ReporteFinalModal } from "./ReporteFinalModal";
import { getCancellations } from "@/lib/dynamic-events-api";
import type { FlightCancellation } from "@/types/simulation/events.types";
import type { FinalReport } from "@/types/simulation/final-report.types";
import { getFinalReport } from "@/lib/final-report-api";
import { dmsToDecimal } from "@/lib/geo";

type SimulationState = 'IDLE' | 'STARTING' | 'RUNNING' | 'PAUSED' | 'STOPPED' | 'COMPLETED' | 'COLLAPSED' | 'ERROR';
type ScenarioType = 'WEEKLY' | 'DAILY' | 'COLLAPSE';

interface SimulacionClientProps {
  sharedSessionId?: string; // Optional session ID for joining existing simulation
}

export default function SimulacionClient({ sharedSessionId }: SimulacionClientProps = {}) {
  const [itinerarios, setItinerarios] = useState<Itinerario[]>([]);
  const [aeropuertos, setAeropuertos] = useState<Aeropuerto[]>([]);
  const [connected, setConnected] = useState(false);
  const [client, setClient] = useState<Client | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  
  // Estados de simulación
  const [simulationState, setSimulationState] = useState<SimulationState>('IDLE');
  const [scenarioType, setScenarioType] = useState<ScenarioType>('WEEKLY');
  const [speed, setSpeed] = useState<number>(1);
  const [startDate, setStartDate] = useState<string>("2025-01-02"); // Default start date
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

  // Final report modal
  const [showFinalReport, setShowFinalReport] = useState(false);
  const [finalReport, setFinalReport] = useState<FinalReport | null>(null);

  // Collapse alert modal
  const [showCollapseAlert, setShowCollapseAlert] = useState(false);
  const [collapseReason, setCollapseReason] = useState<string>("");

  // Share functionality
  const [urlCopied, setUrlCopied] = useState(false);

  // Pedidos (preview + realtime)
  const [previewData, setPreviewData] = useState<SimulationPreview | null>(null);
  const [pedidos, setPedidos] = useState<OrderSummary[]>([]);
  const [metricasPedidos, setMetricasPedidos] = useState<OrderMetrics | null>(null);

  // Dynamic events
  const [cancellations, setCancellations] = useState<FlightCancellation[]>([]);

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

        // If joining shared session, connect directly
        if (sharedSessionId) {
          setSessionId(sharedSessionId);
          console.log("Joining shared session:", sharedSessionId);
          setSimulationState('RUNNING'); // Assume shared session is already running

          // Subscribe to session-specific topic
          stompClient.subscribe(`/topic/simulation/${sharedSessionId}`, (sessionMessage) => {
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
                });
              }

              // Update simulation state
              if (update.state) {
                setSimulationState(update.state);
                setMessage(update.message || "");
              }

              // Update metrics
              if (update.currentIteration !== undefined) setCurrentIteration(update.currentIteration);
              if (update.totalIterations !== undefined) setTotalIterations(update.totalIterations);
              if (update.simulatedTime) {
                setSimulatedTime(update.simulatedTime);
                lastBackendTimeRef.current = update.simulatedTime;
                lastUpdateTimestampRef.current = Date.now();

                // Refresh cancellations on each iteration to update status
                getCancellations().then(data => setCancellations(data)).catch(err => console.error('Error refreshing cancellations:', err));
              }

              // Update orders if present
              if (update.latestResult?.orders) {
                setPedidos(update.latestResult.orders);
              }

              // Update metrics if present
              if (update.latestResult?.metrics) {
                setMetricasPedidos(update.latestResult.metrics);
              }

              // Update itineraries
              if (update.latestResult?.itinerarios) {
                const mappedItinerarios = update.latestResult.itinerarios.map((itin: any) => ({
                  id: itin.id,
                  pedidoId: itin.orderId,
                  segmentos: itin.segments.map((seg: any) => ({
                    numeroSegmento: seg.segmentNumber,
                    vuelo: {
                      origen: {
                        codigo: seg.flight.origin.code,
                        nombre: seg.flight.origin.name || seg.flight.origin.code,
                        latitude: seg.flight.origin.latitude,
                        longitude: seg.flight.origin.longitude,
                        latitud: seg.flight.origin.latitude,
                        longitud: seg.flight.origin.longitude,
                        gmt: seg.flight.origin.gmt,
                        esSede: seg.flight.origin.isHub || false,
                        capacidadAlmacen: seg.flight.origin.totalCapacity || 1000,
                        pais: { id: 0, nombre: "Unknown", continente: "Unknown" },
                      },
                      destino: {
                        codigo: seg.flight.destination.code,
                        nombre: seg.flight.destination.name || seg.flight.destination.code,
                        latitude: seg.flight.destination.latitude,
                        longitude: seg.flight.destination.longitude,
                        latitud: seg.flight.destination.latitude,
                        longitud: seg.flight.destination.longitude,
                        gmt: seg.flight.destination.gmt,
                        esSede: seg.flight.destination.isHub || false,
                        capacidadAlmacen: seg.flight.destination.totalCapacity || 1000,
                        pais: { id: 0, nombre: "Unknown", continente: "Unknown" },
                      },
                      salidaProgramadaISO: seg.flight.scheduledDepartureISO,
                      llegadaProgramadaISO: seg.flight.scheduledArrivalISO,
                      capacidad: seg.flight.capacity,
                      preplanificado: seg.flight.preplanned,
                      estado: seg.flight.status,
                    },
                  })),
                }));
                setItinerarios(mappedItinerarios);
              }

              // Store final metrics when completed
              if (update.state === "COMPLETED" && update.latestResult) {
                setFinalMetrics(update.latestResult);
                // Fetch and show final report
                if (sharedSessionId) {
                  getFinalReport(sharedSessionId)
                    .then((report) => {
                      setFinalReport(report);
                      setShowFinalReport(true);
                    })
                    .catch((error) => {
                      console.error("Error fetching final report:", error);
                    });
                }
              }

              // Handle collapse detection
              if (update.state === "COLLAPSED") {
                setCollapseReason(update.errorMessage || "Sistema colapsado");
                setShowCollapseAlert(true);
                // Also fetch final report for COLLAPSE scenario
                if (sharedSessionId) {
                  getFinalReport(sharedSessionId)
                    .then((report) => {
                      setFinalReport(report);
                    })
                    .catch((error) => {
                      console.error("Error fetching final report:", error);
                    });
                }
              }
            } catch (e) {
              console.error("Error parsing simulation update:", e);
            }
          });

          return; // Skip regular session ID logic
        }

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
                  setProgress((update.progressPercentage || 0) / 100); // Backend sends 0-100, we need 0-1
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

                    // 🔍 DEBUG: Log what data arrives from WebSocket
                    console.log('📡 WebSocket latestResult:', {
                      airports: result.airports?.length || 0,
                      orders: result.orders?.length || 0,
                      itineraries: result.itineraries?.length || 0,
                      firstItinerary: result.itineraries?.[0],
                      firstAirport: result.airports?.[0]
                    });

                    // Update airports
                    if (result.airports && result.airports.length > 0) {
                      const mappedAeropuertos = result.airports.map((a: any) => ({
                        id: a.id,
                        // ✅ FIX CRÍTICO: Campos en inglés requeridos por Aeropuerto interface
                        continent: a.continent || "AMERICA",
                        code: a.code,
                        city: a.city || a.code,
                        country: a.country || "Unknown",
                        cityAcronym: a.cityAcronym || a.code,
                        // ✅ CRÍTICO: Convertir coordenadas DMS a decimal
                        latitude: dmsToDecimal(a.latitude),
                        longitude: dmsToDecimal(a.longitude),
                        capacity: a.totalCapacity || 1000,
                        status: "ACTIVE",
                        isHub: a.isHub || false,
                        // Campos en español para compatibilidad
                        nombre: a.name || a.code,
                        codigo: a.code,
                        ciudad: a.city || a.code,
                        latitud: dmsToDecimal(a.latitude),
                        longitud: dmsToDecimal(a.longitude),
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
                        // ✅ FIX: Backend envía 'segments' (inglés), no 'segmentos' (español)
                        segmentos: (itin.segments || []).map((seg: any) => ({
                          id: `${itin.id}-${seg.order}`,
                          orden: seg.order,
                          vuelo: {
                            // Backend envía 'flight.code' (inglés)
                            id: seg.flight.code,
                            codigo: seg.flight.code,
                            origen: {
                              id: 0,
                              // Backend envía 'origin' (inglés)
                              nombre: seg.flight.origin.name || seg.flight.origin.code,
                              codigo: seg.flight.origin.code,
                              ciudad: seg.flight.origin.city || seg.flight.origin.code,
                              // ✅ CRÍTICO: Backend ya envía decimal, no DMS
                              latitude: seg.flight.origin.latitude,
                              longitude: seg.flight.origin.longitude,
                              // Campos en español para compatibilidad
                              latitud: seg.flight.origin.latitude,
                              longitud: seg.flight.origin.longitude,
                              gmt: seg.flight.origin.gmt,
                              esSede: seg.flight.origin.isHub || false,
                              capacidadAlmacen: seg.flight.origin.totalCapacity || 1000,
                              pais: { id: 0, nombre: "Unknown", continente: "Unknown" },
                            },
                            destino: {
                              id: 0,
                              // Backend envía 'destination' (inglés)
                              nombre: seg.flight.destination.name || seg.flight.destination.code,
                              codigo: seg.flight.destination.code,
                              ciudad: seg.flight.destination.city || seg.flight.destination.code,
                              // ✅ CRÍTICO: Backend ya envía decimal, no DMS
                              latitude: seg.flight.destination.latitude,
                              longitude: seg.flight.destination.longitude,
                              // Campos en español para compatibilidad
                              latitud: seg.flight.destination.latitude,
                              longitud: seg.flight.destination.longitude,
                              gmt: seg.flight.destination.gmt,
                              esSede: seg.flight.destination.isHub || false,
                              capacidadAlmacen: seg.flight.destination.totalCapacity || 1000,
                              pais: { id: 0, nombre: "Unknown", continente: "Unknown" },
                            },
                            // ✅ FIX: Backend envía scheduledDepartureISO/scheduledArrivalISO (inglés)
                            salidaProgramadaISO: seg.flight.scheduledDepartureISO,
                            llegadaProgramadaISO: seg.flight.scheduledArrivalISO,
                            capacidad: seg.flight.capacity,
                            preplanificado: seg.flight.preplanned,
                            estado: seg.flight.status,
                          },
                        })),
                      }));
                      setItinerarios(mappedItinerarios);
                    }
                  }

                  // Store final metrics when completed
                  if (update.state === "COMPLETED" && update.latestResult) {
                    setFinalMetrics(update.latestResult);
                    // Fetch and show final report
                    if (sessionId) {
                      getFinalReport(sessionId)
                        .then((report) => {
                          setFinalReport(report);
                          setShowFinalReport(true);
                        })
                        .catch((error) => {
                          console.error("Error fetching final report:", error);
                        });
                    }
                  }

                  // Handle collapse detection
                  if (update.state === "COLLAPSED") {
                    setCollapseReason(update.errorMessage || "Sistema colapsado");
                    setShowCollapseAlert(true);
                    // Also fetch final report for COLLAPSE scenario
                    if (sessionId) {
                      getFinalReport(sessionId)
                        .then((report) => {
                          setFinalReport(report);
                        })
                        .catch((error) => {
                          console.error("Error fetching final report:", error);
                        });
                    }
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
    }
    // Timer stops automatically when state changes from RUNNING

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

  // Memoized event handlers for EventosPanel
  const loadDynamicEvents = useCallback(async () => {
    try {
      const cancellationsData = await getCancellations();
      setCancellations(cancellationsData);
    } catch (error) {
      console.error('Error loading dynamic events:', error);
    }
  }, []);

  const handleCancellationCreated = useCallback((c: FlightCancellation) => {
    setCancellations(prev => [...prev, c]);
  }, []);

  // Load dynamic events when connected
  useEffect(() => {
    if (connected) {
      loadDynamicEvents();
    }
  }, [connected, loadDynamicEvents]);

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
              // Backend fields (English)
              id: a.id,
              continent: a.continent || "AMERICA",
              code: a.code,
              city: a.city || a.code,
              country: a.country || "Unknown",
              cityAcronym: a.cityAcronym || a.code,
              gmt: a.gmt || 0,
              capacity: a.capacity || 1000,
              // ✅ CRÍTICO: Convertir coordenadas DMS a decimal
              latitude: dmsToDecimal(a.latitude || "0"),
              longitude: dmsToDecimal(a.longitude || "0"),
              status: a.status || "ACTIVE",
              isHub: a.isHub || false,
              // UI tracking fields (Spanish - optional)
              capacidadTotal: a.totalCapacity || a.capacity || 1000,
              capacidadUsada: a.usedCapacity || 0,
              capacidadDisponible: a.availableCapacity || (a.capacity || 1000),
              porcentajeUso: a.usagePercentage || 0,
              pedidosEnEspera: a.pendingOrders || 0,
              productosEnEspera: a.pendingProducts || 0,
              vuelosActivosDesde: a.activeFlightsFrom || 0,
              vuelosActivosHacia: a.activeFlightsTo || 0,
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
        })
        .catch(err => {
          console.error("Error fetching preview:", err);
          const apiUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
          console.warn(`Asegúrate de que el backend esté corriendo en ${apiUrl}`);
          
          // Set empty state so UI doesn't break
          setPedidos([]);
          setAeropuertos([]); // Clear airports on error
          setMetricasPedidos({
            totalOrders: 0,
            pending: 0,
            inTransit: 0,
            completed: 0,
            unassigned: 0,
            totalProducts: 0,
            assignedProducts: 0,
            assignmentRatePercent: 0
          });
          setMessage(`❌ Error: No se pudo conectar con el backend`);
        });
    }
  }, [startDate, scenarioType, simulationState]);

  // Control handlers - Memoized for performance
  const sendControlMessage = useCallback((action: string, extras = {}) => {
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
  }, [client, connected, startDate, startTime, scenarioType, speed]);

  const handleStart = useCallback(() => sendControlMessage("START"), [sendControlMessage]);
  const handlePause = useCallback(() => sendControlMessage("PAUSE"), [sendControlMessage]);
  const handleResume = useCallback(() => sendControlMessage("RESUME"), [sendControlMessage]);
  const handleStop = useCallback(() => {
    sendControlMessage("STOP");
    setFinalMetrics(null);
    setItinerarios([]);
  }, [sendControlMessage]);

  const handleReset = useCallback(() => {
    sendControlMessage("RESET");
    setFinalMetrics(null);
    setItinerarios([]);
    setCurrentIteration(0);
    setProgress(0);
    setElapsedSeconds(0); // ✅ Reset timer
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
  }, [sendControlMessage, previewData]);

  const handleSpeedChange = useCallback((newSpeed: string) => {
    const speedValue = parseFloat(newSpeed);
    setSpeed(speedValue);
    sendControlMessage("SPEED", { speed: speedValue });
  }, [sendControlMessage]);

  // Memoize map props to prevent unnecessary re-renders
  const mapProps = useMemo(() => ({
    itinerarios,
    aeropuertos,
    speedKmh: 800 * speed,
    simulatedTime: interpolatedTime || simulatedTime,
    center: [-60, -15] as [number, number],
    zoom: 3,
    loop: false,
  }), [itinerarios, aeropuertos, speed, interpolatedTime, simulatedTime]);

  return (
    <div className="relative w-full h-screen overflow-hidden">
      {/* CAPA 0: Mapa - Ocupa 100% del espacio disponible */}
      <div className="absolute inset-0 z-0">
        <AnimatedFlights {...mapProps} />
      </div>

      {/* CAPA 1: Barra superior - Flotante sobre el mapa, con margen derecho para evitar solapamiento */}
      <div className="absolute top-0 left-0 right-[400px] z-10 p-3 pointer-events-none">
        {/* Header mínimo con toggle */}
        <div className="flex items-center gap-2 mb-2 pointer-events-auto">
          <h1 className="text-2xl font-bold text-white drop-shadow-lg">Simulación</h1>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsHeaderOpen(!isHeaderOpen)}
            className="h-7 gap-1.5 shadow-lg"
          >
            <Menu className="h-3.5 w-3.5" />
            <span className="text-xs">Controles</span>
            {isHeaderOpen ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
          </Button>
        </div>

        {/* Panel de controles - COLAPSABLE Y MUY COMPACTO */}
        {/* Aplicando patrón del sidebar: siempre montado con transición suave */}
        <Card className={`shadow-xl backdrop-blur-sm bg-white/95 transition-all duration-300 pointer-events-auto ${
          isHeaderOpen ? 'opacity-100 scale-100 max-h-[500px]' : 'opacity-0 scale-95 max-h-0 overflow-hidden pointer-events-none'
        }`}>
          <CardContent className="py-3 px-4">
                {/* Fila 1: Configuración de la simulación */}
                <div className="flex items-center gap-6 mb-3">
                  {/* Escenario */}
                  <div className="flex flex-col gap-1">
                    <Label className="text-xs text-muted-foreground font-semibold uppercase">Escenario</Label>
                    <RadioGroup
                      value={scenarioType}
                      onValueChange={(value: ScenarioType) => setScenarioType(value)}
                      disabled={simulationState === 'RUNNING' || simulationState === 'STARTING' || !!sharedSessionId}
                      className="flex items-center gap-3"
                    >
                      <div className="flex items-center space-x-1.5">
                        <RadioGroupItem value="WEEKLY" id="weekly" className="h-4 w-4" />
                        <Label htmlFor="weekly" className="text-sm font-normal cursor-pointer">
                          Semanal
                        </Label>
                      </div>
                      <div className="flex items-center space-x-1.5">
                        <RadioGroupItem value="COLLAPSE" id="collapse" className="h-4 w-4" />
                        <Label htmlFor="collapse" className="text-sm font-normal cursor-pointer">
                          Colapso
                        </Label>
                      </div>
                    </RadioGroup>
                    {(simulationState === 'RUNNING' || simulationState === 'STARTING') && (
                      <div className="flex items-center gap-1.5 text-amber-600 text-xs mt-1">
                        <AlertCircle className="h-3.5 w-3.5" />
                        <span>Detén la simulación para cambiar escenario</span>
                      </div>
                    )}
                  </div>

                  {/* Fecha y hora de inicio */}
                  <div className="flex flex-col gap-1">
                    <Label className="text-xs text-muted-foreground font-semibold uppercase">Fecha y Hora de Inicio</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        min="2025-01-02"
                        max="2025-12-31"
                        disabled={simulationState === 'RUNNING' || simulationState === 'STARTING' || !!sharedSessionId}
                        className="w-[130px] h-8 text-sm px-2"
                      />
                      <Input
                        type="time"
                        value={startTime}
                        onChange={(e) => setStartTime(e.target.value)}
                        disabled={simulationState === 'RUNNING' || simulationState === 'STARTING' || !!sharedSessionId}
                        className="w-[90px] h-8 text-sm px-2"
                      />
                    </div>
                  </div>

                  {/* Velocidad de simulación */}
                  <div className="flex flex-col gap-1">
                    <Label className="text-xs text-muted-foreground font-semibold uppercase">Velocidad</Label>
                    <Select
                      value={speed.toString()}
                      onValueChange={handleSpeedChange}
                      disabled={simulationState !== 'RUNNING'}
                    >
                      <SelectTrigger className="w-20 h-8 text-sm px-2">
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

                  {/* Progreso y Timer */}
                  <div className="flex flex-col gap-1 ml-auto">
                    <Label className="text-xs text-muted-foreground font-semibold uppercase">Progreso</Label>
                    <div className="flex items-center gap-3">
                      <div className="text-sm">
                        <span className="font-medium">{currentIteration}/{totalIterations}</span>
                        <span className="text-muted-foreground ml-1.5">({Math.round(progress * 100)}%)</span>
                      </div>
                      {simulationState === 'RUNNING' && (
                        <div className="flex items-center gap-1.5 text-sm text-muted-foreground border-l pl-3">
                          <Timer className="h-4 w-4" />
                          <span>{formatElapsedTime(elapsedSeconds)}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Fila 2: Controles de ejecución */}
                <div className="flex items-center gap-2 pt-3 border-t">
                  <Label className="text-xs text-muted-foreground font-semibold uppercase mr-2">Controles</Label>

                  {/* 1. Botón principal: Iniciar/Pausar/Reanudar */}
                  {simulationState === 'PAUSED' ? (
                    <Button
                      variant="default"
                      size="sm"
                      onClick={handleResume}
                      disabled={!connected}
                      className="h-8 px-3 gap-2"
                    >
                      <Play className="h-4 w-4" />
                      <span className="text-sm">Reanudar</span>
                    </Button>
                  ) : simulationState === 'RUNNING' ? (
                    <Button
                      variant="default"
                      size="sm"
                      onClick={handlePause}
                      disabled={!connected}
                      className="h-8 px-3 gap-2"
                    >
                      <Pause className="h-4 w-4" />
                      <span className="text-sm">Pausar</span>
                    </Button>
                  ) : !sharedSessionId ? (
                    <Button
                      variant="default"
                      size="sm"
                      onClick={handleStart}
                      disabled={!connected || simulationState === 'STARTING'}
                      className="h-8 px-3 gap-2"
                    >
                      <Play className="h-4 w-4" />
                      <span className="text-sm">Iniciar</span>
                    </Button>
                  ) : null}

                  {/* 2. Detener */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleStop}
                    disabled={!connected || simulationState === 'IDLE' || simulationState === 'STOPPED'}
                    className="h-8 px-3 gap-2"
                  >
                    <Square className="h-4 w-4" />
                    <span className="text-sm">Detener</span>
                  </Button>

                  {/* 3. Reiniciar */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleReset}
                    disabled={!connected || simulationState === 'STARTING'}
                    className="h-8 px-3 gap-2"
                  >
                    <RotateCcw className="h-4 w-4" />
                    <span className="text-sm">Reiniciar</span>
                  </Button>

                  {/* Separador visual */}
                  <div className="h-6 w-px bg-border mx-1" />

                  {/* 4. Compartir */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      if (sessionId) {
                        const shareUrl = `${window.location.origin}/simulacion/${sessionId}`;
                        navigator.clipboard.writeText(shareUrl).then(() => {
                          setUrlCopied(true);
                          setTimeout(() => setUrlCopied(false), 2000);
                        }).catch((error) => {
                          console.error("Error copying URL:", error);
                        });
                      }
                    }}
                    disabled={!sessionId || simulationState === 'IDLE' || simulationState === 'STARTING'}
                    className="h-8 px-3 gap-2"
                  >
                    {urlCopied ? (
                      <>
                        <Check className="h-4 w-4" />
                        <span className="text-sm">Copiado!</span>
                      </>
                    ) : (
                      <>
                        <Share2 className="h-4 w-4" />
                        <span className="text-sm">Compartir</span>
                      </>
                    )}
                  </Button>

                  {/* 5. Ver Reporte */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      if (sessionId) {
                        getFinalReport(sessionId)
                          .then((report) => {
                            setFinalReport(report);
                            setShowFinalReport(true);
                          })
                          .catch((error) => {
                            console.error("Error fetching final report:", error);
                          });
                      }
                    }}
                    disabled={!sessionId || simulationState === 'IDLE' || simulationState === 'STARTING'}
                    className="h-8 px-3 gap-2"
                  >
                    <FileText className="h-4 w-4" />
                    <span className="text-sm">Ver Reporte</span>
                  </Button>
                </div>
          </CardContent>
        </Card>

        {/* Badges de estado debajo del panel de control - SIEMPRE VISIBLES CON POSICIÓN FIJA */}
        <div className="mt-2 flex items-center gap-2 min-h-[32px]">
          {/* Indicador de sesión compartida */}
          {sharedSessionId && (
            <Badge variant="outline" className="text-xs px-2.5 py-1 shadow-lg bg-blue-50/95 border-blue-300 text-blue-700 backdrop-blur">
              <Share2 className="h-3 w-3 mr-1" />
              Sesión Compartida
            </Badge>
          )}

          {/* Estado */}
          <Badge variant={
            simulationState === 'RUNNING' ? 'default' :
            simulationState === 'COMPLETED' ? 'outline' :
            simulationState === 'COLLAPSED' ? 'destructive' :
            simulationState === 'ERROR' ? 'destructive' : 'secondary'
          } className={`text-xs px-2.5 py-1 shadow-lg backdrop-blur ${
            simulationState === 'RUNNING' ? '' : 'bg-white/95'
          }`}>
            {simulationState === 'COLLAPSED' ? 'COLAPSO' : simulationState}
          </Badge>

          {/* Iteraciones */}
          <Badge variant="outline" className="text-xs px-2.5 py-1 shadow-lg bg-white/95 backdrop-blur">
            {currentIteration}/{totalIterations}
          </Badge>

          {/* Vuelos activos */}
          <Badge variant="outline" className="text-xs px-2.5 py-1 shadow-lg bg-white/95 backdrop-blur">
            <Plane className="h-3.5 w-3.5 mr-1" />
            {itinerarios.length} vuelos
          </Badge>

          {/* Tiempo simulado o Esperando */}
          <Badge variant={displayTime === "Esperando..." ? "secondary" : "outline"}
                 className={`text-xs px-2.5 py-1 shadow-lg backdrop-blur ${
                   displayTime === "Esperando..."
                     ? "bg-amber-100/95 border-amber-300"
                     : "bg-white/95"
                 }`}>
            {displayTime === "Esperando..." ? (
              <>
                <Clock className="h-3.5 w-3.5 mr-1" />
                Esperando...
              </>
            ) : displayTime && displayTime !== "Cargando..." ? (
              <>
                <Calendar className="h-3.5 w-3.5 mr-1" />
                {displayTime}
              </>
            ) : (
              <>
                <Clock className="h-3.5 w-3.5 mr-1" />
                --:--
              </>
            )}
          </Badge>
        </div>
      </div>

      {/* CAPA 2: Panel lateral derecho - Flotante sobre el mapa - MÁS ALTO Y ANGOSTO */}
      {/* Aplicando patrón del sidebar: transición suave + contenido siempre montado */}
      <div className={`absolute right-4 bottom-4 z-10 transition-all duration-300 ${isPanelOpen ? 'w-[380px]' : 'w-[60px]'} top-3`}>
        {/* Botón toggle panel - Ahora en la esquina superior derecha del panel */}
        <div className="flex justify-end mb-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={() => setIsPanelOpen(!isPanelOpen)}
            className="h-9 px-3 shadow-lg gap-2"
            title={isPanelOpen ? "Ocultar panel" : "Mostrar panel"}
          >
            {isPanelOpen ? (
              <>
                <span className="text-xs">Ocultar</span>
                <ChevronRight className="h-4 w-4" />
              </>
            ) : (
              <>
                <span className="text-xs">Panel</span>
                <ChevronLeft className="h-4 w-4" />
              </>
            )}
          </Button>
        </div>

        {/* Panel de contenido - SIEMPRE MONTADO, solo cambia visibilidad */}
        <Card className={`h-[calc(100%-3rem)] shadow-xl backdrop-blur-sm bg-white/95 transition-all duration-300 ${
          isPanelOpen ? 'opacity-100 scale-100' : 'opacity-0 scale-95 pointer-events-none'
        }`}>
          <Tabs defaultValue="pedidos" className="h-full flex flex-col">
            <TabsList className="grid grid-cols-3 m-2 mb-0">
              <TabsTrigger value="pedidos" className="text-xs">
                Pedidos
              </TabsTrigger>
              <TabsTrigger value="vuelos" className="text-xs">
                Vuelos
              </TabsTrigger>
              <TabsTrigger value="eventos" className="text-xs">
                Cancelaciones
              </TabsTrigger>
            </TabsList>

            <TabsContent value="pedidos" className="flex-1 m-0 overflow-hidden">
              <PedidosPanel
                pedidos={pedidos.filter(p => {
                  if (simulationState === 'IDLE') return true;
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

            <TabsContent value="vuelos" className="flex-1 m-0 overflow-hidden">
              <VuelosPanel userId={sessionId || ""} />
            </TabsContent>

            <TabsContent value="eventos" className="flex-1 m-0 overflow-hidden">
              <EventosPanel
                cancellations={cancellations}
                onCancellationCreated={handleCancellationCreated}
                onRefresh={loadDynamicEvents}
                currentSimulationTime={simulatedTime || lastBackendTimeRef.current}
              />
            </TabsContent>
          </Tabs>
        </Card>
      </div>

      {/* Final Report Modal */}
      {/* Collapse Alert Modal */}
      <Dialog open={showCollapseAlert} onOpenChange={setShowCollapseAlert}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-red-600">
              <AlertCircle className="h-5 w-5" />
              Sistema Colapsado
            </DialogTitle>
            <DialogDescription className="pt-4 space-y-3">
              <p className="text-base">
                La simulación ha detectado un colapso del sistema logístico.
              </p>
              <div className="bg-red-50 border border-red-200 rounded-md p-3">
                <p className="text-sm text-red-800 font-medium">
                  {collapseReason}
                </p>
              </div>
              <p className="text-sm text-gray-600">
                El sistema ya no puede asignar pedidos debido a limitaciones de capacidad o tiempo.
                Revisa el reporte final para analizar las métricas y causas del colapso.
              </p>
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              variant="outline"
              onClick={() => setShowCollapseAlert(false)}
            >
              Cerrar
            </Button>
            <Button
              onClick={() => {
                setShowCollapseAlert(false);
                if (finalReport) {
                  setShowFinalReport(true);
                }
              }}
              className="bg-blue-600 hover:bg-blue-700"
            >
              <FileText className="h-4 w-4 mr-2" />
              Ver Reporte Final
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ReporteFinalModal
        open={showFinalReport}
        onOpenChange={setShowFinalReport}
        report={finalReport}
      />
    </div>
  );
}
