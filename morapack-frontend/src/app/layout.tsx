// src/app/layout.tsx
import "@/styles/globals.css";
import type { Metadata } from "next";
import { Toaster } from "sonner"; // opcional si usas sonner

export const metadata: Metadata = { title: "MoraTravel", description: "MoraPack Frontend" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body className="antialiased min-h-dvh">
        {children}
        <Toaster richColors position="top-right" />
      </body>
    </html>
  );
}
