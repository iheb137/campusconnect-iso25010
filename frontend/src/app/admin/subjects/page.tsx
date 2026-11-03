import type { Metadata } from "next";

import SubjectsAdmin from "./SubjectsAdmin";

export const metadata: Metadata = { title: "Matières" };

export default function SubjectsPage() {
  return <SubjectsAdmin />;
}
