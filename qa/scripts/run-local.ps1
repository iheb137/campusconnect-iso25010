<#
  CampusConnect - campagne de tests ISO 25010 sous Windows (sans Docker).
  Tous les rapports sont écrits dans qa\reports\ ; une synthèse est écrite dans qa\reports\summary-windows.md.

  Étape 1 (application ARRÊTÉE : stop.bat)   : .\qa\scripts\run-local.ps1 -Phase backend,frontend
  Étape 2 (application DÉMARRÉE : start.bat) : .\qa\scripts\run-local.ps1 -Phase e2e

  backend  : tests JUnit (unitaires + intégration), couverture JaCoCo + seuils, rapport Surefire, SpotBugs/FindSecBugs
  frontend : ESLint (+ règles de sécurité), TypeScript, build de production, npm audit
  e2e      : Playwright (Chromium, Firefox, WebKit, mobile) + axe WCAG 2.1 AA, Newman (API), Lighthouse
#>
param(
  [string[]]$Phase = @("backend", "frontend"),
  [string]$FrontendUrl = "http://localhost:3011",
  [string]$BackendUrl = "http://localhost:8280"
)

# Avec « powershell -File », -Phase backend,frontend arrive comme une seule chaîne : on la découpe.
$Phase = @($Phase | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim().ToLower() } | Where-Object { $_ })

$ErrorActionPreference = "Continue"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$Reports = Join-Path $Root "qa\reports"
New-Item -ItemType Directory -Force -Path $Reports | Out-Null
$Results = [System.Collections.Generic.List[object]]::new()

function Step([string]$Name, [scriptblock]$Action) {
  Write-Host "`n=== $Name ===" -ForegroundColor Cyan
  $watch = [Diagnostics.Stopwatch]::StartNew()
  & $Action
  $code = $LASTEXITCODE
  $watch.Stop()
  $status = if ($code -eq 0) { "RÉUSSI" } else { "ÉCHEC (code $code)" }
  $color = if ($code -eq 0) { "Green" } else { "Red" }
  Write-Host "--> $Name : $status ($([math]::Round($watch.Elapsed.TotalSeconds)) s)" -ForegroundColor $color
  $Results.Add([pscustomobject]@{ Etape = $Name; Resultat = $status; Duree = "$([math]::Round($watch.Elapsed.TotalSeconds)) s" })
}

function Copy-Report([string]$From, [string]$To) {
  if (Test-Path $From) {
    $target = Join-Path $Reports $To
    if (Test-Path $target) { Remove-Item -Recurse -Force $target }
    Copy-Item -Recurse -Force $From $target
  }
}

# Variables de backend\.env (DB_*, JWT_SECRET, MAVEN_CMD, JAVA_HOME...)
$EnvFile = Join-Path $Root "backend\.env"
if (Test-Path $EnvFile) {
  Get-Content $EnvFile | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    Set-Item "env:$k" $v
  }
}
if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME\bin;$env:PATH" }
$Mvn = if ($env:MAVEN_CMD) { $env:MAVEN_CMD } else { Join-Path $Root "backend\mvnw.cmd" }
$env:SHOW_SQL = "false"

if ($Phase -contains "backend") {
  Push-Location (Join-Path $Root "backend")
  Step "Backend - tests + couverture JaCoCo (seuils)" { & $Mvn -B clean verify *> (Join-Path $Reports "backend-verify.log") }
  Step "Backend - rapport HTML des tests" { & $Mvn -B surefire-report:report-only *> $null }
  Step "Backend - SpotBugs + FindSecBugs" { & $Mvn -B spotbugs:check *> (Join-Path $Reports "spotbugs.log") }
  Copy-Report "target\site\jacoco" "jacoco"
  Copy-Report "target\reports" "surefire"
  Copy-Report "target\surefire-reports" "surefire-xml"
  New-Item -ItemType Directory -Force -Path (Join-Path $Reports "spotbugs") | Out-Null
  foreach ($f in @("target\spotbugs.html", "target\spotbugsXml.xml")) { if (Test-Path $f) { Copy-Item -Force $f (Join-Path $Reports "spotbugs") } }
  Pop-Location
}

if ($Phase -contains "frontend") {
  Push-Location (Join-Path $Root "frontend")
  Step "Frontend - installation (npm ci)" { npm ci --no-fund *> (Join-Path $Reports "npm-ci.log") }
  New-Item -ItemType Directory -Force -Path (Join-Path $Reports "eslint") | Out-Null
  Step "Frontend - ESLint (qualité + sécurité)" { npx eslint . -f html -o (Join-Path $Reports "eslint\index.html"); npx eslint . }
  # Le build régénère les types de routes de Next (.next/types) : TypeScript est vérifié après,
  # sinon des types périmés laissés par « next dev » peuvent faire échouer tsc.
  Step "Frontend - build de production" { npm run build *> (Join-Path $Reports "frontend-build.log") }
  Step "Frontend - TypeScript strict" { npx tsc --noEmit *> (Join-Path $Reports "typescript.log") }
  New-Item -ItemType Directory -Force -Path (Join-Path $Reports "npm-audit") | Out-Null
  Step "Frontend - npm audit (dépendances de production)" {
    npm audit --omit=dev --json > (Join-Path $Reports "npm-audit\frontend.json")
    npm audit --omit=dev | Tee-Object (Join-Path $Reports "npm-audit\frontend.txt")
  }
  Pop-Location
}

if ($Phase -contains "e2e") {
  # Le backend doit tourner (start.bat). Le frontend est testé en build de PRODUCTION sur un port dédié :
  # le serveur de développement (next dev) compile à la demande et fausse les temps et l'hydratation.
  try { Invoke-WebRequest "$BackendUrl/api/health" -UseBasicParsing -TimeoutSec 10 | Out-Null }
  catch { Write-Host "Le backend ne répond pas sur $BackendUrl : lancez start.bat d'abord." -ForegroundColor Red; exit 1 }

  $FrontendPort = ([Uri]$FrontendUrl).Port
  Push-Location (Join-Path $Root "frontend")
  Step "Frontend - build de production pour les tests" { npm run build *> (Join-Path $Reports "frontend-build-e2e.log") }
  $env:BACKEND_URL = $BackendUrl
  $env:COOKIE_SECURE = "false"
  $server = Start-Process -FilePath "cmd.exe" -ArgumentList "/c npx next start -p $FrontendPort" `
    -WorkingDirectory (Get-Location) -WindowStyle Hidden -PassThru
  $ready = $false
  for ($i = 0; $i -lt 60 -and -not $ready; $i++) {
    Start-Sleep -Seconds 1
    try { Invoke-WebRequest "$FrontendUrl/login" -UseBasicParsing -TimeoutSec 5 | Out-Null; $ready = $true } catch { }
  }
  if (-not $ready) { Write-Host "Le frontend de test ne démarre pas sur $FrontendUrl" -ForegroundColor Red }

  try {
    Step "Navigateurs Playwright (installation)" { npx playwright install chromium firefox webkit *> $null }
    $env:E2E_BASE_URL = $FrontendUrl
    Step "E2E Playwright - Chromium, Firefox, WebKit, mobile + axe" { npx playwright test }
    Copy-Report "playwright-report" "playwright"
    Copy-Report "test-results\junit.xml" "playwright-junit.xml"
    Pop-Location

  Push-Location $Root
  Step "Installation des outils QA (newman, lighthouse)" { npm ci --no-fund *> $null }
  Step "API - Newman" {
    npx newman run qa/newman/campusconnect.postman_collection.json --env-var "baseUrl=$BackendUrl" `
      -r cli,htmlextra,junit --reporter-htmlextra-export qa/reports/newman/index.html --reporter-junit-export qa/reports/newman/junit.xml
  }
  New-Item -ItemType Directory -Force -Path (Join-Path $Reports "lighthouse") | Out-Null
  Step "Lighthouse - page de connexion" {
    npx lighthouse "$FrontendUrl/login" --only-categories=accessibility,best-practices,performance `
      --output=html --output=json --output-path=qa/reports/lighthouse/login --chrome-flags="--headless=new" --quiet
  }
  Pop-Location
  }
  finally {
    # Arrêt du frontend de test (processus et enfants)
    if ($server -and -not $server.HasExited) { taskkill /PID $server.Id /T /F *> $null }
  }
}

$summary = Join-Path $Reports "summary-windows.md"
$lines = @("# Synthèse des tests (Windows) - $(Get-Date -Format 'yyyy-MM-dd HH:mm')", "", "Machine : $env:COMPUTERNAME - $([Environment]::OSVersion.VersionString)", "",
  "| Étape | Résultat | Durée |", "|---|---|---|")
$lines += $Results | ForEach-Object { "| $($_.Etape) | $($_.Resultat) | $($_.Duree) |" }
Set-Content -Path $summary -Value $lines -Encoding UTF8
Write-Host "`nSynthèse : $summary" -ForegroundColor Cyan
$Results | Format-Table -AutoSize
