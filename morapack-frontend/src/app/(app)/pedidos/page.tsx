// src/app/(app)/pedidos/page.tsx
import Link from "next/link";
import { apiGet } from "@/lib/api";
import type { PedidoBack } from "@/types/pedido-back";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FileDown, Plus, Upload } from "lucide-react";

export const metadata = { title: "Pedidos | MoraTravel" };

async function getPedidos(): Promise<PedidoBack[]> {
  return apiGet<PedidoBack[]>("/api/orders");
}

const PAGE_SIZE = 10;
const pad = (n: number) => n.toString().padStart(2, "0");
const programado = (p: PedidoBack) => `Día ${p.day} — ${pad(p.hour)}:${pad(p.minute)}`;

type PageProps = {
  searchParams?: { page?: string };
};

export default async function Page({ searchParams }: PageProps) {
  const pedidos = await getPedidos();

  // página actual desde query (?page=), 1-based
  const page = Math.max(1, Number.parseInt(searchParams?.page ?? "1") || 1);
  const total = pedidos.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const start = (page - 1) * PAGE_SIZE;
  const pageRows = pedidos.slice(start, start + PAGE_SIZE);

  return (
    <div className="space-y-6">
      <h1 className="text-4xl font-bold tracking-tight">Gestión de pedidos</h1>
      <p className="text-sm text-muted-foreground">Todos los pedidos</p>

      <Card>
        <CardContent className="p-4 space-y-4">
          {/* Barra superior (placeholder; sin lógica aún) */}
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="flex items-center gap-2">
              <Input
                className="w-72"
                placeholder="Buscar pedido…"
                readOnly
                title="Pendiente de implementar"
              />
              <Button variant="outline" size="sm" disabled title="Pendiente de implementar">
                Filtrar por
              </Button>
            </div>

            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" disabled title="Exportar (pendiente)">
                <FileDown className="mr-2 h-4 w-4" />
                Exportar
              </Button>
              <Button variant="default" size="sm" disabled title="Agregar (pendiente)">
                <Plus className="mr-2 h-4 w-4" />
                Agregar
              </Button>
              <Button variant="secondary" size="sm" disabled title="Carga masiva (pendiente)">
                <Upload className="mr-2 h-4 w-4" />
                Carga masiva
              </Button>
            </div>
          </div>

          {/* Tabla (paginada) */}
          <div className="rounded-md border overflow-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/50">
                <tr className="text-left">
                  <th className="w-10 p-3">
                    <input type="checkbox" disabled title="Selección (pendiente)" />
                  </th>
                  <th className="p-3">ID</th>
                  <th className="p-3">Cliente</th>
                  <th className="p-3">Destino</th>
                  <th className="p-3 text-right">Cantidad de productos</th>
                  <th className="p-3 text-right">Prioridad</th>
                  <th className="p-3">Estado</th>
                  <th className="p-3">Fecha de llegada</th>
                  <th className="w-8 p-3"></th>
                </tr>
              </thead>
              <tbody>
                {pageRows.map((p) => (
                  <tr key={p.id} className="border-t">
                    <td className="p-3">
                      <input type="checkbox" disabled title="Selección (pendiente)" />
                    </td>
                    <td className="p-3 font-medium">{p.id}</td>
                    <td className="p-3">{p.clientId}</td>
                    <td className="p-3">{p.airportDestinationId}</td>
                    <td className="p-3 text-right tabular-nums">{p.packageCount}</td>
                    <td className="p-3 text-right tabular-nums">{p.priority ?? "-"}</td>
                    <td className="p-3">
                      <span className="inline-flex px-2 py-0.5 rounded-full text-xs bg-muted">
                        {p.status}
                      </span>
                    </td>
                    <td className="p-3">{programado(p)}</td>
                    <td className="p-3 text-right">
                      <button className="rounded px-2 py-1 hover:bg-muted" disabled title="Acciones (pendiente)">
                        ⋯
                      </button>
                    </td>
                  </tr>
                ))}
                {pageRows.length === 0 && (
                  <tr>
                    <td colSpan={9} className="p-6 text-center text-muted-foreground">
                      No hay pedidos.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pie con paginación real por URL (?page=) */}
          <div className="flex items-center justify-between text-sm">
            <div className="text-muted-foreground">
              Mostrando {pageRows.length} de {total} — página {page} de {totalPages}
            </div>

            <div className="flex items-center gap-2">
              {page > 1 ? (
                <Button asChild variant="outline" size="sm">
                  <Link href={`/pedidos?page=${page - 1}`}>Previous</Link>
                </Button>
              ) : (
                <Button variant="outline" size="sm" disabled>
                  Previous
                </Button>
              )}

              <span className="text-muted-foreground">Página {page} de {totalPages}</span>

              {page < totalPages ? (
                <Button asChild variant="outline" size="sm">
                  <Link href={`/pedidos?page=${page + 1}`}>Next</Link>
                </Button>
              ) : (
                <Button variant="outline" size="sm" disabled>
                  Next
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
