export interface Order {
  id: number;
  packageCount: number;
  airportDestinationId: string;
  priority: number | null; // si luego dejan de mandar null, cámbialo a number
  clientId: string;
  status: OrderState;
  day: number;
  hour: number;
  minute: number;
}

export enum OrderState {
  PENDIENTE = 'PENDIENTE',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  ANULLED = 'ANULLED',
}

// Tipos para los payloads de creación y actualización
export interface CreateOrderPayload {
  packageCount: number;
  airportDestinationId: string;
  priority: number | null;
  clientId: string;
  status: OrderState;
  day: number;
  hour: number;
  minute: number;
}

export type UpdateOrderPayload = Partial<CreateOrderPayload>;

//carga masiva;BulkCreateCommunityPayload
export interface BulkCreateOrderPayload {
  orders: CreateOrderPayload[];
}

export interface BulkDeleteOrderPayload {
  orders: string[]; // array of order IDs
}
