# syntax=docker/dockerfile:1
# ---------- Dépendances ----------
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# ---------- Construction (sortie « standalone » de Next.js) ----------
FROM node:22-alpine AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1 NEXT_OUTPUT=standalone
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---------- Exécution ----------
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
RUN addgroup -S campus && adduser -S campus -G campus
COPY --from=build --chown=campus:campus /app/.next/standalone ./
COPY --from=build --chown=campus:campus /app/.next/static ./.next/static
COPY --from=build --chown=campus:campus /app/public ./public
USER campus
EXPOSE 3000
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD wget -qO- "http://127.0.0.1:3000/login" >/dev/null || exit 1
CMD ["node", "server.js"]
