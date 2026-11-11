#!/usr/bin/env bash
# ISO 25010 — Sécurité : analyse de composition logicielle (SCA), secrets et configuration (Trivy).
#  - dépôt : dépendances Maven (pom.xml) et npm (package-lock.json), secrets, Dockerfiles
#  - images Docker construites par docker compose (OS + bibliothèques)
source "$(dirname "$0")/common.sh"
OUT=$(mkreport trivy)
IMAGE=aquasec/trivy:0.58.0
trivy() {
  docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache/ \
    -v "$ROOT:/src:ro" -v "$OUT:/out" "$IMAGE" "$@"
}

log "Analyse du dépôt (dépendances, secrets, Dockerfiles)…"
trivy fs --scanners vuln,secret,misconfig --skip-dirs /src/frontend/node_modules --skip-dirs /src/node_modules \
  --format table --output /out/trivy-fs.txt /src
trivy fs --scanners vuln,secret,misconfig --skip-dirs /src/frontend/node_modules --skip-dirs /src/node_modules \
  --format json --output /out/trivy-fs.json /src

for image in campusconnect-backend:latest campusconnect-frontend:latest; do
  if docker image inspect "$image" >/dev/null 2>&1; then
    name=${image%%:*}
    log "Analyse de l'image $image…"
    trivy image --scanners vuln --format table --output "/out/$name.txt" "$image"
    trivy image --scanners vuln --format json --output "/out/$name.json" "$image"
  else
    log "Image $image absente : lancez d'abord qa/scripts/docker-up.sh"
  fi
done

log "Synthèse par sévérité :"
for f in "$OUT"/*.json; do
  printf '  %-32s ' "$(basename "$f")"
  grep -o '"Severity": *"[A-Z]*"' "$f" | sed 's/.*"\([A-Z]*\)"/\1/' | sort | uniq -c | tr '\n' ' '
  echo
done | tee "$OUT/summary.txt"
