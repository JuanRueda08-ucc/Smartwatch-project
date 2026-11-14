package com.atiempo.wear.ui.create

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.AppContainer
import com.atiempo.wear.R
import com.atiempo.wear.domain.model.Parsed
import com.atiempo.wear.domain.model.ParsedEvent
import com.atiempo.wear.domain.model.RoutineKind
import com.atiempo.wear.ui.components.ATiempoPreview
import com.atiempo.wear.ui.components.BottomAction
import com.atiempo.wear.ui.components.BottomActionButton
import com.atiempo.wear.ui.components.BottomFade
import com.atiempo.wear.ui.components.WearPreviews
import com.atiempo.wear.ui.components.dayWithNumber
import com.atiempo.wear.ui.components.rememberTextInput
import com.atiempo.wear.ui.theme.AppTheme
import com.atiempo.wear.ui.theme.TINT_ALPHA
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Qué dato abrir para editar desde la confirmación. */
enum class ConfirmField { TITLE, DATE, TIME, ROUTINE, PLACE }

/** 3.5 Confirmar evento (`create/confirm`): conecta la pantalla con su ViewModel. */
@Composable
fun ConfirmEventRoute(
    container: AppContainer,
    onEdit: (ConfirmField) -> Unit,
    onSaved: () -> Unit,
    onNothingToConfirm: () -> Unit,
) {
    val viewModel: ConfirmEventViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ConfirmEventViewModel(container.drafts, container.events, container.routines, container.clock, container.zone)
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    // El título se cambia con la entrada de texto de Wear (dictado o teclado), sin salir de la pantalla.
    val titleInput = rememberTextInput(
        prompt = stringResource(R.string.confirm_title_prompt),
        onText = viewModel::setTitle,
    )
    ConfirmEventScreen(
        state = state,
        onEdit = { field -> if (field == ConfirmField.TITLE) titleInput.launch() else onEdit(field) },
        onSave = viewModel::save,
        onNothingToConfirm = onNothingToConfirm,
    )
    // Confirmación animada de Wear; al cerrarse vuelve a la lista de eventos.
    val savedText = stringResource(R.string.confirm_saved)
    val savedStyle = ConfirmationDialogDefaults.curvedTextStyle
    SuccessConfirmationDialog(
        visible = state.saved,
        onDismissRequest = {
            viewModel.onLeft()
            onSaved()
        },
        curvedText = { confirmationDialogCurvedText(savedText, savedStyle) },
    )
}

@Composable
fun ConfirmEventScreen(
    state: ConfirmUiState,
    onEdit: (ConfirmField) -> Unit,
    onSave: () -> Unit,
    onNothingToConfirm: () -> Unit,
) {
    val draft = state.draft
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        if (draft == null) {
            if (!state.saved) NothingToConfirm(onNothingToConfirm)
            return@Box
        }
        val listState = rememberTransformingLazyColumnState()
        val spec = rememberTransformationSpec()
        val direction = LocalLayoutDirection.current
        ScreenScaffold(scrollState = listState) { contentPadding ->
            // "Guardar" es la acción principal: va fija abajo y la lista deja libre su espacio.
            val padding = PaddingValues(
                start = contentPadding.calculateStartPadding(direction),
                end = contentPadding.calculateEndPadding(direction),
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + BottomAction.ReservedHeight,
            )
            TransformingLazyColumn(state = listState, contentPadding = padding) {
                item {
                    ListHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(text = stringResource(R.string.confirm_question), style = AppTheme.type.title, color = AppTheme.colors.onSurface)
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.confirm_hint),
                        style = AppTheme.type.label,
                        color = AppTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
                item {
                    FieldCard(R.string.confirm_field_title, draft.title.value, draft.title.confident, spec) { onEdit(ConfirmField.TITLE) }
                }
                item {
                    val days = ChronoUnit.DAYS.between(state.today, draft.date.value).toInt()
                    val day = dayWithNumber(days, draft.date.value.dayOfWeek, draft.date.value.dayOfMonth)
                    FieldCard(R.string.confirm_field_day, day, draft.date.confident && !state.isPast, spec) { onEdit(ConfirmField.DATE) }
                }
                item {
                    FieldCard(
                        label = R.string.confirm_field_time,
                        value = TIME_FORMAT.format(draft.time.value),
                        confident = draft.time.confident && !state.isPast,
                        spec = spec,
                        warning = if (state.isPast) R.string.confirm_time_passed else R.string.confirm_check_this,
                    ) { onEdit(ConfirmField.TIME) }
                }
                item {
                    FieldCard(R.string.confirm_field_routine, stringResource(draft.routine.value.label()), draft.routine.confident, spec) {
                        onEdit(ConfirmField.ROUTINE)
                    }
                }
                item {
                    FieldCard(
                        label = R.string.confirm_field_place,
                        value = draft.place.value ?: stringResource(R.string.confirm_no_place),
                        confident = draft.place.confident,
                        spec = spec,
                    ) { onEdit(ConfirmField.PLACE) }
                }
            }
        }
        BottomFade()
        BottomActionButton(
            label = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = !state.isPast,
        )
    }
}

/**
 * Tarjeta de un dato interpretado. Si el intérprete no está seguro, va con borde y fondo `state3Check`
 * y la marca "Revisa esto" (DISENO_UI §3.5). Nunca solo el color: siempre con ícono y texto.
 */
@Composable
private fun TransformingLazyColumnItemScope.FieldCard(
    @StringRes label: Int,
    value: String,
    confident: Boolean,
    spec: TransformationSpec,
    @StringRes warning: Int = R.string.confirm_check_this,
    onClick: () -> Unit,
) {
    val check = AppTheme.colors.state3Check
    val labelText = stringResource(label)
    val description = if (confident) stringResource(R.string.a11y_field, labelText, value)
    else stringResource(R.string.a11y_field_check, labelText, value)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec)
            .clearAndSetSemantics { contentDescription = description },
        transformation = SurfaceTransformation(spec),
        colors = CardDefaults.cardColors(
            containerColor = if (confident) AppTheme.colors.surface
            else check.copy(alpha = TINT_ALPHA).compositeOver(AppTheme.colors.surface),
            contentColor = AppTheme.colors.onSurface,
        ),
        border = if (confident) BorderStroke(1.dp, AppTheme.colors.surfaceBright) else BorderStroke(2.dp, check),
    ) {
        Text(text = labelText, style = AppTheme.type.caption, color = AppTheme.colors.onSurfaceVariant)
        Text(
            text = value,
            style = AppTheme.type.button,
            color = AppTheme.colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!confident) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = check, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = stringResource(warning), style = AppTheme.type.caption, color = check)
            }
        }
    }
}

@Composable
private fun NothingToConfirm(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.confirm_nothing),
            style = AppTheme.type.body,
            color = AppTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary, contentColor = AppTheme.colors.onState),
            label = { Text(text = stringResource(R.string.action_view_events), style = AppTheme.type.button) },
        )
    }
}

@StringRes
fun RoutineKind.label(): Int = when (this) {
    RoutineKind.CLASS -> R.string.routine_class
    RoutineKind.APPOINTMENT -> R.string.routine_appointment
    RoutineKind.SOCIAL -> R.string.routine_social
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

// ---------- Previews ----------

private val previewDraft = ParsedEvent(
    transcript = "Cine con Laura el sábado a las 6 de la tarde",
    title = Parsed("Cine con Laura", true),
    date = Parsed(LocalDate.of(2026, 11, 21), true),
    time = Parsed(LocalTime.of(18, 0), true),
    routine = Parsed(RoutineKind.SOCIAL, true),
    place = Parsed(null, false),
)
private val previewState = ConfirmUiState(draft = previewDraft, today = LocalDate.of(2026, 11, 16))

@WearPreviews
@Composable
private fun ConfirmLightPreview() = ATiempoPreview(dark = false) { ConfirmEventScreen(previewState, {}, {}, {}) }

@WearPreviews
@Composable
private fun ConfirmDarkPreview() = ATiempoPreview(dark = true) { ConfirmEventScreen(previewState, {}, {}, {}) }

@WearPreviews
@Composable
private fun ConfirmPastLightPreview() = ATiempoPreview(dark = false) {
    ConfirmEventScreen(previewState.copy(isPast = true), {}, {}, {})
}
