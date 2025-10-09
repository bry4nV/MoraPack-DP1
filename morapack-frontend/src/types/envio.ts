import type { Aeropuerto } from './aeropuerto';
import type { Pedido } from './pedido';

export interface Envio {
  id: number;             // Shipment.id
  pedido: Pedido;         // Shipment.parentOrder
  cantidad: number;       // Shipment.quantity
  origen: Aeropuerto;     // Shipment.origin
  destino: Aeropuerto;    // Shipment.destination
}
