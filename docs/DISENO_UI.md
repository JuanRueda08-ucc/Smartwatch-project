# Diseño de interfaz

Este documento es la especificación visual y de interacción. Cualquier pantalla se construye a partir de aquí.

## 1. Principios

1. **Un vistazo, una acción.** Cada pantalla se entiende en menos de 2 segundos y ofrece como máximo
   una acción principal. Muñeca levantada, lectura, muñeca abajo.
2. **El tiempo es el protagonista.** El dato más grande de cada pantalla es siempre un tiempo
   ("12 min", "7:40").
3. **Calma, no alarma.** La app acompaña; no regaña. Lenguaje amable y directo, sin signos de
   exclamación repetidos, sin rojo salvo cuando realmente se va tarde.
4. **El color dice el estado.** Cada uno de los 4 estados tiene un color propio, que se repite en
   la pantalla, el anillo de progreso, la notificación y la línea de tiempo.
5. **Transparente.** Toda alerta que se movió explica por qué y se puede corregir.
6. **Claro primero.** El tema claro es el principal; el oscuro debe verse igual de cuidado.
7. **Diseñado para lo redondo.** Contenido centrado, márgenes seguros del 10 % en los bordes,
   listas que se curvan con `TransformingLazyColumn`.

## 2. Sistema de diseño

### Temas: claro (principal) y oscuro

La app tiene **dos temas** y el **claro es el principal**: es el que se ve al instalar, el que aparece en
el prototipo, en las capturas y en la presentación. El oscuro es la alternativa.

- El usuario elige en **Ajustes → Tema**: *Claro* (predeterminado), *Oscuro* o *Automático*
  (claro de 6:00 a 18:59, oscuro de 19:00 a 5:59). Se guarda en DataStore.
- Wear OS no tiene un ajuste de sistema de tema claro, por eso la elección vive dentro de la app.
- Wear Compose Material 3 está pensado para oscuro: el tema claro se construye pasando un
  `ColorScheme` propio completo a `MaterialTheme`. Los colores de estado van en un
  `CompositionLocal` aparte (`LocalStateColors`) con una versión clara y una oscura.
- **Modo ambiente** (pantalla siempre activa en reposo): siempre fondo negro con texto y contornos
  en gris claro, sin rellenos, en ambos temas. Así lo exige Wear OS para ahorrar batería.
- Cada pantalla se revisa en los dos temas. Ningún color se escribe dentro de una pantalla; siempre
  sale del tema.

### Colores

Mismos nombres de token en ambos temas. `onState` es el color del texto sobre un botón relleno con
el color de un estado.

| Token | Claro (principal) | Oscuro | Uso |
|---|---|---|---|
| `background` | `#F6F7FB` | `#000000` | Fondo |
| `surface` | `#FFFFFF` | `#1B1B1F` | Tarjetas y chips |
| `surfaceBright` | `#E8EAF2` | `#2A2A30` | Tarjeta presionada, bordes, pista del anillo |
| `onSurface` | `#1A1B22` | `#F2F2F5` | Texto principal |
| `onSurfaceVariant` | `#5A5D6B` | `#A8A8B3` | Texto secundario |
| `primary` | `#2F5BD3` | `#8AB4FF` | Acciones generales, marca |
| `state1Finish` | `#5B3FD1` | `#B39DFF` | Estado 1: Termina lo que haces (violeta, calma) |
| `state2Prepare` | `#0B6BB1` | `#5CC8FF` | Estado 2: Prepárate (azul) |
| `state3Check` | `#8A5A00` | `#FFC857` | Estado 3: Revisa tus cosas (ámbar) |
| `state4Leave` | `#0D7548` | `#5EE6A0` | Estado 4: Sal ahora (verde, "adelante") |
| `late` | `#C62828` | `#FF6B6B` | Solo cuando el plan ya va tarde |
| `onState` | `#FFFFFF` | `#000000` | Texto sobre botones de estado |

En el tema claro los colores de estado son más oscuros y saturados para que se lean sobre fondo
claro; en el oscuro son más luminosos. Para fondos suaves de tarjeta (por ejemplo, la tarjeta del
estado activo) se usa el color del estado al 12 % de opacidad sobre `surface`.

Contraste medido (WCAG 2.1, revisión de la tarea 1.1). Se midió cada texto (`onSurface`,
`onSurfaceVariant`, `primary`, los 4 estados y `late`) sobre `background`, `surface`, `surfaceBright` y
sobre el tinte del 12 % de su propio color, más `onState` sobre cada botón relleno:

| Tema | Texto sobre fondos (mínimo – máximo) | `onState` sobre botones | Peor caso |
|---|---|---|---|
| Claro | 4.6:1 – 17.2:1 | ≥ 5.6:1 | `state4Leave` sobre su tinte, 4.6:1 |
| Oscuro | 5.1:1 – 18.8:1 | ≥ 7.6:1 | `late` sobre `surfaceBright`, 5.1:1 |

Todo cumple el mínimo de 4.5:1 para texto. `surfaceBright` sobre `background` (1.1:1 claro,
1.5:1 oscuro) es decorativo: bordes de tarjeta y pista del anillo. Lo que comunica información (el arco
del anillo, los puntos de la línea de tiempo) usa colores de estado, que tienen ≥ 4.9:1 frente al fondo.
Si se cambia algún valor, repetir la medición con `python design/contraste.py` (Fase 5).
Nunca se usa solo el color para comunicar el estado: siempre va con ícono y texto. En el resumen semanal,
las barras "tarde" además van rayadas.

### Tipografía

Fuente del sistema (Roboto Flex vía Material 3 de Wear). Escala:

| Estilo | Tamaño | Uso |
|---|---|---|
| `display` | 40 sp, peso 600, cifras tabulares; unidad ("min", "h", "d", "%") a 18 sp en `onSurfaceVariant`. Menos de una hora: "12 min"; menos de un día: "1:05 h"; más: "3 d" | Cuenta regresiva principal |
| `title` | 20 sp, peso 600 | Nombre del estado o del evento |
| `body` | 15 sp | Texto general |
| `button` | 15 sp, peso 600 | Texto de botones, chips y nombres en tarjetas |
| `label` | 13 sp, peso 500 | Etiquetas, hora secundaria |
| `caption` | 12 sp, peso 500 | Insignias, subtítulos de fila, línea de aprendizaje. Nunca más pequeño |

Las cifras de tiempo usan `fontFeatureSettings = "tnum"` para que no "bailen" al cambiar.
No se usan tamaños fuera de esta escala. `title` y `body` usan corte de línea balanceado
(`LineBreak.Heading`) para no dejar una palabra sola en la última línea.

### Formas y espaciado

- Botones y chips: forma de píldora (`CircleShape` en los extremos).
- Tarjetas: esquinas de 18 dp, relleno `surface` y borde de 1 dp en `surfaceBright` (en el tema claro,
  `surface` sobre `background` casi no se distingue sin el borde).
- Botón principal en la parte inferior: **132 × 48 dp** (≈ 58 % del ancho en Large Round), con su borde
  inferior a 26 dp del borde de la pantalla. Así sus extremos quedan al menos 14 dp dentro del círculo.
  El contenido que se desplaza pasa por debajo de él con un degradado al color `background`.
- **Área segura:** ningún texto ni control en reposo pasa de **103 dp desde el centro** (206 px en
  Large Round). El anillo de progreso ocupa la franja de 107 a 112 dp y no se invade.
- En toda pantalla que se desplaza, cada fila se encoge lo justo para no salirse del área segura y se
  desvanece al acercarse al borde (comportamiento de `TransformingLazyColumn`), no solo en las listas.
- Espaciado base 4 dp; separaciones comunes 8, 12, 16 dp.
- Objetivos táctiles ≥ 48 dp.

### Iconografía

Material Symbols Rounded. Íconos por estado: `hourglass_bottom` (1), `checkroom` (2),
`backpack` (3), `directions_walk` (4).

## 3. Pantallas

Cada pantalla lleva su ruta de navegación entre paréntesis.

### 3.1 Inicio (`home`)
- Arriba, `TimeText` con la hora actual.
- Centro: nombre del próximo evento (`title`), y debajo "Empieza a prepararte en" + cuenta regresiva
  grande (`display`).
- Anillo exterior que se llena a medida que se acerca el inicio de la preparación.
- Abajo: chip "Ver plan" → Detalle. Desliza hacia arriba → Lista de eventos.
- Sin eventos: micrófono en un círculo tintado como ilustración, "Sin eventos", "Dime qué tienes y a qué hora"
  y el botón inferior "Crear con voz" (solo texto: con ícono no cabe en 132 dp, D-018).
- La cuenta regresiva muestra "Ahora" cuando ya es hora de empezar a prepararse.
- Girar la corona en el sentido en que avanzan las listas también abre la Lista de eventos.

### 3.2 Lista de eventos (`events`)
- `TransformingLazyColumn` con tarjetas: hora de inicio (`title`, cifras tabulares), título (`body`) y
  "Sal a las 7:43 · lugar" (`label`). Agrupadas por día con un encabezado: "Hoy", "Mañana" o "Jueves 19".
- Primer elemento: botón "Nuevo evento" con ícono de micrófono.
- Últimos elementos, bajo el encabezado "Más": accesos a "Rutinas", "Resumen semanal" y "Ajustes"
  sobre `surface`, con el ícono en `primary`.
- Sin eventos próximos: "No tienes eventos próximos" entre "Nuevo evento" y "Más".

### 3.3 Detalle del evento (`event/{id}`)
- Línea de tiempo vertical de 4 puntos con el color de cada estado y su hora:
  "7:05 Termina lo que haces · 7:15 Prepárate · 7:35 Revisa tus cosas · 7:40 Sal".
- Cada fila: punto del color del estado, hora (`title`, cifras tabulares) y nombre (`label`). Si ya empezó la
  preparación, el paso que toca va sobre su color al 12 % con borde y los pasados llevan check y texto atenuado.
- Debajo, "Llegas a las 7:55".
- Chip "¿Por qué a esta hora?" → Explicación.
- Botón "Empezar ahora" para iniciar la secuencia antes de tiempo, como **último elemento de la lista**
  (no fijo abajo: fijo tapaba la línea de tiempo, D-019). No aparece si el evento ya empezó.

### 3.4 Crear por voz (`create/voice`)
- Al entrar abre la entrada de texto de Wear (`RemoteInput`: dictado primero, teclado como alternativa,
  D-020). Mientras está abierta, onda animada de 7 barras en `primary`; en reposo, la onda queda quieta.
- Título "Dicta tu evento", botón "Hablar" con micrófono para volver a intentarlo.
- Ejemplos debajo, **tocables**: "Clase de cálculo mañana a las 7", "Cita médica el jueves a las 3 y media",
  "Cine con Laura el sábado a las 6 de la tarde". Pasan por el mismo intérprete que la voz; sirven de respaldo
  si la voz falla en la demo.
- Al entender: check en círculo, "Entendí" y la frase entre comillas durante 1,4 s; luego pasa a Confirmar.
- Si la entrada vuelve sin texto: "No te entendí. Intenta de nuevo o toca un ejemplo."

### 3.5 Confirmar evento (`create/confirm`)
- "¿Lo guardo así?" y "Toca un dato para cambiarlo".
- Tarjetas editables: título, día ("Hoy", "Mañana", "Sábado 21"), hora, rutina y lugar.
- Campos con baja confianza: borde de 2 dp y fondo al 12 % en `state3Check`, con ícono de advertencia y
  "Revisa esto". Al editar un dato, la marca desaparece.
- Cómo se edita cada dato (D-021): título con la entrada de texto de Wear; día con `DatePicker` (de hoy a un
  año); hora con `TimePicker` de 24 horas; rutina con 3 opciones de radio; lugar con "Dictar lugar" o
  "Sin lugar". Cada selector es una pantalla aparte: deslizar para volver regresa a la confirmación.
- Si la fecha y hora ya pasaron: la hora dice "Esa hora ya pasó" y "Guardar" queda deshabilitado.
- Botón "Guardar" fijo abajo. Al guardar, confirmación animada de Wear con "Guardado" y vuelta a la Lista.

### 3.6 Estados de preparación (`prep/{sessionId}`)
Una sola pantalla que cambia según el estado activo:
- Fondo `background`, anillo de progreso completo en el color del estado (tiempo restante de ese paso).
- Centro: ícono del estado (22 dp, sin círculo, para que todo quepa aunque el nombre ocupe dos líneas),
  nombre del estado (`title`, en el color del estado), minutos restantes (`display`) y el mensaje en `label`.
- Estado 3 muestra "Revisados 1 de 4" y la lista de objetos de la rutina como botones marcables (círculo
  con check; marcado = fondo y borde del color del estado). La lista se desplaza bajo "Listo".
- Botón inferior "Listo" (o "Ya salí" en el estado 4). Al tocarlo: vibración corta de confirmación y
  transición al siguiente estado con animación de barrido del anillo.
- Si el tiempo del paso se acaba sin confirmar, el anillo cambia a `late` y el texto a "Vas X min tarde".

| # | Nombre en pantalla | Mensaje secundario |
|---|---|---|
| 1 | Termina lo que haces | "Cierra lo que tienes abierto" |
| 2 | Prepárate | "Vístete y alístate" |
| 3 | Revisa tus cosas | Lista de objetos |
| 4 | Sal ahora | "Llegas a las 7:58" |

### 3.7 Resultado (`prep/{sessionId}/done`)
- "Saliste a tiempo" con check verde, o "Saliste 4 min tarde" sin regaños.
- Una línea de aprendizaje: "Prepararte te tomó 22 min. Ajusté tu rutina."

### 3.8 ¿Por qué te avisé antes? (`explain/{eventId}`)
- Lista de razones con su efecto en minutos, por ejemplo:
  - "Los lunes tardas 6 min más en prepararte" (+6)
  - "Hay lluvia prevista" (+4)
  - "Tráfico alto hacia la universidad" (+5)
- Cada razón en una tarjeta (no tocable): ícono en círculo tintado de `primary`, efecto en minutos ("+6 min",
  `button` en `primary`) y la frase en `body`. Ordenadas de mayor a menor efecto.
- Debajo, el total: "En total te aviso 13 min antes".
- Razones posibles: base aprendida, efecto del día, lluvia, tráfico, ajuste manual y margen de seguridad.
  Hasta la Fase 4 solo hay ajustes manuales y margen (D-025).
- Botón "No es así" → Ajustar estimaciones de esa rutina.

### 3.9 Ajustar estimaciones (`routine/{id}/edit`)
- Por cada paso, un selector de minutos con botones − y + (o rotación de la corona/bisel).
- Indica "Aprendido: 18 min" (o "Aún sin historial") y, si hay ajuste, "Tu ajuste: 15 min" en el color del estado
  con "Usar lo aprendido" para quitarlo. El valor que se ve es el ajuste, si no lo aprendido, si no el de la rutina.
- Botones − y + de 48 dp. Cada toque se guarda al instante (sin botón "Guardar"); rango de 1 a 90 min.

### 3.10 Rutinas (`routines`)
- Tres rutinas iniciales: **Clase**, **Cita**, **Salida social**. Cada tarjeta: nombre, total, una barra con un
  tramo por paso en el color de su estado y largo proporcional a sus minutos, y la lista de objetos. Tocarla
  abre Ajustar estimaciones.

### 3.11 Resumen semanal (`summary`)
- Arriba: porcentaje de salidas a tiempo en grande, con anillo de progreso.
- Gráfico de 7 barras de los **últimos 7 días incluido hoy** (D-027), en orden y con la inicial del día debajo.
  A tiempo: `state4Leave`; tarde: `late` con rayas (nunca solo color); sin salidas: barra mínima `surfaceBright`.
  El alto es el margen (o el retraso) promedio del día. Debajo, la leyenda "A tiempo / Tarde".
- Una frase de tendencia: "Esta semana saliste a tiempo 5 de 6 veces. Tu mejor día fue el miércoles."

### 3.12 Ajustes (`settings`)
- Selector de **Tema** con tres opciones: Claro (predeterminado), Oscuro, Automático. Cada opción muestra
  una miniatura circular del reloj con ese tema. El cambio se aplica al instante con una transición suave
  de color (300 ms).
- La miniatura es un reloj de 28 dp con el fondo, el anillo del estado 2 y dos manecillas del tema; la de
  Automático va partida mitad clara y mitad oscura, con "Oscuro de 19:00 a 5:59" debajo.
- Sección "Avisos": interruptor de vibraciones ("Un patrón distinto por estado"). Si el sistema no permite abrir
  los avisos a pantalla completa, aparece "Avisos a pantalla completa · Toca para permitirlos" con borde
  `state3Check`, que abre el ajuste del sistema (D-028).
- Sección "Acerca de": "A Tiempo · versión 0.1". Mantenerla presionada descubre el interruptor "Modo demo · 1 min
  pasa en 5 s".
- Con el modo demo activo: botón "Ir al próximo aviso", que salta a 3 s (de la demo) antes del siguiente aviso y
  vuelve a Inicio. Todo, incluida la hora de arriba, usa la hora acelerada.
- Con el modo demo activo, sección "Simulación" (D-031): interruptor "Lluvia prevista · Suma tiempo al trayecto"
  y botón "Tráfico bajo / medio / alto · Toca para cambiar" (ícono de semáforo en `primary`; cada toque pasa al
  siguiente nivel). Cambian la hora de salida al instante y aparecen como razones en §3.8. Fuera del modo demo no
  cuentan.
- Se llega desde el final de la Lista de eventos.

## 4. Patrones hápticos

Diseñados para distinguirse sin mirar el reloj. Implementar con `VibrationEffect.createWaveform`
(tiempos en ms, alternando pausa y vibración, amplitudes 0–255, sin repetición: `repeat = -1`).

**Patrones finales** (tarea 1.4):

| Aviso | Tiempos (ms) | Amplitudes | Total | Sensación |
|---|---|---|---|---|
| 1 Termina | `[0, 150]` | `[0, 140]` | 150 ms | Un toque suave |
| 2 Prepárate | `[0, 150, 150, 150]` | `[0, 180, 0, 180]` | 450 ms | Dos toques |
| 3 Revisa | `[0, 90, 110, 90, 110, 90]` | `[0, 210, 0, 210, 0, 210]` | 490 ms | Tres toques rápidos |
| 4 Sal ahora | `[0, 450, 200, 450]` | `[0, 255, 0, 255]` | 1100 ms | Dos vibraciones largas y fuertes |
| Vas tarde | `[0, 450, 200, 450, 200, 450]` | `[0, 255, 0, 255, 0, 255]` | 1750 ms | Tres vibraciones largas; cada 60 s, máximo 3 veces |
| Confirmación | `EFFECT_CLICK` (respaldo `[0, 40]` amp `[0, 160]`) | — | ~40 ms | Clic breve |

Reglas que siguen los patrones:

1. **Se distinguen por ritmo, no solo por fuerza.** La cantidad y la duración de los pulsos
   (1 corto, 2 cortos, 3 rápidos, 2 largos, 3 largos) bastan para reconocerlos, así que también funcionan
   en relojes sin control de amplitud (`Vibrator.hasAmplitudeControl() == false`), donde se llama a
   `createWaveform(timings, -1)` y todo vibra con la fuerza predeterminada.
2. **Pausas de al menos 110 ms entre pulsos.** Con menos, el motor del reloj no alcanza a detenerse y
   los toques se sienten como uno solo.
3. **La intensidad sube con la urgencia** (140 → 180 → 210 → 255) y solo los avisos de salir usan
   pulsos largos.
4. **"Vas tarde" se diferencia de "Sal ahora"** por un tercer pulso largo. Así, sin mirar, se sabe si es
   el primer aviso de salida o un recordatorio de que ya va tarde.
5. **Ningún aviso pasa de 2 s**, y la confirmación usa el efecto del sistema (`VibrationEffect.createPredefined(EFFECT_CLICK)`)
   para sentirse igual que el resto de Wear OS.
6. Si el usuario desactiva las vibraciones en Ajustes, no suena ninguno; la pantalla y la notificación sí aparecen.

Verificación: el emulador de Wear OS no vibra. Los tiempos se revisan en el prototipo HTML (registro y
animación del marco) y, si hay un reloj físico disponible, en la Fase 5.

## 5. Tono de los textos

- Tutea, frases cortas, verbos en imperativo amable: "Prepárate", "Revisa tus cosas".
- Nunca culpes: "Saliste 4 min tarde" en lugar de "Llegaste tarde otra vez".
- Explica en términos de la persona: "Los lunes tardas más" en lugar de "Ajuste por día de la semana".
