"use client";

import { arrowBackOutline, createOutline } from "ionicons/icons";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";

import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import { EmptyState, ErrorState, Loading } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api } from "@/lib/api";
import type { HelpRequest } from "@/lib/types";

import RequestForm from "../../RequestForm";

export default function EditRequest() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const request = useApiData<HelpRequest>(`/api/requests/${id}`);

  if (request.loading) return <Loading />;
  if (request.status === 404) return <EmptyState title="Demande introuvable" action={{ href: "/requests", label: "Voir les demandes" }} />;
  if (request.error || !request.data) return <ErrorState message={request.error ?? "Chargement impossible"} onRetry={request.reload} />;

  const data = request.data;
  if (!data.ownedByCurrentUser || data.status !== "OUVERTE") {
    return (
      <div className="card">
        <EmptyState
          title="Modification impossible"
          text={
            data.ownedByCurrentUser
              ? "Seule une demande ouverte peut être modifiée."
              : "Seul l'auteur peut modifier cette demande."
          }
          action={{ href: `/requests/${data.id}`, label: "Retour à la demande" }}
        />
      </div>
    );
  }

  return (
    <>
      <Link href={`/requests/${data.id}`} className="back-link">
        <Icon icon={arrowBackOutline} size={16} />
        Retour à la demande
      </Link>
      <PageHead icon={createOutline} title="Modifier la demande" subtitle={data.title} />
      <RequestForm
        submitLabel="Enregistrer les modifications"
        initial={{
          title: data.title,
          description: data.description,
          availability: data.availability,
          subjectId: data.subject.id,
          level: data.level,
        }}
        onCancel={() => router.push(`/requests/${data.id}`)}
        onSubmit={async (input) => {
          await api<HelpRequest>(`/api/requests/${data.id}`, { method: "PUT", body: input });
          router.push(`/requests/${data.id}?updated=1`);
        }}
      />
    </>
  );
}
