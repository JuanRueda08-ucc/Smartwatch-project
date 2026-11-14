package com.atiempo.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atiempo.wear.data.settings.SettingsRepository
import com.atiempo.wear.domain.model.ThemeMode
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
)

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {

    private val unlocked = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.vibrationsEnabled,
        settings.demoMode,
        unlocked,
    ) { theme, vibrations, demo, isUnlocked ->
        SettingsUiState(theme, vibrations, demo, demoUnlocked = isUnlocked || demo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setVibrations(enabled: Boolean) = viewModelScope.launch { settings.setVibrationsEnabled(enabled) }

    fun setDemo(enabled: Boolean) = viewModelScope.launch { settings.setDemoMode(enabled) }

    /** Mantener presionada la versión descubre el modo demo (DISENO_UI §3.12). */
    fun unlockDemo() {
        unlocked.value = true
    }
}
