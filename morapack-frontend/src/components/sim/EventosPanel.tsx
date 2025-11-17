"use client";

import { memo, useState } from "react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { Plane, Package, History } from "lucide-react";
import CancellationsTab from "@/components/events/CancellationsTab";
import DynamicOrdersTab from "@/components/events/DynamicOrdersTab";
import HistoryTab from "@/components/events/HistoryTab";
import type { FlightCancellation, DynamicOrder } from "@/types/simulation/events.types";
import type { Aeropuerto } from "@/types/aeropuerto";

interface EventosPanelProps {
  aeropuertos: Aeropuerto[];
  cancellations: FlightCancellation[];
  dynamicOrders: DynamicOrder[];
  onCancellationCreated: (c: FlightCancellation) => void;
  onOrderCreated: (o: DynamicOrder) => void;
  onRefresh: () => void;
}

export const EventosPanel = memo(function EventosPanel({
  aeropuertos,
  cancellations,
  dynamicOrders,
  onCancellationCreated,
  onOrderCreated,
  onRefresh,
}: EventosPanelProps) {
  const [activeTab, setActiveTab] = useState("cancellations");

  return (
    <div className="h-full flex flex-col bg-white">
      {/* Header */}
      <div className="p-3 border-b bg-slate-50">
        <h3 className="font-bold text-base">Eventos Dinamicos</h3>
      </div>

      {/* Tabs */}
      <Tabs 
        value={activeTab} 
        onValueChange={setActiveTab} 
        className="flex-1 flex flex-col overflow-hidden"
      >
        <TabsList className="w-full grid grid-cols-3 mx-3 mt-3 mb-0">
          <TabsTrigger value="cancellations" className="text-xs gap-1.5">
            <Plane className="h-3 w-3" />
            Cancelar
          </TabsTrigger>
          <TabsTrigger value="orders" className="text-xs gap-1.5">
            <Package className="h-3 w-3" />
            Pedidos
          </TabsTrigger>
          <TabsTrigger value="history" className="text-xs gap-1.5">
            <History className="h-3 w-3" />
            Historial
          </TabsTrigger>
        </TabsList>

        <TabsContent value="cancellations" className="flex-1 overflow-hidden m-0">
          <CancellationsTab
            aeropuertos={aeropuertos}
            cancellations={cancellations}
            onCancellationCreated={onCancellationCreated}
            onRefresh={onRefresh}
          />
        </TabsContent>

        <TabsContent value="orders" className="flex-1 overflow-hidden m-0">
          <DynamicOrdersTab
            aeropuertos={aeropuertos}
            orders={dynamicOrders}
            onOrderCreated={onOrderCreated}
            onRefresh={onRefresh}
          />
        </TabsContent>

        <TabsContent value="history" className="flex-1 overflow-hidden m-0">
          <HistoryTab 
            cancellations={cancellations} 
            orders={dynamicOrders} 
          />
        </TabsContent>
      </Tabs>
    </div>
  );
});


