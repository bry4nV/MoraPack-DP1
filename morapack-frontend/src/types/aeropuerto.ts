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
}
