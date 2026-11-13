# Modelo de estimación ("IA")

Objetivo: estimar cuántos minutos necesita la persona en cada paso para que los avisos lleguen en el
momento correcto, y poder **explicar** cada ajuste. Se prioriza un modelo simple, explicable y
probado sobre uno complejo.

## Entradas

- Historial de `StepRecord` de la misma rutina y paso.
- Día de la semana del evento.
- Contexto (simulado en esta entrega): lluvia sí/no, nivel de tráfico bajo/medio/alto.
- Ajuste manual del usuario, si existe.

## Cálculo por paso

1. **Base aprendida:** media móvil exponencial (EMA) de `actualMin` de los registros de esa rutina y paso,
   con α = 0.3 (los registros recientes pesan más). Sin historial: valores por defecto de la rutina.
2. **Efecto del día:** diferencia promedio entre los registros de ese día de la semana y la base,
   solo si hay al menos 3 registros de ese día. Se limita a ±10 min.
3. **Contexto (solo paso LEAVE, desplazamiento):**
   - Lluvia: +15 % del tiempo de desplazamiento (mínimo +2 min).
   - Tráfico medio: +10 %; alto: +25 %.
4. **Ajuste manual:** si el usuario fijó un valor, reemplaza la base aprendida, pero los efectos de
   día y contexto se siguen sumando.
5. **Margen de seguridad:** +10 % del total, redondeado hacia arriba, mínimo 3 min.

```
minutosPaso = (manual ?: EMA) + efectoDía + efectoContexto
inicioPaso4 = horaEvento − desplazamientoAjustado − margen
inicioPaso3 = inicioPaso4 − minutosCheck
...
```

## Explicación

Cada término distinto de cero genera un `Adjustment(reason: String, minutes: Int)`.
La pantalla "¿Por qué te avisé antes?" muestra la lista ordenada por minutos, de mayor a menor.
Ejemplos de textos:
- `"Últimamente tardas %d min en prepararte"`
- `"Los %s tardas %d min más"` (día de la semana)
- `"Hay lluvia prevista"`, `"Tráfico alto hacia %s"`
- `"Tú ajustaste este paso a %d min"`

## Aprendizaje

Al confirmar cada paso se guarda un `StepRecord` con la duración real (desde que empezó el paso hasta
la confirmación, acotada entre 1 y 90 min para descartar olvidos). La próxima estimación ya la incluye.

## Pruebas mínimas (`DurationEstimatorTest`)

- Sin historial devuelve los valores por defecto.
- Con historial constante de 20 min, la base converge a 20.
- El efecto del día no se aplica con menos de 3 registros de ese día.
- Lluvia y tráfico alto suman lo esperado.
- El ajuste manual reemplaza la base pero conserva el contexto.
- Cada ajuste aplicado aparece en la explicación.

## Por qué no TensorFlow Lite en esta entrega

Con pocos datos por persona, una regresión o red neuronal no supera a una media ponderada y es más
difícil de explicar. La interfaz y el modelo están separados por la interfaz `DurationEstimator`,
así que un modelo de TFLite o un servicio en Python podría reemplazarlo después sin tocar las pantallas.
