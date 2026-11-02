import type { NextConfig } from "next";

// Les appels /api/... sont relayés vers le backend par src/app/api/[...path]/route.ts,
// qui ajoute le jeton de connexion (cookie httpOnly). L'adresse du backend vient de BACKEND_URL.
const nextConfig: NextConfig = {};

export default nextConfig;
