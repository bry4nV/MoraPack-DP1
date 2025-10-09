// app/page.tsx
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Plane } from "lucide-react";

export default function HomePage() {
  return (
    <>
      <h1 className="text-4xl font-bold mb-8 flex items-center gap-2">
        <Plane className="size-10" />
        Bienvenido a MoraTravel
      </h1>

      <Card className="max-w-5xl">
        <CardContent className="p-8">
          <h2 className="text-2xl font-semibold mb-4">Rastrea tu pedido</h2>
          <div className="flex flex-col gap-4 md:flex-row">
            <Input placeholder="Ingrese el código de su pedido" className="md:flex-1" />
            <div className="flex gap-3">
              <Button>Buscar</Button>
              <Button variant="outline">Realizar pedido</Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </>
  );
}
