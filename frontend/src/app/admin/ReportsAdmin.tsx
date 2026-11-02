"use client";

import { useState } from "react";

import {
  arrowUndoOutline,
  calendarOutline,
  closeCircleOutline,
  eyeOffOutline,
  flagOutline,
  shieldCheckmarkOutline,
  warningOutline,
} from "ionicons/icons";
import Link from "next/link";

import Avatar from "@/components/Avatar";
import { ReportStatusBadge, RequestStatusBadge } from "@/components/Badges";
import Icon from "@/components/Icon";
import PageHead from "@/components/PageHead";
import { Alert, EmptyState, ErrorState, SkeletonList } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api, errorMessage } from "@/lib/api";
import { formatDate, formatDateTime } from "@/lib/format";
import type { ReportedRequest } from "@/lib/types";

type Action = "hide" | "restore" | "dismiss";

const CONFIRM: Record<Action, string> = {
  hide: "Masquer cette publication ? Elle disparaîtra pour les étudiants et les signalements seront marqués traités.",
  restore: "Rendre cette publication de nouveau visible ?",
  dismiss: "Rejeter les signalements en attente ? La publication restera visible.",
};

const DONE: Record<Action, string> = {
  hide: "Publication masquée.",
  restore: "Publication rétablie.",
  dismiss: "Signalements rejetés.",
};

export default function ReportsAdmin() {
  const [pendingOnly, setPendingOnly] = useState(true);
  const reports = useApiData<ReportedRequest[]>(`/api/admin/reports?pendingOnly=${pendingOnly}`);
  const [busy, setBusy] = useState<number | null>(null);
  const [notice, setNotice] = useState<{ kind: "error" | "success"; text: string } | null>(null);

  async function act(item: ReportedRequest, action: Action) {
    if (!window.confirm(CONFIRM[action])) return;
    setBusy(item.requestId);
    setNotice(null);
    try {
      const updated = await api<ReportedRequest>(`/api/admin/requests/${item.requestId}/${action}`, { method: "POST" });
      const list = (reports.data ?? [])
        .map((current) => (current.requestId === updated.requestId ? updated : current))
        .filter((current) => !pendingOnly || current.pendingReports > 0);
      reports.setData(list);
      setNotice({ kind: "success", text: `${DONE[action]} « ${updated.title} »` });
    } catch (error) {
      setNotice({ kind: "error", text: errorMessage(error) });
      reports.reload();
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <PageHead
        icon={flagOutline}
        title="Publications signalées"
        subtitle="Examinez les signalements, puis masquez la publication ou rejetez les signalements."
      >
        <div className="toolbar-group" role="group" aria-label="Filtre des signalements">
          <button type="button" className="btn btn-sm" aria-pressed={pendingOnly} onClick={() => setPendingOnly(true)}>
            <Icon icon={warningOutline} size={16} />
            À traiter
          </button>
          <button type="button" className="btn btn-sm" aria-pressed={!pendingOnly} onClick={() => setPendingOnly(false)}>
            <Icon icon={calendarOutline} size={16} />
            Historique complet
          </button>
        </div>
      </PageHead>

      {notice ? (
        <div style={{ marginBottom: 16 }}>
          <Alert kind={notice.kind}>{notice.text}</Alert>
        </div>
      ) : null}

      {reports.loading && !reports.data ? (
        <SkeletonList />
      ) : reports.error || !reports.data ? (
        <ErrorState message={reports.error ?? "Chargement impossible"} onRetry={reports.reload} />
      ) : reports.data.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={shieldCheckmarkOutline}
            title={pendingOnly ? "Aucun signalement à traiter" : "Aucune publication n'a été signalée"}
            text={pendingOnly ? "Tout est en ordre." : undefined}
          />
        </div>
      ) : (
        <ul className="list">
          {reports.data.map((item) => (
            <li key={item.requestId} className="card fade-in">
              <article aria-labelledby={`reported-${item.requestId}`}>
                <div className="row" style={{ alignItems: "flex-start", flexWrap: "nowrap", gap: 14 }}>
                  <span className={`icon-tile ${item.pendingReports > 0 ? "tone-warning" : "tone-success"}`}>
                    <Icon icon={item.pendingReports > 0 ? warningOutline : shieldCheckmarkOutline} size={24} />
                  </span>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div className="row-between">
                      <h2 id={`reported-${item.requestId}`} className="card-title">
                        {item.hidden ? (
                          item.title
                        ) : (
                          <Link href={`/requests/${item.requestId}`} className="card-title">
                            {item.title}
                          </Link>
                        )}
                      </h2>
                      <div className="row">
                        {item.hidden ? (
                          <span className="badge badge-danger">
                            <Icon icon={eyeOffOutline} size={14} />
                            Masquée
                          </span>
                        ) : null}
                        <RequestStatusBadge status={item.status} />
                      </div>
                    </div>
                    <div className="meta" style={{ margin: "6px 0 12px" }}>
                      <span className="chip">{item.subject.name}</span>
                      <span className="author">
                        <Avatar name={item.author.fullName} size={22} />
                        {item.author.fullName}
                      </span>
                      <span className="meta-item">
                        <Icon icon={calendarOutline} size={14} />
                        publiée le {formatDate(item.createdAt)}
                      </span>
                      <span className="meta-item">
                        <Icon icon={flagOutline} size={14} />
                        {item.pendingReports} signalement(s) en attente sur {item.reports.length}
                      </span>
                    </div>
                  </div>
                </div>
                <div className="desc-block">
                  <p className="pre-line small">{item.description}</p>
                </div>

                <ul className="report-list" aria-label="Signalements">
                  {item.reports.map((report) => (
                    <li key={report.id} className="report" data-status={report.status}>
                      <Avatar name={report.reporter.fullName} size={32} />
                      <div style={{ minWidth: 0, flex: 1 }}>
                        <div className="row-between">
                          <p className="pre-line quote">« {report.reason} »</p>
                          <ReportStatusBadge status={report.status} />
                        </div>
                        <span className="small muted">
                          {report.reporter.fullName} · {formatDateTime(report.createdAt)}
                        </span>
                      </div>
                    </li>
                  ))}
                </ul>

                <div className="form-actions" style={{ marginTop: 14 }}>
                  {item.hidden ? (
                    <button
                      type="button"
                      className="btn btn-sm"
                      disabled={busy !== null}
                      onClick={() => act(item, "restore")}
                    >
                      <Icon icon={arrowUndoOutline} size={16} />
                      Rétablir la publication
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      disabled={busy !== null}
                      onClick={() => act(item, "hide")}
                    >
                      <Icon icon={eyeOffOutline} size={16} />
                      Masquer la publication
                    </button>
                  )}
                  {item.pendingReports > 0 ? (
                    <button
                      type="button"
                      className="btn btn-sm"
                      disabled={busy !== null}
                      onClick={() => act(item, "dismiss")}
                    >
                      <Icon icon={closeCircleOutline} size={16} />
                      Rejeter les signalements
                    </button>
                  ) : null}
                </div>
              </article>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
