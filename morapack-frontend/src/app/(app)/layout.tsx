// app/(app)/layout.tsx
"use client";

import { SidebarNav } from "@/components/layout/SidebarNav";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <SidebarNav />
      <main className="flex-1 ml-64 overflow-auto">
        <div className="p-8">{children}</div>
      </main>
    </div>
  );
}
