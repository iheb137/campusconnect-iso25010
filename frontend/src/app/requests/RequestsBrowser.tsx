"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";

import {
  addOutline,
  bookOutline,
  calendarOutline,
  chevronBackOutline,
  chevronForwardOutline,
  flagOutline,
  refreshOutline,
  schoolOutline,
  searchOutline,
  timeOutline,
} from "ionicons/icons";

import { useAuth } from "@/components/AuthProvider";
import Avatar from "@/components/Avatar";
import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import { subjectIcon } from "@/lib/icons";
import { RequestStatusBadge } from "@/components/Badges";
import { EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { LEVELS, REQUEST_STATUS_LABEL, formatDate } from "@/lib/format";
import type { HelpRequest, Page, RequestStatus, Subject } from "@/lib/types";

const PAGE_SIZE = 10;
const STATUSES = Object.keys(REQUEST_STATUS_LABEL) as RequestStatus[];

export default function RequestsBrowser() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { user } = useAuth();

  // Les filtres vivent dans l'URL : ils survivent au rechargement et se partagent.
  const subjectId = searchParams.get("subjectId") ?? "";
  const level = searchParams.get("level") ?? "";
  const status = searchParams.get("status") ?? "";
  const q = searchParams.get("q") ?? "";
  const page = Math.max(Number(searchParams.get("page") ?? "0") || 0, 0);

  const query = new URLSearchParams();
  if (subjectId) query.set("subjectId", subjectId);
  if (level) query.set("level", level);
  if (status) query.set("status", status);
  if (q) query.set("q", q);
  query.set("page", String(page));
  query.set("size", String(PAGE_SIZE));

  const subjects = useApiData<Subject[]>("/api/subjects");
  const requests = useApiData<Page<HelpRequest>>(`/api/requests?${query.toString()}`);

  function setFilter(key: string, value: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (value) params.set(key, value);
    else params.delete(key);
    if (key !== "page") params.delete("page");
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: key === "page" });
  }

  // Recherche texte : on attend une courte pause de frappe avant d'interroger l'API.
  const [text, setText] = useState(q);
  const [syncedQ, setSyncedQ] = useState(q);
  if (q !== syncedQ) {
    // L'URL a changé ailleurs (réinitialisation, navigation) : on resynchronise le champ.
    setSyncedQ(q);
    if (text.trim() !== q) setText(q);
  }
  useEffect(() => {
    if (text.trim() === q) return;
    const timer = setTimeout(() => setFilter("q", text.trim()), 350);
    return () => clearTimeout(timer);
    // setFilter dépend de l'URL courante, déjà couverte par q
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [text, q]);

  const hasFilters = Boolean(subjectId || level || status || q);

  return (
    <>
      <PageHead icon={searchOutline} title="Demandes d'aide" subtitle="Trouvez une demande à laquelle vous pouvez répondre.">
        {user?.role === "STUDENT" ? (
          <Link href="/requests/new" className="btn btn-primary">
            <Icon icon={addOutline} size={20} />
            Publier une demande
          </Link>
        ) : null}
      </PageHead>

      <form className="card filters" role="search" aria-label="Filtrer les demandes" onSubmit={(e) => e.preventDefault()}>
        <div className="field filters-search">
          <label htmlFor="filter-q">Rechercher</label>
          <div className="input-wrap">
            <Icon icon={searchOutline} size={18} />
            <input
              id="filter-q"
              className="input"
              type="search"
              placeholder="Mot du titre ou de la description"
              value={text}
              maxLength={100}
              onChange={(event) => setText(event.target.value)}
            />
          </div>
        </div>
        <div className="field">
          <label htmlFor="filter-subject">Matière</label>
          <div className="input-wrap">
          <Icon icon={bookOutline} size={18} />
          <select
            id="filter-subject"
            className="select"
            value={subjectId}
            onChange={(event) => setFilter("subjectId", event.target.value)}
            disabled={!subjects.data}
          >
            <option value="">Toutes</option>
            {subjects.data?.map((subject) => (
              <option key={subject.id} value={subject.id}>
                {subject.name}
              </option>
            ))}
          </select>
          </div>
        </div>
        <div className="field">
          <label htmlFor="filter-level">Niveau</label>
          <div className="input-wrap">
          <Icon icon={schoolOutline} size={18} />
          <select
            id="filter-level"
            className="select"
            value={level}
            onChange={(event) => setFilter("level", event.target.value)}
          >
            <option value="">Tous</option>
            {LEVELS.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
          </div>
        </div>
        <div className="field">
          <label htmlFor="filter-status">Statut</label>
          <div className="input-wrap">
          <Icon icon={flagOutline} size={18} />
          <select
            id="filter-status"
            className="select"
            value={status}
            onChange={(event) => setFilter("status", event.target.value)}
          >
            <option value="">Tous</option>
            {STATUSES.map((value) => (
              <option key={value} value={value}>
                {REQUEST_STATUS_LABEL[value]}
              </option>
            ))}
          </select>
          </div>
        </div>
        <div>
          <button type="button" className="btn btn-block" onClick={() => router.replace(pathname)} disabled={!hasFilters}>
            <Icon icon={refreshOutline} size={18} />
            Réinitialiser
          </button>
        </div>
      </form>

      <section aria-labelledby="results-title" aria-busy={requests.loading} style={{ marginTop: 20 }}>
        <h2 id="results-title" className="visually-hidden">
          Résultats
        </h2>
        {requests.loading ? (
          <SkeletonList />
        ) : requests.error || !requests.data ? (
          <ErrorState message={requests.error ?? "Chargement impossible"} onRetry={requests.reload} />
        ) : requests.data.content.length === 0 ? (
          <div className="card">
            <EmptyState
              icon={searchOutline}
              title={hasFilters ? "Aucune demande ne correspond à ces filtres" : "Aucune demande pour le moment"}
              text={hasFilters ? "Essayez d'élargir votre recherche." : "Soyez le premier à demander de l'aide."}
              action={!hasFilters && user?.role === "STUDENT" ? { href: "/requests/new", label: "Publier une demande", icon: addOutline } : undefined}
            />
          </div>
        ) : (
          <>
            <p className="small muted" aria-live="polite" style={{ marginBottom: 12 }}>
              {requests.data.totalElements} demande{requests.data.totalElements > 1 ? "s" : ""}
            </p>
            <ul className="list">
              {requests.data.content.map((request) => (
                <li key={request.id} className="fade-in">
                  <Link href={`/requests/${request.id}`} className="card card-link request-card">
                    <span className="icon-tile">
                      <Icon icon={subjectIcon(request.subject.name)} size={24} />
                    </span>
                    <div>
                      <div className="row-between">
                        <span className="card-title">{request.title}</span>
                        <RequestStatusBadge status={request.status} />
                      </div>
                      <p className="excerpt">{truncate(request.description, 180)}</p>
                      <div className="request-card-foot">
                        <span className="author">
                          <Avatar name={request.author.fullName} size={26} />
                          <strong>{request.ownedByCurrentUser ? "Vous" : request.author.fullName}</strong>
                        </span>
                        <span className="meta">
                          <span className="chip">{request.subject.name}</span>
                          <span className="meta-item">
                            <Icon icon={schoolOutline} size={15} />
                            {request.level}
                          </span>
                          <span className="meta-item">
                            <Icon icon={timeOutline} size={15} />
                            {truncate(request.availability, 32)}
                          </span>
                          <span className="meta-item">
                            <Icon icon={calendarOutline} size={15} />
                            {formatDate(request.createdAt)}
                          </span>
                        </span>
                      </div>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
            {requests.data.totalPages > 1 ? (
              <nav className="pagination" aria-label="Pagination">
                <button
                  type="button"
                  className="btn btn-sm"
                  disabled={page === 0}
                  onClick={() => setFilter("page", String(page - 1))}
                >
                  <Icon icon={chevronBackOutline} size={16} />
                  Précédent
                </button>
                <span className="small muted">
                  Page {page + 1} sur {requests.data.totalPages}
                </span>
                <button
                  type="button"
                  className="btn btn-sm"
                  disabled={page + 1 >= requests.data.totalPages}
                  onClick={() => setFilter("page", String(page + 1))}
                >
                  Suivant
                  <Icon icon={chevronForwardOutline} size={16} />
                </button>
              </nav>
            ) : null}
          </>
        )}
      </section>
    </>
  );
}

function truncate(text: string, max: number) {
  return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text;
}
