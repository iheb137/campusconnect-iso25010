// Protection des pages selon la connexion et le rôle (contrôle de navigation).
// Les opérations restent protégées par le backend, qui vérifie le jeton et les droits à chaque appel.

import { NextResponse, type NextRequest } from "next/server";

import { TOKEN_COOKIE, homeFor, readClaims } from "@/lib/session";

const PUBLIC_PATHS = ["/", "/login", "/register"];
const GUEST_ONLY = ["/login", "/register"];
/** Pages réservées aux étudiants. */
const STUDENT_ONLY = [/^\/dashboard$/, /^\/requests\/new$/, /^\/requests\/[^/]+\/edit$/];
/** Administration : statistiques, comptes, matières, audit (administrateur seulement). */
const ADMIN_ONLY = /^\/admin\/(stats|users|subjects|audit)(\/|$)/;
/** Modération des signalements : modérateur et administrateur. */
const MODERATION = /^\/admin\/?$/;

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const token = request.cookies.get(TOKEN_COOKIE)?.value;
  const claims = readClaims(token);

  if (!claims) {
    if (PUBLIC_PATHS.includes(pathname)) {
      const response = NextResponse.next();
      if (token) response.cookies.delete(TOKEN_COOKIE);
      return response;
    }
    const login = new URL("/login", request.url);
    login.searchParams.set("next", pathname + search);
    const response = NextResponse.redirect(login);
    if (token) response.cookies.delete(TOKEN_COOKIE);
    return response;
  }

  const home = new URL(homeFor(claims.role), request.url);
  if (GUEST_ONLY.includes(pathname)) return NextResponse.redirect(home);
  if (ADMIN_ONLY.test(pathname) && claims.role !== "ADMIN") return NextResponse.redirect(home);
  if (MODERATION.test(pathname) && claims.role === "STUDENT") return NextResponse.redirect(home);
  if (claims.role !== "STUDENT" && STUDENT_ONLY.some((pattern) => pattern.test(pathname))) {
    return NextResponse.redirect(home);
  }
  return NextResponse.next();
}

export const config = {
  // Toutes les pages, sauf l'API, les fichiers Next et les fichiers statiques.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp|ico)$).*)"],
};
