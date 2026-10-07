@echo off
cd /d "%~dp0"
if not exist "config\gema.properties" copy "config\gema.properties.example" "config\gema.properties" >nul
start "" notepad "config\gema.properties"
echo.
echo Complete host, base, usuario y contrasena; guarde el archivo y vuelva a iniciar GEMA Web.
pause
