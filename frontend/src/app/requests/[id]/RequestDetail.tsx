"use client";

import {
  arrowBackOutline,
  calendarOutline,
  checkmarkDoneOutline,
  createOutline,
  schoolOutline,
  timeOutline,
  trashOutline,
} from "ionicons/icons";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

import { useAuth } from "@/components/AuthProvider";
import Avatar from "@/components/Avatar";
import { RequestStatusBadge } from "@/components/Badges";
import Icon from "@/components/Icon";
import { Alert, EmptyState, ErrorState, Loading } from "@/components/States";
import StatusTimeline from "@/components/StatusTimeline";
import { useApiData } from "@/components/useApiData";
import { api, errorMessage } from "@/lib/api";
import { formatDate, formatDateTime } from "@/lib/format";
import { subjectIcon } from "@/lib/icons";
import type { HelpRequest } from "@/lib/types";

import OfferForm from "./OfferForm";
import OffersPanel from "./OffersPanel";
import RatingsPanel from "./RatingsPanel";
import ReportForm from "./ReportForm";

export default function RequestDetail() {
  const { id } = useParams<{ id: string }>();
  const searchParams = useSearchParams();
  const router = useRouter();
  const { user } = useAuth();
  const request = useApiData<HelpRequest>(`/api/requests/${id}`);
  const [notice, setNotice] = useState<{ kind: "error" | "success"; text: string } | null>(null);
  const [completing, setCompleting] = useState(false);
  const [deleting, setDeleting] = useState(false);

  if (request.loading && !request.data) return <Loading label="Chargement de la demande…" />;
  if (request.status === 404)
    return (
      <div className="card">
        <EmptyState
          title="Demande introuvable"
          text="Elle n'existe pas ou a été retirée par la modération."
          action={{ href: "/requests", label: "Voir les demandes" }}
        />
      </div>
    );
  if (request.error || !request.data)
    return <ErrorState message={request.error ?? "Chargement impossible"} onRetry={request.reload} />;

  const data = request.data;
  const isOwner = data.ownedByCurrentUser;
  const isStudent = user?.role === "STUDENT";

  async function complete() {
    if (!window.confirm("Confirmer que la séance a eu lieu ? La demande passera à « Terminée ».")) return;
    setCompleting(true);
    setNotice(null);
    try {
      const updated = await api<HelpRequest>(`/api/requests/${data.id}/complete`, { method: "POST" });
      request.setData(updated);
      setNotice({ kind: "success", text: "Séance terminée. Vous pouvez maintenant l'évaluer." });
    } catch (error) {
      setNotice({ kind: "error", text: errorMessage(error) });
    } finally {
      setCompleting(false);
    }
  }

  async function remove() {
    if (!window.confirm("Supprimer définitivement cette demande ? Les propositions reçues seront aussi supprimées.")) return;
    setDeleting(true);
    setNotice(null);
    try {
      await api(`/api/requests/${data.id}`, { method: "DELETE" });
      router.push("/dashboard?deleted=1");
    } catch (error) {
      setNotice({ kind: "error", text: errorMessage(error) });
      setDeleting(false);
    }
  }

  return (
    <>
      <Link href="/requests" className="back-link">
        <Icon icon={arrowBackOutline} size={16} />
        Toutes les demandes
      </Link>

      <div className="stack">
        {searchParams.get("created") ? <Alert kind="success">Votre demande est publiée.</Alert> : null}
        {searchParams.get("updated") ? <Alert kind="success">Modifications enregistrées.</Alert> : null}
      </div>

      <div className="grid-2" style={{ marginTop: 12 }}>
        <div className="stack-lg">
          <article className="card fade-in">
            <div className="row" style={{ alignItems: "flex-start", gap: 16, flexWrap: "nowrap" }}>
              <span className="icon-tile">
                <Icon icon={subjectIcon(data.subject.name)} size={26} />
              </span>
              <div style={{ minWidth: 0, flex: 1 }}>
                <div className="row-between">
                  <h1 style={{ overflowWrap: "anywhere", fontSize: "clamp(1.35rem, 1.6vw + 1rem, 1.8rem)" }}>
                    {data.title}
                  </h1>
                  <RequestStatusBadge status={data.status} />
                </div>
                <div className="meta" style={{ marginTop: 8 }}>
                  <span className="chip">{data.subject.name}</span>
                  <span className="meta-item">
                    <Icon icon={schoolOutline} size={15} />
                    Niveau {data.level}
                  </span>
                  <span className="meta-item">
                    <Icon icon={calendarOutline} size={15} />
                    publiée le {formatDate(data.createdAt)}
                  </span>
                </div>
              </div>
            </div>

            <div className="desc-block" style={{ marginTop: 18 }}>
              <p className="pre-line">{data.description}</p>
            </div>

            <ul className="info-list" style={{ marginTop: 18 }}>
              <li>
                <Avatar name={data.author.fullName} size={38} />
                <div>
                  <div className="label">Auteur</div>
                  <div style={{ fontWeight: 600 }}>{isOwner ? "Vous" : data.author.fullName}</div>
                </div>
              </li>
              <li>
                <span className="icon-tile icon-tile-sm tone-info">
                  <Icon icon={timeOutline} size={19} />
                </span>
                <div>
                  <div className="label">Disponibilité</div>
                  <div>{data.availability}</div>
                </div>
              </li>
            </ul>
          </article>

          {notice ? <Alert kind={notice.kind}>{notice.text}</Alert> : null}

          {isOwner ? (
            <OffersPanel request={data} onRequestChanged={request.reload} />
          ) : isStudent && data.status === "OUVERTE" ? (
            <OfferForm requestId={data.id} />
          ) : null}

          {data.status !== "OUVERTE" ? <RatingsPanel key={data.status} request={data} /> : null}
        </div>

        <aside className="stack" aria-label="Suivi et actions">
          <section className="card fade-in">
            <div className="card-head">
              <h2>Suivi</h2>
            </div>
            <StatusTimeline status={data.status} />
            {data.completedAt ? (
              <p className="small muted" style={{ marginTop: 14 }}>
                Terminée le {formatDateTime(data.completedAt)}
              </p>
            ) : null}
            {isOwner && data.status === "OUVERTE" ? (
              <p className="small muted" style={{ marginTop: 14 }}>
                Acceptez une proposition pour passer à l&apos;étape suivante.
              </p>
            ) : null}
            {isOwner && data.status === "AIDE_ACCEPTEE" ? (
              <button
                type="button"
                className="btn btn-success btn-block"
                style={{ marginTop: 18 }}
                onClick={complete}
                disabled={completing}
              >
                <Icon icon={checkmarkDoneOutline} size={19} />
                {completing ? "Clôture…" : "Marquer la séance comme terminée"}
              </button>
            ) : null}
          </section>

          {isOwner && data.status === "OUVERTE" ? (
            <section className="card fade-in">
              <div className="card-head">
                <h2>Gérer</h2>
              </div>
              <div className="stack" style={{ gap: 10 }}>
                <Link href={`/requests/${data.id}/edit`} className="btn btn-block">
                  <Icon icon={createOutline} size={18} />
                  Modifier la demande
                </Link>
                <button type="button" className="btn btn-ghost btn-block" onClick={remove} disabled={deleting} style={{ color: "var(--danger)" }}>
                  <Icon icon={trashOutline} size={18} />
                  {deleting ? "Suppression…" : "Supprimer la demande"}
                </button>
              </div>
            </section>
          ) : null}

          {isStudent && !isOwner ? <ReportForm requestId={data.id} /> : null}
        </aside>
      </div>
    </>
  );
}
