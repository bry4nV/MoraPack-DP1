import type { Vuelo } from "@/types";

export interface SegmentoPlan { orden: number; vuelo: Vuelo; }
export interface Itinerario { id: string; segmentos: SegmentoPlan[]; }
