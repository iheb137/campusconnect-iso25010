"use client";

import { megaphoneOutline } from "ionicons/icons";
import { useRouter } from "next/navigation";

import PageHead from "@/components/PageHead";

import { api } from "@/lib/api";
import type { HelpRequest } from "@/lib/types";

import RequestForm from "../RequestForm";

export default function NewRequest() {
  const router = useRouter();
  return (
    <>
      <PageHead
        icon={megaphoneOutline}
        title="Publier une demande"
        subtitle="Décrivez votre besoin : les étudiants compétents pourront vous proposer leur aide."
      />
      <RequestForm
        submitLabel="Publier"
        onCancel={() => router.back()}
        onSubmit={async (input) => {
          const created = await api<HelpRequest>("/api/requests", { method: "POST", body: input });
          router.push(`/requests/${created.id}?created=1`);
        }}
      />
    </>
  );
}
