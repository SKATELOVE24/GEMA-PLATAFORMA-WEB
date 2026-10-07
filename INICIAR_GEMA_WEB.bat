@echo off
setlocal
cd /d "%~dp0"
title GEMA WEB
cls
echo ==========================================
echo              GEMA WEB 2026
echo ==========================================
echo.
set "MVN=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
  if exist "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" (
    set "MVN=C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
  ) else if exist "C:\Program Files\NetBeans-*\java\maven\bin\mvn.cmd" (
    for /d %%D in ("C:\Program Files\NetBeans-*") do set "MVN=%%~fD\java\maven\bin\mvn.cmd"
  ) else (
    echo No se encontro Maven.
    echo Puede abrir ABRIR_PREVIEW.bat para ver GEMA en modo demo.
    echo O instale Maven / use el Maven de Apache NetBeans.
    pause
    exit /b 1
  )
)
echo Maven: %MVN%
echo La web abrira en http://localhost:8080
start "" cmd /c "timeout /t 8 /nobreak >nul & start http://localhost:8080"
if "%MVN%"=="mvn" (
  mvn spring-boot:run
) else (
  call "%MVN%" spring-boot:run
)
echo.
echo GEMA Web se detuvo.
pause
