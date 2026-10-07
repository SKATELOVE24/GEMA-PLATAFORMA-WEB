#!/bin/zsh
cd "$(dirname "$0")"
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no está instalado. Instalalo y volvé a ejecutar este archivo."
  read -k 1 "?Presioná una tecla para cerrar..."
  exit 1
fi
( sleep 8; open http://localhost:8080 ) &
mvn spring-boot:run
