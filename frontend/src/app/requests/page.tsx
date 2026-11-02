import type { Metadata } from "next";
import { Suspense } from "react";

import { SkeletonList } from "@/components/States";

import RequestsBrowser from "./RequestsBrowser";

export const metadata: Metadata = { title: "Demandes d'aide" };

export default function RequestsPage() {
  return (
    <Suspense fallback={<SkeletonList />}>
      <RequestsBrowser />
    </Suspense>
  );
}
