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

export interface CreateOrderDto {
  orderNumber: string;
  airportDestinationCode: string;
  quantity: number;
  clientCode: string;
}