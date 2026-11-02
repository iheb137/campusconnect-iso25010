"use client";

import { useCallback, useEffect, useState } from "react";

import { api, errorMessage } from "@/lib/api";

export type ApiData<T> = {
  data: T | null;
  error: string | null;
  status: number | null;
  loading: boolean;
  reload: () => void;
  setData: (value: T | null) => void;
};

/** Charge une ressource de l'API avec états de chargement et d'erreur. `null` = ne rien charger. */
export function useApiData<T>(path: string | null): ApiData<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<number | null>(null);
  const [loading, setLoading] = useState(path !== null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (path === null) return;
    const controller = new AbortController();
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    setError(null);
    api<T>(path, { signal: controller.signal })
      .then((result) => {
        setData(result);
        setStatus(200);
        setLoading(false);
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        setError(errorMessage(err));
        setStatus(typeof err === "object" && err && "status" in err ? Number(err.status) : null);
        setLoading(false);
      });
    return () => controller.abort();
  }, [path, version]);

  const reload = useCallback(() => setVersion((value) => value + 1), []);
  return { data, error, status, loading, reload, setData };
}
