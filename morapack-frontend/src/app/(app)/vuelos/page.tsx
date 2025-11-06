// Contenido para tu nuevo src/app/vuelos/page.tsx

"use client";
import { useEffect, useState } from "react";
import { flightColumns } from "./components/columns"; // <-- Importa las nuevas columnas
import { Flight } from "@/types/flight";              // <-- Importa el nuevo "molde"
import { API_ENDPOINTS, api } from "@/lib/api";
import { DataTable } from "@/components/ui/data-table"; // (Asumo que tienes esto)

export default function VuelosPage() {
  const [data, setData] = useState<Flight[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      try {
        // ¡Asegúrate de que este endpoint exista en tu api.ts!
        const response = await api.get(API_ENDPOINTS.FLIGHTS.BASE); 
        setData(response.data);
      } catch (error) {
        console.error("Error al obtener vuelos:", error);
      }
      setIsLoading(false);
    };

    fetchData();
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold tracking-tight">Gestión de Vuelos</h1>
        <p className="text-sm text-muted-foreground mt-2">
          Administra todos los vuelos del sistema
        </p>
      </div>
      
      {/* (Aquí puedes añadir tus botones de "Agregar", "Exportar", etc.) */}

      <DataTable
        columns={flightColumns} // <-- Usa las nuevas columnas
        data={data}
        isLoading={isLoading}
        // (Añade filtros si los necesitas)
      />
    </div>
  );
}