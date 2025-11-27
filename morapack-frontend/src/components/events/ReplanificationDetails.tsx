"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { ChevronDown, ChevronUp, Package, FileText, Zap, TrendingUp } from "lucide-react";
import type { ReplanificationDetails } from "@/types/simulation/events.types";

interface ReplanificationDetailsProps {
  details: ReplanificationDetails;
  affectedProductsCount?: number;
}

export default function ReplanificationDetailsComponent({
  details,
  affectedProductsCount
}: ReplanificationDetailsProps) {
  const [showDetails, setShowDetails] = useState(false);

  return (
    <div className="space-y-1">
      <button
        onClick={() => setShowDetails(!showDetails)}
        className="flex items-center gap-2 text-blue-700 bg-blue-50 p-1.5 rounded mt-1 w-full hover:bg-blue-100 transition-colors"
      >
        <FileText className="h-3 w-3" />
        <span className="font-medium flex-1 text-left text-xs">
          {affectedProductsCount && affectedProductsCount > 0
            ? `✓ ${affectedProductsCount} productos replanificados`
            : `✓ Replanificación completada`}
        </span>
        {showDetails ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
      </button>

      {/* Detalles expandibles de replanificación */}
      {showDetails && (
        <div className="bg-gray-50 rounded p-2 space-y-2 text-xs border border-gray-200">
          <div className="font-semibold text-gray-700 flex items-center gap-1">
            <TrendingUp className="h-3 w-3" />
            Detalles de Replanificación
          </div>

          {/* Estadísticas en grid */}
          <div className="grid grid-cols-2 gap-2">
            <div className="flex items-center gap-1.5 bg-white p-1.5 rounded border">
              <Package className="h-3 w-3 text-orange-600" />
              <div>
                <div className="text-[10px] text-gray-500">Pedidos afectados</div>
                <div className="font-semibold text-gray-900">{details.affectedOrderIds?.length || 0}</div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 bg-white p-1.5 rounded border">
              <FileText className="h-3 w-3 text-blue-600" />
              <div>
                <div className="text-[10px] text-gray-500">Nuevos envíos</div>
                <div className="font-semibold text-gray-900">{details.newShipmentsCount}</div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 bg-white p-1.5 rounded border">
              <Zap className="h-3 w-3 text-purple-600" />
              <div>
                <div className="text-[10px] text-gray-500">Tiempo ejecución</div>
                <div className="font-semibold text-gray-900">{details.executionTimeMs}ms</div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 bg-white p-1.5 rounded border">
              <TrendingUp className="h-3 w-3 text-green-600" />
              <div>
                <div className="text-[10px] text-gray-500">Tasa reasignación</div>
                <div className="font-semibold text-gray-900">{details.reassignmentRate?.toFixed(1)}%</div>
              </div>
            </div>
          </div>

          {/* IDs de pedidos afectados (primeros 10) */}
          {details.affectedOrderIds && details.affectedOrderIds.length > 0 && (
            <div className="bg-white p-1.5 rounded border">
              <div className="text-[10px] text-gray-500 mb-1">Pedidos afectados:</div>
              <div className="flex flex-wrap gap-1">
                {details.affectedOrderIds.slice(0, 10).map(id => (
                  <Badge key={id} variant="outline" className="text-[9px] px-1 py-0">
                    #{id}
                  </Badge>
                ))}
                {details.affectedOrderIds.length > 10 && (
                  <Badge variant="outline" className="text-[9px] px-1 py-0 bg-gray-100">
                    +{details.affectedOrderIds.length - 10} más
                  </Badge>
                )}
              </div>
            </div>
          )}

          {/* Estado y resumen */}
          <div className="space-y-1">
            <div className="text-[10px] text-gray-500">
              Estado: <span className="font-medium text-gray-700">{details.status}</span>
              {details.successful && <span className="text-green-600 ml-1">✓ Exitosa</span>}
            </div>
            {details.summary && (
              <div className="text-[10px] text-gray-600 bg-white p-1.5 rounded border italic">
                {details.summary}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
