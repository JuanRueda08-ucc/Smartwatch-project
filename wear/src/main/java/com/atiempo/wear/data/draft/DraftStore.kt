package com.atiempo.wear.data.draft

import com.atiempo.wear.domain.model.ParsedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Evento interpretado que espera confirmación. Vive en memoria entre "Crear por voz" y
 * "Confirmar evento"; no se guarda en la base hasta que la persona toca "Guardar".
 */
class DraftStore {
    private val _draft = MutableStateFlow<ParsedEvent?>(null)
    val draft: StateFlow<ParsedEvent?> = _draft.asStateFlow()

    fun set(event: ParsedEvent) {
        _draft.value = event
    }

    fun clear() {
        _draft.value = null
    }
}
