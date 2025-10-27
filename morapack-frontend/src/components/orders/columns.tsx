"use client";

import { Order, OrderState } from "@/types/order";
import { Button } from "@/components/ui/button";
import { MoreHorizontal } from "lucide-react";
import { Badge } from "@/components/ui/badge";

const statusColors: Record<OrderState, string> = {
  [OrderState.PENDIENTE]: "bg-yellow-100 text-yellow-800 hover:bg-yellow-100",
  [OrderState.CONFIRMED]: "bg-green-100 text-green-800 hover:bg-green-100",
  [OrderState.CANCELLED]: "bg-red-100 text-red-800 hover:bg-red-100",
  [OrderState.ANULLED]: "bg-gray-100 text-gray-800 hover:bg-gray-100",
};

const statusLabels: Record<OrderState, string> = {
  [OrderState.PENDIENTE]: "Pendiente",
  [OrderState.CONFIRMED]: "Confirmado",
  [OrderState.CANCELLED]: "Cancelado",
  [OrderState.ANULLED]: "Anulado",
};

export interface Column<T> {
  id: string;
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => React.ReactNode;
}

export const orderColumns: Column<Order>[] = [
  {
    id: "id",
    header: "ID",
    accessor: "id",
    cell: (row) => <div className="font-medium">{row.id}</div>,
  },
  {
    id: "clientId",
    header: "Cliente",
    accessor: "clientId",
    cell: (row) => <div>{row.clientId}</div>,
  },
  {
    id: "airportDestinationId",
    header: "Destino",
    accessor: "airportDestinationId",
    cell: (row) => (
      <div className="font-mono text-xs">{row.airportDestinationId}</div>
    ),
  },
  {
    id: "packageCount",
    header: "Paquetes",
    accessor: "packageCount",
    cell: (row) => <div className="text-center">{row.packageCount}</div>,
  },
  {
    id: "status",
    header: "Estado",
    accessor: "status",
    cell: (row) => (
      <Badge className={statusColors[row.status]}>
        {statusLabels[row.status]}
      </Badge>
    ),
  },
  {
    id: "date",
    header: "Fecha",
    cell: (row) => (
      <div className="text-sm">
        Día {row.day} -{" "}
        {String(row.hour).padStart(2, "0")}:
        {String(row.minute).padStart(2, "0")}
      </div>
    ),
  },
  {
    id: "priority",
    header: "Prioridad",
    accessor: "priority",
    cell: (row) =>
      row.priority ? (
        <Badge variant="outline">{row.priority}</Badge>
      ) : (
        <span className="text-muted-foreground text-sm">-</span>
      ),
  },
  {
    id: "actions",
    header: "Acciones",
    cell: (row) => (
      <Button
        variant="ghost"
        size="icon"
        onClick={() => console.log("Ver detalles de orden:", row.id)}
      >
        <MoreHorizontal className="h-4 w-4" />
      </Button>
    ),
  },
];