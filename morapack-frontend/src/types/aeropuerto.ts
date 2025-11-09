// (Tu import de Pais ya no es necesario para este objeto)
// import type { Pais } from './pais';

export interface Aeropuerto {
  // Campos que SÍ vienen del backend
  id: number;
  continent: string;
  code: string;
  city: string;
  country: string;
  cityAcronym: string;
  gmt: number;
  capacity: number;
  latitude: string;
  longitude: string;
  status: string;
  isHub: boolean; // <-- ¡EL CAMPO CLAVE!

  // (Campos de la UI que ya tenías)
  capacidadTotal?: number;
  capacidadUsada?: number;
  capacidadDisponible?: number;
  porcentajeUso?: number;
  pedidosEnEspera?: number;
  pedidosDestino?: number;
  productosEnEspera?: number;
  vuelosActivosDesde?: number;
  vuelosActivosHacia?: number;
  vuelosEnTierra?: string[];
}