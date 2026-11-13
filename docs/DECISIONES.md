# Registro de decisiones

Formato: número, fecha, decisión, motivo. Añade nuevas decisiones al final.

### D-001 · 2026-11-13 · La interfaz manda
La materia es Diseño de Interfaces. Ante cualquier conflicto de tiempo, se prioriza el pulido de las
pantallas sobre funciones adicionales.

### D-002 · 2026-11-13 · Sin app de teléfono ni Data Layer API
La propuesta menciona un teléfono Android y la Data Layer API. Para la entrega, todo corre en el reloj.
Motivo: un segundo módulo (app de teléfono) y la sincronización consumen más de un día y no aportan a la
evaluación de la interfaz del reloj. La arquitectura deja el estimador detrás de una interfaz para moverlo
al teléfono más adelante.

### D-003 · 2026-11-13 · Clima y rutas simulados
No se usan APIs reales de clima, rutas ni calendario. Un `FakeContextProvider` entrega valores que se
pueden cambiar desde el modo demo. Motivo: evitar llaves de API, cuotas y fallos de red durante la
presentación.

### D-004 · 2026-11-13 · Sin Activity Recognition ni ubicación
La detección de movimiento hacia la salida y el Fused Location Provider quedan fuera. El motor de estados
ya acepta el evento `MovementDetected` para mostrar que el diseño lo contempla. Motivo: requieren permisos
de ubicación en segundo plano y pruebas en dispositivo real.

### D-005 · 2026-11-13 · Modelo de estimación por media ponderada en lugar de TFLite
Ver [MODELO_ESTIMACION.md](MODELO_ESTIMACION.md). Más explicable y suficiente con pocos datos.

### D-006 · 2026-11-13 · Intérprete de voz por reglas
La voz se transcribe con el reconocedor del sistema y se interpreta con reglas en español, sin servicios
externos de lenguaje natural. Motivo: funciona sin conexión y es predecible en la demo.

### D-007 · 2026-11-13 · Datos de ejemplo sembrados
Se siembran 3 rutinas y dos semanas de historial para que el resumen semanal y la explicación tengan
contenido real en la presentación.

### D-008 · 2026-11-13 · Tema claro principal y tema oscuro alterno
Requisito del usuario: la interfaz tiene modo claro y oscuro, y el claro es el principal. Como Wear OS no
ofrece un tema claro del sistema y Wear Compose Material 3 está pensado para oscuro, el tema claro se
construye con un `ColorScheme` propio y la elección (Claro, Oscuro, Automático) se guarda en DataStore.
El modo ambiente siempre es negro en ambos temas. Costo: el fondo claro consume más batería en pantallas
OLED; se acepta porque el objetivo es la evaluación de la interfaz.

### D-009 · 2026-11-13 · Avance automático entre estados y estado "tarde"
En los estados 1 a 3, si el tiempo del paso se acaba sin que la persona toque "Listo", la app pasa sola al
siguiente estado: es la alarma programada del siguiente paso, con su vibración. El estado "tarde" aplica
solo al estado 4: si pasa la hora de salida más el margen de seguridad sin "Ya salí", el anillo cambia a
`late`, el texto dice "Vas X min tarde" y suena el patrón "Vas tarde" cada 60 s, máximo 3 veces.
Si la persona confirma antes de tiempo, el siguiente paso empieza en ese momento y conserva su hora de fin.
Motivo: DISENO_UI §3.6 y ARQUITECTURA no coincidían; así la persona nunca queda atascada en un paso viejo
y el rojo se reserva para cuando de verdad va tarde (principio "Calma, no alarma").

### D-010 · 2026-11-13 · Prototipo HTML: escala 1 dp = 2 px e íconos SVG en línea
El prototipo (`design/prototipo/index.html`) simula Wear OS Large Round: 454 px a 320 dpi, es decir
1 dp = 2 px (display 80 px, title 40 px, body 30 px, label 26 px, botón 104 px de alto). Los íconos son
SVG en línea que imitan Material Symbols Rounded, porque el archivo no carga recursos externos.
En la app se usan los Material Symbols reales. En el estado 3 la lista de objetos se desplaza bajo el
botón "Listo" (patrón de lista con botón inferior de Wear) para mantener objetivos táctiles de 48 dp.

### D-011 · 2026-11-13 · Revisión del prototipo: botón inferior más pequeño y área segura
En la revisión del prototipo (tarea 1.3), el usuario aprobó el diseño general, pero señaló que el botón
principal ("Empezar ahora", "Listo", "Guardar") se veía demasiado grande y parecía salirse de la pantalla.
Cambios:
- El botón pasa de 70 % × 52 dp a 132 × 48 dp (el mínimo táctil), a 26 dp del borde inferior.
- Se fija un área segura de 103 dp de radio para texto y controles en reposo, medida en cada pantalla del
  prototipo arriba, en la mitad y al final del desplazamiento.
- La medición encontró textos cortados en Detalle, Confirmar, Explicación, Lista, Rutinas, Ajustes y Resumen.
  Ahora toda pantalla que se desplaza encoge cada fila lo justo para caber (como `TransformingLazyColumn`).
- Inicio ya no muestra el lugar en la línea superior, que rozaba el anillo, y su contenido baja unos píxeles
  para que los nombres de evento largos no lo toquen. Los ejemplos de "Crear por voz" se acortan a dos líneas.

### D-012 · 2026-11-13 · Patrones hápticos finales
Se fijan los tiempos de `DISENO_UI.md` §4. Cambios frente a la primera versión:
- Las pausas entre pulsos suben a 110 ms o más (el estado 3 tenía 80 ms). Con menos, el motor del reloj
  no se detiene a tiempo y los toques se sienten como uno.
- Los patrones se distinguen por cantidad y duración de pulsos, no solo por amplitud, para que también
  funcionen en relojes sin control de amplitud.
- "Vas tarde" deja de ser igual a "Sal ahora": suma un tercer pulso largo, para saber sin mirar si es el
  primer aviso de salida o un recordatorio de que ya va tarde.
- La confirmación usa `EFFECT_CLICK` del sistema (respaldo `[0, 40]`). Comparte "un pulso" con el estado 1,
  pero dura 40 ms frente a 150 ms y siempre llega justo después de un toque en la pantalla.
Pendiente: el emulador no vibra; si hay un reloj físico se prueban en la Fase 5.

### D-013 · 2026-11-13 · Revisión del sistema de diseño (tarea 1.1)
- **Contraste:** se midieron con WCAG 2.1 todas las combinaciones de texto que usa la interfaz (incluidas
  tarjetas presionadas y tarjetas tintadas al 12 %). El tema oscuro cumplía en todo. En el claro fallaban
  por poco `state2Prepare` (4.39:1 sobre `surfaceBright`) y `state4Leave` (4.48:1). Se oscurecieron sin
  cambiar el tono: `#0B6FB8 → #0B6BB1` y `#0E7A4B → #0D7548`. Ahora el peor caso es 4.6:1. Las cifras
  anteriores del documento eran solo sobre `background`; se reemplazan por la tabla completa.
  El script queda en `design/contraste.py`.
- **Tipografía:** el prototipo usaba tamaños fuera de la escala (11, 14, 17 y 18 sp). Se agregan dos
  estilos que la app necesita, `button` (15 sp, 600) y `caption` (12 sp, 500, el mínimo), se fija la
  unidad de `display` en 18 sp y se prohíben los tamaños sueltos.
- **Formas:** se documenta la tarjeta (esquinas de 18 dp, borde de 1 dp), que faltaba. El botón inferior
  y el área segura ya se definieron en D-011.

### D-014 · 2026-11-13 · Versiones del proyecto y SDK
Versiones estables más recientes al crear el proyecto, fijadas en `gradle/libs.versions.toml`:
Gradle 9.9.1, AGP 9.5.1, Kotlin 2.4.21, KSP 2.3.14, Wear Compose 1.7.1, Compose BOM 2026.09.00,
Room 2.8.5, DataStore 1.3.0, lifecycle 2.11.0, activity-compose 1.13.0. Sin alphas.
- AGP 9 trae Kotlin integrado: el módulo no aplica `kotlin-android`. El plugin se declara en la raíz solo
  para fijar Kotlin 2.4.21, igual que el plugin del compilador de Compose.
- Las librerías actuales exigen `compileSdk` 37, así que se compila contra Android 37.2. `targetSdk` queda
  en 36 (Wear OS 6) y `minSdk` en 30 (Wear OS 3).
- Emulador: Wear OS 6 (API 36), perfil Large Round, AVD `ATiempo_Large_Round_API36`.

### D-015 · 2026-11-13 · Tokens propios sobre el ColorScheme de Wear Material 3
Los 12 tokens de `DISENO_UI.md` viven en `ui/theme/Color.kt` (`AppColors`, el único archivo con colores
hexadecimales) y se exponen con `AppTheme.colors`. Para que los componentes estándar de Wear se vean bien
también en tema claro, cada tema construye un `ColorScheme` completo: `background → background`,
`surface → surfaceContainer/surfaceContainerLow`, `surfaceBright → surfaceContainerHigh/outlineVariant`,
`onState → onPrimary`, `late → error`, `state1Finish → tertiary`. Los colores de estado van aparte en
`LocalStateColors` porque Material 3 no tiene roles equivalentes. La escala tipográfica (`AppType`) se
asigna a los slots de la `Typography` de Wear y el cambio de tema anima cada color durante 300 ms.
Hallazgos al probar en el emulador (aplican a toda la Fase 3):
- Algunos componentes de Wear M3 (por ejemplo `RadioButton`) guardan sus colores predeterminados en caché y
  no siguen el cambio de tema hasta volver a entrar a la pantalla. Para que el cambio sea instantáneo, a
  esos componentes se les pasan colores explícitos desde `AppTheme.colors`.
- El `TimeText` predeterminado se ve casi blanco sobre el fondo claro; `MainActivity` lo dibuja con
  `onSurface`.

### D-016 · 2026-11-13 · `Routine` guarda duraciones por defecto
`MODELO_ESTIMACION.md` dice que, sin historial, se usan "los valores por defecto de la rutina", pero la
entidad `Routine` de `ARQUITECTURA.md` solo tenía los ajustes manuales. Se agregan
`defaultFinishMin`, `defaultPrepareMin`, `defaultCheckMin` y `defaultTravelMin`. Además: claves foráneas
con borrado en cascada (evento → sesiones → registros), índices para las consultas por fecha y por
rutina y paso, `dayOfWeek` en formato ISO (1 = lunes) y el esquema exportado en `wear/schemas`.

### D-017 · 2026-11-13 · Forma del historial de ejemplo
El modelo solo aplica el efecto del día con 3 o más registros de ese día, y 14 días traen solo 2 lunes.
Para que la explicación "Los lunes tardas 6 min más" tenga datos sin alargar el historial, la semana más
antigua tiene dos clases el lunes (mañana y laboratorio). Resultado: 13 salidas en 14 días. La semana
reciente queda con 6 salidas, 5 a tiempo, el jueves tarde y el miércoles como mejor día, como el resumen
del prototipo. Los datos son deterministas (semilla fija) y relativos a la fecha del primer arranque; el
primer evento queda unas 2 horas después del arranque para que la demo muestre una cuenta regresiva.
Nota para la Fase 3: el emulador viene en UTC; para la demo conviene ponerlo en America/Bogota.
