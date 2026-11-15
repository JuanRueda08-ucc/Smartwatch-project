package com.atiempo.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.ambient.AmbientLifecycleObserver
import com.atiempo.wear.domain.ThemeResolver
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.ui.components.AmbientClock
import com.atiempo.wear.ui.components.AmbientState
import com.atiempo.wear.ui.components.ClockTimeSource
import com.atiempo.wear.ui.components.LocalAmbient
import com.atiempo.wear.ui.components.clockText
import com.atiempo.wear.ui.navigation.AppNavHost
import com.atiempo.wear.ui.theme.ATiempoTheme
import com.atiempo.wear.ui.theme.AmbientColors
import com.atiempo.wear.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Única Activity de la app: aplica el tema guardado en Ajustes y monta la navegación. */
class MainActivity : ComponentActivity() {

    /** Cambia cada vez que se pide abrir el aviso en curso (al tocar la notificación). */
    private val openPrepRequests = mutableIntStateOf(0)

    /** Modo ambiente (D-034): la preparación sigue a la vista con el reloj en reposo. */
    private val ambientState = AmbientState()
    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            ambientState.isAmbient = true
        }

        override fun onExitAmbient() {
            ambientState.isAmbient = false
        }

        override fun onUpdateAmbient() {
            ambientState.minuteTicks++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ATiempoApp).container
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        handleIntent(intent)
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            val hour by currentHour()
            RequestNotificationPermission()
            ATiempoTheme(dark = ThemeResolver.isDark(mode, hour)) {
                // AppScaffold pone el TimeText curvo arriba en todas las pantallas. El color se fija
                // a `onSurface`: el predeterminado de Wear se ve casi blanco sobre el fondo claro.
                val timeStyle = CurvedTextStyle(AppTheme.type.timeText.copy(color = AppTheme.colors.onSurface))
                // La hora sale del reloj de la app: en modo demo es la hora acelerada.
                val timeSource = remember { ClockTimeSource(container.clock, container.zone) }
                CompositionLocalProvider(LocalAmbient provides ambientState) {
                    // En reposo la hora va en blanco sobre negro, sin la píldora clara (D-034).
                    val ambientTimeStyle = CurvedTextStyle(AppTheme.type.timeText.copy(color = AmbientColors.onBackground))
                    AppScaffold(
                        timeText = {
                            if (ambientState.isAmbient) {
                                TimeText(timeSource = timeSource, backgroundColor = AmbientColors.background) { time ->
                                    timeTextCurvedText(time, ambientTimeStyle)
                                }
                            } else {
                                TimeText(timeSource = timeSource) { time -> timeTextCurvedText(time, timeStyle) }
                            }
                        },
                    ) {
                        AppNavHost(container, openPrepRequest = openPrepRequests.intValue)
                    }
                    // En reposo, las pantallas sin versión ambiente se tapan con la hora en blanco sobre negro.
                    if (ambientState.isAmbient && ambientState.ambientAwareScreens == 0) {
                        val time = remember(ambientState.minuteTicks) { clockText(container.clock.now(), container.zone) }
                        AmbientClock(time)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PREP, false) == true) openPrepRequests.intValue++
    }

    companion object {
        /** El aviso de un estado abre la app directo en la pantalla de la preparación en curso. */
        const val EXTRA_OPEN_PREP = "com.atiempo.wear.extra.OPEN_PREP"
    }
}

/** Desde Android 13 los avisos necesitan permiso de notificaciones. Se pide al abrir la app. */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
