import MapLibre from "@/components/map/MapLibre";
import { Card, CardContent } from "@/components/ui/card";
import { AEROPUERTOS } from "@/data/aeropuertos";

export const metadata = { title: "Mapa | MoraTravel" };

export default function MapaPage() {
  // Construimos los marcadores a partir de la data dummy
  const marcadores = AEROPUERTOS.map(a => ({
    id: a.id,
    lng: a.longitud,
    lat: a.latitud,
    html: `
      <div style="font-weight:600">${a.nombre} (${a.codigo})</div>
      <div style="font-size:12px">${a.ciudad}, ${a.pais.nombre} — ${a.pais.continente}</div>
      <div style="font-size:12px">GMT: ${a.gmt}</div>
      <div style="font-size:12px">Cap. almacén: ${a.capacidadAlmacen}</div>
    `
  }));

  return (
    <div className="space-y-6">
      <h1 className="text-4xl font-bold tracking-tight">Operaciones de día a día</h1>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          {/* Alto pensado para vista con sidebar, ajusta a tu gusto */}
          <div className="h-[calc(100dvh-12rem)]">
            <MapLibre
              // El center/zoom iniciales solo se usan antes del fitBounds
              center={[-60, -15]}
              zoom={3}
              className="rounded-none"
              marcadores={marcadores}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
