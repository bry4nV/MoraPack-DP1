"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent } from "@/components/ui/card";
import { DataTableToolbar } from "@/components/common/data-table/data-table-toolbar";
import { DataTable } from "@/components/common/data-table/data-table";
import { DataTablePagination } from "@/components/common/data-table/data-table-pagination";
import { DeleteDialog } from "@/components/common/delete-dialog";
import { airportColumns } from "@/components/airports/columns";
import { useAirports } from "@/hooks/use-airports";
import { airportsApi } from "@/api/airports/airports";
import { FileDown, Plus, Upload } from "lucide-react";
import type { Airport } from "@/types/airport";

export default function AeropuertosPage() {
  const router = useRouter();
  const { airports, isLoading, error, refetch } = useAirports();
  const [searchValue, setSearchValue] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Función para eliminar aeropuerto
  const handleDelete = async (airport: Airport) => {
    try {
      await airportsApi.deleteAirport(airport.id); // Ahora id es number
      refetch();
    } catch (error) {
      console.error("Error al eliminar aeropuerto:", error);
      throw error;
    }
  };

  // Columnas con la función de eliminar
  const columnsWithActions = useMemo(() => {
    return airportColumns.map(col => {
      if (col.id === "actions") {
        return {
          ...col,
          cell: (row: Airport) => (
            <DeleteDialog
              title="¿Eliminar aeropuerto?"
              description="Esta acción eliminará permanentemente este aeropuerto del sistema."
              itemName={`${row.code} - ${row.city}, ${row.country}`}
              onConfirm={() => handleDelete(row)}
            />
          )
        };
      }
      return col;
    });
  }, []);

  // Filtrar aeropuertos por búsqueda
  const filteredAirports = useMemo(() => {
    if (!searchValue) return airports;

    return airports.filter((airport) =>
      Object.values(airport).some((value) =>
        String(value).toLowerCase().includes(searchValue.toLowerCase())
      )
    );
  }, [airports, searchValue]);

  // Paginación
  const totalPages = Math.max(1, Math.ceil(filteredAirports.length / pageSize));
  const paginatedAirports = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    const paginated = filteredAirports.slice(start, start + pageSize);
    return paginated;
  }, [filteredAirports, currentPage, pageSize]);

  // Acciones del toolbar
  const toolbarActions = [
    {
      label: "Exportar",
      icon: FileDown,
      variant: "outline" as const,
      onClick: () => console.log("Exportar aeropuertos (próximamente)"),
    },
    {
      label: "Carga masiva",
      icon: Upload,
      variant: "outline" as const,
      onClick: () => console.log("Carga masiva aeropuertos (próximamente)"),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">
          Gestión de aeropuertos
        </h1>
        <p className="text-sm text-muted-foreground mt-2">
          Administra todos los aeropuertos del sistema
        </p>
      </div>

      <Card>
        <CardContent className="p-4 space-y-4">
          <DataTableToolbar
            searchPlaceholder="Buscar aeropuerto..."
            searchValue={searchValue}
            onSearchChange={setSearchValue}
            filterButton={{
              label: "Filtrar por",
              onClick: () => console.log("Filtrar aeropuertos (próximamente)"),
            }}
            actions={toolbarActions}
            primaryAction={{
              label: "Agregar",
              icon: Plus,
              onClick: () => router.push("/aeropuertos/agregar"),
            }}
          />

          {error && (
            <div className="p-4 bg-red-50 text-red-600 rounded-md">
              Error: {error.message}
            </div>
          )}

          <DataTable
            columns={columnsWithActions}
            data={paginatedAirports}
            isLoading={isLoading}
            emptyMessage="No se encontraron aeropuertos"
            getRowKey={(airport) => airport.id}
          />

          <DataTablePagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalItems={filteredAirports.length}
            pageSize={pageSize}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </CardContent>
      </Card>
    </div>
  );
}
