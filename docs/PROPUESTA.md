# Propuesta 6. Asistencia adaptativa para salir a tiempo

**Plataforma:** aplicación para smartwatch con Wear OS.

**Idea:** Una aplicación que ayuda al usuario a comenzar su preparación y salir con tiempo suficiente para
llegar a una cita, clase o actividad. No se limita a una alarma fija: aprende cuánto tarda realmente la
persona y divide la preparación en señales breves desde la muñeca.

**Problema:** Las agendas suelen avisar cuándo empieza un evento, pero no consideran cuánto tarda cada
persona en dejar una actividad, organizar sus objetos, prepararse y desplazarse. Como resultado, el usuario
puede recibir una alerta correcta y aun así salir tarde.

**Oportunidad:** El reloj puede intervenir en el momento adecuado mediante vibraciones privadas y difíciles
de ignorar, sin exigir que el usuario abra el teléfono. Con datos históricos, la aplicación puede mejorar
gradualmente la estimación de preparación y desplazamiento.

**Funcionamiento:** El usuario selecciona un evento o lo crea por voz. La aplicación calcula una secuencia:
terminar la actividad actual, comenzar a prepararse, revisar objetos necesarios y salir. Cada transición
tiene una vibración y una pantalla breve. Si el usuario confirma que ya está listo o el reloj detecta
movimiento hacia la salida, el plan se actualiza.

**Funcionamiento de la IA:** La IA interpreta eventos creados con lenguaje natural, aprende la duración real
de las rutinas y estima el momento de cada aviso utilizando historial, ubicación, día, clima y
desplazamiento. Un modelo sencillo de predicción puede ejecutarse en el teléfono o servidor; el reloj
conserva el flujo, las confirmaciones y los patrones hápticos. El sistema explica por qué adelantó una
alerta y permite corregir sus estimaciones.

**Stack tecnológico propuesto:** Kotlin y Jetpack Compose for Wear OS; Data Layer API para comunicación con
el teléfono Android; Fused Location Provider y Activity Recognition para contexto; DataStore o Room para
historial; servicios de calendario, clima y rutas; TensorFlow Lite o modelo de regresión en Python para
estimar tiempos; API de voz para crear eventos.

**Alcance inicial:** Creación de eventos por voz, aprendizaje de tres rutinas, cuatro estados de
preparación, patrones hápticos diferenciados, ajuste manual de estimaciones y resumen semanal de
puntualidad. No se presentará como diagnóstico ni tratamiento de una condición de salud.

---

> Las diferencias entre esta propuesta y lo que se implementa en la entrega están en
> [DECISIONES.md](DECISIONES.md).
