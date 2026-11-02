"use client";

import { useState } from "react";

import { checkmarkCircleOutline, handRightOutline, paperPlaneOutline } from "ionicons/icons";

import Field from "@/components/Field";
import Icon from "@/components/Icon";
import { Alert } from "@/components/States";
import { api, ApiError, errorMessage } from "@/lib/api";
import type { Offer } from "@/lib/types";

/** Proposition d'aide par un autre étudiant sur une demande ouverte. */
export default function OfferForm({ requestId }: { requestId: number }) {
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [sent, setSent] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const text = message.trim();
    if (text.length < 5 || text.length > 500) {
      setFieldError("Le message doit contenir entre 5 et 500 caractères");
      return;
    }
    setFieldError(undefined);
    setError(null);
    setSubmitting(true);
    try {
      await api<Offer>(`/api/requests/${requestId}/offers`, { method: "POST", body: { message: text } });
      setSent(true);
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldError(err.fieldErrors.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (sent) {
    return (
      <section className="card fade-in" aria-live="polite">
        <div className="row" style={{ flexWrap: "nowrap", alignItems: "flex-start" }}>
          <span className="icon-tile tone-success">
            <Icon icon={checkmarkCircleOutline} size={24} />
          </span>
          <div>
            <h2>Proposition envoyée</h2>
            <p className="muted">
              L&apos;auteur de la demande va l&apos;examiner. Suivez sa réponse dans votre tableau de bord.
            </p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="card fade-in" aria-labelledby="offer-title">
      <div className="card-head">
        <h2 id="offer-title">
          <Icon icon={handRightOutline} size={20} />
          Proposer mon aide
        </h2>
      </div>
      <form className="form" onSubmit={onSubmit} noValidate>
        {error ? <Alert kind="error">{error}</Alert> : null}
        <Field
          label="Message à l'auteur"
          error={fieldError}
          hint={`Vos disponibilités, comment vous comptez l'aider (${message.trim().length}/500).`}
          required
        >
          {(props) => (
            <textarea
              {...props}
              className="textarea"
              maxLength={500}
              placeholder="Bonjour ! Je peux vous aider mardi à 18 h, en ligne…"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
            />
          )}
        </Field>
        <div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            <Icon icon={paperPlaneOutline} size={18} />
            {submitting ? "Envoi…" : "Envoyer ma proposition"}
          </button>
        </div>
      </form>
    </section>
  );
}
