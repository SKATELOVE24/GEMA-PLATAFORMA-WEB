# GEMA Web

Versión web de **GEMA — Gestión Escolar y Monitoreo Académico**, construida a partir del proyecto JavaFX proporcionado.

## Abrir rápidamente

### Vista demo (no necesita servidor ni base de datos)
En Windows, doble clic en `ABRIR_PREVIEW.bat`. La interfaz abre en el navegador y podés entrar con Coordinación, Docente o Alumno usando los botones Demo.

### Versión completa con Spring Boot
En Windows, doble clic en `INICIAR_GEMA_WEB.bat`. El script usa Maven del sistema o intenta usar el que trae Apache NetBeans. Cuando aparezca `Started GemaWebApplication`, abrí `http://localhost:8080`.

En macOS, ejecutá `INICIAR_GEMA_WEB.command` (requiere Java 21 y Maven).

## Conectar a la base real
1. Ejecutá `CONFIGURAR_BD.bat`.
2. Completá `db.host`, `db.name`, `db.user` y `db.password` con la misma conexión que usa GEMA escritorio.
3. Guardá y reiniciá GEMA Web.

Las credenciales se leen en el servidor desde `config/gema.properties`: **no se envían al navegador**. El archivo está excluido de Git mediante `.gitignore`.

## Módulos incluidos
- Landing page pública con logo GEMA y descargas.
- Inicio de sesión por cédula/contraseña y roles.
- Panel de coordinación con métricas.
- Docentes y alumnos.
- Materias y cursos.
- Tareas, programación, adjuntos y entregas.
- Calificación de entregas.
- Cargos estudiantiles.
- Sugerencias.
- Calificaciones del alumno.
- Racha y ranking por curso.
- Reportes imprimibles / guardables como PDF.
- Descarga directa de `Instalador_GEMA.exe` para Windows (sin ZIP).
- macOS queda indicado como próximamente hasta disponer de un `.dmg` nativo.
- Diseño responsive para PC, tablet y celular.
- Modo demo automático cuando la base no está configurada.

## Seguridad
No subas `config/gema.properties` a repositorios públicos. Si la web se publica en Internet, lo recomendable es configurar las credenciales mediante variables de entorno y colocar el servidor detrás de HTTPS.
