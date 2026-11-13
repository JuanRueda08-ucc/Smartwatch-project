package com.atiempo.wear

import android.content.Context
import com.atiempo.wear.data.db.AppDatabase
import com.atiempo.wear.data.repo.EventRepository
import com.atiempo.wear.data.repo.HistoryRepository
import com.atiempo.wear.data.repo.RoutineRepository
import com.atiempo.wear.data.settings.SettingsRepository

/** Dependencias de la app creadas a mano, sin Hilt (AGENTS.md). */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val database: AppDatabase by lazy { AppDatabase.create(context) }

    val events by lazy { EventRepository(database.eventDao()) }
    val routines by lazy { RoutineRepository(database.routineDao()) }
    val history by lazy { HistoryRepository(database.prepSessionDao(), database.stepRecordDao()) }
}
