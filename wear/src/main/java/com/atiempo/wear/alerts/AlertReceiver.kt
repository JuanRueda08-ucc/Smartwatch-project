package com.atiempo.wear.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.atiempo.wear.ATiempoApp
import kotlinx.coroutines.launch

/**
 * Suena la alarma: avanza la preparación (el motor vibra y publica el aviso) y deja programada la siguiente.
 * Si la app estaba cerrada, abrir el proceso ya recupera la preparación guardada.
 */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALERT) return
        val app = context.applicationContext as ATiempoApp
        val pending = goAsync()
        app.appScope.launch {
            try {
                app.container.prep.onAlarm()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALERT = "com.atiempo.wear.action.ALERT"
    }
}

/**
 * Tras reiniciar el reloj o actualizar la app, las alarmas se pierden. Basta con que el proceso arranque:
 * `ATiempoApp` recupera la preparación y vuelve a programar la próxima alarma.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Solo responde a las acciones del sistema declaradas en el manifiesto; el trabajo lo hace el arranque.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
    }
}
