"use client";

import { useMemo } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Plane, Package, Clock } from "lucide-react";
import type { FlightCancellation, DynamicOrder } from "@/types/simulation/events.types";

interface HistoryTabProps {
  cancellations: FlightCancellation[];
  orders: DynamicOrder[];
}

type HistoryEvent = {
  id: string;
  type: 'CANCELLATION' | 'DYNAMIC_ORDER';
  timestamp: Date;
  data: FlightCancellation | DynamicOrder;
};

export default function HistoryTab({ cancellations, orders }: HistoryTabProps) {
  const events = useMemo<HistoryEvent[]>(() => {
    const allEvents: HistoryEvent[] = [];

    // Add cancellations that have been executed
    cancellations
      .filter(c => c.status === 'EXECUTED' && c.cancellationTime)
      .forEach(c => {
        allEvents.push({
          id: c.id,
          type: 'CANCELLATION',
          timestamp: new Date(c.cancellationTime!),
          data: c,
        });
      });

    // Add dynamic orders that have been injected
    orders
      .filter(o => o.status === 'INJECTED' && o.injectionTime)
      .forEach(o => {
        allEvents.push({
          id: o.id,
          type: 'DYNAMIC_ORDER',
          timestamp: new Date(o.injectionTime!),
          data: o,
        });
      });

    // Sort by timestamp (most recent first)
    return allEvents.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  }, [cancellations, orders]);

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="p-3 border-b">
        <h4 className="text-sm font-semibold">Eventos Procesados ({events.length})</h4>
        <p className="text-xs text-muted-foreground mt-1">
          Historial de eventos que han sido ejecutados en la simulacion
        </p>
      </div>

      {/* Events List */}
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {events.length === 0 && (
          <div className="text-center text-gray-400 py-8 text-sm">
            No hay eventos procesados aun
          </div>
        )}

        {events.map((event) => (
          <EventCard key={event.id} event={event} />
        ))}
      </div>
    </div>
  );
}

function EventCard({ event }: { event: HistoryEvent }) {
  if (event.type === 'CANCELLATION') {
    const cancellation = event.data as FlightCancellation;
    return (
      <Card className="border border-red-200">
        <CardContent className="p-3 space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-semibold">
              <Plane className="h-3 w-3 text-red-600" />
              <span>Cancelacion de Vuelo</span>
            </div>
            <Badge className="text-[10px] px-2 py-0.5 bg-red-100 text-red-800 border-red-300">
              Ejecutada
            </Badge>
          </div>

          <div className="text-xs text-muted-foreground space-y-1">
            <div>
              <span className="font-medium">Ruta:</span>{' '}
              {cancellation.flightOrigin} → {cancellation.flightDestination}
            </div>
            <div className="flex items-center gap-2">
              <Clock className="h-3 w-3" />
              <span>
                Procesado: {event.timestamp.toLocaleString('es-ES')}
              </span>
            </div>
            {cancellation.reason && (
              <div className="text-xs italic">
                Razon: {cancellation.reason}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (event.type === 'DYNAMIC_ORDER') {
    const order = event.data as DynamicOrder;
    return (
      <Card className="border border-green-200">
        <CardContent className="p-3 space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-semibold">
              <Package className="h-3 w-3 text-green-600" />
              <span>Pedido Inyectado</span>
            </div>
            <Badge className="text-[10px] px-2 py-0.5 bg-green-100 text-green-800 border-green-300">
              Inyectado
            </Badge>
          </div>

          <div className="text-xs text-muted-foreground space-y-1">
            <div>
              <span className="font-medium">Ruta:</span>{' '}
              {order.origin} → {order.destination}
            </div>
            <div>
              <span className="font-medium">Cantidad:</span> {order.quantity} unidades
            </div>
            <div className="flex items-center gap-2">
              <Clock className="h-3 w-3" />
              <span>
                Procesado: {event.timestamp.toLocaleString('es-ES')}
              </span>
            </div>
            {order.reason && (
              <div className="text-xs italic">
                Razon: {order.reason}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  return null;
}


