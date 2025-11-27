'use client';

import React, { useMemo } from 'react';
import { Package, CheckCircle2, AlertCircle, Clock } from 'lucide-react';
import type { ReplanificationDetails, AffectedOrderStatus } from '@/types/simulation/events.types';

interface AffectedOrdersTrackingProps {
  replanificationDetails: ReplanificationDetails;
}

/**
 * Componente para trackear en tiempo real el estado de pedidos afectados por replanificación.
 * Muestra cuántos productos de cada pedido se reasignaron y cuántos están pendientes.
 */
export function AffectedOrdersTracking({ replanificationDetails }: AffectedOrdersTrackingProps) {
  // 🔍 DEBUG: Log data para verificar qué llega del backend
  React.useEffect(() => {
    console.log('🔍 [AffectedOrdersTracking] Datos recibidos:', {
      affectedOrderIds: replanificationDetails.affectedOrderIds,
      productsToReassign: replanificationDetails.productsToReassign,
      productsReassigned: replanificationDetails.productsReassigned,
      productsPending: replanificationDetails.productsPending,
      totalProductsPending: replanificationDetails.totalProductsPending,
    });
  }, [replanificationDetails]);

  // Calcular estado de cada pedido afectado
  const affectedOrders: AffectedOrderStatus[] = useMemo(() => {
    const { affectedOrderIds, productsToReassign, productsReassigned, productsPending } =
      replanificationDetails;

    if (!affectedOrderIds || affectedOrderIds.length === 0) {
      console.warn('⚠️ [AffectedOrdersTracking] No hay affectedOrderIds');
      return [];
    }

    console.log(`📊 [AffectedOrdersTracking] Procesando ${affectedOrderIds.length} pedidos afectados`);

    return affectedOrderIds.map((orderId) => {
      const expected = productsToReassign?.[orderId] ?? 0;
      const reassigned = productsReassigned?.[orderId] ?? 0;
      const pending = productsPending?.[orderId] ?? 0;

      // Determinar estado
      let status: 'completed' | 'partial' | 'pending';
      if (reassigned === expected && expected > 0) {
        status = 'completed';
      } else if (reassigned > 0) {
        status = 'partial';
      } else {
        status = 'pending';
      }

      return {
        orderId,
        expected,
        reassigned,
        pending,
        status,
      };
    });
  }, [replanificationDetails]);

  // Calcular estadísticas globales
  const stats = useMemo(() => {
    const total = affectedOrders.length;
    const completed = affectedOrders.filter((o) => o.status === 'completed').length;
    const partial = affectedOrders.filter((o) => o.status === 'partial').length;
    const pending = affectedOrders.filter((o) => o.status === 'pending').length;

    return { total, completed, partial, pending };
  }, [affectedOrders]);

  if (affectedOrders.length === 0) {
    return null;
  }

  return (
    <div className="space-y-3">
      {/* Header con estadísticas */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Package className="h-4 w-4 text-gray-500" />
          <h4 className="text-sm font-medium text-gray-900">
            Pedidos Afectados ({stats.total})
          </h4>
        </div>

        {/* Badges de estado */}
        <div className="flex items-center gap-2 text-xs">
          {stats.completed > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-green-100 text-green-700">
              <CheckCircle2 className="h-3 w-3" />
              {stats.completed} completos
            </span>
          )}
          {stats.partial > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-700">
              <AlertCircle className="h-3 w-3" />
              {stats.partial} parciales
            </span>
          )}
          {stats.pending > 0 && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-gray-100 text-gray-700">
              <Clock className="h-3 w-3" />
              {stats.pending} pendientes
            </span>
          )}
        </div>
      </div>

      {/* Lista de pedidos */}
      <div className="space-y-2">
        {affectedOrders.map((order) => (
          <OrderStatusCard key={order.orderId} order={order} />
        ))}
      </div>

      {/* Footer con totales */}
      {replanificationDetails.totalProductsPending !== undefined &&
        replanificationDetails.totalProductsPending > 0 && (
          <div className="pt-2 border-t border-gray-200">
            <p className="text-xs text-amber-600">
              <AlertCircle className="inline h-3 w-3 mr-1" />
              {replanificationDetails.totalProductsPending} productos pendientes de reasignación
            </p>
            <p className="text-xs text-gray-500 mt-1">
              Los productos pendientes se intentarán reasignar en las siguientes iteraciones si hay rutas
              disponibles.
            </p>
          </div>
        )}
    </div>
  );
}

/**
 * Tarjeta individual mostrando el estado de un pedido afectado
 */
function OrderStatusCard({ order }: { order: AffectedOrderStatus }) {
  const percentage = order.expected > 0 ? (order.reassigned / order.expected) * 100 : 0;

  // Configuración de colores según estado
  const statusConfig = {
    completed: {
      icon: CheckCircle2,
      iconColor: 'text-green-500',
      bgColor: 'bg-green-50',
      borderColor: 'border-green-200',
      progressColor: 'bg-green-500',
    },
    partial: {
      icon: AlertCircle,
      iconColor: 'text-yellow-500',
      bgColor: 'bg-yellow-50',
      borderColor: 'border-yellow-200',
      progressColor: 'bg-yellow-500',
    },
    pending: {
      icon: Clock,
      iconColor: 'text-gray-400',
      bgColor: 'bg-gray-50',
      borderColor: 'border-gray-200',
      progressColor: 'bg-gray-300',
    },
  };

  const config = statusConfig[order.status];
  const Icon = config.icon;

  return (
    <div
      className={`p-3 rounded-lg border ${config.borderColor} ${config.bgColor} transition-all hover:shadow-sm`}
    >
      <div className="flex items-start justify-between gap-3">
        {/* Info del pedido */}
        <div className="flex items-start gap-2 flex-1 min-w-0">
          <Icon className={`h-4 w-4 mt-0.5 flex-shrink-0 ${config.iconColor}`} />
          <div className="flex-1 min-w-0">
            <div className="flex items-baseline gap-2">
              <span className="text-sm font-medium text-gray-900">Pedido #{order.orderId}</span>
              <span className="text-xs text-gray-500">
                {order.reassigned}/{order.expected} productos
              </span>
            </div>

            {/* Barra de progreso */}
            <div className="mt-2 w-full bg-gray-200 rounded-full h-1.5">
              <div
                className={`${config.progressColor} h-1.5 rounded-full transition-all duration-500`}
                style={{ width: `${percentage}%` }}
              />
            </div>

            {/* Estado detallado */}
            <div className="mt-1.5 flex items-center gap-3 text-xs">
              <span className="text-green-600">✓ {order.reassigned} reasignados</span>
              {order.pending > 0 && (
                <span className="text-amber-600">⏳ {order.pending} pendientes</span>
              )}
            </div>
          </div>
        </div>

        {/* Porcentaje */}
        <div className="text-right flex-shrink-0">
          <div className="text-sm font-semibold text-gray-900">{Math.round(percentage)}%</div>
        </div>
      </div>
    </div>
  );
}