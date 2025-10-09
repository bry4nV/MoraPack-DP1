import { AEROPUERTOS } from "@/data/aeropuertos";
import type { Itinerario } from "@/types/itinerario";
import type { Vuelo } from "@/types";

const byCode = (c:string)=>{
  const ap=AEROPUERTOS.find(a=>a.codigo===c);
  if(!ap) throw new Error(`Aeropuerto ${c} no encontrado`);
  return ap;
};

const mkVuelo = (codigo:string, origen:string, destino:string, horas:number):Vuelo => {
  const o=byCode(origen), d=byCode(destino);
  const salida=new Date(), llegada=new Date(salida.getTime()+horas*3600*1000);
  return {
    codigo, origen:o, destino:d,
    salidaProgramadaISO: salida.toISOString(),
    llegadaProgramadaISO: llegada.toISOString(),
    capacidad: 280, preplanificado: true, estado: "SCHEDULED",
  };
};

export const ITINERARIOS_DUMMY: Itinerario[] = [
  { id:"it-001", segmentos:[
    { orden:1, vuelo: mkVuelo("PK1001","LIM","GYD",18) },
    { orden:2, vuelo: mkVuelo("PK2001","GYD","JFK",12) },
  ]},
  { id:"it-002", segmentos:[
    { orden:1, vuelo: mkVuelo("PK3001","BRU","LAX",12) },
  ]},
];
