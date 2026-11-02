import type { Metadata } from "next";
import { Suspense } from "react";

import { Loading } from "@/components/States";

import RequestDetail from "./RequestDetail";

export const metadata: Metadata = { title: "Demande d'aide" };

export default function RequestPage() {
  return (
    <Suspense fallback={<Loading />}>
      <RequestDetail />
    </Suspense>
  );
}
