@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0.."

REM Stop previous instance on port 8443
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8443" ^| findstr "LISTENING"') do (
  echo Stopping PID %%a on port 8443...
  taskkill /PID %%a /F >nul 2>&1
)

if exist gradlew.bat (
  call gradlew.bat run
) else if exist mvn.cmd (
  call mvn.cmd -q compile exec:java
) else (
  echo Install Gradle wrapper or Maven, or run scripts\build.bat first.
  exit /b 1
)
endlocal
