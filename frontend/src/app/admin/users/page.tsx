import type { Metadata } from "next";

import UsersAdmin from "./UsersAdmin";

export const metadata: Metadata = { title: "Comptes" };

export default function UsersPage() {
  return <UsersAdmin />;
}
