# CLAUDE.md

@AGENTS.md

## Notas específicas para Claude Code

- Al iniciar una sesión, lee `docs/PLAN.md` y di en una línea en qué tarea vas a trabajar.
- Para tareas grandes (una fase completa), usa el modo plan primero y muestra el plan antes de editar.
- Después de cada pantalla nueva, compila. Si el usuario tiene el emulador abierto, instala con
  `./gradlew :wear:installDebug` y pídele una captura para revisar el resultado visual.
- En Windows el wrapper es `gradlew.bat`; desde Git Bash funciona `./gradlew`.
- Si Gradle falla con `Unable to establish loopback connection`, es la carpeta temporal de Windows.
  Antes de compilar: `export TMP='D:\dev\tmp' TEMP='D:\dev\tmp' GRADLE_OPTS='-Djava.io.tmpdir=D:/dev/tmp -Djdk.net.unixdomain.tmpdir=D:/dev/tmp'`.
- Emulador del proyecto: `ATiempo_Large_Round_API36` (Wear OS 6) en `D:\dev\avd`.
- Mantén `docs/PLAN.md` como la fuente de verdad del progreso: marca casillas en el mismo commit
  que termina la tarea.
- Si cambias algo del sistema de diseño (colores, tipografía, patrones hápticos), actualiza
  `docs/DISENO_UI.md` en el mismo commit.
