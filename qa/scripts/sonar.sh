#!/usr/bin/env bash
# ISO 25010 — Maintenabilité : analyse SonarQube (qualité, dette technique, duplications, couverture JaCoCo).
# Étape 1 : qa/scripts/sonar.sh start   → SonarQube sur http://localhost:9000 (admin/admin au premier accès)
#           Créez un jeton : My Account > Security > Generate Token (type « Global Analysis Token »)
# Étape 2 : SONAR_TOKEN=xxxx qa/scripts/sonar.sh scan
#           (la pile Docker doit tourner : les tests backend utilisent sa base PostgreSQL sur le port 55432)
source "$(dirname "$0")/common.sh"
SONAR_URL=${SONAR_URL:-http://localhost:9000}

case "${1:-}" in
  start)
    docker run -d --name campusconnect-sonar -p 9000:9000 sonarqube:community >/dev/null 2>&1 \
      || docker start campusconnect-sonar >/dev/null
    log "Démarrage de SonarQube (1 à 2 minutes)…"
    until curl -fsS "$SONAR_URL/api/system/status" 2>/dev/null | grep -q '"status":"UP"'; do sleep 5; done
    log "SonarQube prêt : $SONAR_URL"
    ;;
  scan)
    : "${SONAR_TOKEN:?Définissez SONAR_TOKEN (voir en-tête du script)}"
    require_env_file
    DB_PASSWORD=$(grep ^DB_PASSWORD docker.env | cut -d= -f2-)
    log "Backend : tests + JaCoCo + analyse…"
    docker run --rm --network host -v "$ROOT/backend:/app" -v campusconnect-m2:/root/.m2 -w /app \
      -e DB_HOST=localhost -e DB_PORT="$(grep ^DB_HOST_PORT docker.env | cut -d= -f2 || echo 55432)" \
      -e DB_NAME="$(grep ^DB_NAME docker.env | cut -d= -f2)" -e DB_USERNAME="$(grep ^DB_USERNAME docker.env | cut -d= -f2)" \
      -e DB_PASSWORD="$DB_PASSWORD" -e JWT_SECRET=sonar-analysis-secret-0123456789abcdef -e SHOW_SQL=false \
      maven:3.9-eclipse-temurin-21 \
      mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
        -Dsonar.host.url="$SONAR_URL" -Dsonar.token="$SONAR_TOKEN"
    log "Frontend : analyse TypeScript…"
    docker run --rm --network host -v "$ROOT/frontend:/usr/src" -e SONAR_HOST_URL="$SONAR_URL" -e SONAR_TOKEN="$SONAR_TOKEN" \
      sonarsource/sonar-scanner-cli \
      -Dsonar.projectKey=campusconnect-frontend -Dsonar.projectName="CampusConnect frontend" \
      -Dsonar.sources=src -Dsonar.tests=e2e -Dsonar.exclusions="**/node_modules/**,**/.next/**"
    log "Résultats : $SONAR_URL/projects"
    ;;
  *) echo "Usage : $0 start | scan" >&2; exit 1 ;;
esac
