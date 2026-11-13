# AGENTS.md

Guía para cualquier agente de código (Claude Code, Codex, Cursor, etc.) que trabaje en este repositorio.
Léela completa antes de tocar código. `CLAUDE.md` la importa.

## Qué es este proyecto

**A Tiempo** (nombre de trabajo): una app para smartwatch con **Wear OS** que ayuda a la persona a
empezar a prepararse y salir con tiempo suficiente para llegar a una cita, clase o actividad.
No es una alarma fija: divide la preparación en **4 estados** con vibraciones distintas y **aprende**
cuánto tarda realmente la persona en cada rutina.

Es un proyecto universitario de la materia **Diseño de Interfaces**. La calidad de la interfaz
es lo que más pesa en la nota. Cuando haya que elegir entre más funcionalidad o una interfaz
más pulida, gana la interfaz.

- Propuesta original: [docs/PROPUESTA.md](docs/PROPUESTA.md)
- Plan y estado de tareas: [docs/PLAN.md](docs/PLAN.md) ← **empieza aquí**
- Sistema de diseño y pantallas: [docs/DISENO_UI.md](docs/DISENO_UI.md)
- Arquitectura técnica: [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md)
- Modelo de estimación ("IA"): [docs/MODELO_ESTIMACION.md](docs/MODELO_ESTIMACION.md)
- Registro de decisiones: [docs/DECISIONES.md](docs/DECISIONES.md)

## Fecha de entrega

**Martes 17 de noviembre de 2026, 11:59 PM (hora de Colombia, UTC-5).**
Todo lo que no esté listo y estable el lunes 16 por la noche se recorta del alcance.

## Cómo trabajar

1. Abre `docs/PLAN.md`, busca la primera tarea sin marcar de la fase actual y trabaja solo en esa.
2. Antes de escribir una pantalla, revisa su especificación en `docs/DISENO_UI.md`.
   Si la especificación no alcanza, decide, implementa y anota la decisión en `docs/DECISIONES.md`.
3. Al terminar una tarea: compila (`./gradlew :wear:assembleDebug`), corre las pruebas que existan
   (`./gradlew test`), marca la casilla en `docs/PLAN.md` y haz un commit pequeño.
4. No avances a la siguiente fase si la app no compila.
5. Si algo del plan resulta inviable en el tiempo disponible, recórtalo, anótalo en
   `docs/DECISIONES.md` y sigue. No bloquees el trabajo esperando respuesta.

## Reglas del código

- **Lenguaje:** Kotlin. **UI:** Jetpack Compose for Wear OS con **Wear Compose Material 3**
  (`androidx.wear.compose:compose-material3`). No uses Views/XML para pantallas.
- Usa la versión estable más reciente de cada librería al crear el proyecto y fíjala en
  `gradle/libs.versions.toml` (version catalog). No uses alphas salvo que no exista estable.
- Arquitectura: una sola Activity, navegación con `androidx.wear.compose:compose-navigation`,
  MVVM con `ViewModel` + `StateFlow`, inyección de dependencias manual (un `AppContainer`),
  sin Hilt para no perder tiempo en configuración.
- Persistencia: **Room** para eventos e historial; **DataStore** para preferencias.
- Todos los textos visibles van en `res/values/strings.xml` **en español**. Nada de textos fijos en el código.
- La app tiene **tema claro (principal, predeterminado) y tema oscuro**. Ver `docs/DISENO_UI.md` §2.
  Colores, tipografía y espaciados salen del tema (`ui/theme`). Nunca escribas un color hexadecimal
  dentro de una pantalla.
- Cada pantalla tiene previews en **tema claro y tema oscuro**, además de `@WearPreviewDevices` y
  `@WearPreviewFontScales` para revisar pantalla redonda, cuadrada y texto grande.
  Las capturas y la presentación se hacen en tema claro.
- Lógica de estimación y del motor de estados en clases puras de Kotlin (sin Android) con pruebas unitarias.
- Nombres de código (clases, funciones, variables) en inglés; comentarios y documentación en español.

## Lo que NO hay que hacer

- No crear app de teléfono ni usar Data Layer API en esta entrega (ver `docs/DECISIONES.md`, D-002).
- No llamar APIs reales de clima o rutas. Se simulan con un proveedor falso (D-003).
- No presentar la app como diagnóstico ni tratamiento de ninguna condición de salud.
  Evita palabras como "TDAH", "terapia", "síntoma" en textos y documentación de la app.
- No subir llaves, tokens ni el archivo `local.properties`.

## Comandos útiles

```bash
./gradlew :wear:assembleDebug      # compilar
./gradlew test                     # pruebas unitarias
./gradlew :wear:installDebug       # instalar en el emulador encendido
```

Emulador recomendado: **Wear OS Large Round** (API 34 o superior) en Android Studio.

## Commits

Mensajes cortos en español con prefijo: `feat:`, `fix:`, `ui:`, `docs:`, `test:`, `chore:`.
Ejemplo: `ui: pantalla de estado "Prepárate" con anillo de progreso`.
