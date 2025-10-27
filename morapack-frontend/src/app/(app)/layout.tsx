// app/(app)/layout.tsx
"use client";

import { SidebarProvider } from "@/components/ui/sidebar";
import { SidebarNav } from "@/components/layout/SidebarNav";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <SidebarProvider>
      <div className="flex min-h-dvh w-full">
        <SidebarNav /> {/* shadcn sidebar */}
        <main className="flex-1 bg-sky-50 p-8">{children}</main>
      </div>
    </SidebarProvider>
  );
}
