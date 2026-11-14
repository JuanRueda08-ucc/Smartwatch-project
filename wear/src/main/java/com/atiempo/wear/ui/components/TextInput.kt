package com.atiempo.wear.ui.components

import android.app.Activity
import android.app.RemoteInput
import android.content.ActivityNotFoundException
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender

/** Clave del texto en el resultado de RemoteInput. */
private const val INPUT_KEY = "text"

/** Entrada de texto de Wear lista para abrir. */
class TextInputLauncher(val available: Boolean, private val open: () -> Unit) {
    fun launch() = open()
}

/**
 * Entrada de texto estándar de Wear (`RemoteInput`): dictado primero, teclado como alternativa.
 * Devuelve el texto en ambos casos (D-020). No necesita permiso de micrófono.
 *
 * - `onText`: hubo texto.
 * - `onEmpty`: la entrada terminó sin texto.
 * - `onCancel`: la persona la cerró.
 */
@Composable
fun rememberTextInput(
    prompt: String,
    onText: (String) -> Unit,
    onEmpty: () -> Unit = {},
    onCancel: () -> Unit = {},
): TextInputLauncher {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    val currentOnEmpty by rememberUpdatedState(onEmpty)
    val currentOnCancel by rememberUpdatedState(onCancel)
    val intent = remember(prompt) {
        val remoteInput = RemoteInput.Builder(INPUT_KEY)
            .setLabel(prompt)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }
            .build()
        RemoteInputIntentHelper.createActionRemoteInputIntent().also {
            RemoteInputIntentHelper.putRemoteInputsExtra(it, listOf(remoteInput))
            RemoteInputIntentHelper.putTitleExtra(it, prompt)
        }
    }
    var available by remember { mutableStateOf(intent.resolveActivity(context.packageManager) != null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.let { RemoteInput.getResultsFromIntent(it) }?.getCharSequence(INPUT_KEY)?.toString()
        when {
            !text.isNullOrBlank() -> currentOnText(text)
            result.resultCode == Activity.RESULT_OK -> currentOnEmpty()
            else -> currentOnCancel()
        }
    }
    return TextInputLauncher(available) {
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            available = false
            currentOnCancel()
        }
    }
}
