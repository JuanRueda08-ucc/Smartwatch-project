# Arquitectura

## Visión general

App independiente para Wear OS (no requiere teléfono). Todo corre en el reloj: interfaz, motor de
estados, avisos, historial y modelo de estimación.

```
┌─────────────────────────── wear (módulo único) ───────────────────────────┐
│  ui/        Pantallas Compose + ViewModels (StateFlow)                     │
│  domain/    PrepStateMachine, DurationEstimator, EventParser (Kotlin puro) │
│  data/      Room (DAOs), DataStore, repositorios, ContextProvider falso    │
│  alerts/    AlarmScheduler, AlertReceiver, Haptics                         │
│  AppContainer   Dependencias creadas a mano                                │
└────────────────────────────────────────────────────────────────────────────┘
```

## Estructura de paquetes

```
wear/src/main/java/com/atiempo/wear/
├── MainActivity.kt
├── ATiempoApp.kt              # Application, crea AppContainer, siembra datos de ejemplo
├── AppContainer.kt
├── ui/
│   ├── theme/                 # Color.kt (claro y oscuro), StateColors.kt, Type.kt, Shape.kt, Theme.kt
│   ├── navigation/            # Routes.kt, AppNavHost.kt
│   ├── components/            # StateRing, TimelineDot, CountdownText, ItemChip...
│   ├── home/  events/  detail/  create/  prep/  explain/  routines/  summary/  settings/
├── domain/
│   ├── model/                 # PrepStep, PrepPlan, Adjustment, ParsedEvent
│   ├── PrepStateMachine.kt
│   ├── DurationEstimator.kt
│   └── EventParser.kt
├── data/
│   ├── db/                    # AppDatabase, entidades, DAOs
│   ├── repo/                  # EventRepository, RoutineRepository, HistoryRepository
│   ├── context/               # ContextProvider (interfaz) + FakeContextProvider
│   └── seed/                  # SampleData.kt
└── alerts/
    ├── AlarmScheduler.kt
    ├── AlertReceiver.kt
    └── Haptics.kt
```

El paquete `com.atiempo.wear` es provisional; se puede cambiar al crear el proyecto (anotar en DECISIONES.md).

## Modelo de datos (Room)

```kotlin
@Entity data class Routine(
    @PrimaryKey val id: Long,
    val name: String,                 // "Clase", "Cita", "Salida social"
    val items: List<String>,          // objetos a revisar (TypeConverter)
    val defaultFinishMin: Int, val defaultPrepareMin: Int,   // sin historial (D-016)
    val defaultCheckMin: Int,  val defaultTravelMin: Int,
    val manualFinishMin: Int?, val manualPrepareMin: Int?,
    val manualCheckMin: Int?,  val manualTravelMin: Int?,
)

@Entity data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startAt: Long,                // epoch millis
    val routineId: Long,
    val place: String?,
    val baseTravelMin: Int,           // tiempo de desplazamiento base
    val createdByVoice: Boolean,
)

@Entity data class PrepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val startedAt: Long,
    val leftAt: Long?,                // cuando confirmó "Ya salí"
    val minutesLate: Int?,            // negativo = salió con margen
)

@Entity data class StepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val routineId: Long,
    val step: PrepStep,               // FINISH, PREPARE, CHECK, LEAVE
    val dayOfWeek: Int,
    val plannedMin: Int,
    val actualMin: Int,
)
```

## Motor de estados

`PrepStateMachine` es una clase pura que recibe un `PrepPlan` (horas de inicio de cada paso) y eventos:
`Tick(now)`, `Confirm`, `Skip`, `MovementDetected`. Emite el estado actual, minutos restantes y si va tarde.

```
IDLE → FINISH → PREPARE → CHECK → LEAVE → DONE
          (Confirm o fin del tiempo avanza; Confirm registra la duración real)
```

Si el usuario confirma antes de tiempo, el siguiente paso empieza en ese momento y conserva su hora de fin
del plan (el paso siguiente tiene más tiempo; la hora de salida no cambia).

Detalles (D-022): es un reductor puro `reduce(estado, evento) → (estado nuevo, efectos)`. Los efectos
(`StepStarted`, `StepCompleted`, `LateReminder`, `Finished`) los ejecuta quien lo usa: vibrar, guardar el
`StepRecord`, cerrar la `PrepSession`. El paso LEAVE dura hasta la **hora límite** = salida + margen; pasada
esa hora se va tarde, con recordatorios cada 60 s (máximo 3). `minutesLate` se mide contra la hora límite:
0 o menos es a tiempo. Si la app estuvo dormida y se saltaron pasos, va directo al que toca con un solo aviso.
La detección de movimiento (Activity Recognition) queda fuera de esta entrega; el evento
`MovementDetected` existe en el motor para mostrar que el diseño lo contempla (D-004).

## Preparación en curso

`data/session/PrepSessionManager` (en `AppContainer`) mantiene la preparación activa, una a la vez. Cada
segundo le pasa un `Tick` al motor y ejecuta sus efectos: vibrar con `alerts/Haptics`, guardar un `StepRecord`
por cada "Listo" y cerrar la `PrepSession` con `leftAt` y `minutesLate`. Si no hay sesión, arranca sola la del
evento cuyo primer aviso llegó (`AutoStart`, Kotlin puro) y la navegación abre su pantalla. La sesión activa vive
en memoria: si el sistema mata el proceso se pierde (pendiente para la tarea 3.8, D-023).

## Reloj y modo demo

Toda la app lee la hora de `domain/time/Clock`. En `AppContainer` es un `DemoClock`: normal, marca la hora del
sistema; en modo demo avanza 12 veces más rápido (1 min en 5 s) y puede saltar al próximo aviso. Las alarmas se
programan con `Clock.toWallTime`, así que también suenan a tiempo durante la demo, y el `TimeText` muestra la hora
del reloj de la app (`ClockTimeSource`).

## Avisos

- `AlarmScheduler` programa con `AlarmManager.setExactAndAllowWhileIdle` una alarma por paso.
  Permiso `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`.
- `AlertReceiver` lanza una notificación con `fullScreenIntent` que abre `prep/{sessionId}` y
  ejecuta el patrón háptico del paso.
- Permisos en el manifiesto: `VIBRATE`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK`.
  `RECORD_AUDIO` no hace falta porque la entrada de voz la hace el sistema (`RemoteInput`).

## Tema

`SettingsRepository` expone `themeMode: Flow<ThemeMode>` (`LIGHT` por defecto, `DARK`, `AUTO`) desde
DataStore. `MainActivity` lo observa y lo pasa a `ATiempoTheme(themeMode)`, que elige el `ColorScheme`
y los `StateColors` correspondientes. En modo `AUTO` se recalcula cada minuto con la hora local.

## Voz

Entrada de texto de Wear con `RemoteInput` (`androidx.wear:wear-input`): dictado o teclado del sistema,
sin permiso de micrófono (D-020). El texto pasa a `EventParser`,
un intérprete por reglas (expresiones regulares) que reconoce:
- Días: hoy, mañana, pasado mañana, lunes…domingo.
- Horas: "a las 7", "a las 3 y media", "a las 15:30", "7 de la mañana/tarde/noche", "al mediodía".
- Rutina por palabras clave: clase/parcial/universidad → Clase; cita/médico/reunión → Cita;
  fiesta/cine/comida → Salida social.
- Lugar: lo que sigue a "en" o "a la/al".
Devuelve `ParsedEvent` con un nivel de confianza por campo para resaltar lo dudoso en la pantalla de confirmación.

## Versiones

Usar la última versión estable al crear el proyecto y fijarla en `gradle/libs.versions.toml`:
Android Gradle Plugin, Kotlin 2.x con el plugin de Compose, `androidx.wear.compose:compose-material3`,
`compose-foundation`, `compose-navigation`, `androidx.room` (con KSP), `androidx.datastore`,
`androidx.wear:wear-tooling-preview`. `minSdk 30`, `targetSdk` y `compileSdk` el más reciente estable.
