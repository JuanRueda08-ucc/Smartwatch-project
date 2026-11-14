package com.atiempo.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.atiempo.wear.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/* Formatos de hora y día compartidos por las pantallas. */

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** Hora sin cero inicial, como en DISENO_UI: "7:40", "14:00". */
fun clockText(epochMillis: Long, zone: ZoneId): String = TIME_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** "Hoy", "Mañana" o el nombre del día ("Jueves"). */
@Composable
fun dayName(dayOffset: Int, weekday: DayOfWeek): String = when (dayOffset) {
    0 -> stringResource(R.string.day_today)
    1 -> stringResource(R.string.day_tomorrow)
    else -> stringArrayResource(R.array.weekdays)[weekday.value - 1]
}

/** "Hoy", "Mañana" o, para días más lejanos, "Jueves 19". */
@Composable
fun dayWithNumber(dayOffset: Int, weekday: DayOfWeek, dayOfMonth: Int): String =
    if (dayOffset in 0..1) dayName(dayOffset, weekday)
    else stringResource(R.string.day_with_number, dayName(dayOffset, weekday), dayOfMonth)

/** "Hoy · 8:00", "Mañana · 8:00" o "Jueves · 15:30". */
@Composable
fun eventWhen(dayOffset: Int, weekday: DayOfWeek, eventAt: Long, zone: ZoneId): String =
    stringResource(R.string.event_when, dayName(dayOffset, weekday), clockText(eventAt, zone))
