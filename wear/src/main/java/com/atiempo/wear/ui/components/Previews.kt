package com.atiempo.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.atiempo.wear.ui.theme.ATiempoTheme

/**
 * Previews obligatorias de cada pantalla (AGENTS.md): pantalla redonda grande y pequeña,
 * cuadrada, y texto grande. Cada pantalla declara una preview clara y otra oscura con esta anotación.
 */
@WearPreviewDevices
@WearPreviewFontScales
annotation class WearPreviews

/** Envuelve una pantalla en el tema y el andamio de la app para las previews. */
@Composable
fun ATiempoPreview(dark: Boolean = false, content: @Composable () -> Unit) {
    ATiempoTheme(dark = dark) {
        AppScaffold { content() }
    }
}
