"use client";

import { useState } from "react";

import { checkmarkOutline, closeOutline, handRightOutline, mailOpenOutline } from "ionicons/icons";

import Avatar from "@/components/Avatar";
import { OfferStatusBadge } from "@/components/Badges";
import Icon from "@/components/Icon";
import { Alert, EmptyState, ErrorState, Loading } from "@/components/States";
import { useApiData } from "@/components/useApiData";
import { api, errorMessage } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { HelpRequest, Offer } from "@/lib/types";

/** Propositions reçues, visibles uniquement par l'auteur, avec acceptation et refus. */
export default function OffersPanel({
  request,
  onRequestChanged,
}: {
  request: HelpRequest;
  onRequestChanged: () => void;
}) {
  const offers = useApiData<Offer[]>(`/api/requests/${request.id}/offers`);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function act(offer: Offer, action: "accept" | "reject") {
    const question =
      action === "accept"
        ? `Accepter l'aide de ${offer.helper.fullName} ? Les autres propositions seront refusées.`
        : `Refuser la proposition de ${offer.helper.fullName} ?`;
    if (!window.confirm(question)) return;
    setBusyId(offer.id);
    setError(null);
    try {
      await api<Offer>(`/api/offers/${offer.id}/${action}`, { method: "POST" });
      offers.reload();
      if (action === "accept") onRequestChanged();
    } catch (err) {
      setError(errorMessage(err));
      offers.reload();
      onRequestChanged();
    } finally {
      setBusyId(null);
    }
  }

  const canDecide = request.status === "OUVERTE";

  return (
    <section className="card fade-in" aria-labelledby="offers-title">
      <div className="card-head">
        <h2 id="offers-title">
          <Icon icon={handRightOutline} size={20} />
          Propositions d&apos;aide
        </h2>
        {offers.data ? <span className="count">{offers.data.length}</span> : null}
      </div>
      {error ? <Alert kind="error">{error}</Alert> : null}
      {offers.loading && !offers.data ? (
        <Loading label="Chargement des propositions…" />
      ) : offers.error || !offers.data ? (
        <ErrorState message={offers.error ?? "Chargement impossible"} onRetry={offers.reload} />
      ) : offers.data.length === 0 ? (
        <EmptyState
          icon={mailOpenOutline}
          title="Aucune proposition pour l'instant"
          text="Les étudiants qui peuvent vous aider apparaîtront ici."
        />
      ) : (
        <ul>
          {offers.data.map((offer) => (
            <li key={offer.id} className="list-item">
              <div className="list-main">
                <Avatar name={offer.helper.fullName} size={40} />
                <div>
                  <div className="row">
                    <strong>{offer.helper.fullName}</strong>
                    <OfferStatusBadge status={offer.status} />
                  </div>
                  <p className="pre-line" style={{ margin: "6px 0" }}>
                    {offer.message}
                  </p>
                  <span className="small muted">{formatDateTime(offer.createdAt)}</span>
                </div>
              </div>
              {canDecide && offer.status === "EN_ATTENTE" ? (
                <div className="row">
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    disabled={busyId !== null}
                    onClick={() => act(offer, "accept")}
                    aria-label={`Accepter la proposition de ${offer.helper.fullName}`}
                  >
                    <Icon icon={checkmarkOutline} size={16} />
                    Accepter
                  </button>
                  <button
                    type="button"
                    className="btn btn-sm"
                    disabled={busyId !== null}
                    onClick={() => act(offer, "reject")}
                    aria-label={`Refuser la proposition de ${offer.helper.fullName}`}
                  >
                    <Icon icon={closeOutline} size={16} />
                    Refuser
                  </button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
