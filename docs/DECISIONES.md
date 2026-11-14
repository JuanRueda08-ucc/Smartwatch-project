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

### D-018 · 2026-11-14 · Pantalla de Inicio (tarea 3.1)
- **Plan:** `PrepPlanner` (Kotlin puro) calcula las horas de cada estado hacia atrás desde el evento, con el
  margen de `MODELO_ESTIMACION.md` (10 % del total, mínimo 3 min). Mientras no exista el estimador (Fase 4),
  las duraciones salen de la rutina (ajuste manual o valor por defecto) y el trayecto, del evento.
- **Reloj:** toda la app lee la hora de `Clock` (en `AppContainer`), para que el modo demo pueda acelerarla.
- **Anillo:** se llena durante los últimos 60 min antes de empezar a prepararse.
- **Estado vacío:** "Crear con voz" con ícono no cabe en el botón de 132 dp y ensancharlo lo sacaría del área
  segura (D-011). El micrófono pasa a ser la ilustración, el botón queda solo con texto y el título se acorta a
  "Sin eventos" para que todo quepa sobre el botón.
- **Corona:** dentro de `SwipeDismissableNavHost` una pantalla solo recibe la corona si pide el foco con
  `requestFocusOnHierarchyActive()`; un `requestFocus()` normal no funciona. Aplica a todas las pantallas.
- **Emulador:** zona horaria puesta en America/Bogota.

### D-019 · 2026-11-14 · Detalle del evento (tarea 3.3) y listas que se curvan
- **"Empezar ahora" va al final de la lista**, no fijo abajo. Fijo tapaba casi media pantalla y el primer
  vistazo mostraba un solo paso de la línea de tiempo. Es una acción secundaria (lo normal es esperar el
  aviso), así que el primer vistazo se lo queda la línea de tiempo.
- **Paso resaltado por la hora:** si ya empezó la ventana de preparación, la fila del paso que toca según el
  reloj va resaltada y las anteriores llevan check. Cuando exista el motor de estados (3.6) se usará el estado
  real de la sesión.
- **Regla de `TransformingLazyColumn`:** `transformedHeight` solo va en elementos que también reciben
  `SurfaceTransformation` (botones, tarjetas, encabezados). En un texto suelto encoge la fila pero no el
  contenido, y el texto se monta sobre la fila vecina al llegar al borde. Corregido también en Eventos.
- **Provisional:** "Empezar ahora" navega a `prep/{eventId}` hasta que la tarea 3.6 cree sesiones reales.

### D-020 · 2026-11-14 · Crear por voz con RemoteInput y ejemplos tocables (tarea 3.4)
- **RemoteInput en lugar de RecognizerIntent.** En Wear, `RecognizerIntent` lo atiende Gboard y devuelve una
  lista vacía cuando la persona escribe con el teclado en vez de dictar. `RemoteInput` (la entrada estándar
  de Wear, `androidx.wear:wear-input` 1.2.0) ofrece dictado y teclado y devuelve el texto en ambos casos.
  Tampoco necesita permiso de micrófono.
- **Ejemplos tocables.** Los ejemplos de la pantalla pasan por el mismo intérprete que la voz. Son el
  respaldo de la demo si la voz falla, como prevé el recorte 4 de `PLAN.md`.
- **No verificado en el emulador:** el emulador no tiene micrófono, y su teclado del sistema no devolvió el
  texto escrito (ni con `RecognizerIntent` ni con `RemoteInput`). Sí se verificó que la entrada se abre con la
  pregunta de la app, que cancelar vuelve a la pantalla y que los ejemplos llegan a Confirmar. Probar el
  dictado en un reloj real en la Fase 5.
- **Intérprete (`EventParser`):** por reglas, sin tildes ni mayúsculas para comparar, con 15 pruebas. La hora
  sin "de la mañana/tarde" se asume: de 1 a 6, de la tarde; de 7 a 12, como se dijo. Esas horas, el día
  deducido, la rutina por defecto y el lugar ausente quedan marcados como dudosos para "Revisa esto" (3.5).

### D-021 · 2026-11-14 · Confirmar evento (tarea 3.5)
- **Componentes de Wear para editar:** `DatePicker` y `TimePicker` (24 horas, igual que el resto de la app),
  opciones de radio para la rutina y la entrada de texto de Wear para título y lugar. El lugar también ofrece
  "Sin lugar", porque es opcional.
- **Selectores como pantallas de la navegación**, no como capas: así deslizar para volver regresa a la
  confirmación y no saca de todo el flujo. Todos editan el mismo borrador (`DraftStore`).
- **Guardar** queda fijo abajo porque es la acción principal de la pantalla (a diferencia de "Empezar ahora",
  D-019). Se deshabilita si la hora ya pasó. Al guardar, el evento usa el trayecto por defecto de su rutina y
  aparece la confirmación animada de Wear.
- **Rutinas:** cada tipo del intérprete corresponde a una de las 3 rutinas sembradas (ids 1, 2 y 3).
- **Textos del sistema:** los selectores muestran sus etiquetas ("Hour") en el idioma del reloj; el emulador
  está en inglés.
- **No verificado:** la versión oscura en el emulador (la entrada de voz del sistema se traba al cerrarla), el
  dictado de título y lugar (sin micrófono) y el caso "Esa hora ya pasó" en pantalla (sí está probado con
  pruebas unitarias).

### D-022 · 2026-11-14 · Motor de estados (tarea 3.6)
- **Reductor puro con efectos.** `PrepStateMachine.reduce(estado, evento)` devuelve el estado nuevo y una lista
  de efectos; no vibra ni guarda nada. Así se prueba sin Android (15 pruebas) y la tarea 3.7/3.8 decide cómo
  ejecutar cada efecto.
- **Hora límite de salida = salida planeada + margen.** El estado 4 "Sal ahora" cuenta hasta esa hora (como el
  prototipo: 7:40 a 7:42). Salir dentro del margen es "a tiempo"; `minutesLate` se mide contra la hora límite.
- **Un solo aviso al ponerse al día.** Si la app estuvo dormida y pasaron varios pasos, salta al que toca y
  vibra una vez, en lugar de encadenar varios patrones.
- **Sin "Listo" no hay registro.** Solo los pasos confirmados guardan duración real (MODELO_ESTIMACION); los
  que avanzan solos no se sabe cuánto duraron.

### D-023 · 2026-11-14 · Pantallas de los 4 estados y sesión en curso (tarea 3.7)
- **Gestor de la sesión en curso** (`PrepSessionManager`): ejecuta los efectos del motor (vibrar, guardar cada
  "Listo", cerrar la sesión) y, con la app abierta, arranca sola la preparación del evento cuyo primer aviso
  llegó y abre su pantalla. Con la app cerrada lo hará la alarma de la tarea 3.8.
- **Vibraciones ya activas con la app abierta**: los patrones de §4 al empezar cada estado, el clic al tocar
  "Listo" seguido del patrón del estado nuevo, y "Vas tarde". Siguen el interruptor de Ajustes.
- **Pantalla de estado compacta**: con el nombre en dos líneas ("Termina lo que haces") el mensaje quedaba bajo
  el botón. El ícono va a 22 dp sin círculo y los mensajes en `label`, como en el prototipo.
- **Inicio durante la preparación**: muestra el estado actual con su color, los minutos y "Volver al aviso".
  Al terminar, Inicio se salta los eventos para los que ya se salió.
- **Cuenta regresiva en días** a más de 24 horas ("3 d"): "71:58 h" no se lee de un vistazo.
- **Pendiente:** la sesión activa vive en memoria; si el sistema mata el proceso se pierde (la sesión queda
  guardada sin salida y no vuelve a arrancar). Recuperarla al reiniciar va con las alarmas (3.8). La Lista de
  eventos todavía no marca los eventos terminados.

### D-024 · 2026-11-14 · Avisos programados (tarea 3.8)
- **Una sola alarma exacta** (`AlarmScheduler`, `USE_EXACT_ALARM`) a la próxima hora en que algo cambia:
  primer aviso, fin de cada paso, hora límite y recordatorios de "tarde" (`PrepStateMachine.nextWakeUp`,
  `AutoStart.nextFirstAlertAt`, con pruebas). Al sonar, `AlertReceiver` despierta la app, avanza el motor (que
  vibra) y publica el aviso; luego programa la siguiente. Tras reiniciar o actualizar, `RescheduleReceiver`.
- **Recuperación:** si el sistema cerró la app, al volver a abrirse retoma la sesión sin "Ya salí" de un evento
  que aún no empieza (`PrepStateMachine.resume`), salvo que ese evento tenga una sesión más reciente.
- **Aviso:** notificación de importancia alta, sin sonido ni vibración propios (la vibración es el patrón
  háptico), con el título del estado, "Evento · faltan N min" y el color del estado. Tocarla abre la pantalla
  del estado. Se pide permiso de notificaciones al abrir la app.
- **Verificado en el emulador:** con la app cerrada y la pantalla apagada, la alarma despertó la app, arrancó la
  preparación y publicó el aviso correcto.
- **Pendiente:** abrirse solo a pantalla completa requiere el acceso especial `USE_FULL_SCREEN_INTENT`, que
  Android 14+ concede por defecto solo a apps de alarma y llamadas; en el emulador quedó rechazado. Falta guiar
  a la persona a concederlo en Ajustes (3.11b) y probar el tema oscuro de los avisos.

### D-025 · 2026-11-14 · Pantalla "¿Por qué te avisé antes?" (tarea 3.9)
- **Razones como datos** (`Adjustment` con `AdjustmentReason`: base aprendida, día, lluvia, tráfico, ajuste manual,
  margen). La pantalla arma cada frase desde `strings.xml`, como pide AGENTS.md, y el estimador de la Fase 4
  solo tendrá que producir esa lista.
- **Fuente provisional:** hasta que exista el `DurationEstimator`, la lista sale de la rutina: cada ajuste manual
  distinto del valor por defecto y el margen de seguridad. La preview ya muestra el ejemplo completo de
  DISENO_UI §3.8 (lunes, tráfico, lluvia).
- Probado en el emulador con un evento de Clase: "+5 min · Margen…", "−2 min · Tú ajustaste «Revisa tus cosas»
  a 5 min" y "En total te aviso 3 min antes".

### D-026 · 2026-11-14 · Ajustar estimaciones y Rutinas (tarea 3.10)
- **Base aprendida ya calculada:** `LearnedDuration.ema` (α = 0.3, MODELO_ESTIMACION) sobre el historial de cada
  rutina y paso, con pruebas; el DurationEstimator de la Fase 4 la reutilizará.
- **Valor que se muestra:** el ajuste manual, si no lo aprendido, si no el valor por defecto de la rutina; la lista de
  Rutinas y el editor usan la misma regla. **Hasta la Fase 4 los avisos (el plan) todavía usan el ajuste manual o el
  valor por defecto, no lo aprendido**: conectarlo es la tarea 4.1.
- **Guardado al instante** con − y + (sin "Guardar"), de 1 a 90 min; "Usar lo aprendido" quita el ajuste.
- **Corona:** no ajusta los minutos en esta entrega (la especificación la daba como alternativa).
- **Para la demo:** las pruebas en el emulador dejaron duraciones reales de 1 min en el historial de Cita; borrar
  los datos de la app antes de presentar (Fase 5).

### D-027 · 2026-11-14 · Resumen semanal (tarea 3.11)
- **Últimos 7 días incluido hoy**, no la semana de lunes a domingo: el día de la entrega (martes) la semana
  calendario tendría solo dos días, y así el resumen refleja también las salidas de la demo de ese día. Las barras
  van del más antiguo a hoy, con la inicial del día.
- Solo cuentan las salidas con "Ya salí"; 0 min tarde o menos es a tiempo. Un día con alguna salida tarde es día
  tarde. El mejor día es el de más margen. Frase con plurales ("1 vez" / "6 veces").
- **Corrección:** la recuperación de la sesión en curso (D-024) ignora una sesión que "empezó" después de ahora;
  venía de cambiar la hora del reloj en las pruebas y dejaba una preparación fantasma en Inicio.

### D-028 · 2026-11-14 · Ajustes (tarea 3.11b)
- Componentes de Wear (`RadioButton` con la miniatura como ícono y `SwitchButton`) con colores explícitos del tema,
  para que el cambio de tema se vea al instante (D-015). Todo se guarda en DataStore al tocar.
- **Avisos a pantalla completa:** la fila solo aparece si `canUseFullScreenIntent()` es falso, y abre el ajuste del
  sistema (con alternativas, porque en Wear algunas pantallas del sistema no existen). En el emulador el permiso
  figura como concedido; el aviso de la prueba de D-024 no se abrió solo porque el reloj estaba en reposo. Queda
  para revisar en un reloj real en la Fase 5.
- **Modo demo:** se descubre manteniendo presionada la versión; su interruptor queda visible mientras esté activo.
  Acelerar el reloj es la tarea 3.12.

### D-029 · 2026-11-14 · Modo demo (tarea 3.12)
- **`DemoClock`** (Kotlin puro, 6 pruebas): con el modo demo activo, el reloj de la app avanza 12 veces más rápido
  desde el momento en que se activa; al apagarlo vuelve a la hora real. `jumpTo` salta y sigue corriendo.
- **Alarmas en hora real:** el gestor programa la alarma con `Clock.toWallTime`, así los avisos suenan a tiempo
  también en la demo y con la app cerrada.
- **"Ir al próximo aviso"** en Ajustes (solo en modo demo): salta a 3 s de la demo antes del siguiente aviso, para
  presentar el flujo completo sin esperar. Equivale al "Saltar al siguiente aviso" del prototipo.
- El `TimeText` muestra la hora del reloj de la app, en el mismo formato "7:05".
- **Limitaciones:** si el sistema reinicia la app en modo demo, la hora acelerada vuelve a empezar desde la hora
  real. El tema Automático sigue la hora real, no la de la demo.
- Probado en el emulador: salto → el primer aviso se abre solo → avanza solo a "Prepárate" → Listo → Ya salí →
  "Saliste a tiempo".

### D-030 · 2026-11-14 · Estimador de duraciones (tarea 4.1)
- **`DurationEstimator`** (Kotlin puro, 10 pruebas) sigue `MODELO_ESTIMACION.md`: base = ajuste manual, si no la
  media móvil (α = 0.3), si no el valor por defecto; efecto del día con 3 o más registros de ese día, limitado a
  ±10 min; lluvia (+15 %, mínimo 2 min) y tráfico (+10 % / +25 %) solo en el trayecto. Cada término distinto de cero
  es una razón (`Adjustment`), ordenadas de mayor a menor efecto. El margen lo sigue sumando `PrepPlanner`.
- **Un ajuste manual no borra el día ni el contexto:** reemplaza la base y su razón dice cuánto movió respecto a lo
  aprendido; el efecto del día y el clima se suman encima.
- **Conectado en toda la app:** `Routine.planFor(event, data)` y `Routine.explain(event, data)` usan el estimador con
  el historial real (`StepRecordDao.observeAll` → `EstimationData`, agrupado por rutina y paso). Inicio, Eventos,
  Detalle, "¿Por qué te avisé antes?", el arranque automático y las alarmas ven los mismos tiempos. Cierra la
  limitación de D-026.
- **El plan se fija al empezar:** lo que se registre durante una preparación cuenta para la siguiente, no mueve los
  avisos de la que está en curso.
- El trayecto por defecto es el del evento (`baseTravelMin`, depende del lugar), no el de la rutina.
- El contexto (clima y tráfico) llega vacío hasta la tarea 4.2.
- Probado en el emulador: "Cine con Laura" muestra "Últimamente tardas 28 min en llegar" (+3) y las demás razones
  aprendidas. Los registros de 1 a 3 min que dejaron las pruebas bajan la media de Clase: limpiar los datos de la
  app antes de la presentación (Fase 5).

### D-031 · 2026-11-14 · Clima y tráfico simulados (tarea 4.2)
- **`ContextProvider`** (interfaz) y **`FakeContextProvider`** en `data/context/`, como pide ARQUITECTURA: combina
  el modo demo con la lluvia y el tráfico guardados en DataStore. 4 pruebas.
- **Solo cuentan con el modo demo activo.** Fuera de él el día es tranquilo (sin lluvia, tráfico bajo), aunque haya
  valores guardados: así una lluvia olvidada después de la presentación no adelanta los avisos de verdad.
- **Controles en Ajustes** bajo "Simulación", visibles solo en modo demo: un interruptor de lluvia y un botón que
  rota el tráfico bajo → medio → alto (un solo toque por cambio, sin abrir otra pantalla).
- El contexto entra al estimador por `AppContainer.estimation`, así que Inicio, Eventos, Detalle, la explicación y
  las alarmas cambian a la vez. El plan de una preparación en curso no se mueve (D-030).
- Probado en el emulador: con lluvia y tráfico alto, "Reunión de grupo" pasa de salir a las 18:47 a las 18:42 y la
  explicación muestra "Tráfico alto hacia Biblioteca" (+3) y "Hay lluvia prevista" (+2).

### D-032 · 2026-11-14 · Texto de la explicación (tarea 4.3)
- **`ReasonPhrases`** (lógica pura, 7 pruebas) decide la frase de cada razón y sus datos (`Phrase` con el texto de
  `strings.xml` y sus argumentos); la pantalla solo la resuelve. Así la elección de la frase se prueba sin Android.
- **Efecto del día con minutos:** "Los martes tardas 3 min más en prepararte", como pide MODELO_ESTIMACION. Si ese
  día eres más rápido: "… min menos en …". Ninguna frase lleva cifras negativas; el signo va en el efecto.
- **Título y total siguen el signo:** con total negativo el título es "¿Por qué te aviso más tarde?" y el total
  "En total te aviso N min después"; con cero, "Te aviso a la hora de siempre".
- **Orden:** de mayor a menor efecto con signo, como dice el modelo: primero lo que adelanta el aviso y al final lo
  que lo atrasa.
- Espacio que no se corta entre la cifra y "min", para que "3" y "min" no queden en líneas distintas.
- Probado en el emulador con "Cita médica" (martes): aparecen los efectos del martes con sus minutos y el título
  "¿Por qué te aviso más tarde?" (el total es negativo por los registros cortos de las pruebas; ver D-030).
