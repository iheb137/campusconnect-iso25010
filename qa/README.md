# Campagne de tests ISO/IEC 25010 — CampusConnect

Chaque caractéristique de qualité est reliée à des exigences mesurables, à des tests exécutables et à un outil.
Les rapports générés vont dans `qa/reports/` (non versionné). Le pipeline GitHub Actions
(`.github/workflows/quality.yml`) rejoue la campagne à chaque push et publie les rapports en artefacts.

| Caractéristique ISO 25010 | Exigences | Tests / outil | Où |
|---|---|---|---|
| Adéquation fonctionnelle | RG-01 à RG-13, FUN-* | JUnit 5 + Mockito (unitaires), Spring Boot Test (intégration HTTP + PostgreSQL), Newman (API), Playwright (E2E) | `backend/src/test`, `qa/newman`, `frontend/e2e` |
| Efficacité de performance | PERF-01 : p95 < 500 ms et < 1 % d'erreurs à 20 utilisateurs ; PERF-02 : limite en stress | k6 (charge, stress), temps < 500 ms dans Newman, Lighthouse | `qa/k6`, `qa/scripts/k6.sh` |
| Compatibilité | COMP-01 : Chromium, Firefox, WebKit ; COMP-02 : mobile sans débordement ; COMP-03 : coexistence (ports dédiés) | Playwright (4 projets) | `frontend/playwright.config.ts` |
| Utilisabilité | USA-01 : WCAG 2.1 AA sans violation (clair + sombre) ; USA-02 : clavier ; USA-03 : messages d'erreur | axe-core via Playwright, Lighthouse | `frontend/e2e/accessibility.spec.ts` |
| Fiabilité | REL-01 à REL-09 : concurrence, atomicité, panne et reprise de la base, redémarrage | JUnit (concurrence, rollback, 503), script de panne Docker | `backend/src/test/.../reliability`, `qa/scripts/reliability.sh` |
| Sécurité | SEC-01 à SEC-11 (JWT, rôles, en-têtes, fuite d'erreurs, force brute, injection, XSS) | JUnit, Playwright, SpotBugs + FindSecBugs (SAST), ESLint security, npm audit + Trivy (SCA), OWASP ZAP (DAST) | `backend/src/test/.../security`, `qa/scripts/zap.sh`, `qa/scripts/trivy.sh` |
| Maintenabilité | MAIN-01 : couverture lignes ≥ 90 % / branches ≥ 75 % ; MAIN-02 : 0 bogue SpotBugs ; MAIN-03 : 0 erreur ESLint/TS ; MAIN-04 : SonarQube | JaCoCo (`mvn verify` échoue sous les seuils), SpotBugs, ESLint, SonarQube | `backend/pom.xml`, `qa/scripts/sonar.sh` |
| Portabilité | PORT-01 : installation depuis zéro par Docker Compose ; PORT-02 : mêmes tests sur Windows (local) et Linux (conteneurs) | Docker Compose, script de portabilité, Newman sur la pile Docker | `docker-compose.yml`, `qa/scripts/portability.sh` |

## 1. Sous Windows (sans Docker)

Pré-requis : JDK 21+, Maven, Node.js 22, PostgreSQL (base `campusconnect_qa`), `backend\.env` rempli.

```powershell
# Application ARRÊTÉE (stop.bat) : tests backend + frontend
powershell -ExecutionPolicy Bypass -File .\qa\scripts\run-local.ps1 -Phase backend,frontend
# Application DÉMARRÉE (start.bat) : E2E 3 navigateurs + axe, Newman, Lighthouse
powershell -ExecutionPolicy Bypass -File .\qa\scripts\run-local.ps1 -Phase e2e
```

Commandes unitaires équivalentes :

```powershell
cd backend ; mvn clean verify                 # tests + JaCoCo + seuils  → target/site/jacoco/index.html
mvn surefire-report:report-only               # → target/reports/surefire.html
mvn spotbugs:check                            # SAST → target/spotbugs.html
cd ..\frontend ; npx playwright install ; npx playwright test   # → playwright-report/index.html
npx playwright test --project=chromium        # un seul navigateur (démonstration rapide)
cd .. ; npm ci ; npm run qa:newman:local      # → qa/reports/newman/index.html
```

## 2. Sous WSL avec Docker

Depuis la racine du dépôt (`cd /mnt/c/Users/<vous>/campusconnect-iso25010`), après `cp docker.env.example docker.env` :

```bash
bash qa/scripts/docker-up.sh        # pile complète : http://localhost:3080 (frontend), :8380 (API), :55432 (PostgreSQL)
bash qa/scripts/k6.sh               # PERF-01 : 20 utilisateurs, seuils p95 < 500 ms
bash qa/scripts/k6.sh stress        # PERF-02 : montée à 100 utilisateurs (informatif)
bash qa/scripts/reliability.sh      # REL-07 à REL-09 : panne / reprise base, arrêt brutal du backend
bash qa/scripts/zap.sh              # DAST OWASP ZAP baseline (FULL=1 pour l'analyse active)
bash qa/scripts/trivy.sh            # SCA : dépendances, secrets, Dockerfiles, images
bash qa/scripts/portability.sh      # PORT-01/02 : réinstallation depuis zéro + Newman sur Docker
bash qa/scripts/sonar.sh start      # puis : SONAR_TOKEN=... bash qa/scripts/sonar.sh scan
```

Les tests E2E peuvent aussi viser la pile Docker depuis Windows (portabilité) :
`$env:E2E_BASE_URL="http://localhost:3080"; npx playwright test`.

## 3. Règle d'or

Aucun résultat n'est inscrit dans le rapport de conformité sans le rapport généré correspondant
(HTML, XML, JSON ou journal). Un outil qui n'a pas pu être exécuté est signalé comme tel, jamais simulé.
