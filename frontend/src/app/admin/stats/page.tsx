import type { Metadata } from "next";
import { Suspense } from "react";

import { Loading } from "@/components/States";

import StatsView from "./StatsView";

export const metadata: Metadata = { title: "Statistiques" };

export default function StatsPage() {
  return (
    <Suspense fallback={<Loading />}>
      <StatsView />
    </Suspense>
  );
}
