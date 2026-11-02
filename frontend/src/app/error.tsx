"use client";

import { ErrorState } from "@/components/States";

export default function Error({ error, reset }: { error: Error; reset: () => void }) {
  return <ErrorState message={error.message || "Erreur inattendue"} onRetry={reset} />;
}
