"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import React from "react";
import {
  Home,
  Map,
  Plane,
  Package,
  Users,
  BarChart3,
  MapPin,
  PlayCircle,
  LogOut,
  Settings,
  User,
  ChevronUp,
  ChevronLeft,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const navItems = [
  { href: "/", label: "Inicio", icon: Home },
  { href: "/mapa", label: "En vivo", icon: Map },
  { href: "/simulacion", label: "Simulación", icon: PlayCircle },
  { href: "/pedidos", label: "Mis pedidos", icon: Package },
  { href: "/aeropuertos", label: "Aeropuertos", icon: MapPin },
  { href: "/vuelos", label: "Vuelos", icon: Plane },
  { href: "/usuarios", label: "Usuarios", icon: Users },
  { href: "/reportes", label: "Reportes", icon: BarChart3 },
];

interface SidebarNavProps {
  isCollapsed: boolean;
  setIsCollapsed: React.Dispatch<React.SetStateAction<boolean>>;
}

export function SidebarNav({ isCollapsed, setIsCollapsed }: SidebarNavProps) {
  const pathname = usePathname();

  return (
    // 1. Ancho colapsado cambiado a w-16 (64px)
    <aside
      className={cn(
        "fixed left-0 top-0 z-40 h-screen border-r bg-background transition-all duration-300",
        isCollapsed ? "w-16" : "w-64"
      )}
    >
      <Button
        variant="ghost"
        size="icon"
        className="absolute -right-4 top-16 z-50 h-8 w-8 rounded-full bg-background hover:bg-muted border"
        onClick={() => setIsCollapsed(!isCollapsed)}
      >
        <ChevronLeft
          className={cn(
            "h-4 w-4 transition-transform",
            isCollapsed && "rotate-180"
          )}
        />
      </Button>

      <div className="flex h-full flex-col">
        {/* Header - 2. Padding ('px') quitado cuando está colapsado */}
        <div
          className={cn(
            "flex h-16 items-center border-b",
            isCollapsed ? "justify-center" : "px-6"
          )}
        >
          <Link
            href="/"
            className="flex items-center gap-2.5 overflow-hidden"
          >
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary shrink-0">
              <Plane className="h-5 w-5 text-primary-foreground" />
            </div>
            <span
              className={cn(
                "text-lg font-bold whitespace-nowrap",
                isCollapsed && "hidden"
              )}
            >
              MoraTravel
            </span>
          </Link>
        </div>

        {/* Navigation - 3. Padding ('px') ajustado */}
        <nav
          className={cn(
            "flex-1 space-y-1 overflow-y-auto py-4",
            isCollapsed ? "px-2" : "px-4"
          )}
        >
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;

            return (
              <Link
                key={item.href}
                href={item.href}
                title={isCollapsed ? item.label : undefined} // Tooltip para íconos
                className={cn(
                  "flex items-center gap-3 rounded-lg py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  // 4. Lógica de centrado mejorada
                  isCollapsed
                    ? "justify-center px-3" // Centrado con padding
                    : "px-3"               // Normal con padding
                )}
              >
                <Icon className="h-5 w-5 shrink-0" />
                <span
                  className={cn(
                    "whitespace-nowrap",
                    isCollapsed && "hidden"
                  )}
                >
                  {item.label}
                </span>
              </Link>
            );
          })}
        </nav>

        {/* Footer - 5. Padding ('px') quitado cuando está colapsado */}
        <div className="border-t p-4">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className={cn(
                  "w-full h-auto hover:bg-muted",
                  isCollapsed
                    ? "justify-center p-0" // Centrado sin padding
                    : "justify-between px-3 py-2"
                )}
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground">
                    <span className="text-sm font-semibold">N</span>
                  </div>
                  <div
                    className={cn(
                      "flex flex-col items-start text-left",
                      isCollapsed && "hidden"
                    )}
                  >
                    <p className="text-sm font-medium whitespace-nowrap">
                      Usuario
                    </p>
                    <p className="text-xs text-muted-foreground whitespace-nowrap">
                      usuario@moratravel.com
                    </p>
                  </div>
                </div>
                <ChevronUp
                  className={cn(
                    "h-4 w-4 text-muted-foreground",
                    isCollapsed && "hidden"
                  )}
                />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              side="top"
              align="end"
              className="w-56"
              sideOffset={8}
            >
              {/* ... (El contenido del Dropdown sigue igual) ... */}
              <DropdownMenuLabel className="font-normal">
                <div className="flex flex-col space-y-1">
                  <p className="text-sm font-medium leading-none">Usuario</p>
                  <p className="text-xs leading-none text-muted-foreground">
                    usuario@moratravel.com
                  </p>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => console.log("Ver perfil")}>
                <User className="mr-2 h-4 w-4" />
                <span>Perfil</span>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => console.log("Configuración")}>
                <Settings className="mr-2 h-4 w-4" />
                <span>Configuración</span>
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => console.log("Cerrar sesión")}
                className="text-red-600 focus:text-red-600 focus:bg-red-50"
              >
                <LogOut className="mr-2 h-4 w-4" />
                <span>Cerrar sesión</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </aside>
  );
}