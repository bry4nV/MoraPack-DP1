"use client";

import { useState, useEffect, useMemo, useTransition } from "react";
import { Flight, FlightStatus } from "@/types/flight";
import { flightColumns } from "@/components/flights/columns";
import { flightsApi } from "@/api/flights/flights";
import { useRouter } from 'next/navigation';
import { ColumnDef } from "@tanstack/react-table";
import { Button } from "@/components/ui/button";
import { DataTable } from "@/components/ui/data-table"; 
import { DataTablePagination } from "@/components/common/data-table/data-table-pagination"; 
import { Input } from "@/components/ui/input";
import { Search } from "lucide-react"; // Importamos el ícono de lupa

export default function VuelosPage() {
  const [data, setData] = useState<Flight[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  
  // --- ¡ASEGÚRATE DE QUE ESTA LÍNEA DIGA 30! ---
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(30); // <-- 30 FILAS

  const [searchValue, setSearchValue] = useState("");
  const router = useRouter(); 
  const [isPending, startTransition] = useTransition();
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const responseData = await flightsApi.getFlights();
      setData(responseData);
    } catch (error) {
      console.error("Error al obtener vuelos:", error);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCancel = (id: number) => {
    setCancellingId(id); 
    startTransition(async () => {
      try {
        await flightsApi.cancelFlight(id);
        fetchData(); 
      } catch (error) {
        console.error("Error al cancelar vuelo:", error);
      } finally {
        setCancellingId(null);
      }
    });
  };

  const filteredFlights = useMemo(() => {
    if (!searchValue) return data;
    const lowerSearch = searchValue.toLowerCase();
    return data.filter((flight) =>
      flight.id.toString().includes(lowerSearch) ||
      flight.flightDate.toLowerCase().includes(lowerSearch) ||
      flight.airportOriginCode.toLowerCase().includes(lowerSearch) ||
      flight.airportDestinationCode.toLowerCase().includes(lowerSearch) ||
      flight.status.toLowerCase().includes(lowerSearch)
    );
  }, [data, searchValue]);

  const totalPages = Math.max(1, Math.ceil(filteredFlights.length / pageSize));
  
  const paginatedFlights = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredFlights.slice(start, start + pageSize);
  }, [filteredFlights, currentPage, pageSize]);

  // Lógica de columnas (sin cambios)
  const columns = useMemo<ColumnDef<Flight>[]>(() => flightColumns.map(col => {
    if (col.id === 'actions') {
      return { /* ... (código del botón cancelar) ... */
        ...col,
        cell: ({ row }) => {
          const flight = row.original;
          const isCancelling = isPending && cancellingId === flight.id;
          if (flight.status === FlightStatus.CANCELLED) {
             return <span className="text-sm text-muted-foreground">—</span>;
          }
          return (
            <Button
              variant="outline" size="sm"
              className="text-red-600 hover:text-red-700 hover:bg-red-50"
              onClick={() => handleCancel(flight.id)}
              disabled={isCancelling}
            >
              {isCancelling ? "Cancelando..." : "Cancelar"}
            </Button>
          );
        }
      };
    }
    return col;
  }), [isPending, cancellingId]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">Gestión de Vuelos</h1>
        {/* Texto de párrafo eliminado */}
      </div>

      {/* Barra de búsqueda con lupa */}
      <div className="relative flex items-center">
        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Buscar por ID, origen, destino, estado..."
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          className="max-w-sm pl-8"
        />
      </div>

      <DataTable
        columns={columns} 
        data={paginatedFlights}
        isLoading={isLoading}
      />

      <DataTablePagination
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={filteredFlights.length}
        pageSize={pageSize} // <-- Le pasamos 30
        onPageChange={setCurrentPage}
        onPageSizeChange={setPageSize}
      />
    </div>
  );
}