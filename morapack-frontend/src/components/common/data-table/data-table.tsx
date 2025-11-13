"use client";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";

export interface Column<T> {
  id: string;
  header: string;
  accessor?: keyof T;
  cell?: (row: T) => React.ReactNode;
  className?: string;
  headerClassName?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  isLoading?: boolean;
  emptyMessage?: string;
  getRowKey: (row: T) => string | number;
}

export function DataTable<T>({
  columns,
  data,
  isLoading = false,
  emptyMessage = "No hay datos disponibles",
  getRowKey,
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-12">
        <div className="text-muted-foreground">Cargando...</div>
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="flex items-center justify-center p-12">
        <div className="text-muted-foreground">{emptyMessage}</div>
      </div>
    );
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent border-b">
            {columns.map((column) => (
              <TableHead
                key={column.id}
                className={cn(
                  "h-11 font-medium",
                  column.headerClassName
                )}
              >
                {column.header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((row) => (
            <TableRow
              key={getRowKey(row)}
              className="border-b transition-colors hover:bg-muted/50"
            >
              {columns.map((column) => (
                <TableCell
                  key={column.id}
                  className={cn("py-3", column.className)}
                >
                  {column.cell
                    ? column.cell(row)
                    : String((row as any)[column.accessor!])}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}