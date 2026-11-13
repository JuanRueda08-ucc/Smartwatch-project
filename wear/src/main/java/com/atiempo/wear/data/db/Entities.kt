package com.atiempo.wear.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.atiempo.wear.domain.model.PrepStep

/**
 * Rutina: duraciones por defecto de cada paso (se usan sin historial), ajustes manuales opcionales
 * y objetos a revisar. Las duraciones por defecto no estaban en ARQUITECTURA.md (D-016).
 */
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey val id: Long,
    val name: String,
    val items: List<String>,
    val defaultFinishMin: Int,
    val defaultPrepareMin: Int,
    val defaultCheckMin: Int,
    val defaultTravelMin: Int,
    val manualFinishMin: Int? = null,
    val manualPrepareMin: Int? = null,
    val manualCheckMin: Int? = null,
    val manualTravelMin: Int? = null,
)

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(entity = Routine::class, parentColumns = ["id"], childColumns = ["routineId"])],
    indices = [Index("routineId"), Index("startAt")],
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Hora de inicio, epoch en milisegundos. */
    val startAt: Long,
    val routineId: Long,
    val place: String?,
    /** Tiempo de desplazamiento base, sin ajustes de clima ni tráfico. */
    val baseTravelMin: Int,
    val createdByVoice: Boolean,
)

@Entity(
    tableName = "prep_sessions",
    foreignKeys = [ForeignKey(entity = Event::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId"), Index("startedAt")],
)
data class PrepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val startedAt: Long,
    /** Cuándo confirmó "Ya salí"; null mientras la preparación sigue. */
    val leftAt: Long? = null,
    /** Minutos tarde respecto a la hora de salida; negativo = salió con margen. */
    val minutesLate: Int? = null,
)

@Entity(
    tableName = "step_records",
    foreignKeys = [ForeignKey(entity = PrepSession::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index(value = ["routineId", "step"])],
)
data class StepRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val routineId: Long,
    val step: PrepStep,
    /** Día de la semana ISO: 1 = lunes … 7 = domingo. */
    val dayOfWeek: Int,
    val plannedMin: Int,
    val actualMin: Int,
)
