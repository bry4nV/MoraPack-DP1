// src/types/pedido-back.ts
export type PedidoBack = {
  id: number;
  packageCount: number;
  airportDestinationId: string;
  priority: number | null; // si luego dejan de mandar null, cámbialo a number
  clientId: string;
  status: string;          // "PENDIENTE", etc.
  day: number;
  hour: number;
  minute: number;
};
