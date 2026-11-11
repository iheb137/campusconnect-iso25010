#!/usr/bin/env bash
# ISO 25010 — Portabilité (installabilité, adaptabilité).
# Installation depuis zéro (images reconstruites, base vide), mesure du temps jusqu'à « prêt »,
# puis vérification fonctionnelle (collection Newman) sur cet environnement Linux conteneurisé.
source "$(dirname "$0")/common.sh"
require_env_file
OUT=$(mkreport portability)
REPORT="$OUT/portability.md"

log "Nettoyage complet (conteneurs + volume de base)…"
compose down -v --remove-orphans >/dev/null 2>&1 || true

start=$SECONDS
log "Construction des images…"
compose build 2>&1 | tail -n 5
build_time=$(( SECONDS - start ))

start=$SECONDS
compose up -d
t_back=$(wait_for "$BACKEND_URL/api/health" 240)
t_front=$(wait_for "$FRONTEND_URL/login" 120)
up_time=$(( SECONDS - start ))

log "Vérification fonctionnelle (Newman) sur la pile Docker…"
set +e
npx --yes newman@6.2.3 run qa/newman/campusconnect.postman_collection.json --env-var "baseUrl=$BACKEND_URL" \
  -r cli,junit --reporter-junit-export "$OUT/newman-docker.xml" > "$OUT/newman-docker.txt" 2>&1
newman_rc=$?
set -e

{
  echo "# Rapport de portabilité — $(date '+%Y-%m-%d %H:%M')"
  echo
  echo "| Mesure | Valeur |"
  echo "|---|---|"
  echo "| Système hôte | $(uname -srm) |"
  echo "| Docker | $(docker version --format '{{.Server.Version}}') |"
  echo "| Docker Compose | $(docker compose version --short) |"
  echo "| Construction des images (depuis zéro) | ${build_time} s |"
  echo "| Démarrage jusqu'à « prêt » (backend / frontend) | ${up_time} s (${t_back} s / ${t_front} s) |"
  echo "| Collection Newman sur la pile Docker | $([[ $newman_rc -eq 0 ]] && echo 'RÉUSSIE' || echo "ÉCHEC (code $newman_rc)") |"
  echo
  echo "## Images"
  echo '```'
  docker image ls --format '{{.Repository}}:{{.Tag}}  {{.Size}}' | grep -E 'campusconnect|postgres:16' || true
  echo '```'
  echo
  echo "## Conteneurs"
  echo '```'
  compose ps
  echo '```'
  echo
  echo "## Résumé Newman"
  echo '```'
  tail -n 22 "$OUT/newman-docker.txt"
  echo '```'
} > "$REPORT"
log "Rapport : $REPORT"
exit $newman_rc
