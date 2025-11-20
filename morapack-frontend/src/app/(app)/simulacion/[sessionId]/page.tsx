"use client";

import dynamic from "next/dynamic";
import { useEffect } from "react";
import { useParams } from "next/navigation";

// Disable SSR for simulation page to avoid hydration mismatches with Radix UI
const SimulacionClient = dynamic(
  () => import("@/components/sim/SimulacionClient"),
  { ssr: false }
);

export default function SharedSimulationPage() {
  const params = useParams();
  const sessionId = params.sessionId as string;

  useEffect(() => {
    document.title = `Simulación Compartida | MoraTravel`;
  }, []);

  return <SimulacionClient sharedSessionId={sessionId} />;
}
