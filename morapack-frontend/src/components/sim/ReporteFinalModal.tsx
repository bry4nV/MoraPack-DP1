"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { FinalReport, SimulationRating } from "@/types/simulation/final-report.types";

interface ReporteFinalModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  report: FinalReport | null;
}

function getRatingColor(rating: SimulationRating): string {
  switch (rating) {
    case "EXCELLENT":
      return "bg-green-600";
    case "GOOD":
      return "bg-blue-600";
    case "MODERATE":
      return "bg-yellow-600";
    case "POOR":
      return "bg-orange-600";
    case "CRITICAL":
      return "bg-red-600";
    default:
      return "bg-gray-600";
  }
}

function getRatingLabel(rating: SimulationRating): string {
  switch (rating) {
    case "EXCELLENT":
      return "Excelente";
    case "GOOD":
      return "Bueno";
    case "MODERATE":
      return "Moderado";
    case "POOR":
      return "Pobre";
    case "CRITICAL":
      return "Crítico";
    default:
      return rating;
  }
}

export function ReporteFinalModal({ open, onOpenChange, report }: ReporteFinalModalProps) {
  if (!report) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold">
            Reporte Final de Simulación
          </DialogTitle>
          <DialogDescription>
            Resumen completo de métricas y resultados
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Alerta de Colapso (si aplica) */}
          {report.collapseDetected && (
            <Card className="border-red-500 bg-red-50 dark:bg-red-950">
              <CardHeader>
                <CardTitle className="text-red-700 dark:text-red-300 flex items-center gap-2">
                  <span className="text-2xl">🚨</span>
                  <span>Sistema Colapsado</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-red-600 dark:text-red-400">{report.collapseReason}</p>
              </CardContent>
            </Card>
          )}

          {/* Calificación General */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>Calificación General</span>
                <Badge className={`${getRatingColor(report.rating)} text-white text-lg px-4 py-1`}>
                  {getRatingLabel(report.rating)}
                </Badge>
              </CardTitle>
            </CardHeader>
          </Card>

          {/* Información General */}
          <Card>
            <CardHeader>
              <CardTitle>Información General</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">Escenario</p>
                  <p className="font-medium">{report.scenarioType}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Valor K</p>
                  <p className="font-medium">{report.k}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Ventana de Tiempo (Sc)</p>
                  <p className="font-medium">{report.scMinutes} minutos</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Total Iteraciones</p>
                  <p className="font-medium">{report.totalIterations}</p>
                </div>
                <div className="col-span-2">
                  <p className="text-sm text-muted-foreground">Período de Simulación</p>
                  <p className="font-medium">
                    {report.startTime} → {report.endTime}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Métricas de Pedidos */}
          <Card>
            <CardHeader>
              <CardTitle>Estado de Pedidos</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Total de pedidos:</span>
                  <span className="font-bold">{report.totalOrders}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm flex items-center gap-2">
                    <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                    Completamente entregados:
                  </span>
                  <span className="font-medium">
                    {report.fullyCompleted} ({report.completionRate.toFixed(1)}%)
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm flex items-center gap-2">
                    <span className="w-2 h-2 bg-yellow-500 rounded-full"></span>
                    Parcialmente entregados:
                  </span>
                  <span className="font-medium">{report.partiallyCompleted}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm flex items-center gap-2">
                    <span className="w-2 h-2 bg-red-500 rounded-full"></span>
                    No completados:
                  </span>
                  <span className="font-medium">{report.notCompleted}</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Métricas de Productos */}
          <Card>
            <CardHeader>
              <CardTitle>Asignación de Productos</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Productos solicitados:</span>
                  <span className="font-bold">{report.totalProductsRequested.toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Productos asignados:</span>
                  <span className="font-bold">{report.totalProductsAssigned.toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Tasa de asignación:</span>
                  <span className="font-bold text-blue-600">
                    {report.productAssignmentRate.toFixed(1)}%
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Métricas de Entregas a Tiempo */}
          <Card>
            <CardHeader>
              <CardTitle>Puntualidad de Entregas (Timezone-Aware)</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Pedidos entregados:</span>
                  <span className="font-bold">{report.deliveredOrders}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm flex items-center gap-2">
                    <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                    A tiempo:
                  </span>
                  <span className="font-medium">
                    {report.onTimeDeliveries} ({report.onTimeRate.toFixed(1)}%)
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm flex items-center gap-2">
                    <span className="w-2 h-2 bg-red-500 rounded-full"></span>
                    Atrasados:
                  </span>
                  <span className="font-medium">
                    {report.lateDeliveries} ({(100 - report.onTimeRate).toFixed(1)}%)
                  </span>
                </div>
                {report.lateDeliveries > 0 && (
                  <>
                    <div className="border-t pt-3 mt-3">
                      <p className="text-sm font-semibold mb-2">Estadísticas de Atrasos:</p>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Atraso promedio:</span>
                      <span className="font-medium">{report.avgDelayHours.toFixed(1)} horas</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Atraso máximo:</span>
                      <span className="font-medium">{report.maxDelayHours} horas</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Total horas de atraso:</span>
                      <span className="font-medium">{report.totalDelayHours} horas</span>
                    </div>
                  </>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Métricas de Shipments */}
          <Card>
            <CardHeader>
              <CardTitle>Envíos Generados</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex justify-between items-center">
                <span className="text-sm">Total de shipments:</span>
                <span className="font-bold">{report.totalShipments}</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </DialogContent>
    </Dialog>
  );
}
