@echo off
setlocal
set FOUND=0
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8443" ^| findstr "LISTENING"') do (
  echo Stopping PID %%a...
  taskkill /PID %%a /F >nul 2>&1
  set FOUND=1
)
if "%FOUND%"=="0" (
  echo No process listening on port 8443.
) else (
  echo Port 8443 is free.
)
endlocal
