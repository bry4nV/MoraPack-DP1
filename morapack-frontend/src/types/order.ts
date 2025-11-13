import { OrderStatus } from './shared';

// ═══════════════════════════════════════════════════════════════
// ORDER (CRUD)
// ═══════════════════════════════════════════════════════════════

/**
 * Main Order interface for CRUD operations in /pedidos.
 * Matches backend OrderDto.java
 */
export interface Order {
  id: number;
  orderNumber: string;
  orderDate: string;
  orderTime: string;
  airportDestinationCode: string;
  quantity: number;
  clientCode: string;
  status: OrderStatus | string;
}

/**
 * @deprecated Use OrderStatus from shared.ts instead
 * Kept for backward compatibility
 */
export const OrderState = {
  UNASSIGNED: 'UNASSIGNED' as OrderStatus,
  PENDING: 'PENDING' as OrderStatus,
  IN_TRANSIT: 'IN_TRANSIT' as OrderStatus,
  COMPLETED: 'COMPLETED' as OrderStatus,
} as const;

// ═══════════════════════════════════════════════════════════════
// API PAYLOADS
// ═══════════════════════════════════════════════════════════════

// --- Payloads para API (Actualizados) ---
// (Estos definen qué datos se necesitan para CREAR o ACTUALIZAR un pedido)

export interface CreateOrderPayload {
  orderNumber: string;
  orderDate: string; // (ej: "YYYY-MM-DD")
  orderTime: string; // (ej: "HH:MM:SS")
  airportDestinationCode: string;
  quantity: number;
  clientCode: string;
  status: OrderStatus;
}

export type UpdateOrderPayload = Partial<CreateOrderPayload>;

export interface BulkCreateOrderPayload {
  orders: CreateOrderPayload[]; // <-- ¡CORREGIDO!
}

export interface BulkDeleteOrderPayload {
  orders: number[]; // array of order IDs (ahora son numéricos)
}