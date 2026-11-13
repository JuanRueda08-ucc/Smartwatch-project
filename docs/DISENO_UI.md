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
| `state2Prepare` | `#0B6FB8` | `#5CC8FF` | Estado 2: Prepárate (azul) |
| `state3Check` | `#8A5A00` | `#FFC857` | Estado 3: Revisa tus cosas (ámbar) |
| `state4Leave` | `#0E7A4B` | `#5EE6A0` | Estado 4: Sal ahora (verde, "adelante") |
| `late` | `#C62828` | `#FF6B6B` | Solo cuando el plan ya va tarde |
| `onState` | `#FFFFFF` | `#000000` | Texto sobre botones de estado |

En el tema claro los colores de estado son más oscuros y saturados para que se lean sobre fondo
claro; en el oscuro son más luminosos. Para fondos suaves de tarjeta (por ejemplo, la tarjeta del
estado activo) se usa el color del estado al 12 % de opacidad sobre `surface`.

Contraste medido (WCAG): todos los textos y colores de estado del tema claro están entre 4.9:1 y 16:1
sobre `#F6F7FB`; los del oscuro entre 7.5:1 y 13.6:1 sobre `#000000`. El blanco sobre cada color de
estado claro es ≥ 5.2:1. Verificar de nuevo en la Fase 5 si se cambia algún valor.
Nunca se usa solo el color para comunicar el estado: siempre va con ícono y texto.

### Tipografía

Fuente del sistema (Roboto Flex vía Material 3 de Wear). Escala:

| Estilo | Tamaño | Uso |
|---|---|---|
| `display` | 40 sp, peso 600, cifras tabulares | Cuenta regresiva principal |
| `title` | 20 sp, peso 600 | Nombre del estado o del evento |
| `body` | 15 sp | Texto general |
| `label` | 13 sp, peso 500 | Chips, etiquetas, hora secundaria |

Las cifras de tiempo usan `fontFeatureSettings = "tnum"` para que no "bailen" al cambiar.

### Formas y espaciado

- Botones y chips: forma de píldora (`CircleShape` en los extremos).
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
- Sin eventos: ilustración simple + botón de micrófono "Crear con voz".

### 3.2 Lista de eventos (`events`)
- `TransformingLazyColumn` con tarjetas: hora de inicio, título, hora de salida estimada.
- Primer elemento: botón "Nuevo evento" con ícono de micrófono.
- Últimos elementos: accesos a "Rutinas", "Resumen semanal" y "Ajustes".

### 3.3 Detalle del evento (`event/{id}`)
- Línea de tiempo vertical de 4 puntos con el color de cada estado y su hora:
  "7:05 Termina lo que haces · 7:15 Prepárate · 7:35 Revisa tus cosas · 7:40 Sal".
- Chip "¿Por qué a esta hora?" → Explicación.
- Botón "Empezar ahora" para iniciar la secuencia antes de tiempo.

### 3.4 Crear por voz (`create/voice`)
- Lanza `RecognizerIntent`. Mientras escucha, onda animada con el color `primary`.
- Ejemplos debajo: "Clase de cálculo mañana a las 7", "Cita médica el jueves a las 3 y media".

### 3.5 Confirmar evento (`create/confirm`)
- Muestra lo interpretado en tarjetas editables: título, día y hora, rutina, lugar.
- Campos con baja confianza resaltados con borde `state3Check` y texto "Revisa esto".
- Botón "Guardar".

### 3.6 Estados de preparación (`prep/{sessionId}`)
Una sola pantalla que cambia según el estado activo:
- Fondo `background`, anillo de progreso completo en el color del estado (tiempo restante de ese paso).
- Centro: ícono del estado, nombre del estado (`title`), minutos restantes (`display`).
- Estado 3 muestra la lista de objetos de la rutina como chips marcables (llaves, billetera, carné…).
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
- Botón "No es así" → Ajustar estimaciones.

### 3.9 Ajustar estimaciones (`routine/{id}/edit`)
- Por cada paso, un selector de minutos con botones − y + (o rotación de la corona/bisel).
- Indica "Aprendido: 18 min" y "Tu ajuste: 15 min".

### 3.10 Rutinas (`routines`)
- Tres rutinas iniciales: **Clase**, **Cita**, **Salida social**. Cada una con sus 4 duraciones y su lista
  de objetos.

### 3.11 Resumen semanal (`summary`)
- Arriba: porcentaje de salidas a tiempo en grande, con anillo de progreso.
- Gráfico de 7 barras (lunes a domingo) coloreadas según puntualidad.
- Una frase de tendencia: "Esta semana saliste a tiempo 5 de 6 veces. Tu mejor día fue el miércoles."

### 3.12 Ajustes (`settings`)
- Selector de **Tema** con tres opciones: Claro (predeterminado), Oscuro, Automático. Cada opción muestra
  una miniatura circular del reloj con ese tema. El cambio se aplica al instante con una transición suave
  de color (300 ms).
- Interruptor de vibraciones y acceso al modo demo (oculto: mantener presionado el número de versión).
- Se llega desde el final de la Lista de eventos.

## 4. Patrones hápticos

Diseñados para distinguirse sin mirar el reloj. Implementar con `VibrationEffect.createWaveform`
(tiempos en ms, alternando pausa y vibración, amplitudes 0–255).

| Estado | Patrón | Sensación |
|---|---|---|
| 1 Termina | `[0, 120]` amp `[0, 120]` | Un toque suave |
| 2 Prepárate | `[0, 120, 100, 120]` amp `[0, 180, 0, 180]` | Dos toques |
| 3 Revisa | `[0, 80, 80, 80, 80, 80]` amp `[0, 200, 0, 200, 0, 200]` | Tres toques rápidos |
| 4 Sal ahora | `[0, 400, 150, 400]` amp `[0, 255, 0, 255]` | Dos vibraciones largas y fuertes |
| Confirmación | `[0, 40]` amp `[0, 150]` | Clic breve |
| Vas tarde | Patrón del estado 4 repetido cada 60 s, máximo 3 veces | Insistente pero limitado |

## 5. Tono de los textos

- Tutea, frases cortas, verbos en imperativo amable: "Prepárate", "Revisa tus cosas".
- Nunca culpes: "Saliste 4 min tarde" en lugar de "Llegaste tarde otra vez".
- Explica en términos de la persona: "Los lunes tardas más" en lugar de "Ajuste por día de la semana".
