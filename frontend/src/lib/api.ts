// Client HTTP du navigateur. Tous les appels passent par /api/... (route Next qui ajoute le jeton).

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type Options = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  signal?: AbortSignal;
};

export async function api<T>(path: string, options: Options = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      method: options.method ?? "GET",
      headers: options.body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal: options.signal,
      cache: "no-store",
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new ApiError(0, "Impossible de joindre le serveur. Vérifiez votre connexion.");
  }

  const text = await response.text();
  const data = text ? safeJson(text) : null;

  if (!response.ok) {
    const message =
      (data && typeof data.message === "string" && data.message) || defaultMessage(response.status);
    const fieldErrors = (data && typeof data.fieldErrors === "object" && data.fieldErrors) || {};
    if (response.status === 401 && typeof window !== "undefined" && !path.startsWith("/api/auth/")) {
      // Session expirée : retour à la connexion en gardant la page demandée.
      const next = encodeURIComponent(window.location.pathname + window.location.search);
      // Hors composant React : rechargement complet volontaire pour repartir d'une session propre.
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.assign(`/login?next=${next}&expired=1`);
    }
    throw new ApiError(response.status, message, fieldErrors);
  }
  return data as T;
}

function safeJson(text: string) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function defaultMessage(status: number): string {
  switch (status) {
    case 400:
      return "Les données envoyées sont invalides";
    case 401:
      return "Authentification requise";
    case 403:
      return "Action non autorisée";
    case 404:
      return "Ressource introuvable";
    case 409:
      return "Action impossible dans l'état actuel";
    case 502:
      return "Le serveur est indisponible";
    default:
      return "Une erreur inattendue est survenue";
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return "Une erreur inattendue est survenue";
}
