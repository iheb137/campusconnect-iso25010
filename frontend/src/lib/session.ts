// Lecture du jeton côté serveur Next (proxy et routes). Le jeton n'est PAS vérifié ici :
// cela sert seulement à orienter la navigation. Le backend Spring vérifie la signature à chaque appel.

import type { Role } from "./types";

export const TOKEN_COOKIE = "cc_token";

const ROLES: Role[] = ["STUDENT", "MODERATOR", "ADMIN"];

export type TokenClaims = { sub: string; role: Role; exp: number };

export function readClaims(token: string | undefined): TokenClaims | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const claims = JSON.parse(atob(padded)) as Partial<TokenClaims>;
    if (typeof claims.exp !== "number" || !ROLES.includes(claims.role as Role)) {
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

/** Page d'accueil de chaque rôle après connexion. */
export function homeFor(role: Role): string {
  if (role === "ADMIN") return "/admin/stats";
  if (role === "MODERATOR") return "/admin";
  return "/dashboard";
}

export const ROLE_LABEL: Record<Role, string> = {
  STUDENT: "Étudiant",
  MODERATOR: "Modérateur",
  ADMIN: "Administrateur",
};
