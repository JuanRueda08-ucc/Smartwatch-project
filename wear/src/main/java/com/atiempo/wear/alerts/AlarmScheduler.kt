package com.atiempo.wear.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Programa una sola alarma: la próxima hora en que algo cambia (primer aviso, fin de un paso, hora límite o
 * recordatorio de "tarde"). Al sonar, `AlertReceiver` despierta la app y la vuelve a programar (D-024).
 */
class AlarmScheduler(private val context: Context) {

    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** Programa la alarma a `at` (epoch ms), o la cancela si es null. Reemplaza la anterior. */
    fun schedule(at: Long?) {
        val intent = pendingIntent()
        if (at == null) {
            alarms.cancel(intent)
            return
        }
        // Exacta si el reloj lo permite (USE_EXACT_ALARM / SCHEDULE_EXACT_ALARM); si no, aproximada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AlertReceiver::class.java).setAction(AlertReceiver.ACTION_ALERT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 1
    }
}
