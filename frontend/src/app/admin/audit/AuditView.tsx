"use client";

import { calendarOutline, filterOutline, mailOutline, receiptOutline, refreshOutline } from "ionicons/icons";
import { useEffect, useState } from "react";

import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import Pagination from "@/components/Pagination";
import { EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { AUDIT_LABEL, auditTone } from "@/lib/audit";
import { formatDateTime } from "@/lib/format";
import type { AuditAction, AuditEntry, Page } from "@/lib/types";

const ACTIONS = Object.keys(AUDIT_LABEL) as AuditAction[];

export default function AuditView() {
  const [action, setAction] = useState<AuditAction | "">("");
  const [emailInput, setEmailInput] = useState("");
  const [email, setEmail] = useState("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timer = setTimeout(() => {
      setEmail(emailInput.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [emailInput]);

  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (action) params.set("action", action);
  if (email) params.set("email", email);
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  const entries = useApiData<Page<AuditEntry>>(`/api/admin/audit?${params.toString()}`);
  const filtered = Boolean(action || email || from || to);

  function reset() {
    setAction("");
    setEmailInput("");
    setEmail("");
    setFrom("");
    setTo("");
    setPage(0);
  }

  return (
    <>
      <PageHead icon={receiptOutline} title="Journal d'audit" subtitle="Les actions sensibles : qui a fait quoi, sur quoi, et quand." />

      <form className="card filters" role="search" aria-label="Filtrer le journal" onSubmit={(event) => event.preventDefault()}>
        <div className="field">
          <label htmlFor="audit-action">Action</label>
          <div className="input-wrap">
            <Icon icon={filterOutline} size={18} />
            <select
              id="audit-action"
              className="select"
              value={action}
              onChange={(e) => {
                setAction(e.target.value as AuditAction | "");
                setPage(0);
              }}
            >
              <option value="">Toutes</option>
              {ACTIONS.map((value) => (
                <option key={value} value={value}>
                  {AUDIT_LABEL[value]}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="field">
          <label htmlFor="audit-email">Email de l&apos;acteur</label>
          <div className="input-wrap">
            <Icon icon={mailOutline} size={18} />
            <input id="audit-email" className="input" value={emailInput} onChange={(e) => setEmailInput(e.target.value)} placeholder="contient…" />
          </div>
        </div>
        <div className="field">
          <label htmlFor="audit-from">Du</label>
          <div className="input-wrap">
            <Icon icon={calendarOutline} size={18} />
            <input id="audit-from" type="date" className="input" value={from} max={to || undefined} onChange={(e) => { setFrom(e.target.value); setPage(0); }} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="audit-to">Au</label>
          <div className="input-wrap">
            <Icon icon={calendarOutline} size={18} />
            <input id="audit-to" type="date" className="input" value={to} min={from || undefined} onChange={(e) => { setTo(e.target.value); setPage(0); }} />
          </div>
        </div>
        <div>
          <button type="button" className="btn btn-block" onClick={reset} disabled={!filtered}>
            <Icon icon={refreshOutline} size={18} />
            Réinitialiser
          </button>
        </div>
      </form>

      <div style={{ marginTop: 20 }}>
        {entries.loading && !entries.data ? (
          <SkeletonList />
        ) : entries.error || !entries.data ? (
          <ErrorState message={entries.error ?? "Chargement impossible"} onRetry={entries.reload} />
        ) : entries.data.content.length === 0 ? (
          <div className="card">
            <EmptyState icon={receiptOutline} title="Aucune entrée" text={filtered ? "Aucune action ne correspond à ces filtres." : undefined} />
          </div>
        ) : (
          <>
            <p className="small muted" style={{ marginBottom: 10 }} aria-live="polite">
              {entries.data.totalElements} entrée(s)
            </p>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th scope="col">Date</th>
                    <th scope="col">Action</th>
                    <th scope="col">Acteur</th>
                    <th scope="col">Cible</th>
                    <th scope="col">Détails</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.data.content.map((entry) => (
                    <tr key={entry.id}>
                      <td className="small" style={{ whiteSpace: "nowrap" }}>
                        {formatDateTime(entry.createdAt)}
                      </td>
                      <td>
                        <span className={`badge ${auditTone(entry.action)}`}>{AUDIT_LABEL[entry.action]}</span>
                      </td>
                      <td className="small">{entry.actorEmail ?? <span className="muted">inconnu</span>}</td>
                      <td className="small muted" style={{ whiteSpace: "nowrap" }}>
                        {entry.targetType ? `${entry.targetType} #${entry.targetId}` : "—"}
                      </td>
                      <td className="small" style={{ maxWidth: 320, overflowWrap: "anywhere" }}>
                        {entry.details ?? ""}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={entries.data.totalPages} onChange={setPage} />
          </>
        )}
      </div>
    </>
  );
}
