// Session courante pour l'interface : { user } si connecté, { user: null } sinon (jamais d'erreur 401).

import { NextResponse, type NextRequest } from "next/server";

import { TOKEN_COOKIE, backendUrl } from "@/lib/session";

export async function GET(request: NextRequest) {
  const token = request.cookies.get(TOKEN_COOKIE)?.value;
  if (!token) return NextResponse.json({ user: null });

  try {
    const upstream = await fetch(`${backendUrl()}/api/users/me`, {
      headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
      cache: "no-store",
    });
    if (upstream.ok) return NextResponse.json({ user: await upstream.json() });
    const response = NextResponse.json({ user: null });
    if (upstream.status === 401) response.cookies.delete(TOKEN_COOKIE);
    return response;
  } catch {
    return NextResponse.json({ user: null });
  }
}
