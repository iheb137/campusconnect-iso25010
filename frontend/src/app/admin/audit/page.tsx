import type { Metadata } from "next";

import AuditView from "./AuditView";

export const metadata: Metadata = { title: "Journal d'audit" };

export default function AuditPage() {
  return <AuditView />;
}
