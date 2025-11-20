"use client";

import { memo } from "react";
import { Plane } from "lucide-react";
import CancellationsTab from "@/components/events/CancellationsTab";
import type { FlightCancellation } from "@/types/simulation/events.types";

interface EventosPanelProps {
  cancellations: FlightCancellation[];
  onCancellationCreated: (c: FlightCancellation) => void;
  onRefresh: () => void;
}

export const EventosPanel = memo(function EventosPanel({
  cancellations,
  onCancellationCreated,
  onRefresh,
}: EventosPanelProps) {
  return (
    <div className="h-full flex flex-col bg-white">
      {/* Header */}
      <div className="p-3 border-b bg-slate-50">
        <div className="flex items-center gap-2">
          <Plane className="h-4 w-4" />
          <h3 className="font-bold text-base">Cancelaciones de Vuelos</h3>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        <CancellationsTab
          cancellations={cancellations}
          onCancellationCreated={onCancellationCreated}
          onRefresh={onRefresh}
        />
      </div>
    </div>
  );
});


