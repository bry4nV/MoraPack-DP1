/**
 * Types for dynamic events during simulation.
 * Includes flight cancellations and dynamic order injections.
 */

// ═══════════════════════════════════════════════════════════════
// FLIGHT CANCELLATIONS
// ═══════════════════════════════════════════════════════════════

/**
 * Detalles de una tarea de replanificación.
 * Incluye información sobre pedidos afectados, nuevos shipments, y métricas.
 */
export interface ReplanificationDetails {
  id: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  cancellationId: string;
  cancelledFlightId: string;
  triggeredTime: string | null;
  startedTime: string | null;
  completedTime: string | null;
  executionTimeMs: number;
  affectedOrderIds: number[];             // IDs de pedidos afectados
  totalAffectedProducts: number;          // Total de productos afectados
  cancelledShipmentsCount: number;        // Envíos cancelados
  newShipmentsCount: number;              // Nuevos envíos creados
  successful: boolean;
  errorMessage: string | null;
  reassignmentRate: number;               // % de productos reasignados
  summary: string;                        // Resumen descriptivo

  // 🆕 Tracking detallado de productos por pedido
  productsToReassign?: Record<number, number>;    // Productos esperados a reasignar por pedido
  productsReassigned?: Record<number, number>;    // Productos efectivamente reasignados por pedido
  productsPending?: Record<number, number>;       // Productos pendientes por pedido
  totalProductsPending?: number;                  // Total de productos pendientes
}

/**
 * Estado de un pedido afectado por replanificación
 */
export interface AffectedOrderStatus {
  orderId: number;
  expected: number;           // Productos esperados a reasignar
  reassigned: number;         // Productos efectivamente reasignados
  pending: number;            // Productos pendientes
  status: 'completed' | 'partial' | 'pending';  // Estado del pedido
}

export interface FlightCancellation {
  id: string;
  type?: string;                          // "SCHEDULED" | "MANUAL"
  status: 'PENDING' | 'EXECUTED' | 'FAILED' | 'CANCELLED';
  flightOrigin: string;
  flightDestination: string;
  scheduledDepartureTime: string;         // HH:mm format (e.g., "03:34")
  flightIdentifier?: string;              // "ORIGIN-DEST-HH:mm"
  cancellationTime: string | null;        // Full ISO datetime when cancellation happens
  executedTime?: string | null;           // Full ISO datetime when cancellation was executed
  reason: string;
  affectedProductsCount?: number;         // Number of products affected (only when EXECUTED)
  replanificationTriggered?: boolean;     // Whether replanification was triggered
  errorMessage?: string | null;           // Error message if failed
  replanificationDetails?: ReplanificationDetails;  // 🆕 Detalles completos de replanificación
}

export interface CancelFlightRequest {
  flightOrigin: string;
  flightDestination: string;
  scheduledDepartureTime: string;
  reason?: string;
}

export interface CancellationApiResponse {
  success: boolean;
  message: string;
  cancellation?: FlightCancellation;
}

export interface CancellationsListResponse {
  success: boolean;
  count: number;
  cancellations: FlightCancellation[];
}

// ═══════════════════════════════════════════════════════════════
// DYNAMIC ORDERS
// ═══════════════════════════════════════════════════════════════

export interface DynamicOrder {
  id: string;
  origin: string;
  destination: string;
  quantity: number;
  deadlineHours: number;
  injectionTime: string | null;
  reason: string;
  status: 'PENDING' | 'INJECTED';
}

export interface DynamicOrderRequest {
  destination: string;
  quantity: number;
  reason?: string;
  // origin y deadlineHours se determinan automáticamente en el backend
}

export interface DynamicOrderApiResponse {
  success: boolean;
  message: string;
  order?: DynamicOrder;
}

export interface DynamicOrdersListResponse {
  success: boolean;
  count: number;
  orders: DynamicOrder[];
}
