#!/usr/bin/env bash
# Fonctions partagées par les scripts QA (à lancer depuis WSL, à la racine du dépôt).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPORTS="$ROOT/qa/reports"
FRONTEND_URL="${FRONTEND_URL:-http://localhost:3080}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8380}"
cd "$ROOT"

log() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

require_env_file() {
  if [[ ! -f docker.env ]]; then
    echo "docker.env introuvable : copiez docker.env.example en docker.env et renseignez-le." >&2
    exit 1
  fi
}

compose() { docker compose --env-file docker.env "$@"; }

# Attend qu'une URL réponde 200 ; renvoie le nombre de secondes écoulées.
wait_for() {
  local url=$1 timeout=${2:-180} start=$SECONDS
  until curl -fsS -o /dev/null "$url" 2>/dev/null; do
    if (( SECONDS - start > timeout )); then echo "Délai dépassé pour $url" >&2; return 1; fi
    sleep 1
  done
  echo $(( SECONDS - start ))
}

mkreport() { mkdir -p "$REPORTS/$1"; chmod 777 "$REPORTS/$1" 2>/dev/null || true; echo "$REPORTS/$1"; }
