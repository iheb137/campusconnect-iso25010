"use client";

import {
  analyticsOutline,
  bookOutline,
  calendarOutline,
  checkmarkDoneCircleOutline,
  documentTextOutline,
  flagOutline,
  peopleOutline,
  refreshOutline,
  star,
  trophyOutline,
} from "ionicons/icons";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

import Avatar from "@/components/Avatar";
import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import { EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { OFFER_STATUS_LABEL, REQUEST_STATUS_LABEL, formatDate } from "@/lib/format";
import { ROLE_LABEL } from "@/lib/session";
import type { AdminStats, OfferStatus, RequestStatus, Role, Subject } from "@/lib/types";

const REQUEST_TONE: Record<RequestStatus, string> = { OUVERTE: "tone-info", AIDE_ACCEPTEE: "tone-warning", TERMINEE: "tone-success" };
const OFFER_TONE: Record<OfferStatus, string> = { EN_ATTENTE: "tone-warning", ACCEPTEE: "tone-success", REFUSEE: "" };

export default function StatsView() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const from = searchParams.get("from") ?? "";
  const to = searchParams.get("to") ?? "";
  const subjectId = searchParams.get("subjectId") ?? "";

  const query = new URLSearchParams();
  if (from) query.set("from", from);
  if (to) query.set("to", to);
  if (subjectId) query.set("subjectId", subjectId);
  const stats = useApiData<AdminStats>(`/api/admin/stats?${query.toString()}`);
  const subjects = useApiData<Subject[]>("/api/subjects");

  function setFilter(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname);
  }

  return (
    <>
      <PageHead icon={analyticsOutline} title="Tableau de bord" subtitle="Activité de la plateforme, filtrable par période et par matière." />

      <form className="card filters" role="search" aria-label="Filtrer les statistiques" onSubmit={(event) => event.preventDefault()}>
        <div className="field">
          <label htmlFor="stats-from">Du</label>
          <div className="input-wrap">
            <Icon icon={calendarOutline} size={18} />
            <input id="stats-from" type="date" className="input" value={from || stats.data?.from || ""} max={to || undefined} onChange={(e) => setFilter("from", e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="stats-to">Au</label>
          <div className="input-wrap">
            <Icon icon={calendarOutline} size={18} />
            <input id="stats-to" type="date" className="input" value={to || stats.data?.to || ""} min={from || undefined} onChange={(e) => setFilter("to", e.target.value)} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="stats-subject">Matière</label>
          <div className="input-wrap">
            <Icon icon={bookOutline} size={18} />
            <select id="stats-subject" className="select" value={subjectId} onChange={(e) => setFilter("subjectId", e.target.value)}>
              <option value="">Toutes</option>
              {subjects.data?.map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.name}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div>
          <button type="button" className="btn btn-block" onClick={() => router.replace(pathname)} disabled={!from && !to && !subjectId}>
            <Icon icon={refreshOutline} size={18} />
            30 derniers jours
          </button>
        </div>
      </form>

      <div style={{ marginTop: 20 }}>
        {stats.loading && !stats.data ? (
          <SkeletonList count={4} />
        ) : stats.error || !stats.data ? (
          <ErrorState message={stats.error ?? "Chargement impossible"} onRetry={stats.reload} />
        ) : (
          <Content data={stats.data} />
        )}
      </div>
    </>
  );
}

function Content({ data }: { data: AdminStats }) {
  const maxDay = Math.max(1, ...data.requestsPerDay.map((day) => day.count));
  const maxSubject = Math.max(1, ...data.bySubject.map((subject) => subject.requests));
  const totalOffers = Object.values(data.offersByStatus).reduce((sum, value) => sum + value, 0);
  const totalUsers = Object.values(data.usersByRole).reduce((sum, value) => sum + value, 0);

  return (
    <div className="stack-lg">
      <p className="small muted" aria-live="polite">
        Période du {formatDate(data.from)} au {formatDate(data.to)}
      </p>
      <section className="stats" aria-label="Indicateurs clés">
        <Kpi icon={documentTextOutline} tone="" value={data.totalRequests} label="Demandes publiées" />
        <Kpi
          icon={checkmarkDoneCircleOutline}
          tone="tone-success"
          value={data.completedSessions}
          label="Séances terminées"
          detail={data.completionRate !== null ? `${data.completionRate} % des demandes` : undefined}
        />
        <Kpi
          icon={star}
          tone="tone-accent"
          value={data.averageRating !== null ? `${data.averageRating.toFixed(1)} / 5` : "—"}
          label="Note moyenne"
          detail={`${data.ratingsCount} évaluation(s)`}
        />
        <Link href="/admin" className="stat card-link" style={{ textDecoration: "none" }}>
          <span className={`icon-tile ${data.pendingReports > 0 ? "tone-danger" : "tone-success"}`}>
            <Icon icon={flagOutline} size={24} />
          </span>
          <div>
            <div className="stat-value">{data.pendingReports}</div>
            <div className="stat-label">Signalements à traiter</div>
          </div>
        </Link>
      </section>

      <section className="card" aria-labelledby="per-day">
        <div className="card-head">
          <h2 id="per-day">
            <Icon icon={calendarOutline} size={20} />
            Demandes publiées par jour
          </h2>
        </div>
        {data.totalRequests === 0 ? (
          <EmptyState icon={documentTextOutline} title="Aucune demande sur cette période" />
        ) : (
          <>
            <div className="chart" role="img" aria-label={`${data.totalRequests} demandes sur la période`}>
              {data.requestsPerDay.map((day) => (
                <span
                  key={day.day}
                  className="chart-col"
                  style={{ height: `${Math.max(2, (day.count / maxDay) * 100)}%` }}
                  title={`${formatDate(day.day)} : ${day.count}`}
                />
              ))}
            </div>
            <div className="chart-axis">
              <span>{formatDate(data.from)}</span>
              <span>max {maxDay} / jour</span>
              <span>{formatDate(data.to)}</span>
            </div>
          </>
        )}
      </section>

      <div className="grid-cards">
        <section className="card" aria-labelledby="by-status">
          <div className="card-head">
            <h2 id="by-status">
              <Icon icon={documentTextOutline} size={20} />
              Demandes par statut
            </h2>
          </div>
          <div className="bar-list">
            {(Object.keys(REQUEST_STATUS_LABEL) as RequestStatus[]).map((status) => (
              <Bar key={status} label={REQUEST_STATUS_LABEL[status]} value={data.requestsByStatus[status] ?? 0} max={Math.max(1, data.totalRequests)} tone={REQUEST_TONE[status]} />
            ))}
          </div>
          <h3 className="small muted" style={{ margin: "20px 0 10px" }}>
            Propositions ({totalOffers})
          </h3>
          <div className="bar-list">
            {(Object.keys(OFFER_STATUS_LABEL) as OfferStatus[]).map((status) => (
              <Bar key={status} label={OFFER_STATUS_LABEL[status]} value={data.offersByStatus[status] ?? 0} max={Math.max(1, totalOffers)} tone={OFFER_TONE[status]} />
            ))}
          </div>
        </section>

        <section className="card" aria-labelledby="by-subject">
          <div className="card-head">
            <h2 id="by-subject">
              <Icon icon={bookOutline} size={20} />
              Par matière
            </h2>
          </div>
          {data.bySubject.length === 0 ? (
            <p className="muted small">Aucune donnée.</p>
          ) : (
            <div className="bar-list">
              {data.bySubject.map((subject) => (
                <Bar key={subject.subjectId} label={subject.name} value={subject.requests} max={maxSubject} detail={`${subject.completed} terminée(s)`} />
              ))}
            </div>
          )}
        </section>

        <section className="card" aria-labelledby="top-helpers">
          <div className="card-head">
            <h2 id="top-helpers">
              <Icon icon={trophyOutline} size={20} />
              Aidants les plus actifs
            </h2>
          </div>
          {data.topHelpers.length === 0 ? (
            <p className="muted small">Aucune séance terminée sur la période.</p>
          ) : (
            <ol className="list">
              {data.topHelpers.map((helper, index) => (
                <li key={helper.userId} className="row" style={{ justifyContent: "space-between" }}>
                  <span className="row">
                    <span className="count">{index + 1}</span>
                    <Avatar name={helper.fullName} size={30} />
                    {helper.fullName}
                  </span>
                  <span className="small muted">{helper.sessions} séance(s)</span>
                </li>
              ))}
            </ol>
          )}
        </section>

        <section className="card" aria-labelledby="users">
          <div className="card-head">
            <h2 id="users">
              <Icon icon={peopleOutline} size={20} />
              Comptes ({totalUsers})
            </h2>
            <Link href="/admin/users" className="small">
              Gérer
            </Link>
          </div>
          <div className="bar-list">
            {(Object.keys(ROLE_LABEL) as Role[]).map((role) => (
              <Bar key={role} label={ROLE_LABEL[role]} value={data.usersByRole[role] ?? 0} max={Math.max(1, totalUsers)} />
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function Kpi({ icon, tone, value, label, detail }: { icon: string; tone: string; value: number | string; label: string; detail?: string }) {
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

function Bar({ label, value, max, tone = "", detail }: { label: string; value: number; max: number; tone?: string; detail?: string }) {
  return (
    <div className="bar-row">
      <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={label}>
        {label}
      </span>
      <span className="bar-track" role="img" aria-label={`${label} : ${value}`}>
        <span className={`bar-fill ${tone}`} style={{ display: "block", width: `${(value / max) * 100}%` }} />
      </span>
      <span className="small" style={{ fontWeight: 600 }}>
        {value}
        {detail ? <span className="muted" style={{ fontWeight: 400 }}> · {detail}</span> : null}
      </span>
    </div>
  );
}
