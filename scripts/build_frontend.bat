@echo off
setlocal
cd /d "%~dp0..\frontend"

if not exist package.json (
  echo frontend\package.json not found.
  exit /b 1
)

if not exist node_modules (
  call npm.cmd install
)
call npm.cmd run build
echo Built UI to frontend\dist
endlocal
