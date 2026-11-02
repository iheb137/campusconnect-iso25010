"use client";

import { useState } from "react";

import { checkmarkCircleOutline, flagOutline } from "ionicons/icons";

import Field from "@/components/Field";
import Icon from "@/components/Icon";
import { Alert } from "@/components/States";
import { api, ApiError, errorMessage } from "@/lib/api";

/** Signalement d'une publication à l'administrateur. */
export default function ReportForm({ requestId }: { requestId: number }) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const text = reason.trim();
    if (text.length < 5 || text.length > 500) {
      setFieldError("Le motif doit contenir entre 5 et 500 caractères");
      return;
    }
    setFieldError(undefined);
    setError(null);
    setSubmitting(true);
    try {
      await api(`/api/requests/${requestId}/reports`, { method: "POST", body: { reason: text } });
      setDone(true);
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldError(err.fieldErrors.reason);
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <section className="card fade-in" aria-live="polite">
        <p className="small row" style={{ flexWrap: "nowrap", color: "var(--success)" }}>
          <Icon icon={checkmarkCircleOutline} size={20} />
          Merci, la publication a été signalée à l&apos;administrateur.
        </p>
      </section>
    );
  }

  return (
    <section className="card">
      {!open ? (
        <button
          type="button"
          className="btn btn-ghost btn-sm btn-block"
          aria-expanded={false}
          aria-controls="report-form"
          onClick={() => setOpen(true)}
        >
          <Icon icon={flagOutline} size={17} />
          Signaler cette publication
        </button>
      ) : (
        <form id="report-form" className="form" onSubmit={onSubmit} noValidate>
          <h2 className="row" style={{ marginBottom: 0 }}>
            <Icon icon={flagOutline} size={20} />
            Signaler
          </h2>
          {error ? <Alert kind="error">{error}</Alert> : null}
          <Field label="Motif" error={fieldError} hint="Entre 5 et 500 caractères" required>
            {(props) => (
              <textarea
                {...props}
                className="textarea"
                style={{ minHeight: 90 }}
                maxLength={500}
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                autoFocus
              />
            )}
          </Field>
          <div className="row">
            <button type="submit" className="btn btn-danger btn-sm" disabled={submitting}>
              <Icon icon={flagOutline} size={16} />
              {submitting ? "Envoi…" : "Envoyer le signalement"}
            </button>
            <button type="button" className="btn btn-sm" onClick={() => setOpen(false)} disabled={submitting}>
              Annuler
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
