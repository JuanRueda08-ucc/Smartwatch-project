package com.atiempo.wear

import android.content.Context
import com.atiempo.wear.data.settings.SettingsRepository

/** Dependencias de la app creadas a mano, sin Hilt (AGENTS.md). */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
}
