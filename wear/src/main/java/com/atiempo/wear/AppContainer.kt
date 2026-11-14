package com.atiempo.wear

import android.content.Context
import com.atiempo.wear.alerts.AlarmScheduler
import com.atiempo.wear.alerts.AlertNotifier
import com.atiempo.wear.alerts.Haptics
import com.atiempo.wear.data.db.AppDatabase
import com.atiempo.wear.data.draft.DraftStore
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.session.PrepSessionManager
import com.atiempo.wear.data.settings.SettingsRepository
import com.atiempo.wear.domain.time.Clock
import com.atiempo.wear.domain.time.DemoClock
import com.atiempo.wear.domain.time.SystemClock
import kotlinx.coroutines.CoroutineScope
import java.time.ZoneId

/** Dependencias de la app creadas a mano, sin Hilt (AGENTS.md). */
class AppContainer(context: Context, appScope: CoroutineScope) {
    val settings = SettingsRepository(context)
    /** Reloj de la app. En modo demo avanza 12 veces más rápido (tarea 3.12, D-029). */
    val demoClock = DemoClock(SystemClock)
    val clock: Clock = demoClock
    val zone: ZoneId get() = ZoneId.systemDefault()
    val database: AppDatabase by lazy { AppDatabase.create(context) }

    val events by lazy { EventRepository(database.eventDao()) }
    val routines by lazy { RoutineRepository(database.routineDao()) }
    val history by lazy { HistoryRepository(database.prepSessionDao(), database.stepRecordDao()) }

    /** Datos del estimador: historial (y, desde la tarea 4.2, el contexto). */
    val estimation by lazy { history.estimationData(zone) }

    /** Evento dictado que espera confirmación (tareas 3.4 y 3.5). */
    val drafts = DraftStore()

    val haptics = Haptics(context)
    private val alarms = AlarmScheduler(context)
    private val notifier = AlertNotifier(context)

    /** Preparación en curso, con sus alarmas y avisos (tareas 3.7 y 3.8). */
    val prep by lazy { PrepSessionManager(appScope, clock, zone, events, routines, history, estimation, haptics, alarms, notifier) }

    /** Modo demo: salta a unos segundos antes del próximo aviso. false si no hay ninguno. */
    fun jumpToNextAlert(): Boolean {
        val next = prep.nextAlertAt() ?: return false
        demoClock.jumpTo(next - JUMP_LEAD_MS)
        return true
    }

    private companion object {
        /** Llegar 3 s (de la demo) antes del aviso: se ve la cuenta regresiva llegar a cero. */
        const val JUMP_LEAD_MS = 3_000L
    }
}
