import MapLibre from "@/components/map/MapLibre";
import { Card, CardContent } from "@/components/ui/card";

export const metadata = { title: "Mapa | MoraTravel" };

export default function MapaPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-4xl font-bold tracking-tight">Operaciones de día a día</h1>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          {/* Alto pensado para vista con sidebar, ajusta a tu gusto */}
          <div className="h-[calc(100dvh-12rem)]">
            <MapLibre
              // centro aproximado (América del Sur). Cambia según necesites
              center={[-60, -15]}
              zoom={3}
              className="rounded-none"
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
