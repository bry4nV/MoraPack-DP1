// src/app/(app)/pedidos/page.tsx
"use client";

import { useState, useMemo } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { DataTableToolbar } from "@/components/common/data-table/data-table-toolbar";
import { DataTable } from "@/components/common/data-table/data-table";
import { DataTablePagination } from "@/components/common/data-table/data-table-pagination";
import { orderColumns } from "@/components/orders/columns";
import { useOrders } from "@/hooks/use-orders";
import { FileDown, Plus, Upload } from "lucide-react";

export default function PedidosPage() {
  const { orders, isLoading } = useOrders();
  const [searchValue, setSearchValue] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Filtrar pedidos por búsqueda
  const filteredOrders = useMemo(() => {
    if (!searchValue) return orders;

    return orders.filter((order) =>
      Object.values(order).some((value) =>
        String(value).toLowerCase().includes(searchValue.toLowerCase())
      )
    );
  }, [orders, searchValue]);

  // Paginación
  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / pageSize));
  const paginatedOrders = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredOrders.slice(start, start + pageSize);
  }, [filteredOrders, currentPage, pageSize]);

  // Acciones del toolbar (sin disabled)
  const toolbarActions = [
    {
      label: "Exportar",
      icon: FileDown,
      variant: "outline" as const,
      onClick: () => console.log("Exportar (próximamente)"),
    },
    {
      label: "Carga masiva",
      icon: Upload,
      variant: "outline" as const,
      onClick: () => console.log("Carga masiva (próximamente)"),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">
          Gestión de pedidos
        </h1>
        <p className="text-sm text-muted-foreground mt-2">
          Administra todos los pedidos del sistema
        </p>
      </div>

      <Card>
        <CardContent className="p-4 space-y-4">
          <DataTableToolbar
            searchPlaceholder="Buscar pedido..."
            searchValue={searchValue}
            onSearchChange={setSearchValue}
            filterButton={{
              label: "Filtrar por",
              onClick: () => console.log("Filtrar (próximamente)"),
            }}
            actions={toolbarActions}
            primaryAction={{
              label: "Agregar",
              icon: Plus,
              onClick: () => console.log("Agregar pedido (próximamente)"),
            }}
          />

          <DataTable
            columns={orderColumns}
            data={paginatedOrders}
            isLoading={isLoading}
            emptyMessage="No se encontraron pedidos"
            getRowKey={(order) => order.id}
          />

          <DataTablePagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={filteredOrders.length}
            pageSize={pageSize}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </CardContent>
      </Card>
    </div>
  );
}
