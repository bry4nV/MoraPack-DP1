"use client";

import { useEffect, useState, useCallback } from "react";
import dynamic from "next/dynamic";
import { toast } from "sonner";
import { dmsToDecimal } from "@/lib/geo";

// --- API SERVICES ---
import { ordersApi } from "@/api/orders/orders";

// Componentes UI
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";

// Íconos
import { 
  Plus, Upload, Activity, ChevronRight, ChevronLeft,
  Package, Ban, CheckCircle2, LayoutPanelLeft, AlertCircle, LayoutDashboard,
  X, MapPin, User, Box, Plane, CalendarClock
} from "lucide-react";

// (Eliminamos la importación de datos locales que causaba el error)

// Mapa Dinámico
const AnimatedFlights = dynamic(
  () => import("@/components/map/AnimatedFlights"), 
  { ssr: false }
);

// --- 📅 CONFIGURACIÓN DE FECHA OPERATIVA ---
const OPERATIONAL_DATE = "2025-01-02"; 

export default function MapaPage() {
  const [loading, setLoading] = useState(true);
  const [aeropuertosMap, setAeropuertosMap] = useState<any[]>([]);
  
  // RELOJ PARA ANIMACIÓN EN VIVO
  const [currentTime, setCurrentTime] = useState(`${OPERATIONAL_DATE}T00:00:00`);

  // ESTADOS DEL PANEL
  const [isPanelOpen, setIsPanelOpen] = useState(true);
  
  // ESTADOS DEL MODAL
  const [showModal, setShowModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    clientCode: "",      
    destinationCode: "", 
    packetCount: 1       
  });

  // DATOS DE VUELOS (ITINERARIOS)
  const [itinerarios, setItinerarios] = useState<any[]>([]); 
  
  // MÉTRICAS
  const metrics = {
    total: itinerarios.length, 
    pendientes: 0,
    enTransito: itinerarios.length,
    completados: 0,
    sinAsignar: 0
  };

  // --- 1. RELOJ DEL SISTEMA ---
  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      const timePart = now.toTimeString().split(' ')[0]; // HH:mm:ss
      setCurrentTime(`${OPERATIONAL_DATE}T${timePart}`);
    };
    
    updateClock();
    const timer = setInterval(updateClock, 1000);
    return () => clearInterval(timer);
  }, []);

  // --- 2. FETCH DE AEROPUERTOS (SOLO BACKEND) ---
  const fetchAeropuertos = useCallback(async () => {
    try {
      const response = await fetch('http://localhost:8080/api/airports');
      if (!response.ok) throw new Error("Error fetching airports");
      const data = await response.json();
      
      const adaptados = data.map((a: any) => ({
        id: a.id,
        code: a.code,
        city: a.city || a.code,
        country: typeof a.country === 'object' ? a.country?.name : (a.country || "Unknown"),
        latitude: dmsToDecimal(a.latitude || "0"),
        longitude: dmsToDecimal(a.longitude || "0"),
        isHub: Boolean(a.isHub),
        capacity: a.totalCapacity || 1000,
        nombre: a.name || a.code,
        pedidosEnEspera: a.pendingOrders || 0,
      }));
      setAeropuertosMap(adaptados);
    } catch (error) {
      console.error("Error cargando aeropuertos del backend:", error);
      toast.error("No se pudieron cargar los aeropuertos");
      setAeropuertosMap([]); // Fallback vacío si falla el backend
    }
  }, []);

  // --- 3. FETCH DE VUELOS (USANDO FECHA OPERATIVA) ---
  const fetchActiveFlights = useCallback(async () => {
    try {
      const apiUrl = 'http://localhost:8080';
      const url = `${apiUrl}/api/simulation/preview?startDate=${OPERATIONAL_DATE}&scenarioType=WEEKLY`;
      
      console.log("📡 Sincronizando vuelos para fecha operativa:", OPERATIONAL_DATE); 

      const response = await fetch(url);
      if (response.ok) {
        const preview = await response.json();
        
        if (preview.itineraries && preview.itineraries.length > 0) {
          const mappedItinerarios = preview.itineraries.map((itin: any) => ({
            id: itin.id,
            pedidoId: itin.orderId, 
            segmentos: (itin.segments || []).map((seg: any) => ({
              numeroSegmento: seg.segmentNumber || seg.order,
              vuelo: {
                id: seg.flight.code,
                codigo: seg.flight.code,
                origen: {
                  codigo: seg.flight.origin.code,
                  nombre: seg.flight.origin.name || seg.flight.origin.code,
                  latitude: seg.flight.origin.latitude,   
                  longitude: seg.flight.origin.longitude, 
                  latitud: seg.flight.origin.latitude,    
                  longitud: seg.flight.origin.longitude,  
                  ciudad: seg.flight.origin.city || seg.flight.origin.code,
                },
                destino: {
                  codigo: seg.flight.destination.code,
                  nombre: seg.flight.destination.name || seg.flight.destination.code,
                  latitude: seg.flight.destination.latitude,
                  longitude: seg.flight.destination.longitude,
                  latitud: seg.flight.destination.latitude,
                  longitud: seg.flight.destination.longitude,
                  ciudad: seg.flight.destination.city || seg.flight.destination.code,
                },
                salidaProgramadaISO: seg.flight.scheduledDepartureISO,
                llegadaProgramadaISO: seg.flight.scheduledArrivalISO,
                estado: seg.flight.status,
              }
            }))
          }));
          
          console.log(`✈️ ${mappedItinerarios.length} vuelos encontrados para ${OPERATIONAL_DATE}.`);
          setItinerarios(mappedItinerarios);
        } else {
          console.log(`⚠️ 0 vuelos para ${OPERATIONAL_DATE}.`);
        }
      }
    } catch (error) {
      console.warn("⚠️ Error cargando vuelos activos:", error);
    }
  }, []);

  // Carga inicial
  useEffect(() => {
    const initData = async () => {
      setLoading(true);
      await Promise.all([fetchAeropuertos(), fetchActiveFlights()]);
      setLoading(false);
    };
    initData();
    
    // Auto-refresh cada 10s
    const interval = setInterval(fetchActiveFlights, 10000);
    return () => clearInterval(interval);
  }, [fetchAeropuertos, fetchActiveFlights]);


  // --- HANDLERS MODAL ---
  const handleAbrirModal = () => {
    setFormData({ clientCode: "", destinationCode: "", packetCount: 1 });
    setShowModal(true);
  };

  const handleCerrarModal = () => setShowModal(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // --- LÓGICA DE CREACIÓN DE PEDIDO ---
  const handleSubmitPedido = async () => {
    if (!formData.clientCode || !formData.destinationCode) {
      toast.error("Complete cliente y destino");
      return;
    }

    setIsSubmitting(true);

    try {
      const now = new Date();
      const timePart = now.toTimeString().split(' ')[0]; // HH:mm:ss
      const simulationDateTime = `${OPERATIONAL_DATE}T${timePart}`;

      const payload: any = {
        orderNumber: "", 
        clientCode: formData.clientCode.trim(),
        airportDestinationCode: formData.destinationCode.toUpperCase().trim(),
        quantity: Math.max(1, Math.floor(Number(formData.packetCount))),
        date: simulationDateTime 
      };

      console.log("📨 Enviando pedido sincronizado:", payload);

      await ordersApi.createOrder(payload); 

      toast.success("Pedido registrado y sincronizado");
      setShowModal(false);

      toast.info("Asignando a vuelo disponible...");
      
      setTimeout(() => {
        fetchActiveFlights(); 
      }, 3000); 

    } catch (error: any) {
      console.error("❌ Error creando pedido:", error);
      const msg = error.message || "Error desconocido";
      toast.error(`Error: ${msg}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCargaMasivaPedidos = () => toast.success("Carga Masiva iniciada...");

  return (
    <div className="relative h-[calc(100vh-4rem)] w-full overflow-hidden bg-background">
      
      {/* 🌍 MAPA */}
      <div className="absolute inset-0 z-0">
        <AnimatedFlights
          key={aeropuertosMap.length} 
          itinerarios={itinerarios}
          aeropuertos={aeropuertosMap}
          speedKmh={900}
          simulatedTime={currentTime} 
        />
      </div>

      {/* HEADER FLOTANTE CON INFO DE FECHA */}
      <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-10 flex gap-3 pointer-events-none">
          <div className="bg-white/90 backdrop-blur-md px-4 py-2 rounded-lg shadow-lg border flex items-center gap-3 pointer-events-auto">
              <div className="p-1.5 bg-gray-100 rounded-full text-gray-600"><LayoutDashboard className="h-4 w-4" /></div>
              <div>
                  <p className="text-[10px] uppercase text-muted-foreground font-bold">Vuelos Activos</p>
                  <p className="text-lg font-bold leading-none">{itinerarios.length}</p>
              </div>
          </div>
          
          {/* Se eliminó el indicador de Fecha Operativa a petición del usuario */}
      </div>

      {!isPanelOpen && (
        <Button variant="secondary" className="absolute top-4 right-4 z-20 shadow-xl gap-2 bg-white text-black" onClick={() => setIsPanelOpen(true)}>
          <LayoutPanelLeft className="h-4 w-4" /> Ver Panel
        </Button>
      )}

      {loading && (
          <div className="absolute bottom-4 left-4 z-10 bg-black/80 text-white px-3 py-1 rounded-full text-xs flex items-center gap-2 shadow-lg">
              <Activity className="h-3 w-3 animate-spin" /> Conectando con Torre de Control...
          </div>
      )}

      {/* PANEL DERECHO */}
      <div className={`absolute right-0 top-0 h-full bg-white/95 backdrop-blur-md shadow-2xl z-30 border-l transition-all duration-300 ease-in-out flex flex-col ${isPanelOpen ? "w-96 translate-x-0" : "w-0 translate-x-full opacity-0"}`}>
        <div className="p-4 border-b flex items-center justify-between bg-muted/30">
          <div className="font-semibold text-sm flex items-center gap-2"><Activity className="h-4 w-4 text-blue-600" /> Control de Operaciones</div>
          <Button variant="ghost" size="sm" onClick={() => setIsPanelOpen(false)}>Ocultar <ChevronRight className="h-4 w-4" /></Button>
        </div>

        <Tabs defaultValue="pedidos" className="flex-1 flex flex-col">
          <div className="px-4 pt-4">
            <TabsList className="grid w-full grid-cols-3 bg-muted/50">
              <TabsTrigger value="pedidos" className="text-xs">Pedidos</TabsTrigger>
              <TabsTrigger value="vuelos" className="text-xs">Vuelos</TabsTrigger>
              <TabsTrigger value="cancelaciones" className="text-[10px]">Cancelaciones</TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value="pedidos" className="flex-1 flex flex-col p-0 m-0 overflow-hidden">
            <div className="p-4 space-y-3">
              <Button onClick={handleAbrirModal} className="w-full bg-black text-white hover:bg-gray-800 shadow"><Plus className="mr-2 h-4 w-4" /> Nuevo Pedido</Button>
              <Button onClick={handleCargaMasivaPedidos} variant="outline" className="w-full border-dashed"><Upload className="mr-2 h-4 w-4" /> Carga Masiva</Button>
            </div>
            <div className="px-4 pb-4 grid grid-cols-2 gap-2">
                <div className="bg-blue-50 p-2 rounded text-blue-700 text-center">
                    <div className="text-[10px] font-bold uppercase">En Aire</div>
                    <div className="text-xl font-bold">{metrics.enTransito}</div>
                </div>
                 <div className="bg-gray-50 p-2 rounded text-gray-700 text-center">
                    <div className="text-[10px] font-bold uppercase">Total</div>
                    <div className="text-xl font-bold">{metrics.total}</div>
                </div>
            </div>
            <Separator />
            <div className="flex-1 overflow-y-auto p-4 flex flex-col items-center justify-center text-muted-foreground">
               <Package className="h-10 w-10 mb-2 opacity-10" />
               <p className="text-sm">Gestione los pedidos aquí.</p>
            </div>
          </TabsContent>
          
          <TabsContent value="vuelos" className="flex-1 p-4"><div className="text-center text-sm text-muted-foreground">Listado de vuelos...</div></TabsContent>
          <TabsContent value="cancelaciones" className="flex-1 p-4"><div className="text-center text-sm text-muted-foreground">Gestión de bloqueos...</div></TabsContent>
        </Tabs>
      </div>

      {/* MODAL AGREGAR PEDIDO */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in zoom-in-95 duration-200">
          <div className="bg-white rounded-lg shadow-2xl w-full max-w-md border overflow-hidden">
            <div className="px-6 py-4 border-b flex justify-between bg-gray-50/50">
              <h3 className="font-semibold text-lg">Nuevo Pedido</h3>
              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleCerrarModal}><X className="h-4 w-4" /></Button>
            </div>
            <div className="p-6 space-y-4">
              
              {/* Se eliminó el aviso de fecha sincronizada a petición del usuario */}

              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-500 uppercase">Cliente (Código)</label>
                <div className="relative">
                  <User className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                  <input 
                    name="clientCode" 
                    value={formData.clientCode} 
                    onChange={handleInputChange} 
                    placeholder="Ej: 0007729" 
                    className="pl-9 flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring border-gray-200" 
                  />
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-500 uppercase">Destino (Aeropuerto)</label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                  <input 
                    name="destinationCode" 
                    value={formData.destinationCode} 
                    onChange={handleInputChange} 
                    placeholder="Ej: EBCI" 
                    maxLength={4} 
                    className="pl-9 flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring border-gray-200 uppercase" 
                  />
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-xs font-semibold text-gray-500 uppercase">Paquetes</label>
                <div className="relative">
                  <Box className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
                  <input 
                    name="packetCount" 
                    type="number" 
                    min={1} 
                    value={formData.packetCount} 
                    onChange={handleInputChange} 
                    className="pl-9 flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring border-gray-200" 
                  />
                </div>
              </div>
            </div>
            <div className="px-6 py-4 bg-gray-50 border-t flex justify-end gap-3">
              <Button variant="outline" onClick={handleCerrarModal} disabled={isSubmitting}>Cancelar</Button>
              <Button className="bg-black text-white hover:bg-gray-800" onClick={handleSubmitPedido} disabled={isSubmitting}>
                {isSubmitting ? <Activity className="h-4 w-4 animate-spin" /> : "Guardar y Enviar"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}