// src/data/simulacion.ts
import { AEROPUERTOS } from "@/data/aeropuertos";
import type { Aeropuerto } from "@/types";
import type { Itinerario } from "@/types/itinerario";

/** Utils */
const pick = <T,>(arr: T[]) => arr[Math.floor(Math.random() * arr.length)];
const shuffle = <T,>(arr: T[]) => [...arr].sort(() => Math.random() - 0.5);

function sedes(aeropuertos: Aeropuerto[]) {
  const s = aeropuertos.filter((a) => (a as any).esSede);
  return s.length ? s : aeropuertos;
}

/** Semanal: 1–3 tramos por itinerario (sin loop) */
export function buildItinerariosSemana(count = 12): Itinerario[] {
  const aer = AEROPUERTOS;
  const bases = sedes(aer);

  return Array.from({ length: count }).map((_, i) => {
    const legs = 1 + Math.floor(Math.random() * 3); // 1..3
    const origenBase = pick(bases);
    const destinos = shuffle(aer.filter((a) => a.id !== origenBase.id)).slice(0, legs);

    // Tipamos explícitamente como el tipo de segmentos que pide Itinerario
    const segmentos: Itinerario["segmentos"] = destinos.map((dest, idx) => {
      const origen = idx === 0 ? origenBase : destinos[idx - 1];
      return {
        id: `SEG-${i + 1}-${idx + 1}`,
        orden: idx + 1, // 👈 requerido por SegmentoPlan
        vuelo: {
          id: `V-${i + 1}-${idx + 1}`,
          codigo: `MT${100 + i}${idx}`,
          origen,
          destino: dest,
          // Si tu tipo de Vuelo exige más campos obligatorios, añádelos aquí.
        } as any,
      };
    });

    return {
      id: `IT-${i + 1}`,
      segmentos,
    };
  });
}

/** Colapso: mismo generador (loopeará en la UI con loop=true) */
export function buildItinerariosColapso(count = 16): Itinerario[] {
  return buildItinerariosSemana(count);
}
