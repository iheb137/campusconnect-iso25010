@echo off
rem ============================================================
rem  CampusConnect - demarrage (backend Spring Boot + frontend Next.js)
rem  Configuration : backend\.env (non versionne, voir backend\.env.example)
rem ============================================================
setlocal EnableExtensions
set "ROOT=%~dp0"
set "ENV_FILE=%ROOT%backend\.env"

if not exist "%ENV_FILE%" (
  echo [ERREUR] Fichier backend\.env introuvable.
  echo          Copiez backend\.env.example en backend\.env puis renseignez vos valeurs.
  exit /b 1
)

rem --- Chargement des variables (lignes NOM=valeur, # = commentaire)
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
  if not "%%A"=="" set "%%A=%%B"
)

if not defined DB_PORT set "DB_PORT=5434"
if not defined BACKEND_PORT set "BACKEND_PORT=8280"
if not defined FRONTEND_PORT set "FRONTEND_PORT=3010"
if not defined MAVEN_CMD set "MAVEN_CMD=%ROOT%backend\mvnw.cmd"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

rem --- Verifications
if not defined DB_PASSWORD ( echo [ERREUR] DB_PASSWORD manquant dans backend\.env & exit /b 1 )
if not defined JWT_SECRET ( echo [ERREUR] JWT_SECRET manquant dans backend\.env & exit /b 1 )
where npm >nul 2>&1 || ( echo [ERREUR] npm introuvable : installez Node.js. & exit /b 1 )

call :listening %BACKEND_PORT% && (
  echo [INFO] Un serveur ecoute deja sur le port %BACKEND_PORT%. Lancez stop.bat d'abord.
  exit /b 1
)
call :listening %DB_PORT% || (
  echo [ATTENTION] Aucun PostgreSQL ne repond sur le port %DB_PORT%. Verifiez le service PostgreSQL.
)

rem --- Dependances du frontend (premier lancement)
if not exist "%ROOT%frontend\node_modules" (
  echo [INFO] Installation des dependances du frontend...
  pushd "%ROOT%frontend"
  call npm install || ( popd & echo [ERREUR] npm install a echoue & exit /b 1 )
  popd
)

rem --- Lancement : chaque partie dans sa propre fenetre (CC_WINDOW sert a stop.bat)
echo [INFO] Demarrage du backend sur http://localhost:%BACKEND_PORT% ...
start "CampusConnect QA - backend" /D "%ROOT%backend" cmd /k "set CC_WINDOW=ccqa-backend&& set SERVER_PORT=%BACKEND_PORT%&& call "%MAVEN_CMD%" spring-boot:run"

echo [INFO] Demarrage du frontend sur http://localhost:%FRONTEND_PORT% ...
start "CampusConnect QA - frontend" /D "%ROOT%frontend" cmd /k "set CC_WINDOW=ccqa-frontend&& set BACKEND_URL=http://localhost:%BACKEND_PORT%&& npm run dev -- -p %FRONTEND_PORT%"

rem --- Attente du backend (3 minutes max : le premier lancement telecharge les dependances)
echo [INFO] Attente du backend...
for /l %%i in (1,1,90) do (
  curl -s -o nul "http://localhost:%BACKEND_PORT%/api/health" && goto backend_ready
  ping -n 3 127.0.0.1 >nul
)
echo [ATTENTION] Le backend ne repond pas encore. Consultez la fenetre "CampusConnect QA - backend".
goto open_browser

:backend_ready
echo [OK] Backend pret.
for /l %%i in (1,1,30) do (
  curl -s -o nul "http://localhost:%FRONTEND_PORT%" && goto open_browser
  ping -n 3 127.0.0.1 >nul
)

:open_browser
echo [OK] CampusConnect : http://localhost:%FRONTEND_PORT%
start "" "http://localhost:%FRONTEND_PORT%"
exit /b 0

rem --- Renvoie 0 si un programme ecoute sur le port %1
:listening
netstat -ano -p tcp | findstr /R /C:":%~1 .*LISTENING" >nul
exit /b %errorlevel%
