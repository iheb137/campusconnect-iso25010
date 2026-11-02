"use client";

import { useEffect, useState } from "react";

type State = "loading" | "ok" | "error";

/** Indique si l'API CampusConnect répond (appel public /api/health). */
export default function BackendStatus() {
  const [state, setState] = useState<State>("loading");

  useEffect(() => {
    let cancelled = false;
    fetch("/api/health", { cache: "no-store" })
      .then((response) => !cancelled && setState(response.ok ? "ok" : "error"))
      .catch(() => !cancelled && setState("error"));
    return () => {
      cancelled = true;
    };
  }, []);

  const label =
    state === "loading" ? "Vérification du serveur…" : state === "ok" ? "Serveur CampusConnect : ok" : "Serveur injoignable";
  return (
    <span className="status-line" role="status">
      <span className={`dot ${state === "ok" ? "dot-ok" : state === "error" ? "dot-error" : ""}`} aria-hidden="true" />
      {label}
    </span>
  );
}
