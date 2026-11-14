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
import com.atiempo.wear.domain.time.SystemClock
import kotlinx.coroutines.CoroutineScope
import java.time.ZoneId

/** Dependencias de la app creadas a mano, sin Hilt (AGENTS.md). */
class AppContainer(context: Context, appScope: CoroutineScope) {
    val settings = SettingsRepository(context)
    /** Reloj de la app; el modo demo (3.12) lo reemplazará por uno acelerado. */
    val clock: Clock = SystemClock
    val zone: ZoneId get() = ZoneId.systemDefault()
    val database: AppDatabase by lazy { AppDatabase.create(context) }

    val events by lazy { EventRepository(database.eventDao()) }
    val routines by lazy { RoutineRepository(database.routineDao()) }
    val history by lazy { HistoryRepository(database.prepSessionDao(), database.stepRecordDao()) }

    /** Evento dictado que espera confirmación (tareas 3.4 y 3.5). */
    val drafts = DraftStore()

    val haptics = Haptics(context)
    private val alarms = AlarmScheduler(context)
    private val notifier = AlertNotifier(context)

    /** Preparación en curso, con sus alarmas y avisos (tareas 3.7 y 3.8). */
    val prep by lazy { PrepSessionManager(appScope, clock, zone, events, routines, history, haptics, alarms, notifier) }
}
