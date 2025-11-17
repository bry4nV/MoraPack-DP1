"use client";

import { useEffect, useRef } from "react";
import maplibregl, { Map, Marker } from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

type Marcador = {
  id: string | number;
  lng: number;
  lat: number;
  html?: string;
  color?: string;
};

type Props = {
  center?: [number, number];  // [lng, lat]
  zoom?: number;
  className?: string;
  marcadores?: Marcador[];
};

export default function MapLibre({
  center = [-60, -15],
  zoom = 3,
  className = "",
  marcadores = [],
}: Props) {
  const contRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<Map | null>(null);
  const markerRefs = useRef<Record<string | number, Marker>>({});

  // 1) Crear el mapa UNA SOLA VEZ (montaje)
  useEffect(() => {
    if (!contRef.current || mapRef.current) return;

    const map = new maplibregl.Map({
      container: contRef.current,
      style: "https://demotiles.maplibre.org/style.json",
      center,
      zoom,
      attributionControl: false,
    });

    map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), "top-right");
    map.addControl(new maplibregl.ScaleControl({ unit: "metric" }), "bottom-right");

    mapRef.current = map;

    return () => {
      // Cleanup SEGURO (dev StrictMode puede llamar 2 veces)
      try {
        mapRef.current?.remove?.();
      } catch {
        // ya estaba destruido; ignorar
      } finally {
        mapRef.current = null;
        markerRefs.current = {};
      }
    };
    // deps vacías: solo una vez
  }, []);

  // 2) Pintar/actualizar marcadores cuando cambien
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    // borrar marcadores previos
    Object.values(markerRefs.current).forEach((m) => m.remove());
    markerRefs.current = {};

    // agregar nuevos
    marcadores.forEach((m) => {
      const mk = new maplibregl.Marker({ color: m.color ?? "#2563eb" })
        .setLngLat([m.lng, m.lat])
        .setPopup(new maplibregl.Popup({ offset: 12 }).setHTML(m.html ?? `<b>${m.id}</b>`))
        .addTo(map);
      markerRefs.current[m.id] = mk;
    });

    // encuadre automático
    if (marcadores.length > 1) {
      const bounds = new maplibregl.LngLatBounds();
      marcadores.forEach((m) => bounds.extend([m.lng, m.lat]));
      if (!bounds.isEmpty()) map.fitBounds(bounds, { padding: 40, maxZoom: 8 });
    } else if (marcadores.length === 1) {
      map.setCenter([marcadores[0].lng, marcadores[0].lat]);
      map.setZoom(8);
    }
  }, [JSON.stringify(marcadores)]);

  return (
    <div ref={contRef} className={`w-full h-full ${className}`} style={{
      // Ocultar elementos residuales de MapLibre (logo, atribución vacía)
      '--maplibre-ctrl-attrib-bg': 'transparent',
    } as React.CSSProperties}>
      <style jsx>{`
        :global(.maplibregl-ctrl-logo),
        :global(.maplibregl-ctrl-attrib),
        :global(.maplibregl-compact) {
          display: none !important;
        }
      `}</style>
    </div>
  );
}
