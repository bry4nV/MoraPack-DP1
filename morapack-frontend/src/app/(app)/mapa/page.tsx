"use client";

import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { toast } from "sonner";
import { dmsToDecimal } from "@/lib/geo";

// Componentes UI
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";

// Íconos
import { 
  Plus, Upload, Plane, Activity, ChevronRight, ChevronLeft, 
  Package, Ban, CheckCircle2, LayoutPanelLeft, AlertCircle, LayoutDashboard
} from "lucide-react";

// Datos
import { AEROPUERTOS } from "@/data/aeropuertos";

// Mapa Dinámico
const AnimatedFlights = dynamic(
  () => import("@/components/map/AnimatedFlights"), 
  { ssr: false }
);

export default function MapaPage() {
  const [loading, setLoading] = useState(true);
  const [aeropuertosMap, setAeropuertosMap] = useState<any[]>([]);
  
  // ESTADOS DEL PANEL
  const [isPanelOpen, setIsPanelOpen] = useState(true);
  
  // DATOS
  const [itinerarios] = useState<any[]>([]); 
  
  // MÉTRICAS
  const metrics = {
    total: 0,
    pendientes: 0,
    enTransito: 0,
    completados: 0,
    sinAsignar: 0
  };

  // 1. FETCH DE AEROPUERTOS
  useEffect(() => {
    const fetchAeropuertos = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/airports');
        if (!response.ok) throw new Error(`Error HTTP: ${response.status}`);
        const data = await response.json();
        
        if (!Array.isArray(data)) throw new Error("Datos vacíos");

        const adaptados = data.map((a: any) => {
          const capTotal = a.totalCapacity || a.maxCapacity || a.capacity || 1000;
          const capUsada = a.usedCapacity || 0;
          const pct = capTotal > 0 ? (capUsada / capTotal) * 100 : 0;

          return {
            id: a.id,
            code: a.code,
            city: a.city || a.code,
            country: typeof a.country === 'object' ? a.country?.name : (a.country || "Unknown"),
            continent: a.continent || "AMERICA",
            latitude: dmsToDecimal(a.latitude || "0"),
            longitude: dmsToDecimal(a.longitude || "0"),
            isHub: Boolean(a.isHub),
            capacity: capTotal,
            capacidadTotal: capTotal,
            capacidadUsada: capUsada,
            porcentajeUso: pct,
            nombre: a.name || a.code,
            pedidosEnEspera: a.pendingOrders || 0,
          };
        });
        setAeropuertosMap(adaptados);
      } catch (error) {
        console.error("Usando respaldo local:", error);
        const respaldo = AEROPUERTOS.map((a: any) => ({
           id: a.id,
           code: a.codigo || a.code,
           city: a.ciudad || a.city,
           country: typeof a.pais === 'object' ? a.pais?.nombre : (a.pais || a.country),
           latitude: Number(a.latitud ?? a.latitude),
           longitude: Number(a.longitud ?? a.longitude),
           isHub: (a.codigo === 'LIM' || a.code === 'LIM'),
           capacidadTotal: 900,
           capacidadUsada: 0,
           porcentajeUso: 0
        }));
        setAeropuertosMap(respaldo);
      } finally {
        setLoading(false);
      }
    };
    fetchAeropuertos();
  }, []);

  // HANDLERS
  const handleAgregarPedido = () => toast.info("Abriendo formulario de pedido...");
  const handleCargaMasivaPedidos = () => toast.success("Carga Masiva de PEDIDOS iniciada...");
  const handleCargaMasivaCancelaciones = () => toast.error("Carga Masiva de CANCELACIONES iniciada...");

  return (
    // CONTENEDOR RELATIVO (El mapa ocupará todo el fondo)
    <div className="relative h-[calc(100vh-4rem)] w-full overflow-hidden bg-background">
      
      {/* 🌍 MAPA (FONDO TOTAL) */}
      <div className="absolute inset-0 z-0">
        <AnimatedFlights
          key={aeropuertosMap.length}
          itinerarios={itinerarios}
          aeropuertos={aeropuertosMap}
          speedKmh={900}
        />
      </div>

      {/* CAPA DE INTERFAZ FLOTANTE (Sobre el mapa) */}
      
      {/* 1. BARRA SUPERIOR DE ESTADO */}
      <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-10 flex gap-3 pointer-events-none">
          {/* Tarjetas flotantes del mapa (Total / Entregados) */}
          <div className="bg-white/90 backdrop-blur-md px-4 py-2 rounded-lg shadow-lg border flex items-center gap-3 pointer-events-auto">
              <div className="p-1.5 bg-gray-100 rounded-full text-gray-600">
                  <LayoutDashboard className="h-4 w-4" />
              </div>
              <div>
                  <p className="text-[10px] uppercase text-muted-foreground font-bold">Total</p>
                  <p className="text-lg font-bold leading-none">{metrics.total}</p>
              </div>
          </div>
          <div className="bg-white/90 backdrop-blur-md px-4 py-2 rounded-lg shadow-lg border flex items-center gap-3 pointer-events-auto">
              <div className="p-1.5 bg-green-100 rounded-full text-green-600">
                  <CheckCircle2 className="h-4 w-4" />
              </div>
              <div>
                  <p className="text-[10px] uppercase text-muted-foreground font-bold">Entregados</p>
                  <p className="text-lg font-bold leading-none">{metrics.completados}</p>
              </div>
          </div>
      </div>

      {/* 2. BOTÓN ABRIR PANEL (Solo visible si el panel está cerrado) */}
      {!isPanelOpen && (
        <Button 
          variant="secondary" 
          className="absolute top-4 right-4 z-20 shadow-xl gap-2 bg-white hover:bg-gray-50 text-black"
          onClick={() => setIsPanelOpen(true)}
        >
          <LayoutPanelLeft className="h-4 w-4" /> Ver Panel
        </Button>
      )}

      {/* 3. INDICADOR DE CARGA (Izquierda abajo) */}
      {loading && (
          <div className="absolute bottom-4 left-4 z-10 bg-black/80 text-white px-3 py-1 rounded-full text-xs flex items-center gap-2 shadow-lg">
              <Activity className="h-3 w-3 animate-spin" /> Sincronizando red...
          </div>
      )}

      {/* 🎛️ PANEL DERECHO FLOTANTE (Overlay) */}
      <div 
        className={`
          absolute right-0 top-0 h-full bg-white/95 backdrop-blur-md shadow-2xl z-30 
          border-l transition-all duration-300 ease-in-out flex flex-col
          ${isPanelOpen ? "w-96 translate-x-0" : "w-0 translate-x-full opacity-0"}
        `}
      >
        {/* CABECERA PANEL */}
        <div className="p-4 border-b flex items-center justify-between bg-muted/30">
          <div className="font-semibold text-sm flex items-center gap-2">
            <Activity className="h-4 w-4 text-blue-600" />
            Control de Operaciones
          </div>
          <Button 
            variant="ghost" 
            size="sm" 
            className="text-xs gap-1 text-muted-foreground hover:text-foreground hover:bg-gray-200/50" 
            onClick={() => setIsPanelOpen(false)}
          >
            Ocultar <ChevronRight className="h-4 w-4" />
          </Button>
        </div>

        {/* TABS */}
        <Tabs defaultValue="pedidos" className="flex-1 flex flex-col">
          <div className="px-4 pt-4">
            <TabsList className="grid w-full grid-cols-3 bg-muted/50">
              <TabsTrigger value="pedidos" className="text-xs">Pedidos</TabsTrigger>
              <TabsTrigger value="vuelos" className="text-xs">Vuelos</TabsTrigger>
              <TabsTrigger value="cancelaciones" className="text-[10px]">Cancelaciones</TabsTrigger>
            </TabsList>
          </div>

          {/* --- TAB 1: PEDIDOS --- */}
          <TabsContent value="pedidos" className="flex-1 flex flex-col p-0 m-0 overflow-hidden">
            
            {/* BOTONES ACCIÓN */}
            <div className="p-4 space-y-3">
              <Button 
                onClick={handleAgregarPedido} 
                className="w-full bg-black hover:bg-gray-800 text-white shadow transition-colors"
              >
                <Plus className="mr-2 h-4 w-4" /> Nuevo Pedido
              </Button>
              
              <Button 
                onClick={handleCargaMasivaPedidos} 
                variant="outline" 
                className="w-full border-dashed border-gray-300 hover:bg-gray-50"
              >
                <Upload className="mr-2 h-4 w-4" /> Carga Masiva
              </Button>
            </div>

            {/* 🔥 SUB-PANEL DE MÉTRICAS IDÉNTICO A LA FOTO 🔥 */}
            <div className="px-4 pb-4 grid grid-cols-4 gap-2">
                {/* Pendientes */}
                <div className="flex flex-col items-center justify-center p-2 rounded-md bg-orange-100 text-orange-700">
                    <span className="text-[10px] font-bold uppercase opacity-70">Pend.</span>
                    <span className="text-xl font-bold leading-none mt-1">{metrics.pendientes}</span>
                </div>
                
                {/* Tránsito */}
                <div className="flex flex-col items-center justify-center p-2 rounded-md bg-blue-100 text-blue-700">
                    <span className="text-[10px] font-bold uppercase opacity-70">Tran.</span>
                    <span className="text-xl font-bold leading-none mt-1">{metrics.enTransito}</span>
                </div>
                
                {/* Completados */}
                <div className="flex flex-col items-center justify-center p-2 rounded-md bg-green-100 text-green-700">
                    <span className="text-[10px] font-bold uppercase opacity-70">Comp.</span>
                    <span className="text-xl font-bold leading-none mt-1">{metrics.completados}</span>
                </div>
                
                {/* Alertas / Sin Asignar */}
                <div className="flex flex-col items-center justify-center p-2 rounded-md bg-red-100 text-red-700">
                    <span className="text-[10px] font-bold uppercase opacity-70">Alertas</span>
                    <span className="text-xl font-bold leading-none mt-1">{metrics.sinAsignar}</span>
                </div>
            </div>
            
            <Separator />

            {/* LISTA */}
            <div className="flex-1 overflow-y-auto p-4 flex flex-col items-center justify-center text-muted-foreground">
               <Package className="h-10 w-10 mb-2 opacity-10" />
               <p className="text-sm">No hay pedidos recientes.</p>
            </div>
          </TabsContent>

          {/* --- TAB 2: VUELOS --- */}
          <TabsContent value="vuelos" className="flex-1 flex flex-col p-0 m-0 overflow-hidden">
            <div className="flex-1 overflow-y-auto p-4 flex flex-col items-center justify-center text-muted-foreground">
               <Plane className="h-10 w-10 mb-2 opacity-10" />
               <p className="text-sm">Programación de vuelos vacía.</p>
            </div>
          </TabsContent>

          {/* --- TAB 3: CANCELACIONES --- */}
          <TabsContent value="cancelaciones" className="flex-1 flex flex-col p-0 m-0 overflow-hidden">
            <div className="p-4 space-y-3">
                <div className="p-3 bg-red-50 border border-red-100 rounded-md text-xs text-red-800 flex items-start gap-2">
                    <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
                    <div>
                      Gestión de bloqueos y cancelaciones de vuelos programados.
                    </div>
                </div>
                
                <Button 
                    onClick={handleCargaMasivaCancelaciones} 
                    variant="outline" 
                    className="w-full border-dashed border-red-200 hover:bg-red-50 hover:text-red-600 text-red-600"
                >
                    <Upload className="mr-2 h-4 w-4" /> Carga Masiva
                </Button>
            </div>
            <Separator />
            <div className="flex-1 overflow-y-auto p-4 flex flex-col items-center justify-center text-muted-foreground">
               <Ban className="h-10 w-10 mb-2 opacity-10" />
               <p className="text-sm">Sin cancelaciones registradas.</p>
            </div>
          </TabsContent>

        </Tabs>
        
        {/* FOOTER */}
        <div className="p-3 border-t bg-muted/20 text-[10px] text-center text-muted-foreground">
            MoraPack Ops • {new Date().toLocaleDateString()}
        </div>
      </div>
    </div>
  );
}