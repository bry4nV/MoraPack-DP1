"use client";

import { Order, OrderState } from "@/types/order"; // El import ahora sí coincide
import { Button } from "@/components/ui/button";
import { MoreHorizontal, Trash2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";

// --- ¡ACTUALIZADO! ---
// Colores para los nuevos estados de 'order.ts'
const statusColors: Record<string, string> = {
  [OrderState.UNASSIGNED]: "bg-gray-100 text-gray-800 hover:bg-gray-100",
  [OrderState.PENDING]: "bg-yellow-100 text-yellow-800 hover:bg-yellow-100",
  [OrderState.IN_TRANSIT]: "bg-blue-100 text-blue-800 hover:bg-blue-100",
  [OrderState.COMPLETED]: "bg-green-100 text-green-800 hover:bg-green-100",
};

// --- ¡ACTUALIZADO! ---
// Etiquetas para los nuevos estados de 'order.ts'
const statusLabels: Record<string, string> = {
  [OrderState.UNASSIGNED]: "Sin Asignar",
  [OrderState.PENDING]: "Pendiente",
  [OrderState.IN_TRANSIT]: "En Tránsito",
  [OrderState.COMPLETED]: "Completado",
};

export interface Column<T> {
  id: string;
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => React.ReactNode;
}

export const orderColumns: Column<Order>[] = [
  {
    id: "orderNumber", // <-- ¡ACTUALIZADO!
    header: "N° Pedido",
    accessor: "orderNumber",
    cell: (row) => <div className="font-mono text-xs font-medium">{row.orderNumber}</div>,
  },
  {
    id: "clientCode", // <-- ¡ACTUALIZADO!
    header: "Cliente",
    accessor: "clientCode",
    cell: (row) => <div>{row.clientCode}</div>,
  },
  {
    id: "airportDestinationCode", // <-- ¡ACTUALIZADO!
    header: "Destino",
    accessor: "airportDestinationCode",
    cell: (row) => (
      <div className="font-mono text-xs">{row.airportDestinationCode}</div>
    ),
  },
  {
    id: "quantity", // <-- ¡ACTUALIZADO!
    header: "Paquetes",
    accessor: "quantity",
    cell: (row) => <div className="text-center">{row.quantity}</div>,
  },
  {
    id: "status",
    header: "Estado",
    accessor: "status",
    cell: (row) => {
      // Lógica actualizada para manejar los nuevos estados
      const statusKey = row.status as OrderState;
      const label = statusLabels[statusKey] || row.status;
      const color = statusColors[statusKey] || statusColors[OrderState.UNASSIGNED];
      return <Badge className={color}>{label}</Badge>;
    }
  },
  {
    id: "date",
    header: "Fecha y Hora", // <-- ¡ACTUALIZADO!
    cell: (row) => (
      // Lógica actualizada para arreglar "Día - undefinedundefinedundefined"
      <div className="text-sm">
        <div>{row.orderDate}</div> {/* Muestra "YYYY-MM-DD" */}
        <div className="text-xs text-muted-foreground">
          {row.orderTime} {/* Muestra "HH:MM:SS" */}
        </div>
      </div>
    ),
  },
  // La columna "priority" se elimina porque ya no existe en el nuevo 'order.ts'
  {
    id: "actions",
    header: "Acciones",
    cell: (row) => (
      <div className="flex items-center gap-2">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => console.log("Ver detalles de orden:", row.id)}
          title="Ver detalles"
        >
          <MoreHorizontal className="h-4 w-4" />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => console.log("Eliminar orden:", row.id)}
          className="text-red-600 hover:text-red-700 hover:bg-red-50"
          title="Eliminar orden"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    ),
  },
];