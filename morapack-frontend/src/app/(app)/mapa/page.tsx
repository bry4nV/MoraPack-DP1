"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import dynamic from "next/dynamic";
import { toast } from "sonner";
import { dmsToDecimal } from "@/lib/geo";

// --- WEBSOCKET IMPORTS ---
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// --- API SERVICES ---
import { ordersApi } from "@/api/orders/orders";

// Componentes UI Básicos
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";
// 🔥 Agregamos DialogFooter para mejor estética
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Label } from "@/components/ui/label"; // Usamos Label para mejor semántica

// Íconos
import { 
  Plus, Upload, Activity, ChevronRight,
  Package, Ban, CheckCircle2, LayoutPanelLeft, AlertCircle, 
  LayoutDashboard, MapPin, User, Box, Clock,
  Trash2, Plane
} from "lucide-react";

// Mapa Dinámico (DailyFlights)
const DailyFlights = dynamic(
  () => import("@/components/map/DailyFlights"), 
  { ssr: false }
);

// --- 📅 CONFIGURACIÓN ---
const OPERATIONAL_DATE = "2025-01-02"; 

// --- 🛡️ DATOS DE RESPALDO ---
const FALLBACK_AIRPORTS = [
  { id: 1, code: 'LIM', city: 'Lima', country: 'Peru', latitude: -12.024, longitude: -77.112, isHub: true, capacity: 5000 },
  { id: 3, code: 'UBBB', city: 'Baku', country: 'Azerbaijan', latitude: 40.467, longitude: 50.050, isHub: true, capacity: 5000 },
  { id: 14, code: 'EBCI', city: 'Brussels', country: 'Belgium', latitude: 50.4592, longitude: 4.4536, isHub: true, capacity: 440 },
  { id: 2, code: 'MAD', city: 'Madrid', country: 'Spain', latitude: 40.471, longitude: -3.562, isHub: false, capacity: 1000 },
  { id: 4, code: 'MIA', city: 'Miami', country: 'USA', latitude: 25.793, longitude: -80.290, isHub: false, capacity: 1000 },
];

export default function MapaPage() {
  const [loading, setLoading] = useState(true);
  const [aeropuertosMap, setAeropuertosMap] = useState<any[]>([]);
  const [isPanelOpen, setIsPanelOpen] = useState(true);
  
  const fileInputRef = useRef<HTMLInputElement>(null);

  // ESTADOS DEL MODAL
  const [showModal, setShowModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    clientCode: "",      
    destinationCode: "", 
    packetCount: 1       
  });

  // DATOS
  const [itinerarios, setItinerarios] = useState<any[]>([]); 
  const [ordersList, setOrdersList] = useState<any[]>([]);   
  
  const metrics = {
    total: ordersList.length,
    pendientes: ordersList.filter((p: any) => p.status === 'PENDING' || p.status === 'Registrado').length,
    enTransito: ordersList.filter((p: any) => p.status === 'IN_TRANSIT' || p.status === 'En Vuelo').length,
    completados: ordersList.filter((p: any) => p.status === 'COMPLETED' || p.status === 'Entregado').length,
    sinAsignar: ordersList.filter((p: any) => p.status === 'UNASSIGNED' || p.status === 'Cancelado').length
  };

  const fetchAeropuertos = useCallback(async () => {
    try {
      const response = await fetch('http://localhost:8080/api/airports');
      if (!response.ok) throw new Error("Error fetching");
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
        pedidosEnEspera: Math.floor(Math.random() * 5),
        capacidadUsada: Math.floor(Math.random() * 500),
        porcentajeUso: Math.floor(Math.random() * 80)
      }));
      setAeropuertosMap(adaptados);
    } catch (error) {
      setAeropuertosMap(FALLBACK_AIRPORTS);
    }
  }, []);

  const fetchOrders = useCallback(async () => {
    try {
        const data = await ordersApi.getOrders(); 
        setOrdersList(data);
    } catch (error) {
        console.error("Error pedidos:", error);
    }
  }, []);

  // --- WEBSOCKET ---
  useEffect(() => {
    const socket = new SockJS('http://localhost:8080/ws-morapack');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('✅ Conectado a Torre de Control (WebSocket)');
        
        stompClient.subscribe('/topic/flights', (msg) => {
          if (msg.body) {
            const evento = JSON.parse(msg.body);
            if (evento.tipo === 'TAKEOFF') {
                const datosVuelo = evento.datos;
                const ahoraMismo = new Date();
                const llegadaCalculada = new Date(ahoraMismo.getTime() + 15000); 

                const nuevoItinerario = {
                    id: datosVuelo.id,
                    pedidoId: evento.pedidoId,
                    segmentos: [{
                        numeroSegmento: 1,
                        vuelo: {
                            id: datosVuelo.id,
                            codigo: datosVuelo.id,
                            origen: {
                                codigo: datosVuelo.origen.codigo,
                                latitude: Number(datosVuelo.origen.latitude),
                                longitude: Number(datosVuelo.origen.longitude)
                            },
                            destino: {
                                codigo: datosVuelo.destino.codigo,
                                latitude: Number(datosVuelo.destino.latitude),
                                longitude: Number(datosVuelo.destino.longitude)
                            },
                            salidaProgramadaISO: ahoraMismo.toISOString(),
                            llegadaProgramadaISO: llegadaCalculada.toISOString(),
                            estado: 'IN_TRANSIT'
                        }
                    }]
                };
                setItinerarios(prev => [...prev, nuevoItinerario]);
                toast.success(`✈️ Vuelo ${datosVuelo.id} despegando!`);
                fetchOrders(); 
            } else if (evento.tipo === 'LANDING') {
                setItinerarios(prev => prev.filter(it => 
                    it.segmentos[0].vuelo.id !== evento.vueloId
                ));
                toast.success("📦 Pedido Entregado");
                fetchOrders(); 
            }
          }
        });

        stompClient.subscribe('/topic/orders', (msg) => {
            if (msg.body === 'UPDATE') fetchOrders();
        });
      },
    });

    stompClient.activate();
    return () => { stompClient.deactivate(); };
  }, [fetchOrders]);

  useEffect(() => {
    const initData = async () => {
      setLoading(true);
      try {
        await Promise.all([fetchAeropuertos(), fetchOrders()]);
      } catch (e) { console.error(e); } 
      finally { setLoading(false); }
    };
    initData();
  }, [fetchAeropuertos, fetchOrders]);

  // --- HANDLERS ---
  const handleAbrirModal = () => {
    setFormData({ clientCode: "", destinationCode: "", packetCount: 1 });
    setShowModal(true);
  };
  const handleCerrarModal = () => setShowModal(false);
  
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };
  
  const handleSubmitPedido = async () => {
    if (!formData.clientCode || !formData.destinationCode) {
        toast.error("Complete todos los campos"); return;
    }
    setIsSubmitting(true);
    try {
      const payload = {
        clientCode: formData.clientCode.trim(),
        airportDestinationCode: formData.destinationCode.toUpperCase().trim(),
        quantity: Math.max(1, Math.floor(Number(formData.packetCount)))
      };
      await ordersApi.createOrder(payload as any); 
      toast.success("Pedido registrado, procesando...");
      setShowModal(false);
      await fetchOrders(); 
    } catch (e: any) { 
        toast.error(`Error: ${e.message}`); 
    } finally { 
        setIsSubmitting(false); 
    }
  };

  const handleResetOrders = async () => {
    if(!confirm("¿Está seguro de eliminar TODOS los pedidos? Esta acción no se puede deshacer.")) return;
    try {
        await ordersApi.clearOrders(); 
        setOrdersList([]); 
        setItinerarios([]); 
        toast.success("✅ Base de datos reiniciada correctamente");
    } catch (e) { toast.error("Error al limpiar DB"); }
  };

  const handleCargaMasivaClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.name.endsWith('.csv')) {
        toast.error("Por favor sube un archivo .csv");
        return;
    }

    const toastId = toast.loading("Procesando archivo CSV...");

    try {
        await ordersApi.uploadCsv(file); 
        toast.dismiss(toastId);
        toast.success("✅ Carga masiva completada");
        e.target.value = '';
        await fetchOrders(); 
    } catch (error) {
        toast.dismiss(toastId);
        toast.error("Error al procesar el archivo CSV");
        console.error(error);
    }
  };

  const handleCargaMasivaCancelaciones = () => toast.error("Simulando cancelaciones...");

  return (
    <div className="relative h-[calc(100vh-4rem)] w-full overflow-hidden bg-background">
      
      {/* MAPA */}
      <div className="absolute inset-0 z-0">
        <DailyFlights key={aeropuertosMap.length} itinerarios={itinerarios} aeropuertos={aeropuertosMap} />
      </div>

      {/* HEADER FLOTANTE */}
      <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-10 flex gap-3 pointer-events-none">
          <div className="bg-white/90 backdrop-blur-md px-4 py-2 rounded-lg shadow-lg border flex items-center gap-3 pointer-events-auto">
              <div className="p-1.5 bg-gray-100 rounded-full text-gray-600"><LayoutDashboard className="h-4 w-4" /></div>
              <div><p className="text-[10px] uppercase text-muted-foreground font-bold">Total</p><p className="text-lg font-bold leading-none">{metrics.total}</p></div>
          </div>
          <div className="bg-white/90 backdrop-blur-md px-4 py-2 rounded-lg shadow-lg border flex items-center gap-3 pointer-events-auto">
              <div className="p-1.5 bg-green-100 rounded-full text-green-600"><CheckCircle2 className="h-4 w-4" /></div>
              <div><p className="text-[10px] uppercase text-muted-foreground font-bold">Entregados</p><p className="text-lg font-bold leading-none">{metrics.completados}</p></div>
          </div>
      </div>

      {!isPanelOpen && (
        <Button variant="secondary" className="absolute top-4 right-4 z-20 shadow-xl gap-2 bg-white text-black" onClick={() => setIsPanelOpen(true)}>
          <LayoutPanelLeft className="h-4 w-4" /> Ver Panel
        </Button>
      )}

      {/* PANEL DERECHO */}
      <div className={`absolute right-0 top-0 h-full bg-white/95 backdrop-blur-md shadow-2xl z-30 border-l transition-all duration-300 ease-in-out flex flex-col ${isPanelOpen ? "w-96 translate-x-0" : "w-0 translate-x-full opacity-0"}`}>
        <div className="p-4 border-b flex items-center justify-between bg-muted/30">
          <div className="font-semibold text-sm flex items-center gap-2"><Activity className="h-4 w-4 text-blue-600" /> Control de Operaciones</div>
          <Button variant="ghost" size="sm" onClick={() => setIsPanelOpen(false)}>Ocultar <ChevronRight className="h-4 w-4" /></Button>
        </div>

        <Tabs defaultValue="pedidos" className="flex-1 flex flex-col h-full overflow-hidden">
          <div className="px-4 pt-4 flex-none">
            <TabsList className="grid w-full grid-cols-3 bg-muted/50">
              <TabsTrigger value="pedidos" className="text-xs">Pedidos</TabsTrigger>
              <TabsTrigger value="vuelos" className="text-xs">Vuelos</TabsTrigger>
              <TabsTrigger value="cancelaciones" className="text-[10px]">Alertas</TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value="pedidos" className="flex-1 flex flex-col p-0 m-0 h-full overflow-hidden">
            <div className="p-4 space-y-3 flex-none">
              <Button onClick={handleAbrirModal} className="w-full bg-black text-white hover:bg-gray-800 shadow transition-transform active:scale-95"><Plus className="mr-2 h-4 w-4" /> Nuevo Pedido</Button>
              
              <div className="flex gap-2">
                  <input 
                    type="file" 
                    ref={fileInputRef} 
                    onChange={handleFileChange} 
                    accept=".csv" 
                    className="hidden" 
                    aria-label="Carga masiva de pedidos por CSV" 
                  />
                  <Button variant="outline" className="flex-1 border-dashed hover:bg-gray-100" onClick={handleCargaMasivaClick}><Upload className="mr-2 h-4 w-4" /> Masiva</Button>
                  <Button variant="destructive" className="flex-1 hover:bg-red-600" onClick={handleResetOrders}><Trash2 className="mr-2 h-4 w-4" /> Reiniciar</Button>
              </div>
              
              <div className="grid grid-cols-4 gap-2 text-center pt-2">
                  <div className="bg-orange-50 p-2 rounded border border-orange-100"><div className="text-[9px] font-bold text-orange-700 uppercase">Pend.</div><div className="text-lg font-bold text-orange-800">{metrics.pendientes}</div></div>
                  <div className="bg-blue-50 p-2 rounded border border-blue-100"><div className="text-[9px] font-bold text-blue-700 uppercase">Tran.</div><div className="text-lg font-bold text-blue-800">{metrics.enTransito}</div></div>
                  <div className="bg-green-50 p-2 rounded border border-green-100"><div className="text-[9px] font-bold text-green-700 uppercase">Comp.</div><div className="text-lg font-bold text-green-800">{metrics.completados}</div></div>
                  <div className="bg-red-50 p-2 rounded border border-red-100"><div className="text-[9px] font-bold text-red-700 uppercase">Alert</div><div className="text-lg font-bold text-red-800">{metrics.sinAsignar}</div></div>
              </div>
            </div>
            
            <Separator />
            
            <div className="flex-1 overflow-y-auto p-4 space-y-2 min-h-0">
                {ordersList.length === 0 ? (
                    <div className="flex flex-col items-center justify-center text-muted-foreground py-10"><Package className="h-10 w-10 mb-2 opacity-10" /><p className="text-sm">No hay pedidos registrados.</p></div>
                ) : (
                    [...ordersList].reverse().map((p: any, i: number) => (
                        <div key={i} className="p-3 bg-white border rounded-lg shadow-sm hover:border-blue-300 transition-all">
                            <div className="flex justify-between items-start">
                                <div><div className="font-bold text-sm text-gray-800">{p.clientName || p.clientCode || "Cliente"}</div><div className="text-xs text-gray-500 mt-1 flex items-center gap-1"><MapPin className="h-3 w-3" /> {p.originCode || "LIM"} <ChevronRight className="h-3 w-3" /> {p.destinationCode || p.airportDestinationCode}</div></div>
                                <span className={`text-[10px] px-2 py-1 rounded-full font-bold ${ (p.status === 'COMPLETED') ? 'bg-green-100 text-green-700' : (p.status === 'IN_TRANSIT') ? 'bg-blue-100 text-blue-700' : 'bg-orange-100 text-orange-700' }`}>{p.status || 'PENDING'}</span>
                            </div>
                            <div className="mt-2 flex justify-between items-center text-xs text-gray-400">
                                <span className="flex items-center gap-1"><Package className="h-3 w-3" /> {p.quantity} paq.</span>
                                <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {p.registrationDate ? new Date(p.registrationDate).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}) : '--:--'}</span>
                            </div>
                        </div>
                    ))
                )}
            </div>
          </TabsContent>
          <TabsContent value="vuelos" className="flex-1 p-4"><div className="text-center text-sm text-muted-foreground">Listado de vuelos...</div></TabsContent>
          <TabsContent value="cancelaciones" className="flex-1 flex flex-col p-0 m-0 overflow-hidden">
            <div className="p-4 space-y-3">
                <div className="p-3 bg-red-50 border border-red-100 rounded-md text-xs text-red-800 flex items-start gap-2"><AlertCircle className="h-4 w-4 mt-0.5 shrink-0" /><div>Gestión de bloqueos y cancelaciones.</div></div>
                <Button variant="outline" className="w-full border-dashed border-red-200 hover:bg-red-50 hover:text-red-600 text-red-600" onClick={handleCargaMasivaCancelaciones}><Upload className="mr-2 h-4 w-4" /> Carga Masiva</Button>
            </div>
            <Separator />
            <div className="flex-1 overflow-y-auto p-4 flex flex-col items-center justify-center text-muted-foreground"><Ban className="h-10 w-10 mb-2 opacity-10" /><p className="text-sm">Sin cancelaciones registradas.</p></div>
          </TabsContent>
        </Tabs>
      </div>

      {/* 🔥 MODAL REDISEÑADO - MÁS ESTÉTICO 🔥 */}
      <Dialog open={showModal} onOpenChange={setShowModal}>
        <DialogContent className="sm:max-w-[450px]">
            <DialogHeader>
              <DialogTitle className="text-xl font-bold">Nuevo Pedido</DialogTitle>
              <DialogDescription>
                Complete los datos para programar un nuevo envío.
              </DialogDescription>
            </DialogHeader>
            
            <div className="grid gap-5 py-4">
                {/* Input Cliente */}
                <div className="space-y-2">
                    <div className="flex items-center justify-between">
                        <Label htmlFor="clientCode" className="text-sm font-medium text-gray-700">Código del Cliente</Label>
                    </div>
                    <div className="relative group">
                        <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 group-focus-within:text-black transition-colors" />
                        <Input 
                            id="clientCode"
                            name="clientCode" 
                            placeholder="Ej: CLI001" 
                            value={formData.clientCode} 
                            onChange={handleInputChange} 
                            className="pl-10"
                        />
                    </div>
                </div>

                {/* Select Destino - 🔥 AHORA MUESTRA CIUDAD Y CÓDIGO */}
                <div className="space-y-2">
                    <Label htmlFor="destinationSelect" className="text-sm font-medium text-gray-700">Destino del Envío</Label>
                    <div className="relative group">
                        <Plane className="absolute left-3 top-3 h-4 w-4 text-gray-400 z-10 group-focus-within:text-black transition-colors" />
                        <Select onValueChange={(val) => setFormData(prev => ({...prev, destinationCode: val}))}>
                            <SelectTrigger id="destinationSelect" className="pl-10">
                                <SelectValue placeholder="Seleccionar Aeropuerto" />
                            </SelectTrigger>
                            <SelectContent>
                                {aeropuertosMap.map((a: any) => (
                                    <SelectItem key={a.code} value={a.code}>
                                        <span className="font-medium text-gray-900">{a.city}</span>
                                        <span className="ml-2 text-gray-500 font-mono text-xs">({a.code})</span>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </div>

                {/* Input Paquetes */}
                <div className="space-y-2">
                    <Label htmlFor="packetCount" className="text-sm font-medium text-gray-700">Cantidad de Paquetes</Label>
                    <div className="relative group">
                        <Box className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 group-focus-within:text-black transition-colors" />
                        <Input 
                            id="packetCount" 
                            name="packetCount" 
                            type="number" 
                            min="1" 
                            placeholder="Ej: 50" 
                            value={formData.packetCount} 
                            onChange={handleInputChange} 
                            className="pl-10"
                        />
                    </div>
                </div>
            </div>

            <DialogFooter>
                <Button variant="outline" onClick={handleCerrarModal} disabled={isSubmitting}>Cancelar</Button>
                <Button className="bg-black text-white hover:bg-gray-800" onClick={handleSubmitPedido} disabled={isSubmitting}>
                    {isSubmitting ? <Activity className="mr-2 h-4 w-4 animate-spin" /> : "Guardar Pedido"}
                </Button>
            </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}