#!/usr/bin/env bash
# ISO 25010 — Sécurité : analyse dynamique (DAST) OWASP ZAP, mode « baseline » (passif, sans attaque).
#   FULL=1 qa/scripts/zap.sh   ajoute une analyse active du frontend (plus longue, ~15-30 min).
# Les rapports HTML/JSON sont dans qa/reports/zap/.
source "$(dirname "$0")/common.sh"
OUT=$(mkreport zap)
IMAGE=ghcr.io/zaproxy/zaproxy:stable
docker pull -q "$IMAGE" >/dev/null

run_zap() {
  local name=$1 script=$2 target=$3
  log "ZAP $script → $target"
  set +e
  docker run --rm --network host -v "$OUT:/zap/wrk:rw" "$IMAGE" \
    "$script" -t "$target" -r "$name.html" -J "$name.json" -w "$name.md" -I -j
  local rc=$?
  set -e
  log "ZAP $name terminé (code $rc : 0 = aucune alerte, 2 = avertissements, 1 = échecs)"
}

run_zap zap-frontend zap-baseline.py "$FRONTEND_URL/login"
run_zap zap-backend zap-baseline.py "$BACKEND_URL/api/health"
if [[ "${FULL:-0}" == 1 ]]; then
  run_zap zap-frontend-full zap-full-scan.py "$FRONTEND_URL/login"
fi
log "Rapports : $OUT"
