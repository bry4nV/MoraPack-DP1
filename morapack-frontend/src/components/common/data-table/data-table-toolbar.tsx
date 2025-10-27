"use client";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Filter, LucideIcon } from "lucide-react";

interface ToolbarAction {
  label: string;
  icon: LucideIcon;
  variant?: "default" | "outline" | "secondary" | "ghost" | "link" | "destructive";
  onClick: () => void;
}

interface DataTableToolbarProps {
  searchPlaceholder?: string;
  searchValue: string;
  onSearchChange: (value: string) => void;
  filterButton?: {
    label: string;
    onClick: () => void;
  };
  actions?: ToolbarAction[];
  primaryAction?: ToolbarAction;
}

export function DataTableToolbar({
  searchPlaceholder = "Buscar...",
  searchValue,
  onSearchChange,
  filterButton,
  actions = [],
  primaryAction,
}: DataTableToolbarProps) {
  return (
    <div className="flex items-center justify-between gap-3">
      {/* Lado izquierdo: Búsqueda y filtro */}
      <div className="flex items-center gap-3 flex-1">
        <Input
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={(e) => onSearchChange(e.target.value)}
          className="max-w-sm"
        />
        {filterButton && (
          <Button variant="outline" onClick={filterButton.onClick}>
            <Filter className="mr-2 h-4 w-4" />
            {filterButton.label}
          </Button>
        )}
      </div>

      {/* Lado derecho: Acciones secundarias + primaria */}
      <div className="flex items-center gap-2">
        {actions.map((action, index) => {
          const Icon = action.icon;
          return (
            <Button
              key={index}
              variant={action.variant || "outline"}
              onClick={action.onClick}
            >
              <Icon className="mr-2 h-4 w-4" />
              {action.label}
            </Button>
          );
        })}
        
        {primaryAction && (
          <Button onClick={primaryAction.onClick}>
            {primaryAction.icon && <primaryAction.icon className="mr-2 h-4 w-4" />}
            {primaryAction.label}
          </Button>
        )}
      </div>
    </div>
  );
}