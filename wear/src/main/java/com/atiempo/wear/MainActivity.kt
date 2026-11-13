package com.atiempo.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Text
import com.atiempo.wear.domain.ThemeResolver
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.ui.theme.ATiempoTheme
import com.atiempo.wear.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Única Activity de la app. Aplica el tema guardado en Ajustes; la navegación llega en la tarea 2.3. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ATiempoApp).container
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            val hour by currentHour()
            ATiempoTheme(dark = ThemeResolver.isDark(mode, hour)) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AppTheme.colors.background),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.placeholder_getting_ready), style = AppTheme.type.body)
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
