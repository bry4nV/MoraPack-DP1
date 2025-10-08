"use client";

import { useEffect, useRef } from "react";
import maplibregl, { Map } from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

type Props = {
  center?: [number, number];   // [lng, lat]
  zoom?: number;
  className?: string;
};

export default function MapLibre({ center = [-74.0, -9.2], zoom = 3.5, className = "" }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<Map | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    // Estilo público de demo (puedes cambiarlo por tu propio style.json)
    const styleUrl = "https://demotiles.maplibre.org/style.json";

    const map = new maplibregl.Map({
      container: containerRef.current,
      style: styleUrl,
      center,
      zoom,
      attributionControl: true,
    });

    map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), "top-right");
    map.addControl(new maplibregl.ScaleControl({ unit: "metric" }), "bottom-right");

    mapRef.current = map;

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
    };
  }, [center, zoom]);

  return <div ref={containerRef} className={`w-full h-full ${className}`} />;
}
