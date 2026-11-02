import type { Metadata } from "next";

import ReportsAdmin from "./ReportsAdmin";

export const metadata: Metadata = { title: "Administration" };

export default function AdminPage() {
  return <ReportsAdmin />;
}
