// src/types/pais.ts
import type { Continente } from './continente';

/** Equivalente a pe.edu.pucp.morapack.model.Country */
export interface Pais {
  id: number;
  nombre: string;         // Country.name
  continente: Continente; // Country.continent
}
