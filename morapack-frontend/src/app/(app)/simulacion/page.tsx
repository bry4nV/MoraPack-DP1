"use client";

import dynamic from "next/dynamic";
import { useEffect } from "react";

// Disable SSR for simulation page to avoid hydration mismatches with Radix UI
const SimulacionClient = dynamic(
  () => import("@/components/sim/SimulacionClient"),
  { ssr: false }
);

export default function Page() {
  useEffect(() => {
    document.title = "Simulación | MoraTravel";
  }, []);

  return <SimulacionClient />;
}
