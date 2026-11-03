import type { Metadata } from "next";

import { Suspense } from "react";

import { SkeletonList } from "@/components/States";

import DashboardView from "./DashboardView";

export const metadata: Metadata = { title: "Tableau de bord" };

export default function DashboardPage() {
  return (
    <Suspense fallback={<SkeletonList count={4} />}>
      <DashboardView />
    </Suspense>
  );
}
