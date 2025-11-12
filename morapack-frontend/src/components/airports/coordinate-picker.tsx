"use client";

import { useState, useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMapEvents } from "react-leaflet";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Map } from "lucide-react";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// Fix para los iconos de Leaflet en Next.js
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon-2x.png",
  iconUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-icon.png",
  shadowUrl: "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/images/marker-shadow.png",
});

interface CoordinatePickerProps {
  latitude: string;
  longitude: string;
  onCoordinatesChange: (lat: string, lng: string) => void;
}

function LocationMarker({ position, setPosition }: any) {
  useMapEvents({
    click(e) {
      setPosition(e.latlng);
    },
  });

  return position === null ? null : <Marker position={position} />;
}

export function CoordinatePicker({ latitude, longitude, onCoordinatesChange }: CoordinatePickerProps) {
  const [open, setOpen] = useState(false);
  const [mounted, setMounted] = useState(false);
  const [position, setPosition] = useState<L.LatLng | null>(null);

  useEffect(() => {
    setMounted(true);
    // Convertir coordenadas existentes a LatLng si hay valores
    if (latitude && longitude) {
      try {
        const lat = parseFloat(latitude);
        const lng = parseFloat(longitude);
        if (!isNaN(lat) && !isNaN(lng)) {
          setPosition(L.latLng(lat, lng));
        }
      } catch (e) {
        console.error("Error parsing coordinates:", e);
      }
    }
  }, [latitude, longitude]);

  const handleConfirm = () => {
    if (position) {
      // Convertir a formato DMS
      const latDMS = convertToDMS(position.lat, true);
      const lngDMS = convertToDMS(position.lng, false);
      onCoordinatesChange(latDMS, lngDMS);
      setOpen(false);
    }
  };

  const convertToDMS = (decimal: number, isLat: boolean) => {
    const absolute = Math.abs(decimal);
    const degrees = Math.floor(absolute);
    const minutesNotTruncated = (absolute - degrees) * 60;
    const minutes = Math.floor(minutesNotTruncated);
    const seconds = Math.floor((minutesNotTruncated - minutes) * 60);
    
    let direction;
    if (isLat) {
      direction = decimal >= 0 ? "N" : "S";
    } else {
      direction = decimal >= 0 ? "E" : "W";
    }

    return `${String(degrees).padStart(2, '0')}° ${String(minutes).padStart(2, '0')}' ${String(seconds).padStart(2, '0')}" ${direction}`;
  };

  if (!mounted) return null;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button type="button" variant="outline" size="sm" className="mt-2">
          <Map className="h-4 w-4 mr-2" />
          Seleccionar en mapa
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>Seleccionar coordenadas en el mapa</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Haz clic en el mapa para seleccionar las coordenadas del aeropuerto
          </p>
          <div className="h-[400px] rounded-md overflow-hidden border">
            <MapContainer
              center={position || [0, 0]}
              zoom={position ? 6 : 2}
              style={{ height: "100%", width: "100%" }}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <LocationMarker position={position} setPosition={setPosition} />
            </MapContainer>
          </div>
          {position && (
            <div className="grid grid-cols-2 gap-4 p-4 bg-muted rounded-md">
              <div>
                <Label className="text-xs">Latitud (Decimal)</Label>
                <p className="font-mono text-sm">{position.lat.toFixed(6)}</p>
              </div>
              <div>
                <Label className="text-xs">Longitud (Decimal)</Label>
                <p className="font-mono text-sm">{position.lng.toFixed(6)}</p>
              </div>
              <div>
                <Label className="text-xs">Latitud (DMS)</Label>
                <p className="font-mono text-sm">{convertToDMS(position.lat, true)}</p>
              </div>
              <div>
                <Label className="text-xs">Longitud (DMS)</Label>
                <p className="font-mono text-sm">{convertToDMS(position.lng, false)}</p>
              </div>
            </div>
          )}
          <div className="flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              Cancelar
            </Button>
            <Button type="button" onClick={handleConfirm} disabled={!position}>
              Confirmar coordenadas
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
