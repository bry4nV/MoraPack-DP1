"use client";

import { Aeropuerto } from "@/types/aeropuerto"; // Asegúrate que la ruta sea correcta
import { Button } from "@/components/ui/button";
import { MoreHorizontal, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";

export interface Column<T> {
  id: string;
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => React.ReactNode;
}

export const airportColumns: Column<Aeropuerto>[] = [
  {
    id: "code", // <-- Usa el nuevo campo 'code'
    header: "Código",
    accessor: "code",
    cell: (row) => <div className="font-medium">{row.code}</div>,
  },
  {
    id: "city",
    header: "Ciudad",
    accessor: "city",
    cell: (row) => <div>{row.city}</div>,
  },
  {
    id: "country",
    header: "País",
    accessor: "country",
    cell: (row) => <div>{row.country}</div>,
  },
  {
    id: "continent",
    header: "Continente",
    accessor: "continent",
    cell: (row) => (
      <Badge variant="outline" className="text-xs">
        {row.continent}
      </Badge>
    ),
  },
  {
    id: "capacity",
    header: "Capacidad",
    accessor: "capacity",
    cell: (row) => <div className="text-center">{row.capacity?.toLocaleString()}</div>,
  },
  {
    id: "gmt",
    header: "GMT",
    accessor: "gmt",
    cell: (row) => <div className="font-mono text-xs">{row.gmt}</div>,
  },
  {
    id: "isHub", // <-- Usa el nuevo campo 'isHub'
    header: "Sede",
    accessor: "isHub",
    cell: (row) =>
      // Esta lógica ahora funcionará porque row.isHub será 'true'
      row.isHub ? (
        <Badge className="bg-green-100 text-green-800 hover:bg-green-100">
          Si
        </Badge>
      ) : (
        <span className="text-muted-foreground text-sm">No</span>
      ),
  },
  {
    id: "actions",
    header: "Acciones",
    cell: (row) => (
      <div className="flex items-center gap-2">
        {/* ... (Tus botones de acciones) ... */}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => console.log("Eliminar aeropuerto:", row.id)}
          className="text-red-600 hover:text-red-700 hover:bg-red-50"
          title="Eliminar aeropuerto"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    ),
  },
];