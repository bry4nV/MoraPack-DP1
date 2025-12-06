"use client";

import { useEffect, useRef } from "react";
import maplibregl, { Marker } from "maplibre-gl"; // Quitamos 'Map' de aquí para evitar conflictos
import "maplibre-gl/dist/maplibre-gl.css";
import type { Itinerario } from "@/types/simulation/itinerary.types";
import type { Aeropuerto } from "@/types";
import { interpolateGreatCircle } from "@/lib/geo";

// Función auxiliar para calcular rotación
function bearingDegrees([lon1, lat1]: [number, number], [lon2, lat2]: [number, number]): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const toDeg = (r: number) => (r * 180) / Math.PI;
  const φ1 = toRad(lat1), φ2 = toRad(lat2);
  const λ1 = toRad(lon1), λ2 = toRad(lon2);
  const y = Math.sin(λ2 - λ1) * Math.cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(λ2 - λ1);
  return (toDeg(Math.atan2(y, x)) + 360) % 360;
}

type Props = {
  itinerarios: Itinerario[];
  aeropuertos?: Aeropuerto[];
  center?: [number, number];
  zoom?: number;
  className?: string;
};

export default function DailyFlights({
  itinerarios,
  aeropuertos = [],
  center = [-60, -15],
  zoom = 2.8,
  className = "",
}: Props) {
  const divRef = useRef<HTMLDivElement | null>(null);
  // 🔥 CORRECCIÓN 1: Usamos maplibregl.Map explícitamente
  const mapRef = useRef<maplibregl.Map | null>(null);
  
  const planeRefs = useRef<Marker[]>([]);
  const planeIconEls = useRef<HTMLElement[]>([]);
  const airportRefs = useRef<Marker[]>([]);
  const reqRef = useRef<number | null>(null);

  // Inicializar Mapa
  useEffect(() => {
    if (!divRef.current || mapRef.current) return;
    
    const map = new maplibregl.Map({
      container: divRef.current,
      style: "https://demotiles.maplibre.org/style.json",
      center,
      zoom,
      attributionControl: { compact: true },
    });
    
    map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), "bottom-left");
    mapRef.current = map;

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
    };
  }, []); // Solo al montar

  // Lógica de Dibujado y Animación
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return; // Si es null, no hacemos nada

    // 🔥 CORRECCIÓN 2: Lógica inline en lugar de función externa para evitar error de tipos
    const startDrawing = () => {
        // 1. Limpiar todo lo anterior
        planeRefs.current.forEach((p) => p.remove());
        airportRefs.current.forEach((p) => p.remove());
        planeRefs.current = [];
        planeIconEls.current = [];
        airportRefs.current = [];

        const style = map.getStyle();
        if (style?.layers) {
            style.layers.filter((l) => l.id.startsWith("rt-line-")).forEach((l) => map.removeLayer(l.id));
        }
        if (style?.sources) {
            Object.keys(style.sources).filter((id) => id.startsWith("rt-line-")).forEach((id) => map.removeSource(id));
        }

        // 2. DIBUJAR AEROPUERTOS
        aeropuertos.forEach((a) => {
            const lng = Number(a.longitude);
            const lat = Number(a.latitude);
            if (isNaN(lng) || isNaN(lat)) return;

            // Info de capacidad
            const capacidadTotal = a.capacidadTotal ?? a.capacity ?? 1000;
            const capacidadUsada = a.capacidadUsada ?? 0;
            const porcentajeUso = a.porcentajeUso ?? 0;
            
            const getCapacityColor = (pct: number) => {
                if (pct >= 90) return '#ef4444'; 
                if (pct >= 70) return '#f59e0b';
                if (pct >= 50) return '#eab308';
                return '#10b981';
            };
            
            const capacityHtml = `
                <div style="margin:6px 0; border-top:1px solid #eee; padding-top:4px;">
                    <div style="font-size:10px; color:#555; display:flex; justify-content:space-between;">
                        <span>Capacidad:</span> <strong>${capacidadUsada}/${capacidadTotal}</strong>
                    </div>
                    <div style="width:100%; background:#f3f4f6; border-radius:2px; height:6px; margin-top:2px; overflow:hidden;">
                        <div style="width:${Math.min(100, porcentajeUso)}%; background:${getCapacityColor(porcentajeUso)}; height:100%;"></div>
                    </div>
                </div>
            `;

            // Elemento PIN
            const el = document.createElement("div");
            el.className = "airport-pin";
            const pinColor = a.isHub ? "#10b981" : "#3b82f6";

            el.innerHTML = `
            <svg width="30" height="40" viewBox="0 0 30 40" fill="none" xmlns="http://www.w3.org/2000/svg" style="display:block; filter: drop-shadow(0 2px 3px rgba(0,0,0,0.3));">
                <path d="M15 0C6.7 0 0 6.7 0 15C0 26.2 15 40 15 40C15 40 30 26.2 30 15C30 6.7 23.3 0 15 0Z" fill="${pinColor}"/>
                <circle cx="15" cy="15" r="6" fill="white"/>
            </svg>
            `;
            el.style.width = "30px";
            el.style.height = "40px";
            el.style.cursor = "pointer";

            const popupHTML = `
                <div style="text-align:center; min-width:120px;">
                    <div style="font-weight:bold; font-size:14px;">${a.code}</div>
                    <div style="font-size:11px; color:#666;">${a.city}</div>
                    ${a.isHub ? '<span style="background:#10b981; color:white; font-size:9px; padding:1px 4px; border-radius:3px;">HUB</span>' : ''}
                    ${capacityHtml}
                </div>
            `;

            const mk = new maplibregl.Marker({ element: el, anchor: 'bottom' })
            .setLngLat([lng, lat])
            .setPopup(new maplibregl.Popup({ offset: 35, closeButton: false }).setHTML(popupHTML))
            .addTo(map);
            
            airportRefs.current.push(mk);
        });

        // 3. DIBUJAR LÍNEAS Y AVIONES
        itinerarios.forEach((it, i) => {
            if (!it.segmentos || it.segmentos.length === 0) return;
            const firstSeg = it.segmentos[0].vuelo;
            const oLng = Number(firstSeg.origen.longitude);
            const oLat = Number(firstSeg.origen.latitude);
            const dLng = Number(firstSeg.destino.longitude);
            const dLat = Number(firstSeg.destino.latitude);

            // Línea
            const lineId = `rt-line-${it.id}`;
            if (!map.getSource(lineId)) {
                map.addSource(lineId, {
                    type: "geojson",
                    data: {
                        type: "Feature",
                        geometry: { type: "LineString", coordinates: [[oLng, oLat], [dLng, dLat]] },
                        properties: {}
                    }
                });
                map.addLayer({
                    id: lineId, type: "line", source: lineId,
                    layout: { "line-cap": "round", "line-join": "round" },
                    paint: { "line-width": 2, "line-color": "#f59e0b", "line-dasharray": [2, 3], "line-opacity": 0.7 }
                });
            }

            // Avión
            const container = document.createElement("div");
            container.style.zIndex = "20";
            const icon = document.createElement("div");
            icon.innerHTML = `<svg width="24" height="24" viewBox="0 0 24 24" fill="#000000" stroke="white" stroke-width="1"><path d="M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9L2 14v2l8-2.5V19l-2 1.5V22l3-1 3 1v-1.5L13 19v-5.5l8 2.5z"/></svg>`;
            icon.style.position = "absolute";
            icon.style.transform = "translate(-50%, -50%)";
            container.appendChild(icon);

            const mk = new maplibregl.Marker({ element: container })
            .setLngLat([oLng, oLat])
            .addTo(map);

            planeRefs.current.push(mk);
            planeIconEls.current.push(icon);
        });

        // 4. INICIAR ANIMACIÓN
        if (reqRef.current) cancelAnimationFrame(reqRef.current);
        reqRef.current = requestAnimationFrame(animate);
    };

    const animate = () => {
        const now = Date.now();
        itinerarios.forEach((it, idx) => {
            const marker = planeRefs.current[idx];
            const icon = planeIconEls.current[idx];
            const vuelo = it.segmentos?.[0]?.vuelo;
            
            if (!marker || !icon || !vuelo?.salidaProgramadaISO) return;

            const tStart = new Date(vuelo.salidaProgramadaISO).getTime();
            const tEnd = new Date(vuelo.llegadaProgramadaISO).getTime();
            const dur = tEnd - tStart;

            const o = [Number(vuelo.origen.longitude), Number(vuelo.origen.latitude)];
            const d = [Number(vuelo.destino.longitude), Number(vuelo.destino.latitude)];

            if (now < tStart) {
                // @ts-ignore
                marker.setLngLat(o);
            } else if (now > tEnd) {
                // @ts-ignore
                marker.setLngLat(d);
            } else {
                const progress = (now - tStart) / dur;
                // @ts-ignore
                const pos = interpolateGreatCircle(o, d, progress);
                // @ts-ignore
                const rot = bearingDegrees(o, d);
                marker.setLngLat(pos);
                icon.style.transform = `translate(-50%, -50%) rotate(${rot}deg)`;
            }
        });
        reqRef.current = requestAnimationFrame(animate);
    };

    // Ejecutar lógica cuando el estilo esté listo
    if (map.isStyleLoaded()) {
        startDrawing();
    } else {
        map.once("load", startDrawing);
    }

    return () => { if (reqRef.current) cancelAnimationFrame(reqRef.current); };
  }, [itinerarios, aeropuertos]);

  return <div ref={divRef} className={`w-full h-full ${className}`} />;
}