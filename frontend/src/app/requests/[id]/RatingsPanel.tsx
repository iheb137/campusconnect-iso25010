"use client";

import { useState } from "react";

import { arrowForwardOutline, hourglassOutline, paperPlaneOutline, star, starOutline } from "ionicons/icons";

import Avatar from "@/components/Avatar";
import { Stars } from "@/components/Badges";
import Icon from "@/components/Icon";
import Field from "@/components/Field";
import { Alert, Loading } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api, ApiError, errorMessage } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { HelpRequest, Rating, SessionRatings } from "@/lib/types";

const SCORE_LABEL = ["", "Décevant", "Moyen", "Bien", "Très bien", "Excellent"];

/**
 * Évaluations de la séance. Le serveur ne répond qu'aux participants (auteur et aidant accepté) :
 * pour les autres (403), la section n'est pas affichée.
 */
export default function RatingsPanel({ request }: { request: HelpRequest }) {
  const ratings = useApiData<SessionRatings>(`/api/requests/${request.id}/ratings`);

  if (ratings.loading && !ratings.data) return <Loading label="Chargement des évaluations…" />;
  if (ratings.status === 403 || ratings.status === 404) return null;
  if (ratings.error || !ratings.data) return <Alert kind="error">{ratings.error ?? "Évaluations indisponibles"}</Alert>;

  const { canRate, ratings: list } = ratings.data;

  return (
    <section className="card fade-in" id="evaluation" aria-labelledby="ratings-title">
      <div className="card-head">
        <h2 id="ratings-title">
          <Icon icon={starOutline} size={20} />
          Évaluation de la séance
        </h2>
        {list.length > 0 ? <span className="count">{list.length}</span> : null}
      </div>
      {request.status !== "TERMINEE" ? (
        <p className="muted row" style={{ flexWrap: "nowrap" }}>
          <Icon icon={hourglassOutline} size={18} />
          L&apos;évaluation sera possible quand l&apos;auteur aura marqué la séance comme terminée.
        </p>
      ) : null}
      {canRate ? <RatingForm requestId={request.id} onRated={ratings.reload} /> : null}
      {list.length > 0 ? (
        <ul style={{ marginTop: canRate ? 20 : 0 }}>
          {list.map((rating: Rating) => (
            <li key={rating.id} className="list-item">
              <div className="list-main">
                <Avatar name={rating.rater.fullName} size={38} />
                <div>
                  <div className="row">
                    <Stars score={rating.score} />
                    <span className="small muted row" style={{ gap: 4 }}>
                      {rating.rater.fullName}
                      <Icon icon={arrowForwardOutline} size={13} />
                      {rating.rated.fullName}
                    </span>
                  </div>
                  {rating.comment ? (
                    <p className="pre-line quote" style={{ marginTop: 4 }}>
                      « {rating.comment} »
                    </p>
                  ) : null}
                </div>
              </div>
              <span className="small muted">{formatDate(rating.createdAt)}</span>
            </li>
          ))}
        </ul>
      ) : request.status === "TERMINEE" && !canRate ? (
        <p className="muted">Merci, votre évaluation a été enregistrée.</p>
      ) : null}
    </section>
  );
}

function RatingForm({ requestId, onRated }: { requestId: number; onRated: () => void }) {
  const [score, setScore] = useState(0);
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const found: Record<string, string> = {};
    if (score < 1 || score > 5) found.score = "Choisissez une note de 1 à 5";
    if (comment.trim().length > 500) found.comment = "Le commentaire ne doit pas dépasser 500 caractères";
    setFieldErrors(found);
    setError(null);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    try {
      await api(`/api/requests/${requestId}/ratings`, {
        method: "POST",
        body: { score, comment: comment.trim() || null },
      });
      onRated();
    } catch (err) {
      setError(errorMessage(err));
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={onSubmit} noValidate>
      {error ? <Alert kind="error">{error}</Alert> : null}
      <fieldset className="field">
        <legend>Votre note *</legend>
        <div className="star-input">
          {[1, 2, 3, 4, 5].map((value) => (
            <span key={value}>
              <input
                type="radio"
                id={`score-${value}`}
                name="score"
                value={value}
                checked={score === value}
                onChange={() => setScore(value)}
              />
              <label htmlFor={`score-${value}`} data-active={value <= score} title={SCORE_LABEL[value]}>
                <Icon icon={value <= score ? star : starOutline} size={24} />
                <span className="visually-hidden">
                  {value} sur 5 — {SCORE_LABEL[value]}
                </span>
              </label>
            </span>
          ))}
        </div>
        <span className="hint" aria-live="polite">
          {score ? SCORE_LABEL[score] : "Aucune note sélectionnée"}
        </span>
        {fieldErrors.score ? <span className="error">{fieldErrors.score}</span> : null}
      </fieldset>
      <Field label="Commentaire" error={fieldErrors.comment} hint={`Facultatif (${comment.trim().length}/500)`}>
        {(props) => (
          <textarea
            {...props}
            className="textarea"
            maxLength={500}
            placeholder="Qu'avez-vous pensé de cette séance ?"
            value={comment}
            onChange={(event) => setComment(event.target.value)}
          />
        )}
      </Field>
      <div>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          <Icon icon={paperPlaneOutline} size={18} />
          {submitting ? "Envoi…" : "Envoyer mon évaluation"}
        </button>
      </div>
    </form>
  );
}
