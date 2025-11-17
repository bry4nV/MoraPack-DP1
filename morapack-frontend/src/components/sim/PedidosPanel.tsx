"use client";

import { memo, useMemo, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { MapPin, Target, Plane, Package, Calendar, ArrowRight } from "lucide-react";
import type { OrderSummary, OrderMetrics } from "@/types/simulation/order-summary.types";
import type { OrderStatus } from "@/types/shared";

interface PedidosPanelProps {
  pedidos: OrderSummary[];
  metricas: OrderMetrics | null;
  mode?: "preview" | "realtime";
  onSelectPedido?: (pedidoId: number) => void;
}

export const PedidosPanel = memo(function PedidosPanel({
  pedidos,
  metricas,
  mode = "realtime",
  onSelectPedido,
}: PedidosPanelProps) {
  const [filtro, setFiltro] = useState<"todos" | OrderStatus>("todos");
  const [busqueda, setBusqueda] = useState("");
  const [selectedPedido, setSelectedPedido] = useState<OrderSummary | null>(null);

  const pedidosFiltrados = useMemo(() => {
    let filtered = pedidos;

    // Filtrar por estado
    if (filtro !== "todos") {
      filtered = filtered.filter((p) => p.status === filtro);
    }

    // Búsqueda por código, origen o destino (temporalmente deshabilitado)
    /*
    if (busqueda) {
      const search = busqueda.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.code.toLowerCase().includes(search) ||
          p.originName.toLowerCase().includes(search) ||
          p.destinationName.toLowerCase().includes(search) ||
          p.originCode.toLowerCase().includes(search) ||
          p.destinationCode.toLowerCase().includes(search)
      );
    }
    */

    return filtered;
  }, [pedidos, filtro]);

  // Recalcular métricas basadas en pedidos visibles (filtrados por tiempo)
  const metricasAjustadas = useMemo(() => {
    if (!metricas) return null;

    // Contar estados en pedidos visibles
    const pending = pedidos.filter(p => p.status === 'PENDING').length;
    const inTransit = pedidos.filter(p => p.status === 'IN_TRANSIT').length;
    const completed = pedidos.filter(p => p.status === 'COMPLETED').length;
    const unassigned = pedidos.filter(p => p.status === 'UNASSIGNED').length;

    const totalProducts = pedidos.reduce((sum, p) => sum + p.totalQuantity, 0);
    const assignedProducts = pedidos.reduce((sum, p) => sum + p.assignedQuantity, 0);

    return {
      totalOrders: pedidos.length,
      pending,
      inTransit,
      completed,
      unassigned,
      totalProducts,
      assignedProducts,
      assignmentRatePercent: totalProducts > 0 ? (assignedProducts / totalProducts) * 100 : 0
    };
  }, [pedidos, metricas]);

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

        {metricasAjustadas && (
          <div className="grid grid-cols-4 gap-1.5 text-xs">
            <MetricBadge
              label="Total"
              value={metricasAjustadas.totalOrders}
              color="slate"
            />
            <MetricBadge
              label="Pendientes"
              value={metricasAjustadas.pending}
              color="yellow"
            />
            <MetricBadge
              label="En tránsito"
              value={metricasAjustadas.inTransit}
              color="blue"
            />
            <MetricBadge
              label="Completados"
              value={metricasAjustadas.completed}
              color="green"
            />
          </div>
        )}

        {metricasAjustadas && mode === "realtime" && (
          <div className="mt-2 pt-2 border-t">
            <div className="flex justify-between text-[11px] mb-1">
              <span className="text-muted-foreground">Asignación</span>
              <span className="font-semibold">
                {metricasAjustadas.assignedProducts.toLocaleString()} /{" "}
                {metricasAjustadas.totalProducts.toLocaleString()}
              </span>
            </div>
            <Progress
              value={metricasAjustadas.assignmentRatePercent}
              className="h-1.5"
            />
            <div className="text-right text-[10px] text-muted-foreground mt-0.5">
              {metricasAjustadas.assignmentRatePercent.toFixed(1)}%
            </div>
          </div>
        )}
      </div>

      {/* Filtros */}
      <div className="p-3 border-b space-y-2">
        {/* Barra de búsqueda oculta temporalmente
        <Input
          placeholder="Buscar pedido..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="text-sm h-9"
        />
        */}

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
              Comp.
            </TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {/* Lista de pedidos (scrolleable) */}
      <div className="flex-1 overflow-y-auto p-2 space-y-2">
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
            onClick={() => {
              setSelectedPedido(pedido);
              onSelectPedido?.(pedido.id);
            }}
          />
        ))}
      </div>

      {/* Modal de detalles del pedido */}
      <PedidoDetailsModal
        pedido={selectedPedido}
        onClose={() => setSelectedPedido(null)}
      />
    </div>
  );
});

// Metric badge component
function MetricBadge({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: "slate" | "yellow" | "blue" | "green";
}) {
  const bgColors = {
    slate: "bg-slate-100 text-slate-800",
    yellow: "bg-yellow-100 text-yellow-800",
    blue: "bg-blue-100 text-blue-800",
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

  const config = estadoConfig[pedido.status];

  return (
    <Card
      className="cursor-pointer hover:shadow-md transition-shadow border"
      onClick={onClick}
    >
      <CardContent className="p-3 space-y-2">
        {/* Header: Código y Estado */}
        <div className="flex items-center justify-between">
          <span className="font-bold text-sm">{pedido.code}</span>
          <Badge className={`text-xs px-2 py-0.5 ${config.color}`}>{config.label}</Badge>
        </div>

        {/* Destino */}
        <div className="flex items-center gap-2 text-sm">
          <Target className="h-4 w-4 text-green-500 flex-shrink-0" />
          <span className="truncate">
            {pedido.destinationName} ({pedido.destinationCode})
          </span>
        </div>

        {/* Cantidad de productos */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground pt-1 border-t">
          <Package className="h-4 w-4" />
          <span className="font-medium">
            {pedido.totalQuantity} producto{pedido.totalQuantity !== 1 && "s"}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}

// Modal de detalles del pedido
function PedidoDetailsModal({
  pedido,
  onClose,
}: {
  pedido: OrderSummary | null;
  onClose: () => void;
}) {
  if (!pedido) return null;

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

  const config = estadoConfig[pedido.status];

  return (
    <Dialog open={!!pedido} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center justify-between">
            <span>{pedido.code}</span>
            <Badge className={`text-xs px-2.5 py-1 ${config.color}`}>{config.label}</Badge>
          </DialogTitle>
          <DialogDescription>
            Detalles completos del pedido
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 mt-4">
          {/* Información básica */}
          <div className="space-y-3">
            <h3 className="font-semibold text-sm text-muted-foreground uppercase">Información del Pedido</h3>

            {/* Ruta */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">Origen</Label>
                <div className="flex items-center gap-2 text-sm">
                  <MapPin className="h-4 w-4 text-blue-500" />
                  <span>{pedido.originName} ({pedido.originCode})</span>
                </div>
              </div>
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">Destino</Label>
                <div className="flex items-center gap-2 text-sm">
                  <Target className="h-4 w-4 text-green-500" />
                  <span>{pedido.destinationName} ({pedido.destinationCode})</span>
                </div>
              </div>
            </div>

            {/* Productos y fecha */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">Cantidad de Productos</Label>
                <div className="flex items-center gap-2 text-sm">
                  <Package className="h-4 w-4" />
                  <span className="font-medium">{pedido.totalQuantity} producto{pedido.totalQuantity !== 1 && "s"}</span>
                </div>
              </div>
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">Fecha de Solicitud</Label>
                <div className="flex items-center gap-2 text-sm">
                  <Calendar className="h-4 w-4" />
                  <span>
                    {new Date(pedido.requestDateISO).toLocaleDateString('es-PE', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </span>
                </div>
              </div>
            </div>

            {/* Progreso */}
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Progreso de Asignación</Label>
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span>Productos asignados</span>
                  <span className="font-bold">{pedido.assignedQuantity} / {pedido.totalQuantity}</span>
                </div>
                <Progress value={pedido.progressPercent} className="h-2" />
                <div className="text-right text-xs text-muted-foreground">
                  {pedido.progressPercent.toFixed(1)}%
                </div>
              </div>
            </div>
          </div>

          {/* Vuelos asignados */}
          {pedido.assignedFlights.length > 0 && (
            <div className="space-y-3">
              <h3 className="font-semibold text-sm text-muted-foreground uppercase flex items-center gap-2">
                <Plane className="h-4 w-4" />
                Ruta Asignada ({pedido.assignedFlights.length} vuelo{pedido.assignedFlights.length !== 1 && "s"})
              </h3>

              <div className="space-y-2">
                {pedido.assignedFlights.map((segment, index) => (
                  <div key={index} className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg border">
                    <div className="flex items-center gap-2 flex-1">
                      <span className="font-medium text-sm">{segment.originCode}</span>
                      <ArrowRight className="h-4 w-4 text-muted-foreground" />
                      <span className="font-medium text-sm">{segment.destinationCode}</span>
                    </div>
                    <div className="flex flex-col items-end gap-0.5">
                      <span className="text-xs font-mono text-muted-foreground">{segment.flightCode}</span>
                      <span className="text-[10px] text-muted-foreground">Vuelo #{index + 1}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Sin vuelos asignados */}
          {pedido.assignedFlights.length === 0 && (
            <div className="p-4 bg-amber-50 border border-amber-200 rounded-lg">
              <p className="text-sm text-amber-800">
                Este pedido aún no tiene vuelos asignados.
              </p>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

