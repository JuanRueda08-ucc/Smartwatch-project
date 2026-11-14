package com.atiempo.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.settings.SettingsRepository
import com.atiempo.wear.data.context.next
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.domain.model.TrafficLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val vibrations: Boolean = true,
    val demoMode: Boolean = false,
    /** El interruptor del modo demo aparece tras mantener presionada la versión (o si ya está activo). */
    val demoUnlocked: Boolean = false,
    /** Clima y tráfico simulados; solo se muestran (y cuentan) con el modo demo activo. */
    val rain: Boolean = false,
    val traffic: TrafficLevel = TrafficLevel.LOW,
)

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    private val unlocked = MutableStateFlow(false)

    private val simulated = combine(settings.demoRain, settings.demoTraffic) { rain, traffic -> rain to traffic }

    val state: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.vibrationsEnabled,
        settings.demoMode,
        unlocked,
        simulated,
    ) { theme, vibrations, demo, isUnlocked, (rain, traffic) ->
        SettingsUiState(theme, vibrations, demo, demoUnlocked = isUnlocked || demo, rain = rain, traffic = traffic)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setVibrations(enabled: Boolean) = viewModelScope.launch { settings.setVibrationsEnabled(enabled) }

    fun setDemo(enabled: Boolean) = viewModelScope.launch { settings.setDemoMode(enabled) }

    fun setRain(rain: Boolean) = viewModelScope.launch { settings.setDemoRain(rain) }

    fun nextTraffic() = viewModelScope.launch { settings.setDemoTraffic(state.value.traffic.next()) }

    /** Mantener presionada la versión descubre el modo demo (DISENO_UI §3.12). */
    fun unlockDemo() {
        unlocked.value = true
    }
}
