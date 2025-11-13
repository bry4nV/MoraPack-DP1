"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Package, MapPin, Target, Clock, AlertCircle } from "lucide-react";
import { addDynamicOrder } from "@/lib/dynamic-events-api";
import type { DynamicOrder } from "@/types/simulation/events.types";
import type { Aeropuerto } from "@/types";

interface DynamicOrdersTabProps {
  aeropuertos: Aeropuerto[];
  orders: DynamicOrder[];
  onOrderCreated: (o: DynamicOrder) => void;
  onRefresh: () => void;
}

export default function DynamicOrdersTab({
  aeropuertos,
  orders,
  onOrderCreated,
  onRefresh,
}: DynamicOrdersTabProps) {
  const [destination, setDestination] = useState("");
  const [quantity, setQuantity] = useState("100");
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);
    setLoading(true);

    try {
      const result = await addDynamicOrder({
        destination,
        quantity: parseInt(quantity, 10),
        reason: reason || "Manual order",
      });

      if (result.success && result.order) {
        onOrderCreated(result.order);
        setSuccess(true);
        // Reset form
        setDestination("");
        setQuantity("100");
        setReason("");
        // Refresh list
        setTimeout(() => onRefresh(), 500);
      } else {
        setError(result.message);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Form Section */}
      <div className="p-3 border-b">
        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="destination" className="text-xs">Destino</Label>
            <Select value={destination} onValueChange={setDestination} required>
              <SelectTrigger id="destination" className="h-9 text-sm">
                <SelectValue placeholder="Seleccionar aeropuerto" />
              </SelectTrigger>
              <SelectContent>
                {aeropuertos.map((a) => (
                  <SelectItem key={a.codigo} value={a.codigo}>
                    {a.codigo} - {a.nombre}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground mt-1">
              El pedido se inyecta inmediatamente. Origen y plazo (48h/72h) se determinan automáticamente según continente.
            </p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="quantity" className="text-xs">Cantidad</Label>
            <Input
              id="quantity"
              type="number"
              min="1"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="h-9 text-sm"
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reason" className="text-xs">Razon (opcional)</Label>
            <Textarea
              id="reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Ej: Pedido urgente"
              className="text-sm resize-none"
              rows={2}
            />
          </div>

          {error && (
            <div className="text-xs text-red-600 bg-red-50 p-2 rounded border border-red-200">
              {error}
            </div>
          )}

          {success && (
            <div className="text-xs text-green-600 bg-green-50 p-2 rounded border border-green-200">
              Pedido agregado exitosamente
            </div>
          )}

          <Button
            type="submit"
            disabled={loading || !destination || !quantity}
            className="w-full h-9 text-sm"
          >
            {loading ? "Agregando..." : "Agregar Pedido"}
          </Button>
        </form>
      </div>

      {/* List Section */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        <div className="flex items-center justify-between mb-2">
          <h4 className="text-sm font-semibold">Pedidos Dinamicos ({orders.length})</h4>
        </div>

        {orders.length === 0 && (
          <div className="text-center text-gray-400 py-8 text-sm">
            No hay pedidos dinamicos
          </div>
        )}

        {orders.map((order) => (
          <DynamicOrderCard key={order.id} order={order} />
        ))}
      </div>
    </div>
  );
}

function DynamicOrderCard({ order }: { order: DynamicOrder }) {
  const statusConfig = {
    PENDING: {
      color: "bg-yellow-100 text-yellow-800 border-yellow-300",
      label: "Pendiente",
    },
    INJECTED: {
      color: "bg-green-100 text-green-800 border-green-300",
      label: "Inyectado",
    },
  };

  const config = statusConfig[order.status];

  return (
    <Card className="border">
      <CardContent className="p-3 space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-semibold">
            <Package className="h-3 w-3" />
            <span>{order.origin} → {order.destination}</span>
          </div>
          <Badge className={`text-[10px] px-2 py-0.5 ${config.color}`}>
            {config.label}
          </Badge>
        </div>

        <div className="text-xs text-muted-foreground space-y-1">
          <div className="flex items-center gap-2">
            <Package className="h-3 w-3" />
            <span>Cantidad: {order.quantity} unidades</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock className="h-3 w-3" />
            <span>Plazo: {order.deadlineHours} horas</span>
          </div>
          {order.reason && (
            <div className="flex items-start gap-2">
              <AlertCircle className="h-3 w-3 mt-0.5 flex-shrink-0" />
              <span>{order.reason}</span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}


