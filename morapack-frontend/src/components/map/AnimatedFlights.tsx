// src/components/map/AnimatedFlights.tsx
"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import maplibregl, { Map, Marker } from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import type { Itinerario } from "@/types/itinerario";
import type { Aeropuerto } from "@/types";
import { haversineDistanceMeters, interpolateGreatCircle } from "@/lib/geo";

function bearingDegrees(
  [lon1, lat1]: [number, number],
  [lon2, lat2]: [number, number]
): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const toDeg = (r: number) => (r * 180) / Math.PI;
  const φ1 = toRad(lat1), φ2 = toRad(lat2);
  const λ1 = toRad(lon1), λ2 = toRad(lon2);
  const y = Math.sin(λ2 - λ1) * cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(λ2 - λ1);
  return (toDeg(Math.atan2(y, x)) + 360) % 360; // 0..360 (0 = norte)
}
const cos = Math.cos;

type Props = {
  itinerarios: Itinerario[];
  aeropuertos?: Aeropuerto[];
  speedKmh?: number;
  /** ISO timestamp from backend - if provided, planes move based on simulated time instead of visual speed */
  simulatedTime?: string;
  center?: [number, number];
  zoom?: number;
  className?: string;
  /** Si true, los vuelos hacen loop infinito (modo "Colapso"); si false, se detienen en destino (modo "Semanal"). */
  loop?: boolean;
};

export default function AnimatedFlights({
  itinerarios,
  aeropuertos = [],
  speedKmh: speedKmhProp = 200,
  simulatedTime,
  center = [-60, -15],
  zoom = 2.8,
  className = "",
  loop = false, // ← por defecto SIN loop (mantiene comportamiento actual)
}: Props) {
  const divRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<Map | null>(null);
  const planeRefs = useRef<Marker[]>([]);
  const planeIconEls = useRef<HTMLElement[]>([]);  // hijo rotado
  const airportRefs = useRef<Marker[]>([]);
  const reqRef = useRef<number | null>(null);

  const [playing, setPlaying] = useState(true);
  const [speedKmh, setSpeedKmh] = useState(speedKmhProp);

  const elapsedSecRef = useRef(0);
  const lastTsRef = useRef<number | null>(null);

  // estado de "vuelo terminado" por itinerario (sólo usado cuando loop=false)
  const finishedRef = useRef<boolean[]>([]);

  // ✅ Sync speedKmh when prop changes (from simulation Speed control)
  useEffect(() => {
    setSpeedKmh(speedKmhProp);
  }, [speedKmhProp]);

  // ✅ Reset finished state when itineraries change (new simulation)
  useEffect(() => {
    finishedRef.current = new Array(itinerarios.length).fill(false);
  }, [itinerarios.length]);

  const segLengths = useMemo(
    () =>
      itinerarios.map((it) =>
        it.segmentos.map((seg) =>
          haversineDistanceMeters(
            [seg.vuelo.origen.longitud, seg.vuelo.origen.latitud],
            [seg.vuelo.destino.longitud, seg.vuelo.destino.latitud]
          )
        )
      ),
    [JSON.stringify(itinerarios)]
  );
  const totalLengths = useMemo(
    () => segLengths.map((arr) => arr.reduce((a, b) => a + b, 0)),
    [segLengths]
  );

  useEffect(() => {
    if (!divRef.current || mapRef.current) return;
    const map = new maplibregl.Map({
      container: divRef.current,
      style: "https://demotiles.maplibre.org/style.json",
      center,
      zoom,
      attributionControl: { compact: true },
    });
    map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), "top-right");
    mapRef.current = map;

    return () => {
      try { mapRef.current?.remove?.(); } catch {}
      mapRef.current = null;
      planeRefs.current.forEach((m) => m.remove());
      airportRefs.current.forEach((m) => m.remove());
      planeRefs.current = [];
      planeIconEls.current = [];
      airportRefs.current = [];
      if (reqRef.current) cancelAnimationFrame(reqRef.current);
    };
  }, []);

  function whenStyleReady(map: Map, cb: () => void) {
    if (map.isStyleLoaded()) cb();
    else {
      const onLoad = () => { cb(); map.off("load", onLoad); };
      map.on("load", onLoad);
    }
  }

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    whenStyleReady(map, () => {
      // limpiar
      planeRefs.current.forEach((p) => p.remove());
      airportRefs.current.forEach((p) => p.remove());
      planeRefs.current = [];
      planeIconEls.current = [];
      airportRefs.current = [];
      finishedRef.current = new Array(itinerarios.length).fill(false); // reinicio flags

      const style = map.getStyle?.();
      const layerIds = style?.layers?.map((l) => l.id) ?? [];
      layerIds.filter((id) => id.startsWith("line-it-")).forEach((id) => map.getLayer(id) && map.removeLayer(id));
      const sourceIds = Object.keys(style?.sources ?? {});
      sourceIds.filter((sid) => sid.startsWith("line-it-")).forEach((sid) => map.getSource(sid) && map.removeSource(sid));

      // líneas punteadas (gris)
      itinerarios.forEach((it, i) => {
        it.segmentos.forEach((seg, s) => {
          const o = seg.vuelo.origen, d = seg.vuelo.destino;
          const id = `line-it-${i}-${s}`;
          if (!map.getSource(id)) {
            map.addSource(id, {
              type: "geojson",
              data: {
                type: "Feature",
                geometry: { type: "LineString", coordinates: [[o.longitud, o.latitud], [d.longitud, d.latitud]] },
                properties: {},
              },
            });
          }
          if (!map.getLayer(id)) {
            map.addLayer({
              id,
              type: "line",
              source: id,
              layout: { "line-cap": "round", "line-join": "round" },
              paint: {
                "line-width": 2,
                "line-color": "#9ca3af",
                "line-opacity": 0.65,
                "line-dasharray": [2, 2],
              },
            });
          }
        });
      });

      // aeropuertos
      aeropuertos.forEach((a) => {
        // ✅ Build capacity info section
        const capacidadTotal = a.capacidadTotal ?? a.capacidadAlmacen ?? 1000;
        const capacidadUsada = a.capacidadUsada ?? 0;
        const porcentajeUso = a.porcentajeUso ?? 0;
        
        // Determine capacity bar color
        const getCapacityColor = (percentage: number) => {
          if (percentage >= 90) return '#ef4444'; // Red
          if (percentage >= 70) return '#f59e0b'; // Orange
          if (percentage >= 50) return '#eab308'; // Yellow
          return '#10b981'; // Green
        };
        
        const capacityHtml = `
          <hr style="margin:6px 0; border:none; border-top:1px solid #ddd"/>
          <div style="font-size:11px; color:#333; margin-bottom:4px">
            <strong>📦 Capacidad:</strong> ${capacidadUsada}/${capacidadTotal} productos
          </div>
          <div style="width:100%; background:#e5e7eb; border-radius:4px; height:10px; overflow:hidden">
            <div style="width:${Math.min(100, porcentajeUso)}%; background:${getCapacityColor(porcentajeUso)}; height:100%; transition:width 0.3s"></div>
          </div>
          <div style="font-size:10px; color:#666; margin-top:2px">
            ${porcentajeUso.toFixed(1)}% usado${porcentajeUso >= 90 ? ' ⚠️ CASI LLENO' : ''}
          </div>
        `;
        
        // ✅ Build dynamic info section if available
        const hasRuntimeInfo = (a.pedidosEnEspera ?? 0) > 0 || (a.pedidosDestino ?? 0) > 0 || 
                               (a.vuelosActivosDesde ?? 0) > 0 || (a.vuelosActivosHacia ?? 0) > 0;
        
        const runtimeInfoHtml = hasRuntimeInfo ? `
          <hr style="margin:6px 0; border:none; border-top:1px solid #ddd"/>
          <div style="font-size:11px; color:#555">
            ${(a.pedidosEnEspera ?? 0) > 0 ? `📦 Pedidos en espera: <strong>${a.pedidosEnEspera}</strong><br/>` : ''}
            ${(a.productosEnEspera ?? 0) > 0 ? `📊 Productos en espera: <strong>${a.productosEnEspera}</strong><br/>` : ''}
            ${(a.pedidosDestino ?? 0) > 0 ? `🎯 Pedidos destino: <strong>${a.pedidosDestino}</strong><br/>` : ''}
            ${(a.vuelosActivosDesde ?? 0) > 0 ? `✈️ Vuelos saliendo: <strong>${a.vuelosActivosDesde}</strong><br/>` : ''}
            ${(a.vuelosActivosHacia ?? 0) > 0 ? `🛬 Vuelos llegando: <strong>${a.vuelosActivosHacia}</strong><br/>` : ''}
            ${(a.vuelosEnTierra && a.vuelosEnTierra.length > 0) ? `🛫 En tierra: <strong>${a.vuelosEnTierra.length}</strong>` : ''}
          </div>
        ` : '';
        
        const mk = new maplibregl.Marker({ color: a.esSede ? "#10b981" : "#2563eb" })
          .setLngLat([a.longitud, a.latitud])
          .setPopup(
            new maplibregl.Popup({ offset: 12 }).setHTML(
              `<div style="font-weight:600">${a.nombre} (${a.codigo})</div>
               <div style="font-size:12px">${a.ciudad}, ${a.pais.nombre} — ${a.pais.continente}</div>
               <div style="font-size:12px">GMT: ${a.gmt}</div>
               ${a.esSede ? '<div style="font-size:12px;font-weight:600">SEDE / ORIGEN</div>' : ""}
               ${capacityHtml}
               ${runtimeInfoHtml}`
            )
          ).addTo(map);
        airportRefs.current.push(mk);
      });

      // aviones (contenedor + hijo rotado)
      itinerarios.forEach((it) => {
        const firstSeg = it.segmentos[0];
        const A = firstSeg.vuelo.origen;
        const B = firstSeg.vuelo.destino;
        const initBearing = bearingDegrees([A.longitud, A.latitud], [B.longitud, B.latitud]);

        const container = document.createElement("div");
        container.style.pointerEvents = "none";
        container.style.width = "0";
        container.style.height = "0";

        const icon = document.createElement("div");
        icon.innerHTML = `
          <svg width="22" height="22" viewBox="0 0 24 24" fill="#000000" xmlns="http://www.w3.org/2000/svg" style="display:block">
            <path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9L2 14v2l8-2.5V19l-2 1.5V22l3-1 3 1v-1.5L13 19v-5.5l8 2.5z"/>
          </svg>
        `;
        icon.style.position = "relative";
        icon.style.left = "0";
        icon.style.top = "0";
        icon.style.transform = `translate(-50%, -50%) rotate(${initBearing}deg)`;
        icon.style.userSelect = "none";

        container.appendChild(icon);

        const mk = new maplibregl.Marker({ element: container, anchor: "center" })
          .setLngLat([A.longitud, A.latitud])
          .addTo(map);

        planeRefs.current.push(mk);
        planeIconEls.current.push(icon as HTMLElement);
      });

      // fit bounds
      const b = new maplibregl.LngLatBounds();
      aeropuertos.forEach((a) => b.extend([a.longitud, a.latitud]));
      itinerarios.forEach((it) =>
        it.segmentos.forEach((s) => {
          b.extend([s.vuelo.origen.longitud, s.vuelo.origen.latitud]);
          b.extend([s.vuelo.destino.longitud, s.vuelo.destino.latitud]);
        })
      );
      if (!b.isEmpty()) map.fitBounds(b, { padding: 40, maxZoom: 5.5 });

      // animación
      elapsedSecRef.current = 0;
      lastTsRef.current = null;

      const step = (now: number) => {
        if (!mapRef.current) return;

        if (lastTsRef.current == null) lastTsRef.current = now;
        const dt = (now - lastTsRef.current) / 1000;
        lastTsRef.current = now;

        if (playing) elapsedSecRef.current += dt;

        const speedMps = (speedKmh * 1000000) / 3600;

        itinerarios.forEach((it, idx) => {
          const total = totalLengths[idx] || 1;

          // Si ya terminó y NO es loop, mantenerlo en destino final
          if (!loop && finishedRef.current[idx]) {
            const lastSeg = it.segmentos[it.segmentos.length - 1];
            const dest = lastSeg.vuelo.destino;
            const finalBearing = bearingDegrees(
              [lastSeg.vuelo.origen.longitud, lastSeg.vuelo.origen.latitud],
              [dest.longitud, dest.latitud]
            );
            const icon = planeIconEls.current[idx];
            if (icon) icon.style.transform = `translate(-50%, -50%) rotate(${finalBearing}deg)`;
            planeRefs.current[idx].setLngLat([dest.longitud, dest.latitud]);
            return;
          }

          // ✅ SINCRONIZACIÓN POR VUELO (no por ruta completa)
          let dist: number;
          
          // Calcular en qué segmento (vuelo) debería estar el avión según tiempo simulado
          let targetDistByFlights: number | null = null;
          let allFlightsCompleted = false;
          
          // 🚀 MODO PRESENTACIÓN: Forzar movimiento visual ignorando horarios
          const FORCE_VISUAL_MOVEMENT = true; // Cambia a false para modo normal
          
          if (simulatedTime && it.segmentos.length > 0 && !FORCE_VISUAL_MOVEMENT) {
            const currentTime = new Date(simulatedTime).getTime();
            let accumulatedDist = 0;
            let foundCurrentFlight = false;
            
            // 🔍 DEBUG (solo para el primer itinerario)
            if (idx === 0 && Math.random() < 0.01) { // Log 1% del tiempo
              console.log(`[AnimatedFlights] Itinerario 0:`, {
                simulatedTime,
                currentTimeDate: new Date(currentTime).toISOString(),
                totalSegments: it.segmentos.length,
                firstFlightDep: it.segmentos[0]?.vuelo.salidaProgramadaISO,
                firstFlightArr: it.segmentos[0]?.vuelo.llegadaProgramadaISO,
              });
            }
            
            for (let s = 0; s < it.segmentos.length; s++) {
              const flight = it.segmentos[s].vuelo;
              const depTime = new Date(flight.salidaProgramadaISO).getTime();
              const arrTime = new Date(flight.llegadaProgramadaISO).getTime();
              const flightDuration = arrTime - depTime;
              const segmentLength = segLengths[idx][s];
              
              // Caso 1: Vuelo aún no ha despegado → avión debe estar en origen
              if (currentTime < depTime) {
                targetDistByFlights = accumulatedDist;
                foundCurrentFlight = true;
                
                // 🔍 DEBUG
                if (idx === 0 && s === 0 && Math.random() < 0.01) {
                  console.log(`[AnimatedFlights] Vuelo no despegado aún`);
                }
                break;
              }
              
              // Caso 2: Vuelo en progreso → interpolar dentro de este segmento
              if (currentTime >= depTime && currentTime < arrTime) {
                const elapsed = currentTime - depTime;
                const progress = flightDuration > 0 ? elapsed / flightDuration : 1;
                targetDistByFlights = accumulatedDist + (progress * segmentLength);
                foundCurrentFlight = true;
                
                // 🔍 DEBUG
                if (idx === 0 && Math.random() < 0.01) {
                  console.log(`[AnimatedFlights] Vuelo ${s} en progreso:`, {
                    progress: (progress * 100).toFixed(1) + '%',
                    elapsed: (elapsed / 1000 / 60).toFixed(0) + ' min',
                    targetDist: targetDistByFlights.toFixed(0),
                  });
                }
                break;
              }
              
              // Caso 3: Vuelo completado → acumular y continuar al siguiente
              accumulatedDist += segmentLength;
            }
            
            // Si todos los vuelos completaron → avión en destino final
            if (!foundCurrentFlight) {
              targetDistByFlights = total;
              allFlightsCompleted = true;
            }
          }
          
          // Caso 1: Todos los vuelos completados → FORZAR a destino
          if (allFlightsCompleted) {
            dist = total;
            if (!loop) {
              finishedRef.current[idx] = true;
            }
          }
          // Caso 2: Hay sincronización por vuelos → combinar con animación visual
          else if (targetDistByFlights !== null) {
            // Usar velocidad visual para movimiento suave
            const traveled = elapsedSecRef.current * speedMps;
            const visualDist = loop ? (traveled % total) : Math.min(traveled, total);
            
            // 🔍 DEBUG
            if (idx === 0 && Math.random() < 0.005) {
              console.log(`[AnimatedFlights] Movimiento:`, {
                elapsedSec: elapsedSecRef.current.toFixed(1),
                speedMps: speedMps.toFixed(0),
                traveled: traveled.toFixed(0),
                visualDist: visualDist.toFixed(0),
                targetDist: targetDistByFlights.toFixed(0),
                total: total.toFixed(0),
              });
            }
            
            // Aplicar corrección según desviación
            const deviation = Math.abs(visualDist - targetDistByFlights);
            if (deviation > total * 0.15) {
              // Desviación grande: corrección agresiva
              dist = visualDist * 0.5 + targetDistByFlights * 0.5;
            } else if (deviation > total * 0.05) {
              // Desviación media: corrección suave
              dist = visualDist * 0.8 + targetDistByFlights * 0.2;
            } else {
              // Bien sincronizado: solo visual
              dist = visualDist;
            }
          }
          // Caso 3: Sin tiempo simulado → velocidad visual pura
          else {
            const traveled = elapsedSecRef.current * speedMps;
            dist = loop ? (traveled % total) : Math.min(traveled, total);
          }

          let rem = dist;
          let pos: [number, number] = [
            it.segmentos[0].vuelo.origen.longitud,
            it.segmentos[0].vuelo.origen.latitud,
          ];
          let rumboDeg = 0;

          for (let s = 0; s < it.segmentos.length; s++) {
            const o: [number, number] = [it.segmentos[s].vuelo.origen.longitud, it.segmentos[s].vuelo.origen.latitud];
            const d: [number, number] = [it.segmentos[s].vuelo.destino.longitud, it.segmentos[s].vuelo.destino.latitud];
            const L = segLengths[idx][s];

            if (rem <= L) {
              const t = Math.min(1, Math.max(0, rem / L));
              pos = interpolateGreatCircle(o, d, t);
              rumboDeg = bearingDegrees(o, d);
              break;
            }
            rem -= L;
          }

          // Si no hay loop y ya llegó al final, fijar y marcar como terminado
          if (!loop && dist >= total) {
            finishedRef.current[idx] = true;
            const lastSeg = it.segmentos[it.segmentos.length - 1];
            const dest = lastSeg.vuelo.destino;
            const finalBearing = bearingDegrees(
              [lastSeg.vuelo.origen.longitud, lastSeg.vuelo.origen.latitud],
              [dest.longitud, dest.latitud]
            );
            const icon = planeIconEls.current[idx];
            if (icon) icon.style.transform = `translate(-50%, -50%) rotate(${finalBearing}deg)`;
            planeRefs.current[idx].setLngLat([dest.longitud, dest.latitud]);
            return;
          }

          // movimiento normal
          const icon = planeIconEls.current[idx];
          if (icon) icon.style.transform = `translate(-50%, -50%) rotate(${rumboDeg}deg)`;
          planeRefs.current[idx].setLngLat(pos);
        });

        // Si es semanal y todos terminaron, paramos el loop para ahorrar CPU
        if (!loop) {
          const allDone = finishedRef.current.length > 0 && finishedRef.current.every(Boolean);
          if (allDone) { reqRef.current = null; return; }
        }

        reqRef.current = requestAnimationFrame(step);
      };

      if (reqRef.current) cancelAnimationFrame(reqRef.current);
      reqRef.current = requestAnimationFrame(step);
    });

    return () => { if (reqRef.current) cancelAnimationFrame(reqRef.current); };
  }, [JSON.stringify(itinerarios), JSON.stringify(aeropuertos), segLengths, totalLengths, playing, speedKmh, loop]);

  return (
    <div className={`w-full h-full relative ${className}`}>
      <div ref={divRef} className="w-full h-full" />
      <div className="absolute left-4 bottom-4 z-10 flex items-center gap-3 rounded-xl bg-white/90 p-3 shadow">
        <button
          className="px-3 py-1 rounded bg-gray-900 text-white text-sm"
          onClick={() => setPlaying((p) => !p)}
        >
          {playing ? "Pausa" : "Play"}
        </button>
      </div>
    </div>
  );
}
