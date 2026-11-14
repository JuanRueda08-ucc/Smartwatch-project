package com.atiempo.wear.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import com.atiempo.wear.MainActivity
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.PrepStep
import com.atiempo.wear.ui.components.icon
import com.atiempo.wear.ui.components.title
import com.atiempo.wear.ui.theme.LightColors

/**
 * Aviso de cada estado: notificación con intent a pantalla completa que abre la pantalla del estado
 * (ARQUITECTURA, Avisos). No suena ni vibra por su cuenta: la vibración es el patrón háptico del estado.
 */
class AlertNotifier(private val context: Context) {

    private val notifications = context.getSystemService(NotificationManager::class.java)

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        notifications.createNotificationChannel(channel)
    }

    /** Publica o reemplaza el aviso del estado actual: "Prepárate · Clase de cálculo · faltan 12 min". */
    fun show(step: PrepStep, eventTitle: String, minutesLeft: Int, late: Boolean, minutesLate: Int) {
        if (!notifications.areNotificationsEnabled()) return
        val text = if (late) context.getString(R.string.notif_text_late, eventTitle, minutesLate)
        else context.getString(R.string.notif_text, eventTitle, minutesLeft)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_PREP, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // El color de la notificación es el del estado (los avisos se ven igual en ambos temas del sistema).
        val color = when {
            late -> LightColors.late
            step == PrepStep.FINISH -> LightColors.state1Finish
            step == PrepStep.PREPARE -> LightColors.state2Prepare
            step == PrepStep.CHECK -> LightColors.state3Check
            else -> LightColors.state4Leave
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(step.icon())
            .setContentTitle(context.getString(step.title()))
            .setContentText(text)
            .setColor(color.toArgb())
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()
        notifications.notify(NOTIFICATION_ID, notification)
    }

    fun cancel() = notifications.cancel(NOTIFICATION_ID)

    private companion object {
        const val CHANNEL_ID = "prep_alerts"
        const val NOTIFICATION_ID = 1
    }
}
