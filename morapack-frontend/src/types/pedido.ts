import type { Aeropuerto } from './aeropuerto';

export interface Pedido {
  id: number;                   // Order.id
  cantidadTotal: number;        // Order.totalQuantity
  origen: Aeropuerto;           // Order.origin
  destino: Aeropuerto;          // Order.destination
  maxHorasEntrega: number;      // Order.maxDeliveryHours
  fechaRegistroISO: string;     // Order.orderTime -> ISO string
}
