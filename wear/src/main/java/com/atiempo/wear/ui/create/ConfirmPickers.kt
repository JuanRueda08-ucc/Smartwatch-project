package com.atiempo.wear.ui.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.RadioButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.Parsed
import com.atiempo.wear.domain.model.RoutineKind
import com.atiempo.wear.ui.components.rememberTextInput
import com.atiempo.wear.ui.theme.AppTheme
import java.time.Instant

/*
 * Selectores de la confirmación (3.5). Cada uno es una pantalla de la navegación, así que deslizar para
 * volver regresa a la confirmación. Editan el borrador compartido y dejan el dato como seguro.
 */

/** Día: selector de fecha de Wear, desde hoy hasta un año. */
@Composable
fun ConfirmDateRoute(container: AppContainer, onDone: () -> Unit) {
    val draft by container.drafts.draft.collectAsStateWithLifecycle()
    val current = draft ?: return onDone()
    val today = Instant.ofEpochMilli(container.clock.now()).atZone(container.zone).toLocalDate()
    DatePicker(
        initialDate = current.date.value.coerceAtLeast(today),
        onDatePicked = { picked ->
            container.drafts.update { it.copy(date = Parsed(picked, true)) }
            onDone()
        },
        minValidDate = today,
        maxValidDate = today.plusYears(1),
    )
}

/** Hora: selector de Wear en formato de 24 horas, como el resto de la app ("14:00"). */
@Composable
fun ConfirmTimeRoute(container: AppContainer, onDone: () -> Unit) {
    val draft by container.drafts.draft.collectAsStateWithLifecycle()
    val current = draft ?: return onDone()
    TimePicker(
        initialTime = current.time.value,
        onTimePicked = { picked ->
            container.drafts.update { it.copy(time = Parsed(picked, true)) }
            onDone()
        },
        timePickerType = TimePickerType.HoursMinutes24H,
    )
}

/** Rutina: las 3 rutinas como opciones de radio. */
@Composable
fun ConfirmRoutineRoute(container: AppContainer, onDone: () -> Unit) {
    val draft by container.drafts.draft.collectAsStateWithLifecycle()
    val current = draft ?: return onDone()
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(text = stringResource(R.string.confirm_field_routine), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            RoutineKind.entries.forEach { kind ->
                item {
                    RadioButton(
                        selected = current.routine.value == kind,
                        onSelect = {
                            container.drafts.update { it.copy(routine = Parsed(kind, true)) }
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = RadioButtonDefaults.radioButtonColors(
                            selectedContainerColor = AppTheme.colors.surfaceBright,
                            selectedContentColor = AppTheme.colors.onSurface,
                            selectedControlColor = AppTheme.colors.primary,
                            unselectedContainerColor = AppTheme.colors.surface,
                            unselectedContentColor = AppTheme.colors.onSurface,
                            unselectedControlColor = AppTheme.colors.onSurfaceVariant,
                        ),
                        label = { Text(text = stringResource(kind.label()), style = AppTheme.type.button) },
                    )
                }
            }
        }
    }
}

/** Lugar: dictarlo o confirmar que no hay lugar. */
@Composable
fun ConfirmPlaceRoute(container: AppContainer, onDone: () -> Unit) {
    val draft by container.drafts.draft.collectAsStateWithLifecycle()
    val current = draft ?: return onDone()
    val setPlace = { place: String? ->
        container.drafts.update { it.copy(place = Parsed(place?.trim()?.ifEmpty { null }, true)) }
        onDone()
    }
    val input = rememberTextInput(prompt = stringResource(R.string.confirm_place_prompt), onText = { setPlace(it) })
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec), transformation = SurfaceTransformation(spec)) {
                    Text(text = stringResource(R.string.confirm_field_place), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                }
            }
            if (current.place.value != null) {
                item {
                    Text(
                        text = current.place.value!!,
                        style = AppTheme.type.body,
                        color = AppTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            if (input.available) {
                item {
                    Button(
                        onClick = { input.launch() },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primary,
                            contentColor = AppTheme.colors.onState,
                            iconColor = AppTheme.colors.onState,
                        ),
                        icon = { Icon(painterResource(R.drawable.ic_mic), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        label = { Text(text = stringResource(R.string.confirm_place_dictate), style = AppTheme.type.button) },
                    )
                }
            }
            item {
                Button(
                    onClick = { setPlace(null) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface, contentColor = AppTheme.colors.onSurface),
                    border = BorderStroke(1.dp, AppTheme.colors.surfaceBright),
                    label = { Text(text = stringResource(R.string.confirm_no_place), style = AppTheme.type.button) },
                )
            }
        }
    }
}
