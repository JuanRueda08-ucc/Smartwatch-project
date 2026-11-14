package com.atiempo.wear.ui.create

import androidx.lifecycle.ViewModel
import com.atiempo.wear.data.draft.DraftStore
import com.atiempo.wear.domain.EventParser
import com.atiempo.wear.domain.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface CreateVoiceUiState {
    /**
     * Esperando la frase. `listening` = la entrada de voz del sistema está abierta;
     * `notUnderstood` = la última vez volvió sin texto.
     */
    data class Ready(val listening: Boolean, val notUnderstood: Boolean = false) : CreateVoiceUiState

    /** Ya se entendió la frase; la pantalla pasa sola a "Confirmar evento". */
    data class Heard(val transcript: String) : CreateVoiceUiState
}

class CreateVoiceViewModel(
    private val drafts: DraftStore,
    private val clock: Clock,
    private val zone: ZoneId,
) : ViewModel() {

    private val _state = MutableStateFlow<CreateVoiceUiState>(CreateVoiceUiState.Ready(listening = false))
    val state: StateFlow<CreateVoiceUiState> = _state.asStateFlow()

    fun onListening() {
        _state.value = CreateVoiceUiState.Ready(listening = true)
    }

    /** La persona cerró la entrada de voz sin dictar nada. */
    fun onCancelled() {
        _state.value = CreateVoiceUiState.Ready(listening = false)
    }

    /** La entrada de voz terminó pero no devolvió texto: se le dice a la persona. */
    fun onNotUnderstood() {
        _state.value = CreateVoiceUiState.Ready(listening = false, notUnderstood = true)
    }

    /** Frase reconocida o ejemplo tocado: se interpreta y queda como borrador. */
    fun onTranscript(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return onNotUnderstood()
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(clock.now()), zone)
        drafts.set(EventParser.parse(clean, now))
        _state.value = CreateVoiceUiState.Heard(clean)
    }
}
