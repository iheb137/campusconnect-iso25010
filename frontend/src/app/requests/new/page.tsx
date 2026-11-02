import type { Metadata } from "next";

import NewRequest from "./NewRequest";

export const metadata: Metadata = { title: "Publier une demande" };

export default function NewRequestPage() {
  return <NewRequest />;
}
