import type { Segmento } from './segmento';

// Equivalente a Route.java (secuencia de segmentos)
export interface Ruta {
  id: number;
  orderId: number;        // Order.id (si en el back Route pertenece a un Order)
  segmentos: Segmento[];  // secuencia LIM->...->destino
}
