"use client";

import {
  addOutline,
  calendarOutline,
  checkmarkDoneCircleOutline,
  documentTextOutline,
  eyeOffOutline,
  handRightOutline,
  mailUnreadOutline,
  peopleOutline,
  searchOutline,
  star,
  starOutline,
} from "ionicons/icons";
import Link from "next/link";

import { useAuth } from "@/components/AuthProvider";
import Avatar from "@/components/Avatar";
import { OfferStatusBadge, RequestStatusBadge } from "@/components/Badges";
import Icon from "@/components/Icon";
import { EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { formatDate } from "@/lib/format";
import { subjectIcon } from "@/lib/icons";
import type { Dashboard } from "@/lib/types";

export default function DashboardView() {
  const { user } = useAuth();
  const dashboard = useApiData<Dashboard>("/api/dashboard");
  const firstName = user?.fullName.split(" ")[0];

  return (
    <div className="stack-lg">
      <section className="welcome fade-in" aria-labelledby="welcome-title">
        <div style={{ position: "relative", zIndex: 1 }}>
          <h1 id="welcome-title">Bonjour{firstName ? `, ${firstName}` : ""}</h1>
          <p>Vos demandes, vos aides proposées et vos séances réalisées.</p>
        </div>
        <Link href="/requests/new" className="btn">
          <Icon icon={addOutline} size={20} />
          Publier une demande
        </Link>
      </section>

      {dashboard.loading && !dashboard.data ? (
        <SkeletonList count={4} />
      ) : dashboard.error || !dashboard.data ? (
        <ErrorState message={dashboard.error ?? "Chargement impossible"} onRetry={dashboard.reload} />
      ) : (
        <Content data={dashboard.data} />
      )}
    </div>
  );
}

function Content({ data }: { data: Dashboard }) {
  const { stats } = data;
  const toRate = data.sessions.filter((session) => !session.ratedByMe).length;

  return (
    <>
      <section aria-label="Statistiques" className="stats fade-in">
        <Stat
          icon={documentTextOutline}
          tone=""
          value={stats.publishedRequests}
          label="Demandes publiées"
          detail={`${stats.openRequests} ouverte(s)`}
        />
        <Stat
          icon={handRightOutline}
          tone="tone-info"
          value={stats.offersMade}
          label="Aides proposées"
          detail={`${stats.offersAccepted} acceptée(s)`}
        />
        <Stat
          icon={checkmarkDoneCircleOutline}
          tone="tone-success"
          value={stats.completedSessions}
          label="Séances réalisées"
          detail={toRate ? `${toRate} à évaluer` : "Tout est évalué"}
        />
        <Stat
          icon={star}
          tone="tone-accent"
          value={stats.averageRating !== null ? `${stats.averageRating.toFixed(1)} / 5` : "—"}
          label="Note moyenne reçue"
          detail={`${stats.ratingsReceived} évaluation(s)`}
        />
      </section>

      <section className="card fade-in" aria-labelledby="my-requests">
        <div className="card-head">
          <h2 id="my-requests">
            <Icon icon={documentTextOutline} size={20} />
            Mes demandes publiées
          </h2>
          <span className="count">{data.myRequests.length}</span>
        </div>
        {data.myRequests.length === 0 ? (
          <EmptyState
            icon={documentTextOutline}
            title="Vous n'avez publié aucune demande"
            action={{ href: "/requests/new", label: "Publier une demande", icon: addOutline }}
          />
        ) : (
          <ul>
            {data.myRequests.map((request) => (
              <li key={request.id} className="list-item">
                <div className="list-main">
                  <span className="icon-tile icon-tile-sm">
                    <Icon icon={subjectIcon(request.subject.name)} size={19} />
                  </span>
                  <div>
                    {request.hidden ? (
                      <span className="card-title">{request.title}</span>
                    ) : (
                      <Link href={`/requests/${request.id}`} className="card-title">
                        {request.title}
                      </Link>
                    )}
                    <div className="meta" style={{ marginTop: 4 }}>
                      <span>{request.subject.name}</span>
                      <span className="meta-item">
                        <Icon icon={calendarOutline} size={14} />
                        {formatDate(request.createdAt)}
                      </span>
                      <span className="meta-item">
                        <Icon icon={mailUnreadOutline} size={14} />
                        {request.offersCount} proposition(s)
                        {request.pendingOffersCount > 0 ? ` · ${request.pendingOffersCount} en attente` : ""}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="row">
                  {request.hidden ? (
                    <span className="badge badge-danger">
                      <Icon icon={eyeOffOutline} size={14} />
                      Masquée par la modération
                    </span>
                  ) : null}
                  <RequestStatusBadge status={request.status} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card fade-in" aria-labelledby="my-offers">
        <div className="card-head">
          <h2 id="my-offers">
            <Icon icon={handRightOutline} size={20} />
            Mes aides proposées
          </h2>
          <span className="count">{data.myOffers.length}</span>
        </div>
        {data.myOffers.length === 0 ? (
          <EmptyState
            icon={handRightOutline}
            title="Vous n'avez proposé aucune aide"
            text="Parcourez les demandes ouvertes de vos matières."
            action={{ href: "/requests?status=OUVERTE", label: "Voir les demandes ouvertes", icon: searchOutline }}
          />
        ) : (
          <ul>
            {data.myOffers.map((offer) => (
              <li key={offer.id} className="list-item">
                <div className="list-main">
                  <Avatar name={offer.requestAuthor.fullName} size={38} />
                  <div>
                    <Link href={`/requests/${offer.requestId}`} className="card-title">
                      {offer.requestTitle}
                    </Link>
                    <div className="meta" style={{ marginTop: 4 }}>
                      <span>{offer.subject.name}</span>
                      <span>pour {offer.requestAuthor.fullName}</span>
                      <span className="meta-item">
                        <Icon icon={calendarOutline} size={14} />
                        {formatDate(offer.createdAt)}
                      </span>
                    </div>
                  </div>
                </div>
                <OfferStatusBadge status={offer.status} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card fade-in" aria-labelledby="my-sessions">
        <div className="card-head">
          <h2 id="my-sessions">
            <Icon icon={peopleOutline} size={20} />
            Séances réalisées
          </h2>
          <span className="count">{data.sessions.length}</span>
        </div>
        {data.sessions.length === 0 ? (
          <EmptyState
            icon={peopleOutline}
            title="Aucune séance terminée"
            text="Vos séances apparaîtront ici une fois clôturées."
          />
        ) : (
          <ul>
            {data.sessions.map((session) => (
              <li key={session.requestId} className="list-item">
                <div className="list-main">
                  <Avatar name={session.partner.fullName} size={38} />
                  <div>
                    <Link href={`/requests/${session.requestId}`} className="card-title">
                      {session.title}
                    </Link>
                    <div className="meta" style={{ marginTop: 4 }}>
                      <span>{session.subject.name}</span>
                      <span>
                        {session.role === "AUTEUR" ? "Aidé par" : "Vous avez aidé"} {session.partner.fullName}
                      </span>
                      <span className="meta-item">
                        <Icon icon={calendarOutline} size={14} />
                        le {formatDate(session.completedAt)}
                      </span>
                    </div>
                  </div>
                </div>
                {session.ratedByMe ? (
                  <span className="badge badge-success">
                    <Icon icon={star} size={14} />
                    Évaluée
                  </span>
                ) : (
                  <Link href={`/requests/${session.requestId}#evaluation`} className="btn btn-primary btn-sm">
                    <Icon icon={starOutline} size={16} />
                    Évaluer
                  </Link>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

function Stat({
  icon,
  tone,
  value,
  label,
  detail,
}: {
  icon: string;
  tone: string;
  value: number | string;
  label: string;
  detail?: string;
}) {
  return (
    <div className="stat">
      <span className={`icon-tile ${tone}`}>
        <Icon icon={icon} size={24} />
      </span>
      <div>
        <div className="stat-value">{value}</div>
        <div className="stat-label">{label}</div>
        {detail ? <div className="stat-detail">{detail}</div> : null}
      </div>
    </div>
  );
}
