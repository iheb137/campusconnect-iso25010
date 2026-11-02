@echo off
rem ============================================================
rem  CampusConnect - arret du backend et du frontend lances par start.bat
rem ============================================================
setlocal EnableExtensions
set "ROOT=%~dp0"
set "BACKEND_PORT=8280"
set "FRONTEND_PORT=3010"
if exist "%ROOT%backend\.env" (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ROOT%backend\.env") do (
    if /i "%%A"=="BACKEND_PORT" set "BACKEND_PORT=%%B"
    if /i "%%A"=="FRONTEND_PORT" set "FRONTEND_PORT=%%B"
  )
)

echo [INFO] Arret de CampusConnect...

rem 1) Fenetres ouvertes par start.bat, avec tous leurs processus (Maven, Java, npm, Node)
powershell -NoProfile -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'cmd.exe' -and $_.CommandLine -like '*CC_WINDOW=ccqa-*' } | ForEach-Object { taskkill /PID $_.ProcessId /T /F | Out-Null }"

rem 2) Securite : tout processus qui ecoute encore sur les ports de l'application
call :kill_port %BACKEND_PORT%
call :kill_port %FRONTEND_PORT%

echo [OK] CampusConnect est arrete.
exit /b 0

:kill_port
for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr /R /C:":%~1 .*LISTENING"') do (
  if not "%%P"=="0" taskkill /PID %%P /T /F >nul 2>&1
)
exit /b 0
