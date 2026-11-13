# CLAUDE.md

@AGENTS.md

## Notas específicas para Claude Code

- Al iniciar una sesión, lee `docs/PLAN.md` y di en una línea en qué tarea vas a trabajar.
- Para tareas grandes (una fase completa), usa el modo plan primero y muestra el plan antes de editar.
- Después de cada pantalla nueva, compila. Si el usuario tiene el emulador abierto, instala con
  `./gradlew :wear:installDebug` y pídele una captura para revisar el resultado visual.
- En Windows el wrapper es `gradlew.bat`; desde Git Bash funciona `./gradlew`.
- Mantén `docs/PLAN.md` como la fuente de verdad del progreso: marca casillas en el mismo commit
  que termina la tarea.
- Si cambias algo del sistema de diseño (colores, tipografía, patrones hápticos), actualiza
  `docs/DISENO_UI.md` en el mismo commit.
