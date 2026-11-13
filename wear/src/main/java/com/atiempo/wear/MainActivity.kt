package com.atiempo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.domain.ThemeResolver
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.ui.navigation.AppNavHost
import com.atiempo.wear.ui.theme.ATiempoTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Única Activity de la app: aplica el tema guardado en Ajustes y monta la navegación. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ATiempoApp).container
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            val hour by currentHour()
            ATiempoTheme(dark = ThemeResolver.isDark(mode, hour)) {
                // AppScaffold pone el TimeText curvo arriba en todas las pantallas. El color se fija
                // a `onSurface`: el predeterminado de Wear se ve casi blanco sobre el fondo claro.
                val timeStyle = CurvedTextStyle(AppTheme.type.timeText.copy(color = AppTheme.colors.onSurface))
                AppScaffold(timeText = { TimeText { time -> timeTextCurvedText(time, timeStyle) } }) {
                    AppNavHost(container)
                }
            }
        }
    }
}

/** Hora local, actualizada al empezar cada minuto, para el tema automático. */
@Composable
private fun currentHour() = produceState(initialValue = LocalTime.now().hour) {
    while (true) {
        val now = LocalTime.now()
        delay((60 - now.second) * 1_000L)
        value = LocalTime.now().hour
    }
}
