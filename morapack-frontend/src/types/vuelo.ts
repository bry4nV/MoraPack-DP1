import type { Aeropuerto } from './aeropuerto';

export type EstadoVuelo = 'SCHEDULED' | 'DELAYED' | 'CANCELLED' | 'COMPLETED';

export interface Vuelo {
  codigo: string;               // Flight.code
  origen: Aeropuerto;           // Flight.origin
  destino: Aeropuerto;          // Flight.destination
  salidaProgramadaISO: string;  // Flight.departureTime -> ISO string
  llegadaProgramadaISO: string; // Flight.arrivalTime   -> ISO string
  capacidad: number;            // Flight.capacity
  preplanificado: boolean;      // Flight.preplanned
  estado: EstadoVuelo;          // Flight.status
}
