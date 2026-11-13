package com.atiempo.wear.data.seed

import android.util.Log
import androidx.room.withTransaction
import com.atiempo.wear.data.db.AppDatabase
import com.atiempo.wear.data.settings.SettingsRepository
import java.time.LocalDateTime
import java.time.ZoneId

/** Siembra los datos de ejemplo una sola vez, en el primer arranque (tarea 2.5). */
class Seeder(private val db: AppDatabase, private val settings: SettingsRepository) {

    suspend fun seedIfNeeded() {
        if (settings.isSeeded()) return
        // Si la bandera se perdió pero la base ya tiene datos, no se duplican.
        if (db.eventDao().count() > 0) {
            settings.markSeeded()
            return
        }
        val seed = SampleData.generate(LocalDateTime.now(), ZoneId.systemDefault())
        db.withTransaction {
            db.routineDao().upsertAll(seed.routines)
            db.eventDao().insertAll(seed.events)
            seed.sessions.forEach { db.prepSessionDao().insert(it) }
            db.stepRecordDao().insertAll(seed.records)
        }
        settings.markSeeded()
        Log.i(TAG, "Sembrados ${seed.routines.size} rutinas, ${seed.events.size} eventos, ${seed.sessions.size} sesiones y ${seed.records.size} registros")
    }

    private companion object {
        const val TAG = "ATiempoSeed"
    }
}
