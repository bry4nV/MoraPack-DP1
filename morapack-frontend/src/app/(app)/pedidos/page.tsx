// src/app/(app)/pedidos/page.tsx
"use client";

// --- Importaciones Actualizadas ---
import { useState, useMemo, useEffect } from "react"; // Añadido useEffect
import { Card, CardContent } from "@/components/ui/card";
import { DataTableToolbar } from "@/components/common/data-table/data-table-toolbar";
import { DataTable } from "@/components/common/data-table/data-table";
import { DataTablePagination } from "@/components/common/data-table/data-table-pagination";
import { orderColumns } from "@/components/orders/columns";
import { ordersApi } from "@/api/orders/orders"; // <-- AÑADIDO: Para llamar a la API
import { Order } from "@/types/order"; // <-- AÑADIDO: El tipo de dato
import { CreateOrderModal } from "@/components/orders/CreateOrderModal"; // <-- AÑADIDO: El modal
import { FileDown, Plus, Upload } from "lucide-react";

export default function PedidosPage() {
  // --- Estados de Datos (reemplaza a useOrders) ---
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // --- Estados de la UI (ya los tenías) ---
  const [searchValue, setSearchValue] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // --- ¡NUEVO! Estado para el modal ---
  const [isModalOpen, setIsModalOpen] = useState(false);

  // --- ¡NUEVO! Función para cargar datos ---
  // (Igual que en Vuelos)
  const fetchData = async () => {
    setIsLoading(true);
    try {
      const responseData = await ordersApi.getOrders();
      setOrders(responseData);
    } catch (error) {
      console.error("Error al obtener pedidos:", error);
    }
    setIsLoading(false);
  };

  // --- ¡NUEVO! useEffect para cargar datos al inicio ---
  useEffect(() => {
    fetchData();
  }, []);

  // --- Lógica de filtro (sin cambios) ---
  const filteredOrders = useMemo(() => {
    if (!searchValue) return orders;
    return orders.filter((order) =>
      Object.values(order).some((value) =>
        String(value).toLowerCase().includes(searchValue.toLowerCase())
      )
    );
  }, [orders, searchValue]);

  // --- Lógica de paginación (sin cambios) ---
  const totalPages = Math.max(1, Math.ceil(filteredOrders.length / pageSize));
  const paginatedOrders = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredOrders.slice(start, start + pageSize);
  }, [filteredOrders, currentPage, pageSize]);

  // --- Acciones del toolbar (sin cambios) ---
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

  // --- ¡NUEVO! Función para refrescar después de crear ---
  const handleOrderCreated = () => {
    fetchData(); // Vuelve a cargar los datos
    setIsModalOpen(false); // Cierra el modal
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">
          Gestión de pedidos
        </h1>
        <p className="text-sm text-muted-foreground mt-2">
          
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
              // --- ¡CAMBIO! Abre el modal ---
              onClick: () => setIsModalOpen(true),
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

      {/* --- ¡NUEVO! Renderiza el modal --- */}
      {/* (Está oculto por defecto hasta que isOpen sea true) */}
      <CreateOrderModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onOrderCreated={handleOrderCreated}
      />
    </div>
  );
}