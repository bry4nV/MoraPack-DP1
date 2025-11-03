"use client";

import { useMemo, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { MapPin, Target, Plane, Package } from "lucide-react";
import type { OrderSummary, OrderMetrics, OrderStatus } from "@/types";

interface PedidosPanelProps {
  pedidos: OrderSummary[];
  metricas: OrderMetrics | null;
  mode?: "preview" | "realtime";
  onSelectPedido?: (pedidoId: number) => void;
}

export function PedidosPanel({
  pedidos,
  metricas,
  mode = "realtime",
  onSelectPedido,
}: PedidosPanelProps) {
  const [filtro, setFiltro] = useState<"todos" | OrderStatus>("todos");
  const [busqueda, setBusqueda] = useState("");

  const pedidosFiltrados = useMemo(() => {
    let filtered = pedidos;

    // Filtrar por estado
    if (filtro !== "todos") {
      filtered = filtered.filter((p) => p.estado === filtro);
    }

    // Búsqueda por código, origen o destino
    if (busqueda) {
      const search = busqueda.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.codigo.toLowerCase().includes(search) ||
          p.origenNombre.toLowerCase().includes(search) ||
          p.destinoNombre.toLowerCase().includes(search) ||
          p.origenCodigo.toLowerCase().includes(search) ||
          p.destinoCodigo.toLowerCase().includes(search)
      );
    }

    return filtered;
  }, [pedidos, filtro, busqueda]);

  return (
    <div className="h-full flex flex-col bg-white border-l">
      {/* Header con métricas */}
      <div className="p-3 border-b bg-slate-50">
        <div className="flex items-center justify-between mb-2">
          <h3 className="font-bold text-base">Pedidos</h3>
          {mode === "preview" && (
            <Badge variant="outline" className="text-xs">
              Vista previa
            </Badge>
          )}
        </div>

        {metricas && (
          <div className="grid grid-cols-4 gap-1.5 text-xs">
            <MetricBadge
              label="Total"
              value={metricas.totalPedidos}
              color="blue"
            />
            <MetricBadge
              label="Pendientes"
              value={metricas.pendientes}
              color="yellow"
            />
            <MetricBadge
              label="En tránsito"
              value={metricas.enTransito}
              color="orange"
            />
            <MetricBadge
              label="Completados"
              value={metricas.completados}
              color="green"
            />
          </div>
        )}

        {metricas && mode === "realtime" && (
          <div className="mt-2 pt-2 border-t">
            <div className="flex justify-between text-[11px] mb-1">
              <span className="text-muted-foreground">Asignación</span>
              <span className="font-semibold">
                {metricas.productosAsignados.toLocaleString()} /{" "}
                {metricas.totalProductos.toLocaleString()}
              </span>
            </div>
            <Progress
              value={metricas.tasaAsignacionPercent}
              className="h-1.5"
            />
            <div className="text-right text-[10px] text-muted-foreground mt-0.5">
              {metricas.tasaAsignacionPercent.toFixed(1)}%
            </div>
          </div>
        )}
      </div>

      {/* Filtros */}
      <div className="p-3 border-b space-y-2">
        <Input
          placeholder="🔍 Buscar pedido..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="text-sm h-9"
        />

        <Tabs
          value={filtro}
          onValueChange={(value: string) => setFiltro(value as any)}
          className="w-full"
        >
          <TabsList className="grid w-full grid-cols-5 h-8">
            <TabsTrigger value="todos" className="text-xs">
              Todos
            </TabsTrigger>
            <TabsTrigger value="PENDING" className="text-xs">
              Pend.
            </TabsTrigger>
            <TabsTrigger value="IN_TRANSIT" className="text-xs">
              Trán.
            </TabsTrigger>
            <TabsTrigger value="COMPLETED" className="text-xs">
              OK
            </TabsTrigger>
            <TabsTrigger value="UNASSIGNED" className="text-xs">
              Sin
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {/* Lista de pedidos (scrolleable) */}
      <div className="flex-1 overflow-y-auto p-3 space-y-3">
        {pedidosFiltrados.length === 0 && (
          <div className="text-center text-gray-400 py-8 text-sm">
            {busqueda
              ? "No se encontraron pedidos"
              : "No hay pedidos en esta categoría"}
          </div>
        )}

        {pedidosFiltrados.map((pedido) => (
          <PedidoCard
            key={pedido.id}
            pedido={pedido}
            mode={mode}
            onClick={() => onSelectPedido?.(pedido.id)}
          />
        ))}
      </div>
    </div>
  );
}

// Metric badge component
function MetricBadge({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: "blue" | "yellow" | "orange" | "green";
}) {
  const bgColors = {
    blue: "bg-blue-100 text-blue-800",
    yellow: "bg-yellow-100 text-yellow-800",
    orange: "bg-orange-100 text-orange-800",
    green: "bg-green-100 text-green-800",
  };

  return (
    <div className={`${bgColors[color]} rounded-md px-2 py-1.5 text-center`}>
      <div className="text-[10px] text-muted-foreground leading-tight">{label}</div>
      <div className="text-base font-bold leading-tight mt-0.5">{value}</div>
    </div>
  );
}

// Card individual de pedido
function PedidoCard({
  pedido,
  mode,
  onClick,
}: {
  pedido: OrderSummary;
  mode: "preview" | "realtime";
  onClick: () => void;
}) {
  const estadoConfig = {
    PENDING: {
      color: "bg-yellow-100 text-yellow-800 border-yellow-300",
      label: "Pendiente",
    },
    IN_TRANSIT: {
      color: "bg-blue-100 text-blue-800 border-blue-300",
      label: "En tránsito",
    },
    COMPLETED: {
      color: "bg-green-100 text-green-800 border-green-300",
      label: "Completado",
    },
    UNASSIGNED: {
      color: "bg-red-100 text-red-800 border-red-300",
      label: "Sin asignar",
    },
  };

  const config = estadoConfig[pedido.estado];

  return (
    <Card
      className="cursor-pointer hover:shadow-md transition-shadow border"
      onClick={onClick}
    >
      <CardContent className="p-4 space-y-3">
        {/* Header */}
        <div className="flex items-center justify-between">
          <span className="font-bold text-base">{pedido.codigo}</span>
          <Badge className={`text-xs px-2.5 py-0.5 ${config.color}`}>{config.label}</Badge>
        </div>

        {/* Ruta */}
        <div className="text-sm space-y-1.5">
          <div className="flex items-center gap-2">
            <MapPin className="h-4 w-4 text-blue-500 flex-shrink-0" />
            <span className="truncate">
              {pedido.origenNombre} ({pedido.origenCodigo})
            </span>
          </div>
          <div className="flex items-center gap-2">
            <Target className="h-4 w-4 text-green-500 flex-shrink-0" />
            <span className="truncate">
              {pedido.destinoNombre} ({pedido.destinoCodigo})
            </span>
          </div>
        </div>

        {/* Productos */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Package className="h-4 w-4" />
          <span className="font-medium">
            {pedido.cantidadTotal} producto{pedido.cantidadTotal !== 1 && "s"}
          </span>
        </div>

        {/* Progreso (solo en modo realtime) */}
        {mode === "realtime" && (
          <div>
            <div className="flex justify-between text-sm mb-1.5">
              <span className="text-muted-foreground">Asignado</span>
              <span className="font-bold">
                {pedido.cantidadAsignada}/{pedido.cantidadTotal}
              </span>
            </div>
            <Progress value={pedido.progresoPercent} className="h-2.5" />
          </div>
        )}

        {/* Vuelos asignados (solo en modo realtime si hay) */}
        {mode === "realtime" && pedido.vuelosAsignados.length > 0 && (
          <div className="text-sm text-muted-foreground flex items-center gap-2">
            <Plane className="h-4 w-4" />
            <span className="font-medium">
              {pedido.vuelosAsignados.length} vuelo
              {pedido.vuelosAsignados.length !== 1 && "s"}
            </span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

