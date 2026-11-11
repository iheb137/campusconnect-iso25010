#!/usr/bin/env bash
# Portabilité : construit et démarre la pile complète (PostgreSQL + backend + frontend) avec Docker Compose.
source "$(dirname "$0")/common.sh"
require_env_file
log "Construction et démarrage des conteneurs…"
compose up -d --build
log "Attente du backend ($BACKEND_URL)…"; t_back=$(wait_for "$BACKEND_URL/api/health" 240)
log "Attente du frontend ($FRONTEND_URL)…"; t_front=$(wait_for "$FRONTEND_URL/login" 120)
compose ps
log "Pile prête : backend en ${t_back}s, frontend en ${t_front}s après le démarrage."
