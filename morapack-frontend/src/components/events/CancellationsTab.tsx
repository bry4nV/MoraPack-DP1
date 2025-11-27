"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Plane, Clock, AlertCircle, CheckCircle, Upload, List, FileText, RefreshCw } from "lucide-react";
import BulkCancellationUpload from "@/components/events/BulkCancellationUpload";
import ReplanificationDetailsComponent from "@/components/events/ReplanificationDetails";
import type { FlightCancellation } from "@/types/simulation/events.types";

interface CancellationsTabProps {
  cancellations: FlightCancellation[];
  onCancellationCreated: (c: FlightCancellation) => void;
  onRefresh: () => void;
  currentSimulationTime?: string;
  simulationStartDate?: string;
}

export default function CancellationsTab({
  cancellations,
  onCancellationCreated,
  onRefresh,
  currentSimulationTime,
  simulationStartDate,
}: CancellationsTabProps) {
  const [activeTab, setActiveTab] = useState<string>("upload");

  // Separar cancelaciones por estado
  const pendingCancellations = cancellations.filter(c => c.status === "PENDING");
  const executedCancellations = cancellations.filter(c => c.status === "EXECUTED");

  // DEBUG: Log cancellations status
  console.log("🔍 [CancellationsTab] Total cancellations:", cancellations.length);
  console.log("🔍 [CancellationsTab] Pending:", pendingCancellations.length);
  console.log("🔍 [CancellationsTab] Executed:", executedCancellations.length);
  if (cancellations.length > 0) {
    console.log("🔍 [CancellationsTab] First cancellation:", cancellations[0]);
  }

  return (
    <div className="h-full flex flex-col">
      <Tabs value={activeTab} onValueChange={setActiveTab} className="h-full flex flex-col">
        <TabsList className="grid w-full grid-cols-2 mx-3 mt-3">
          <TabsTrigger value="upload" className="text-sm">
            <Upload className="h-4 w-4 mr-2" />
            Cargar CSV
          </TabsTrigger>
          <TabsTrigger value="list" className="text-sm">
            <List className="h-4 w-4 mr-2" />
            Ver Cancelaciones
            {cancellations.length > 0 && (
              <Badge variant="secondary" className="ml-2 text-xs">
                {cancellations.length}
              </Badge>
            )}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="upload" className="flex-1 overflow-y-auto p-3 m-0">
          <BulkCancellationUpload
            onCancellationsUploaded={onRefresh}
            currentSimulationTime={currentSimulationTime}
            simulationStartDate={simulationStartDate}
          />
        </TabsContent>

        <TabsContent value="list" className="flex-1 overflow-y-auto p-3 space-y-4 m-0">
        {/* Header con botón de refrescar */}
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-sm font-semibold text-gray-700">
            Cancelaciones Registradas
          </h3>
          <button
            onClick={onRefresh}
            className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-md transition-colors"
            title="Refrescar cancelaciones"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refrescar
          </button>
        </div>

        {/* Cancelaciones Programadas */}
        {pendingCancellations.length > 0 && (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-semibold flex items-center gap-2">
                <Clock className="h-4 w-4 text-yellow-600" />
                Programadas ({pendingCancellations.length})
              </h4>
            </div>
            <div className="space-y-2">
              {pendingCancellations.map((cancellation) => (
                <CancellationCard key={cancellation.id} cancellation={cancellation} />
              ))}
            </div>
          </div>
        )}

        {/* Cancelaciones Ejecutadas */}
        {executedCancellations.length > 0 && (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <h4 className="text-sm font-semibold flex items-center gap-2">
                <CheckCircle className="h-4 w-4 text-green-600" />
                Ejecutadas ({executedCancellations.length})
              </h4>
            </div>
            <div className="space-y-2">
              {executedCancellations.map((cancellation) => (
                <CancellationCard key={cancellation.id} cancellation={cancellation} />
              ))}
            </div>
          </div>
        )}

          {/* Empty state */}
          {cancellations.length === 0 && (
            <div className="text-center text-gray-400 py-12 text-sm">
              <Plane className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p className="font-medium">No hay cancelaciones</p>
              <p className="text-xs mt-1">Carga un archivo CSV para programar cancelaciones</p>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}

function CancellationCard({ cancellation }: { cancellation: FlightCancellation }) {
  const isPending = cancellation.status === "PENDING";

  const statusConfig: Record<string, { color: string; label: string; icon: any }> = {
    PENDING: {
      color: "bg-yellow-100 text-yellow-800 border-yellow-300",
      label: "Programada",
      icon: Clock,
    },
    EXECUTED: {
      color: "bg-green-100 text-green-800 border-green-300",
      label: "Ejecutada",
      icon: CheckCircle,
    },
    FAILED: {
      color: "bg-red-100 text-red-800 border-red-300",
      label: "Fallida",
      icon: AlertCircle,
    },
  };

  // 🔍 DEBUG: Log status
  console.log('🔍 [CancellationCard] Status:', cancellation.status, 'Full object:', cancellation);

  const config = statusConfig[cancellation.status] || statusConfig.PENDING;
  const StatusIcon = config.icon;

  return (
    <Card className="border">
      <CardContent className="p-3 space-y-2">
        {/* Header: Ruta + Estado */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-semibold">
            <Plane className="h-3.5 w-3.5 text-muted-foreground" />
            <span>{cancellation.flightOrigin} → {cancellation.flightDestination}</span>
          </div>
          <Badge className={`text-[10px] px-2 py-0.5 ${config.color} flex items-center gap-1`}>
            <StatusIcon className="h-3 w-3" />
            {config.label}
          </Badge>
        </div>

        {/* Detalles */}
        <div className="text-xs text-muted-foreground space-y-1">
          <div className="flex items-center gap-2">
            <Clock className="h-3 w-3" />
            {cancellation.cancellationTime ? (
              <span>Programado: {new Date(cancellation.cancellationTime).toLocaleString('es-ES', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
              })} (hora vuelo: {cancellation.scheduledDepartureTime})</span>
            ) : (
              <span>Hora vuelo: {cancellation.scheduledDepartureTime}</span>
            )}
          </div>

          {/* 🆕 Detalles de replanificación (expandible) */}
          {cancellation.status === "EXECUTED" && cancellation.replanificationTriggered && cancellation.replanificationDetails && (
            <ReplanificationDetailsComponent
              details={cancellation.replanificationDetails}
              affectedProductsCount={cancellation.affectedProductsCount}
            />
          )}

          {/* Fallback: Mostrar info básica si no hay detalles completos */}
          {cancellation.status === "EXECUTED" && cancellation.replanificationTriggered && !cancellation.replanificationDetails && (
            <div className="flex items-center gap-2 text-blue-700 bg-blue-50 p-1.5 rounded mt-1">
              <FileText className="h-3 w-3" />
              <span className="font-medium text-xs">
                {cancellation.affectedProductsCount && cancellation.affectedProductsCount > 0
                  ? `✓ ${cancellation.affectedProductsCount} productos replanificados`
                  : `✓ Replanificación completada`}
              </span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
