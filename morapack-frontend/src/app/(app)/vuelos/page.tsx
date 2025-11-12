"use client";

// 1. Imports de React (añadimos 'useMemo')
import { useState, useEffect, useMemo } from "react";

// 2. Import del "molde"
import { Flight } from "@/types/flight";

// 3. Import de las columnas (ruta corregida)
import { flightColumns } from "@/components/flights/columns";

// 4. Import de la API de vuelos (ruta corregida)
import { flightsApi } from "@/api/flights/flights"; 

// 5. Imports de la Tabla y Paginación
// (¡OJO! Asumo que estas rutas a 'common/data-table' son correctas,
// las copié de tu 'aeropuertos/page.tsx')
import { DataTable } from "@/components/common/data-table/data-table";
import { DataTablePagination } from "@/components/common/data-table/data-table-pagination";



export default function VuelosPage() {
  const [data, setData] = useState<Flight[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // --- ¡LÓGICA DE PAGINACIÓN AÑADIDA! ---
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10); // Muestra 10 por página

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      try {
        const responseData = await flightsApi.getFlights();
        setData(responseData); // Carga TODOS los datos
      } catch (error) {
        console.error("Error al obtener vuelos:", error);
      }
      setIsLoading(false);
    };

    fetchData();
  }, []);

  // --- LÓGICA PARA PAGINAR LOS DATOS (usando useMemo) ---
  const totalPages = Math.max(1, Math.ceil(data.length / pageSize));
  
  const paginatedFlights = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return data.slice(start, start + pageSize);
  }, [data, currentPage, pageSize]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">Gestión de Vuelos</h1>
        <p className="text-sm text-muted-foreground mt-2">
          Administra todos los vuelos del sistema
        </p>
      </div>
      
      {/* Idealmente, esto iría dentro de un <Card> y <CardContent> 
        como en tu página de aeropuertos, pero lo mantenemos simple por ahora.
      */}

      <DataTable
        columns={flightColumns}
        data={paginatedFlights} // <-- ¡CAMBIADO! Usa solo los datos paginados
        isLoading={isLoading}
        getRowKey={(row: Flight) => row.id}
        emptyMessage="No se encontraron vuelos"
      />

      {/* --- ¡COMPONENTE DE PAGINACIÓN AÑADIDO! --- */}
      <DataTablePagination
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={data.length} // El total de items
        pageSize={pageSize}
        onPageChange={setCurrentPage}
        onPageSizeChange={setPageSize}
      />
    </div>
  );
}