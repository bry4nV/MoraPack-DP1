"use client";

import { Flight, FlightStatus } from "@/types/flight";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ColumnDef } from "@tanstack/react-table"; // <-- ¡CAMBIO IMPORTANTE!

// Mapas de colores y etiquetas (estos están bien)
const statusColors: Record<string, string> = {
  [FlightStatus.SCHEDULED]: "bg-blue-100 text-blue-800 hover:bg-blue-100",
  [FlightStatus.DELAYED]: "bg-yellow-100 text-yellow-800 hover:bg-yellow-100",
  [FlightStatus.COMPLETED]: "bg-green-100 text-green-800 hover:bg-green-100",
  [FlightStatus.CANCELLED]: "bg-red-100 text-red-800 hover:bg-red-100",
};

const statusLabels: Record<string, string> = {
  [FlightStatus.SCHEDULED]: "Programado",
  [FlightStatus.DELAYED]: "Retrasado",
  [FlightStatus.COMPLETED]: "Completado",
  [FlightStatus.CANCELLED]: "Cancelado",
};

// Ya no necesitamos la interfaz 'Column<T>' personalizada

// ¡CAMBIO IMPORTANTE! Usamos ColumnDef y el formato de tanstack-table
export const flightColumns: ColumnDef<Flight>[] = [
  {
    accessorKey: "id", // <-- 'accessorKey' en lugar de 'id' y 'accessor'
    header: "ID",     // <-- 'header'
  },
  {
    accessorKey: "flightDate",
    header: "Fecha",
  },
  {
    accessorKey: "airportOriginCode",
    header: "Origen",
  },
  {
    accessorKey: "airportDestinationCode",
    header: "Destino",
  },
  {
    accessorKey: "departureTime",
    header: "Salida",
  },
  {
    accessorKey: "arrivalTime",
    header: "Llegada",
  },
  {
    accessorKey: "capacity",
    header: "Capacidad",
  },
  {
    accessorKey: "status", // <-- 'accessorKey'
    header: "Estado",
    cell: ({ row }) => { // <-- formato de 'cell'
      const statusKey = row.original.status as FlightStatus; // 'row.original'
      const label = statusLabels[statusKey] || row.status;
      const color = statusColors[statusKey] || "bg-gray-100 text-gray-800";
      return <Badge className={color}>{label}</Badge>;
    },
  },
  // --- ¡COLUMNA DE ACCIONES AÑADIDA! ---
  {
    id: "actions",
    header: "Acciones",
    // Esta celda es un 'placeholder'. 
    // La lógica real del botón la inyecta tu 'page.tsx' (como te envié antes).
    cell: () => <div className="text-center">...</div>,
  },
];