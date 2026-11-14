package com.atiempo.wear.ui.settings

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.theme.AppColors
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.ui.theme.DarkColors
import com.atiempo.wear.ui.theme.LightColors

/** 3.12 Ajustes (`settings`). */
@Composable
fun SettingsRoute(container: AppContainer) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container.settings) } })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Se revisa al volver de los ajustes del sistema.
    var fullScreenAllowed by remember { mutableStateOf(canUseFullScreen(context)) }
    LifecycleResumeEffect(Unit) {
        fullScreenAllowed = canUseFullScreen(context)
        onPauseOrDispose { }
    }
    SettingsScreen(
        state = state,
        fullScreenAllowed = fullScreenAllowed,
        onTheme = { viewModel.setTheme(it) },
        onVibrations = { viewModel.setVibrations(it) },
        onDemo = { viewModel.setDemo(it) },
        onUnlockDemo = viewModel::unlockDemo,
        onAllowFullScreen = { openFullScreenSettings(context) },
    )
}

/**
 * Tema con miniaturas circulares del reloj en cada tema, vibraciones, avisos a pantalla completa y la versión,
 * que al mantenerla presionada descubre el modo demo (DISENO_UI §3.12).
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    fullScreenAllowed: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onVibrations: (Boolean) -> Unit,
    onDemo: (Boolean) -> Unit,
    onUnlockDemo: () -> Unit,
    onAllowFullScreen: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(text = stringResource(R.string.settings_title), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            item { SectionLabel(stringResource(R.string.settings_theme)) }
            listOf(
                Triple(ThemeMode.LIGHT, R.string.theme_light, null),
                Triple(ThemeMode.DARK, R.string.theme_dark, null),
                Triple(ThemeMode.AUTO, R.string.theme_auto, R.string.settings_auto_detail),
            ).forEach { (mode, label, detail) ->
                item(key = mode.name) {
                    RadioButton(
                        selected = state.themeMode == mode,
                        onSelect = { onTheme(mode) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = radioColors(),
                        icon = { ThemeThumbnail(mode) },
                        secondaryLabel = detail?.let { { Text(text = stringResource(it), style = AppTheme.type.caption) } },
                        label = { Text(text = stringResource(label), style = AppTheme.type.button) },
                    )
                }
            }
            item { SectionLabel(stringResource(R.string.settings_alerts)) }
            item {
                Toggle(
                    checked = state.vibrations,
                    onCheckedChange = onVibrations,
                    label = stringResource(R.string.settings_vibrations),
                    detail = stringResource(R.string.settings_vibrations_detail),
                    spec = spec,
                )
            }
            if (!fullScreenAllowed) {
                item {
                    Button(
                        onClick = onAllowFullScreen,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.surface,
                            contentColor = AppTheme.colors.onSurface,
                            secondaryContentColor = AppTheme.colors.state3Check,
                            iconColor = AppTheme.colors.state3Check,
                        ),
                        border = BorderStroke(1.dp, AppTheme.colors.state3Check),
                        icon = { Icon(painterResource(R.drawable.ic_warning), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        label = { Text(text = stringResource(R.string.settings_full_screen), style = AppTheme.type.button) },
                        secondaryLabel = { Text(text = stringResource(R.string.settings_full_screen_detail), style = AppTheme.type.caption) },
                    )
                }
            }
            item { SectionLabel(stringResource(R.string.settings_about)) }
            if (state.demoUnlocked) {
                item {
                    Toggle(
                        checked = state.demoMode,
                        onCheckedChange = onDemo,
                        label = stringResource(R.string.settings_demo),
                        detail = stringResource(R.string.settings_demo_detail),
                        spec = spec,
                    )
                }
            }
            item { VersionRow(onUnlockDemo) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = AppTheme.type.label,
        color = AppTheme.colors.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun TransformingLazyColumnItemScope.Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    detail: String,
    spec: TransformationSpec,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = SwitchButtonDefaults.switchButtonColors(
            checkedContainerColor = AppTheme.colors.surfaceBright,
            checkedContentColor = AppTheme.colors.onSurface,
            checkedSecondaryContentColor = AppTheme.colors.onSurfaceVariant,
            checkedThumbColor = AppTheme.colors.onState,
            checkedTrackColor = AppTheme.colors.primary,
            checkedTrackBorderColor = AppTheme.colors.primary,
            uncheckedContainerColor = AppTheme.colors.surface,
            uncheckedContentColor = AppTheme.colors.onSurface,
            uncheckedSecondaryContentColor = AppTheme.colors.onSurfaceVariant,
            uncheckedThumbColor = AppTheme.colors.onSurfaceVariant,
            uncheckedTrackColor = AppTheme.colors.surfaceBright,
            uncheckedTrackBorderColor = AppTheme.colors.onSurfaceVariant,
        ),
        label = { Text(text = label, style = AppTheme.type.button) },
        secondaryLabel = { Text(text = detail, style = AppTheme.type.caption) },
    )
}

/** La versión: mantenerla presionada descubre el modo demo. */
@Composable
private fun VersionRow(onUnlockDemo: () -> Unit) {
    val hint = stringResource(R.string.settings_version_hint)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onUnlockDemo, onLongClickLabel = hint)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_version, VERSION),
            style = AppTheme.type.caption,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Miniatura circular del reloj en cada tema: fondo, anillo del estado 2 y una barra de texto. */
@Composable
private fun ThemeThumbnail(mode: ThemeMode) {
    val light = LightColors
    val dark = DarkColors
    Canvas(modifier = Modifier.size(28.dp)) {
        fun face(colors: AppColors) {
            drawCircle(colors.background)
            drawCircle(colors.surfaceBright, style = Stroke(1.dp.toPx()))
            val inset = 3.dp.toPx()
            drawArc(
                color = colors.state2Prepare,
                startAngle = -90f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(2.5.dp.toPx()),
            )
            // Dos manecillas: se lee como "el reloj en este tema" (una barra sola parecía un signo menos).
            val c = Offset(size.width / 2, size.height / 2)
            val hand = 1.5.dp.toPx()
            drawLine(colors.onSurface, c, Offset(c.x, size.height * 0.28f), strokeWidth = hand, cap = StrokeCap.Round)
            drawLine(colors.onSurface, c, Offset(size.width * 0.68f, c.y), strokeWidth = hand, cap = StrokeCap.Round)
        }
        when (mode) {
            ThemeMode.LIGHT -> face(light)
            ThemeMode.DARK -> face(dark)
            ThemeMode.AUTO -> {
                clipRect(right = size.width / 2) { face(light) }
                clipRect(left = size.width / 2) { face(dark) }
            }
        }
    }
}

@Composable
private fun radioColors() = RadioButtonDefaults.radioButtonColors(
    selectedContainerColor = AppTheme.colors.surfaceBright,
    selectedContentColor = AppTheme.colors.onSurface,
    selectedSecondaryContentColor = AppTheme.colors.onSurfaceVariant,
    selectedControlColor = AppTheme.colors.primary,
    unselectedContainerColor = AppTheme.colors.surface,
    unselectedContentColor = AppTheme.colors.onSurface,
    unselectedSecondaryContentColor = AppTheme.colors.onSurfaceVariant,
    unselectedControlColor = AppTheme.colors.onSurfaceVariant,
)

private const val VERSION = "0.1"

/** Desde Android 14, abrirse a pantalla completa es un acceso especial que hay que conceder (D-028). */
private fun canUseFullScreen(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

private fun openFullScreenSettings(context: Context) {
    val uri = Uri.parse("package:${context.packageName}")
    val intents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, uri))
        add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
    }
    // En Wear algunas pantallas del sistema no existen: se prueba la siguiente.
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (e: ActivityNotFoundException) {
            continue
        }
    }
}

// ---------- Previews ----------

@WearPreviews
@Composable
private fun SettingsLightPreview() = ATiempoPreview(dark = false) {
    SettingsScreen(SettingsUiState(), fullScreenAllowed = false, {}, {}, {}, {}, {})
}

@WearPreviews
@Composable
private fun SettingsDarkPreview() = ATiempoPreview(dark = true) {
    SettingsScreen(SettingsUiState(themeMode = ThemeMode.DARK, demoUnlocked = true), fullScreenAllowed = true, {}, {}, {}, {}, {})
}
