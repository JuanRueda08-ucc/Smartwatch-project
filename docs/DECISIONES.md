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
