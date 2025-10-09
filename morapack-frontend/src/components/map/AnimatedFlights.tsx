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
  const y = Math.sin(λ2 - λ1) * Math.cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(λ2 - λ1);
  return (toDeg(Math.atan2(y, x)) + 360) % 360; // 0..360 (0 = norte)
}

type Props = {
  itinerarios: Itinerario[];
  aeropuertos?: Aeropuerto[];
  speedKmh?: number;
  center?: [number, number];
  zoom?: number;
  className?: string;
};

export default function AnimatedFlights({
  itinerarios,
  aeropuertos = [],
  speedKmh: speedKmhProp = 200,
  center = [-60, -15],
  zoom = 2.8,
  className = "",
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

  // 👇 nuevo: estado de “vuelo terminado” por itinerario
  const finishedRef = useRef<boolean[]>([]);

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
      finishedRef.current = new Array(itinerarios.length).fill(false); // 👈 inicializamos finalizados

      const style = map.getStyle?.();
      const layerIds = style?.layers?.map((l) => l.id) ?? [];
      layerIds.filter((id) => id.startsWith("line-it-")).forEach((id) => map.getLayer(id) && map.removeLayer(id));
      const sourceIds = Object.keys(style?.sources ?? {});
      sourceIds.filter((sid) => sid.startsWith("line-it-")).forEach((sid) => map.getSource(sid) && map.removeSource(sid));

      // líneas punteadas
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
        const mk = new maplibregl.Marker({ color: a.esSede ? "#10b981" : "#2563eb" })
          .setLngLat([a.longitud, a.latitud])
          .setPopup(
            new maplibregl.Popup({ offset: 12 }).setHTML(
              `<div style="font-weight:600">${a.nombre} (${a.codigo})</div>
               <div style="font-size:12px">${a.ciudad}, ${a.pais.nombre} — ${a.pais.continente}</div>
               <div style="font-size:12px">GMT: ${a.gmt}</div>
               <div style="font-size:12px">Cap. almacén: ${a.capacidadAlmacen}</div>
               ${a.esSede ? '<div style="font-size:12px;font-weight:600">SEDE / ORIGEN</div>' : ""}`
            )
          ).addTo(map);
        airportRefs.current.push(mk);
      });

      // aviones (contenedor + hijo)
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

      // animación (sin loop; se queda en destino)
      elapsedSecRef.current = 0;
      lastTsRef.current = null;

      const step = (now: number) => {
        if (!mapRef.current) return;

        if (lastTsRef.current == null) lastTsRef.current = now;
        const dt = (now - lastTsRef.current) / 1000;
        lastTsRef.current = now;

        if (playing) elapsedSecRef.current += dt;

        const speedMps = (speedKmh * 1000000) / 3600; // ✅ velocidad correcta

        itinerarios.forEach((it, idx) => {
          // si ya terminó, mantenerlo en destino final
          if (finishedRef.current[idx]) {
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

          const total = totalLengths[idx] || 1;
          const dist = Math.min(elapsedSecRef.current * speedMps, total); // 👈 sin módulo

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

          if (dist >= total) {
            // llegó al final: fijar y marcar como terminado
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

        // si todos terminaron, detener animación
        const allDone = finishedRef.current.length > 0 && finishedRef.current.every(Boolean);
        if (allDone) {
          reqRef.current = null;
          return;
        }

        reqRef.current = requestAnimationFrame(step);
      };

      if (reqRef.current) cancelAnimationFrame(reqRef.current);
      reqRef.current = requestAnimationFrame(step);
    });

    return () => { if (reqRef.current) cancelAnimationFrame(reqRef.current); };
  }, [JSON.stringify(itinerarios), JSON.stringify(aeropuertos), segLengths, totalLengths, playing, speedKmh]);

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
        <label className="text-sm">
          Velocidad: <span className="font-medium">{speedKmh} km/h</span>
        </label>
        <input
          type="range"
          min={50}
          max={1000}
          step={10}
          value={speedKmh}
          onChange={(e) => setSpeedKmh(parseInt(e.target.value))}
        />
      </div>
    </div>
  );
}
