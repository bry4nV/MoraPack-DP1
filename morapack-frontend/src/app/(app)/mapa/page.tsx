import { Card, CardContent } from "@/components/ui/card";
import AnimatedFlights from "@/components/map/AnimatedFlights";
import { ITINERARIOS_DUMMY } from "@/data/itinerarios";
import { AEROPUERTOS } from "@/data/aeropuertos";

export const metadata = { title: "Mapa | MoraTravel" };

export default function MapaPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-4xl font-bold tracking-tight">Operaciones de día a día</h1>

      <Card className="overflow-hidden">
        <CardContent className="p-0">
          <div className="h-[calc(100dvh-12rem)]">
            <AnimatedFlights
              itinerarios={ITINERARIOS_DUMMY}
              aeropuertos={AEROPUERTOS}   // ⬅️ ahora también mostramos los aeropuertos
              speedKmh={900}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
