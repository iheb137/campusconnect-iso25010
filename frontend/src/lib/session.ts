// Lecture du jeton côté serveur Next (proxy et routes). Le jeton n'est PAS vérifié ici :
// cela sert seulement à orienter la navigation. Le backend Spring vérifie la signature à chaque appel.

import type { Role } from "./types";

export const TOKEN_COOKIE = "cc_token";

export type TokenClaims = { sub: string; role: Role; exp: number };

export function readClaims(token: string | undefined): TokenClaims | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const claims = JSON.parse(atob(padded)) as Partial<TokenClaims>;
    if (typeof claims.exp !== "number" || (claims.role !== "STUDENT" && claims.role !== "ADMIN")) {
      return null;
    }
    if (claims.exp * 1000 <= Date.now()) return null;
    return claims as TokenClaims;
  } catch {
    return null;
  }
}

export function backendUrl(): string {
  return process.env.BACKEND_URL ?? "http://localhost:8280";
}

export function homeFor(role: Role): string {
  return role === "ADMIN" ? "/admin" : "/dashboard";
}
