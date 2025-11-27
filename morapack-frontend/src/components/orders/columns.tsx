"use client";

import { Order } from "@/types/order";
import { Calendar, Clock } from "lucide-react";
import { Badge } from "@/components/ui/badge";

const statusColors: Record<string, string> = {
  UNASSIGNED: "bg-gray-100 text-gray-800 hover:bg-gray-100",
  PENDING: "bg-yellow-100 text-yellow-800 hover:bg-yellow-100",
  IN_TRANSIT: "bg-blue-100 text-blue-800 hover:bg-blue-100",
  COMPLETED: "bg-green-100 text-green-800 hover:bg-green-100",
};

const statusLabels: Record<string, string> = {
  UNASSIGNED: "Sin Asignar",
  PENDING: "Pendiente",
  IN_TRANSIT: "En Tránsito",
  COMPLETED: "Completado",
};

export interface Column<T> {
  id: string;
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => React.ReactNode;
  className?: string;
  headerClassName?: string;
}

export const orderColumns: Column<Order>[] = [
  {
    id: "orderNumber",
    header: "Nro. Pedido",
    accessor: "orderNumber",
    headerClassName: "pl-6",
    className: "pl-6",
    cell: (row) => <div className="font-mono text-sm font-medium">{row.orderNumber || "-"}</div>,
  },
  {
    id: "clientCode",
    header: "Cliente",
    accessor: "clientCode",
    cell: (row) => <div>{row.clientCode || "-"}</div>,
  },
  {
    id: "airportDestinationCode",
    header: "Destino",
    accessor: "airportDestinationCode",
    cell: (row) => (
      <div className="font-mono text-sm">{row.airportDestinationCode || "-"}</div>
    ),
  },
  {
    id: "quantity",
    header: "Paquetes",
    accessor: "quantity",
    headerClassName: "text-center",
    className: "text-center",
    cell: (row) => <div className="tabular-nums">{row.quantity || 0}</div>,
  },
  {
    id: "status",
    header: "Estado",
    accessor: "status",
    cell: (row) => {
      const status = String(row.status);
      return (
        <Badge className={statusColors[status] || statusColors.UNASSIGNED}>
          {statusLabels[status] || status}
        </Badge>
      );
    },
  },
  {
    id: "orderDate",
    header: "Fecha",
    accessor: "orderDate",
    cell: (row) => (
      <div className="flex items-center gap-1.5 text-sm">
        <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
        <span>{row.orderDate || "-"}</span>
      </div>
    ),
  },
  {
    id: "orderTime",
    header: "Hora",
    accessor: "orderTime",
    cell: (row) => (
      <div className="flex items-center gap-1.5 text-sm">
        <Clock className="h-3.5 w-3.5 text-muted-foreground" />
        <span className="font-mono">{row.orderTime || "-"}</span>
      </div>
    ),
  },
  {
    id: "actions",
    header: "Acciones",
    headerClassName: "text-right pr-6 w-24",
    className: "text-right pr-6 w-24",
    cell: () => null, // Será reemplazado dinámicamente
  },
];