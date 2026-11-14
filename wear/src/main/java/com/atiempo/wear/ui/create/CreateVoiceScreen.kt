package com.atiempo.wear.ui.create

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.TintedIcon
import com.atiempo.wear.ui.components.VoiceWave
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.rememberTextInput
import com.atiempo.wear.ui.theme.AppTheme
import kotlinx.coroutines.delay

/** Ejemplos de DISENO_UI §3.4. Se pueden tocar: pasan por el mismo intérprete que la voz. */
private val EXAMPLES = listOf(R.string.voice_example_class, R.string.voice_example_appointment, R.string.voice_example_social)

/** Tiempo que se muestra "Entendí" antes de pasar a confirmar. */
private const val HEARD_PAUSE_MS = 1_400L

/** 3.4 Crear por voz (`create/voice`). Abre el reconocedor del sistema al entrar (D-020). */
@Composable
fun CreateVoiceRoute(container: AppContainer, onParsed: () -> Unit) {
    val viewModel: CreateVoiceViewModel = viewModel(
        factory = viewModelFactory { initializer { CreateVoiceViewModel(container.drafts, container.clock, container.zone) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Entrada de texto de Wear: dictado o teclado (D-020).
    val input = rememberTextInput(
        prompt = stringResource(R.string.voice_prompt),
        onText = viewModel::onTranscript,
        onEmpty = viewModel::onNotUnderstood,
        onCancel = viewModel::onCancelled,
    )
    val voiceAvailable = input.available
    val listen = {
        viewModel.onListening()
        input.launch()
    }
    // Al entrar se abre el reconocedor una sola vez; si se cancela, queda la pantalla con los ejemplos.
    var autoLaunched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(voiceAvailable) {
        if (voiceAvailable && !autoLaunched) {
            autoLaunched = true
            listen()
        }
    }
    val heard = state as? CreateVoiceUiState.Heard
    if (heard != null) {
        LaunchedEffect(heard.transcript) {
            delay(HEARD_PAUSE_MS)
            onParsed()
        }
    }
    CreateVoiceScreen(
        state = state,
        voiceAvailable = voiceAvailable,
        onSpeak = listen,
        onExample = { viewModel.onTranscript(context.getString(it)) },
    )
}

@Composable
fun CreateVoiceScreen(
    state: CreateVoiceUiState,
    voiceAvailable: Boolean,
    onSpeak: () -> Unit,
    onExample: (exampleRes: Int) -> Unit,
) {
    when (state) {
        is CreateVoiceUiState.Heard -> HeardContent(state.transcript)
        is CreateVoiceUiState.Ready -> ReadyContent(state.listening, state.notUnderstood, voiceAvailable, onSpeak, onExample)
    }
}

@Composable
private fun ReadyContent(
    listening: Boolean,
    notUnderstood: Boolean,
    voiceAvailable: Boolean,
    onSpeak: () -> Unit,
    onExample: (Int) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val listeningLabel = stringResource(R.string.a11y_listening)
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(text = stringResource(R.string.voice_title), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                    VoiceWave(
                        active = listening,
                        modifier = Modifier.semantics { if (listening) contentDescription = listeningLabel },
                    )
                }
            }
            if (voiceAvailable) {
                item {
                    Button(
                        onClick = onSpeak,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            contentColor = AppTheme.colors.onState,
                            iconColor = AppTheme.colors.onState,
                        ),
                        icon = { Icon(painterResource(R.drawable.ic_mic), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        label = { Text(text = stringResource(R.string.voice_speak), style = AppTheme.type.button) },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(
                        when {
                            !voiceAvailable -> R.string.voice_unavailable
                            notUnderstood -> R.string.voice_not_understood
                            else -> R.string.voice_try_example
                        },
                    ),
                    style = AppTheme.type.label,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                )
            }
            EXAMPLES.forEach { example ->
                item { ExampleButton(example, spec, onClick = { onExample(example) }) }
            }
        }
    }
}

@Composable
private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.ExampleButton(
    @StringRes example: Int,
    spec: androidx.wear.compose.material3.lazy.TransformationSpec,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.onSurface,
        ),
        border = BorderStroke(1.dp, AppTheme.colors.surfaceBright),
        label = {
            Text(
                text = stringResource(R.string.voice_quote, stringResource(example)),
                style = AppTheme.type.label,
                maxLines = 3,
            )
        },
    )
}

@Composable
private fun HeardContent(transcript: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TintedIcon(icon = R.drawable.ic_check, color = AppTheme.colors.primary, size = 44)
        Spacer(Modifier.height(6.dp))
        Text(text = stringResource(R.string.voice_heard), style = AppTheme.type.label, color = AppTheme.colors.onSurfaceVariant)
        Text(
            text = stringResource(R.string.voice_quote, transcript),
            style = AppTheme.type.body,
            color = AppTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 4,
        )
    }
}

// ---------- Previews ----------

@WearPreviews
@Composable
private fun CreateVoiceLightPreview() = ATiempoPreview(dark = false) {
    CreateVoiceScreen(CreateVoiceUiState.Ready(listening = true), voiceAvailable = true, onSpeak = {}, onExample = {})
}

@WearPreviews
@Composable
private fun CreateVoiceDarkPreview() = ATiempoPreview(dark = true) {
    CreateVoiceScreen(CreateVoiceUiState.Ready(listening = false, notUnderstood = true), voiceAvailable = true, onSpeak = {}, onExample = {})
}

@WearPreviews
@Composable
private fun CreateVoiceUnavailablePreview() = ATiempoPreview(dark = false) {
    CreateVoiceScreen(CreateVoiceUiState.Ready(listening = false), voiceAvailable = false, onSpeak = {}, onExample = {})
}

@WearPreviews
@Composable
private fun CreateVoiceHeardLightPreview() = ATiempoPreview(dark = false) {
    CreateVoiceScreen(CreateVoiceUiState.Heard("Cine con Laura el sábado a las 6 de la tarde"), true, {}, {})
}
