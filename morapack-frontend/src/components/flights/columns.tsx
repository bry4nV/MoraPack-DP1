"use client";

import { Flight, FlightStatus } from "@/types/flight"; // Asegúrate que la ruta sea correcta
import { Badge } from "@/components/ui/badge";

// Mapas de colores y etiquetas para los estados de VUELO
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

export interface Column<T> {
  id: string;
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => React.ReactNode;
}

export const flightColumns: Column<Flight>[] = [
  {
    id: "id",
    header: "ID",
    accessor: "id",
    cell: (row) => <div className="font-mono text-xs font-medium">{row.id}</div>,
  },
  {
    id: "flightDate",
    header: "Fecha",
    accessor: "flightDate",
    cell: (row) => <div>{row.flightDate}</div>,
  },
  {
    id: "airportOriginCode",
    header: "Origen",
    accessor: "airportOriginCode",
    cell: (row) => <div className="font-mono text-xs">{row.airportOriginCode}</div>,
  },
  {
    id: "airportDestinationCode",
    header: "Destino",
    accessor: "airportDestinationCode",
    cell: (row) => <div className="font-mono text-xs">{row.airportDestinationCode}</div>,
  },
  {
    id: "departureTime",
    header: "Salida",
    accessor: "departureTime",
    cell: (row) => <div className="font-mono text-xs">{row.departureTime}</div>,
  },
  {
    id: "arrivalTime",
    header: "Llegada",
    accessor: "arrivalTime",
    cell: (row) => <div className="font-mono text-xs">{row.arrivalTime}</div>,
  },
  {
    id: "capacity",
    header: "Capacidad",
    accessor: "capacity",
    cell: (row) => <div className="text-center">{row.capacity}</div>,
  },
  {
    id: "status",
    header: "Estado",
    accessor: "status",
    cell: (row) => {
      const statusKey = row.status as FlightStatus;
      const label = statusLabels[statusKey] || row.status;
      const color = statusColors[statusKey] || "bg-gray-100 text-gray-800";
      return <Badge className={color}>{label}</Badge>;
    },
  },
  // (Puedes añadir una columna de "Acciones" aquí si la necesitas)
];