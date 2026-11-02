// Relais vers l'API Spring Boot : ajoute le jeton (cookie httpOnly) dans l'en-tête Authorization.
// Le navigateur ne voit jamais le jeton, et le frontend n'a pas de problème de CORS.

import { NextResponse, type NextRequest } from "next/server";

import { TOKEN_COOKIE, backendUrl } from "@/lib/session";

async function forward(request: NextRequest, ctx: { params: Promise<{ path: string[] }> }) {
  const { path } = await ctx.params;
  const target = `${backendUrl()}/api/${path.map(encodeURIComponent).join("/")}${request.nextUrl.search}`;

  const headers = new Headers();
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("Content-Type", contentType);
  headers.set("Accept", "application/json");
  // Routes publiques : jamais de jeton. Spring refuse un jeton invalide même sur une route publique,
  // et un ancien cookie (clé JWT changée) bloquerait alors l'inscription.
  const isPublic = path[0] === "auth" || path[0] === "health";
  const token = isPublic ? undefined : request.cookies.get(TOKEN_COOKIE)?.value;
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const hasBody = request.method !== "GET" && request.method !== "HEAD";
  let upstream: Response;
  try {
    upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? await request.text() : undefined,
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { status: 502, error: "Bad Gateway", message: "Le serveur CampusConnect est injoignable", fieldErrors: {} },
      { status: 502 },
    );
  }

  const body = upstream.status === 204 ? null : await upstream.text();
  const response = new NextResponse(body, {
    status: upstream.status,
    headers: { "Content-Type": upstream.headers.get("content-type") ?? "application/json" },
  });
  if (upstream.status === 401 && token) {
    // Jeton expiré ou invalide : on l'efface pour que le proxy renvoie vers la connexion.
    response.cookies.delete(TOKEN_COOKIE);
  }
  return response;
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;
