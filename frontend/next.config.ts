import type { NextConfig } from "next";

// Les appels /api/... sont relayés vers le backend par src/app/api/[...path]/route.ts,
// qui ajoute le jeton de connexion (cookie httpOnly). L'adresse du backend vient de BACKEND_URL.

const isDev = process.env.NODE_ENV !== "production";

// Politique de sécurité du contenu (OWASP). Next.js injecte des scripts en ligne pour l'hydratation :
// 'unsafe-inline' reste nécessaire sans nonce (écart documenté dans le rapport de conformité).
const contentSecurityPolicy = [
  "default-src 'self'",
  `script-src 'self' 'unsafe-inline'${isDev ? " 'unsafe-eval'" : ""}`,
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  `connect-src 'self'${isDev ? " ws:" : ""}`,
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
].join("; ");

const securityHeaders = [
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(), payment=()" },
  { key: "Cross-Origin-Opener-Policy", value: "same-origin" },
];

const nextConfig: NextConfig = {
  // Image Docker légère (voir frontend/Dockerfile) ; « next start » reste utilisé en local.
  output: process.env.NEXT_OUTPUT === "standalone" ? "standalone" : undefined,
  poweredByHeader: false,
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

export default nextConfig;
