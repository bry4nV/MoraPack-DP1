/**
 * Esta es la interfaz principal para un Pedido (Order).
 * Coincide con lo que envía el OrderDto.java del backend.
 */
export interface Order {
  id: number;
  orderNumber: string;
  orderDate: string; // El backend lo envía como String (ej: "2025-11-06")
  orderTime: string; // El backend lo envía como String (ej: "14:30:00")
  airportDestinationCode: string;
  quantity: number;
  clientCode: string;
  status: OrderState | string; // Acepta el enum o un string por si acaso
}

/**
 * Estados posibles de un Pedido.
 * Sincronizado con la nueva base de datos.
 */
export enum OrderState {
  UNASSIGNED = 'UNASSIGNED',
  PENDING = 'PENDING',
  IN_TRANSIT = 'IN_TRANSIT',
  COMPLETED = 'COMPLETED',
}

// --- Payloads para API (Actualizados) ---
// (Estos definen qué datos se necesitan para CREAR o ACTUALIZAR un pedido)

export interface CreateOrderPayload {
  orderNumber: string;
  orderDate: string; // (ej: "YYYY-MM-DD")
  orderTime: string; // (ej: "HH:MM:SS")
  airportDestinationCode: string;
  quantity: number;
  clientCode: string;
  status: OrderState;
}

export type UpdateOrderPayload = Partial<CreateOrderPayload>;

export interface BulkCreateOrderPayload {
  orders: CreateOrderPayload[]; // <-- ¡CORREGIDO!
}

export interface BulkDeleteOrderPayload {
  orders: number[]; // array of order IDs (ahora son numéricos)
}