package com.atiempo.wear.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.Text
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.PlaceholderScreen
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.theme.AppTheme

/**
 * 3.12 Ajustes (`settings`). Por ahora solo el selector de tema, que ya guarda en DataStore y
 * cambia el tema al instante. Las miniaturas, las vibraciones y el modo demo llegan en la tarea 3.11b.
 */
@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
        ThemeMode.AUTO to R.string.theme_auto,
    )
    PlaceholderScreen(title = R.string.settings_title, description = R.string.settings_theme) {
        options.forEach { (mode, label) ->
            item {
                RadioButton(
                    selected = themeMode == mode,
                    onSelect = { onThemeModeChange(mode) },
                    modifier = Modifier.fillMaxWidth(),
                    // Colores explícitos: los predeterminados de Wear quedan en caché y no siguen
                    // el cambio de tema hasta volver a entrar a la pantalla.
                    colors = RadioButtonDefaults.radioButtonColors(
                        selectedContainerColor = AppTheme.colors.surfaceBright,
                        selectedContentColor = AppTheme.colors.onSurface,
                        selectedControlColor = AppTheme.colors.primary,
                        unselectedContainerColor = AppTheme.colors.surface,
                        unselectedContentColor = AppTheme.colors.onSurface,
                        unselectedControlColor = AppTheme.colors.onSurfaceVariant,
                    ),
                    label = { Text(text = stringResource(label), style = AppTheme.type.button) },
                )
            }
        }
    }
}

@WearPreviews
@Composable
private fun SettingsLightPreview() = ATiempoPreview(dark = false) { SettingsScreen(ThemeMode.LIGHT) {} }

@WearPreviews
@Composable
private fun SettingsDarkPreview() = ATiempoPreview(dark = true) { SettingsScreen(ThemeMode.DARK) {} }
