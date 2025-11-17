"use client";

// 1. IMPORTAMOS lo que necesitamos
import { useState } from "react";
import { SidebarNav } from "@/components/layout/SidebarNav";
import { cn } from "@/lib/utils"; // Importamos la utilidad para clases condicionales

// ... (imports)
export default function AppLayout({ children }: { children: React.ReactNode }) {
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <div className="flex min-h-screen">
      <SidebarNav 
        isCollapsed={isCollapsed} 
        setIsCollapsed={setIsCollapsed} 
      />
      <main 
        className={cn(
          "flex-1 overflow-auto transition-all duration-300",
          isCollapsed ? "ml-16" : "ml-64" // <-- ¡CAMBIO AQUÍ! (ml-16 = 64px)
        )}
      >
        <div className="p-8">{children}</div>
      </main>
    </div>
  );
}