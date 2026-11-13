# Plan de trabajo

Entrega: **martes 17 de noviembre de 2026, 11:59 PM (UTC-5)**.
Marca cada casilla en el mismo commit que termina la tarea.

Prioridad general: **interfaz pulida > funciones completas > "IA" sofisticada**.

---

## Fase 1. Diseño de la interfaz (viernes 13 – sábado 14)

Objetivo: tener todas las pantallas definidas y aprobadas antes de programarlas.

- [x] 1.1 Revisar y ajustar el sistema de diseño de `DISENO_UI.md` (paletas clara y oscura, tipografía, formas).
- [x] 1.2 Prototipo navegable en HTML (`design/prototipo/index.html`) con marco redondo de 454×454 px
      que muestre todas las pantallas de `DISENO_UI.md` y la navegación entre ellas, en tema claro
      por defecto y con un interruptor para ver el tema oscuro.
- [x] 1.3 Revisión del prototipo con el usuario; anotar cambios en `DECISIONES.md`.
- [x] 1.4 Definir los 4 patrones hápticos finales (tiempos en ms) en `DISENO_UI.md`.

**Listo cuando:** el usuario aprueba el prototipo.

## Fase 2. Base de la app Wear OS (sábado 14)

- [x] 2.1 Crear el proyecto Android: módulo `wear`, `minSdk 30`, Kotlin, Compose, version catalog.
- [x] 2.2 Tema: `ui/theme` con `ColorScheme` claro (predeterminado) y oscuro, `LocalStateColors`,
      tipografía y formas de `DISENO_UI.md`. Preferencia de tema en DataStore.
- [x] 2.3 Navegación (`SwipeDismissableNavHost`) con todas las rutas como pantallas vacías.
- [x] 2.4 Room: entidades `Event`, `Routine`, `PrepSession`, `StepRecord` (ver `ARQUITECTURA.md`).
- [ ] 2.5 Datos de ejemplo: 3 rutinas y 2 semanas de historial sembrados en el primer arranque
      (para que la demo y el resumen semanal tengan contenido).

**Listo cuando:** la app abre en el emulador y se puede navegar entre todas las pantallas vacías.

## Fase 3. Pantallas y flujo principal (domingo 15 – lunes 16)

- [ ] 3.1 Inicio: próximo evento con cuenta regresiva a "empieza a prepararte".
- [ ] 3.2 Lista de eventos (`TransformingLazyColumn`).
- [ ] 3.3 Detalle del evento: línea de tiempo de los 4 estados con horas calculadas.
- [ ] 3.4 Crear evento por voz (`RecognizerIntent`) + intérprete de frases en español.
- [ ] 3.5 Confirmar evento interpretado (editar título, hora, rutina, lugar).
- [ ] 3.6 Motor de estados (`PrepStateMachine`) con pruebas unitarias.
- [ ] 3.7 Pantallas de los 4 estados con anillo de progreso y botón "Listo".
- [ ] 3.8 Avisos programados (`AlarmManager` + notificación de pantalla completa) y vibraciones
      con `VibrationEffect.createWaveform`.
- [ ] 3.9 Pantalla "¿Por qué te avisé antes?".
- [ ] 3.10 Ajuste manual de estimaciones por rutina y por paso.
- [ ] 3.11 Resumen semanal de puntualidad.
- [ ] 3.11b Ajustes: selector de tema (Claro / Oscuro / Automático) y vibraciones.
- [ ] 3.12 Modo demo: botón oculto que acelera el reloj (1 min = 5 s) para presentar el flujo completo.

**Listo cuando:** se puede crear un evento por voz y recorrer los 4 estados hasta "Saliste a tiempo".

## Fase 4. Modelo de estimación (lunes 16)

- [ ] 4.1 `DurationEstimator` según `MODELO_ESTIMACION.md`, con pruebas.
- [ ] 4.2 `ContextProvider` falso para clima y tráfico, con valores controlables desde el modo demo.
- [ ] 4.3 Generar el texto de explicación de cada ajuste.
- [ ] 4.4 Registrar duraciones reales al confirmar cada paso y recalcular.

## Fase 5. Pulido y entrega (martes 17)

- [ ] 5.1 Revisar todas las pantallas en tema claro y oscuro, en Large Round, Small Round y Square, y con texto grande.
      Revisar también el modo ambiente.
- [ ] 5.2 Accesibilidad: `contentDescription`, contraste ≥ 4.5:1, objetivos táctiles ≥ 48 dp.
- [ ] 5.3 Ícono de la app y nombre final.
- [ ] 5.4 Capturas de cada pantalla en `design/capturas/` (claro como principal, más una muestra en oscuro)
      y un video corto de la demo.
- [ ] 5.5 `README.md` con descripción, capturas, cómo compilar y decisiones de diseño.
- [ ] 5.6 Último commit y push antes de las 11:00 PM.

---

## Recortes en orden (si falta tiempo)

1. Modo demo acelerado → se presenta con eventos a 2–3 minutos.
2. Pantalla cuadrada → solo redonda.
3. Ajuste manual por paso → solo por rutina.
4. Voz → el intérprete se prueba con texto escrito.

Nunca se recortan: los 4 estados con vibración, la pantalla de explicación, el resumen semanal
y los dos temas (claro y oscuro).
