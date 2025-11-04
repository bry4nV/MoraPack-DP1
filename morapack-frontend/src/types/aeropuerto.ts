import type { Pais } from './pais';
export interface Aeropuerto {
  id: number;
  nombre: string;           // Airport.name
  codigo: string;           // Airport.code (IATA)
  ciudad: string;           // Airport.city
  pais: Pais;               // Airport.country
  capacidadAlmacen: number; // Airport.storageCapacity
  gmt: number;              // Airport.gmt
  latitud: number;          // Airport.latitude
  longitud: number;         // Airport.longitude
  esSede?: boolean;         // (sólo UI)
  
  // ✅ Capacity information (real-time)
  capacidadTotal?: number;       // Total storage capacity
  capacidadUsada?: number;       // Current capacity used
  capacidadDisponible?: number;  // Available capacity
  porcentajeUso?: number;        // Usage percentage (0-100)
  
  // Dynamic runtime information (from simulation)
  pedidosEnEspera?: number;      // Orders waiting at this airport (origin)
  pedidosDestino?: number;       // Orders with this airport as destination
  productosEnEspera?: number;    // Total products waiting
  vuelosActivosDesde?: number;   // Active flights departing from here
  vuelosActivosHacia?: number;   // Active flights arriving here
  vuelosEnTierra?: string[];     // Flight IDs currently at this airport
}
