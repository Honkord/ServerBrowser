@echo off
setlocal
cd /d "%~dp0.."

if exist gradlew.bat (
  call gradlew.bat build
  exit /b %ERRORLEVEL%
)

if exist mvn.cmd (
  call mvn.cmd -q package -DskipTests
  exit /b %ERRORLEVEL%
)

echo Install Gradle ^(gradlew.bat^) or Maven ^(mvn^).
exit /b 1
