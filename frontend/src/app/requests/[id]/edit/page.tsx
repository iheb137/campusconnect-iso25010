import type { Metadata } from "next";

import EditRequest from "./EditRequest";

export const metadata: Metadata = { title: "Modifier la demande" };

export default function EditRequestPage() {
  return <EditRequest />;
}
