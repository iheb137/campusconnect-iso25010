#!/usr/bin/env bash
# ISO 25010 — Efficacité de performance : test de charge k6 (20 utilisateurs, p95 < 500 ms).
#   qa/scripts/k6.sh            test de charge (seuils bloquants)
#   qa/scripts/k6.sh stress     montée à 100 utilisateurs (informatif)
# Conseil : DEMO_REQUESTS=2000 dans docker.env pour mesurer sur un volume réaliste.
source "$(dirname "$0")/common.sh"
OUT=$(mkreport k6)
script=${1:-load}
[[ $script == stress ]] && file=stress-test.js || file=load-test.js
log "k6 $file → $BACKEND_URL"
set +e
docker run --rm -i --network host -v "$ROOT/qa:/qa" -e BASE_URL="$BACKEND_URL" -e VUS="${VUS:-20}" -e DURATION="${DURATION:-2m}" \
  -e K6_WEB_DASHBOARD=true -e K6_WEB_DASHBOARD_EXPORT="/qa/reports/k6/$script-dashboard.html" \
  grafana/k6:0.57.0 run --summary-export="/qa/reports/k6/$script-summary.json" "/qa/k6/$file" 2>&1 | tee "$OUT/$script-output.txt"
rc=${PIPESTATUS[0]}
set -e
log "k6 terminé (code $rc : 0 = seuils respectés, 99 = seuil dépassé). Rapports : $OUT"
exit $rc
