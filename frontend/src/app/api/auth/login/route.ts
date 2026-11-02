// Connexion : appelle le backend puis stocke le jeton dans un cookie httpOnly (inaccessible au JavaScript).

import { NextResponse, type NextRequest } from "next/server";

import { TOKEN_COOKIE, backendUrl } from "@/lib/session";

type BackendLogin = { token: string; expiresInSeconds: number; user: unknown };

export async function POST(request: NextRequest) {
  let upstream: Response;
  try {
    upstream = await fetch(`${backendUrl()}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: await request.text(),
      cache: "no-store",
    });
  } catch {
    return NextResponse.json(
      { status: 502, error: "Bad Gateway", message: "Le serveur CampusConnect est injoignable", fieldErrors: {} },
      { status: 502 },
    );
  }

  if (!upstream.ok) {
    return new NextResponse(await upstream.text(), {
      status: upstream.status,
      headers: { "Content-Type": upstream.headers.get("content-type") ?? "application/json" },
    });
  }

  const data = (await upstream.json()) as BackendLogin;
  const response = NextResponse.json({ user: data.user });
  response.cookies.set(TOKEN_COOKIE, data.token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: data.expiresInSeconds,
  });
  return response;
}
