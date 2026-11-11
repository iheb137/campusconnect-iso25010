#!/usr/bin/env bash
# ISO 25010 — Fiabilité (disponibilité, tolérance aux fautes, récupérabilité) sur la pile Docker.
#  REL-07 : base arrêtée → l'API répond 503 rapidement (pas de blocage), sans trace technique.
#  REL-08 : base redémarrée → l'API se rétablit seule, sans redémarrer le backend (temps de reprise mesuré).
#  REL-09 : backend arrêté brutalement (kill) → Docker le redémarre, temps de reprise mesuré, données intactes.
source "$(dirname "$0")/common.sh"
require_env_file
OUT=$(mkreport reliability)
REPORT="$OUT/reliability.md"
results=()
record() { results+=("| $1 | $2 | $3 | $4 |"); log "$1 : $4 — $3"; }

status_of() { curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$1"; }
time_of() { curl -s -o /dev/null -w '%{time_total}' --max-time 10 "$1"; }

log "État initial…"
[[ $(status_of "$BACKEND_URL/api/health") == 200 ]] || { echo "Lancez d'abord qa/scripts/docker-up.sh" >&2; exit 1; }
token=$(curl -s -X POST "$BACKEND_URL/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$(grep ^ADMIN_EMAIL docker.env | cut -d= -f2)\",\"password\":\"$(grep ^ADMIN_PASSWORD docker.env | cut -d= -f2)\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[[ -n $token ]] || { echo "Connexion administrateur impossible (ADMIN_EMAIL/ADMIN_PASSWORD de docker.env)" >&2; exit 1; }
before=$(curl -s "$BACKEND_URL/api/admin/stats" -H "Authorization: Bearer $token" | sed -n 's/.*"usersByRole":{\([^}]*\)}.*/\1/p')

# ----- REL-07 : panne de la base
log "Arrêt de PostgreSQL…"
compose stop db >/dev/null
sleep 2
h=$(status_of "$BACKEND_URL/api/health"); ht=$(time_of "$BACKEND_URL/api/health")
api_body=$(curl -s --max-time 15 "$BACKEND_URL/api/requests" -H "Authorization: Bearer $token")
api=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$BACKEND_URL/api/requests" -H "Authorization: Bearer $token")
apit=$(curl -s -o /dev/null -w '%{time_total}' --max-time 15 "$BACKEND_URL/api/requests" -H "Authorization: Bearer $token")
verdict=$([[ $h == 503 && $api == 503 ]] && awk "BEGIN{exit !($apit < 5)}" && ! grep -qi 'exception' <<<"$api_body" && echo CONFORME || echo NON CONFORME)
record "REL-07" "Base arrêtée" "health=$h (${ht}s), /api/requests=$api (${apit}s), corps : $(tr -d '\n' <<<"$api_body" | cut -c1-120)" "$verdict"

# ----- REL-08 : reprise automatique
log "Redémarrage de PostgreSQL…"
start=$SECONDS
compose start db >/dev/null
until [[ $(status_of "$BACKEND_URL/api/health") == 200 ]]; do sleep 1; (( SECONDS - start > 120 )) && break; done
recovery=$(( SECONDS - start ))
api=$(curl -s -o /dev/null -w '%{http_code}' "$BACKEND_URL/api/subjects" -H "Authorization: Bearer $token")
verdict=$([[ $api == 200 && $recovery -le 60 ]] && echo CONFORME || echo NON CONFORME)
record "REL-08" "Base redémarrée (backend non redémarré)" "API rétablie en ${recovery}s, /api/subjects=$api" "$verdict"

# ----- REL-09 : arrêt brutal du backend
log "Arrêt brutal du backend (kill)…"
compose kill -s SIGKILL backend >/dev/null
start=$SECONDS
compose up -d backend >/dev/null
until [[ $(status_of "$BACKEND_URL/api/health") == 200 ]]; do sleep 1; (( SECONDS - start > 180 )) && break; done
restart=$(( SECONDS - start ))
token=$(curl -s -X POST "$BACKEND_URL/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$(grep ^ADMIN_EMAIL docker.env | cut -d= -f2)\",\"password\":\"$(grep ^ADMIN_PASSWORD docker.env | cut -d= -f2)\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
after=$(curl -s "$BACKEND_URL/api/admin/stats" -H "Authorization: Bearer $token" | sed -n 's/.*"usersByRole":{\([^}]*\)}.*/\1/p')
verdict=$([[ $restart -le 120 && "$before" == "$after" && -n $after ]] && echo CONFORME || echo NON CONFORME)
record "REL-09" "Backend tué puis relancé" "service rétabli en ${restart}s ; comptes avant {$before} / après {$after}" "$verdict"

{
  echo "# Rapport de fiabilité — $(date '+%Y-%m-%d %H:%M')"
  echo
  echo "Pile Docker Compose : $BACKEND_URL"
  echo
  echo "| Exigence | Scénario | Observation | Verdict |"
  echo "|---|---|---|---|"
  printf '%s\n' "${results[@]}"
} > "$REPORT"
log "Rapport : $REPORT"
