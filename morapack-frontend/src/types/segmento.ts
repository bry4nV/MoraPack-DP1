// Equivalente a Segment.java (un tramo de una ruta)
export type EstadoSegmento =
  | 'PROGRAMADO'
  | 'EN_VUELO'
  | 'ARRIBADO'
  | 'DEMORADO'
  | 'CANCELADO';

export interface Segmento {
  id: number;
  vueloId: number;               // Flight.id
  salidaProgramada: string;      // ISO datetime
  llegadaProgramada: string;     // ISO datetime
  salidaReal?: string;
  llegadaReal?: string;
  estado?: EstadoSegmento;
}
