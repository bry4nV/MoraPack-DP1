// components/layout/SidebarNav.tsx
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Sidebar, SidebarContent, SidebarGroup, SidebarGroupLabel,
  SidebarGroupContent, SidebarMenu, SidebarMenuButton, SidebarMenuItem,
  SidebarFooter
} from "@/components/ui/sidebar";
import {
  Home, Map, BadgePercent, PackageSearch, Building2, Plane, Users, BarChart3, LogOut
} from "lucide-react";
import { Button } from "@/components/ui/button";

const NAV = [
  { href: "/", label: "Inicio", icon: Home },
  { href: "/mapa", label: "Mapa", icon: Map },
  { href: "/simulacion", label: "Simulación", icon: BadgePercent },
  { href: "/mis-pedidos", label: "Mis pedidos", icon: PackageSearch },
  { href: "/aeropuertos", label: "Aeropuertos", icon: Building2 },
  { href: "/vuelos", label: "Vuelos", icon: Plane },
  { href: "/usuarios", label: "Usuarios", icon: Users },
  { href: "/reportes", label: "Reportes", icon: BarChart3 },
];

export function SidebarNav() {
  const pathname = usePathname();

  return (
    <Sidebar className="border-r">
      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel className="text-xl font-semibold">MoraTravel</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {NAV.map(({ href, label, icon: Icon }) => {
                const active = pathname === href || pathname.startsWith(href + "/");
                return (
                  <SidebarMenuItem key={href}>
                    <SidebarMenuButton asChild isActive={active}>
                      <Link href={href}>
                        <Icon className="mr-2 size-4" />
                        <span>{label}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                );
              })}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="p-3 border-t">
        <form action="/logout" method="POST" className="w-full">
          <Button type="submit" variant="secondary" className="w-full flex items-center justify-center gap-2">
            <LogOut className="size-4" />
            Cerrar sesión
          </Button>
        </form>
      </SidebarFooter>
    </Sidebar>
  );
}
