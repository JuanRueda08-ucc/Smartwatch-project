package com.atiempo.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.atiempo.wear.AppContainer
import com.atiempo.wear.domain.model.ThemeMode
import com.atiempo.wear.ui.create.ConfirmDateRoute
import com.atiempo.wear.ui.create.ConfirmEventRoute
import com.atiempo.wear.ui.create.ConfirmField
import com.atiempo.wear.ui.create.ConfirmPlaceRoute
import com.atiempo.wear.ui.create.ConfirmRoutineRoute
import com.atiempo.wear.ui.create.ConfirmTimeRoute
import com.atiempo.wear.ui.create.CreateVoiceRoute
import com.atiempo.wear.ui.detail.EventDetailRoute
import com.atiempo.wear.ui.events.EventsRoute
import com.atiempo.wear.ui.explain.ExplainScreen
import com.atiempo.wear.ui.home.HomeRoute
import com.atiempo.wear.ui.prep.PrepDoneScreen
import com.atiempo.wear.ui.prep.PrepScreen
import com.atiempo.wear.ui.routines.RoutineEditScreen
import com.atiempo.wear.ui.routines.RoutinesScreen
import com.atiempo.wear.ui.settings.SettingsScreen
import com.atiempo.wear.ui.summary.SummaryScreen
import kotlinx.coroutines.launch

/** Id provisional mientras las pantallas no leen la base de datos (Fase 3). */
private const val SAMPLE_ID = 1L

/**
 * Grafo de navegación con deslizar para volver. Los enlaces siguen el prototipo aprobado
 * (`design/prototipo/index.html`).
 */
@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberSwipeDismissableNavController()
    val scope = rememberCoroutineScope()

    SwipeDismissableNavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                container = container,
                onViewPlan = { eventId -> nav.navigate(Routes.eventDetail(eventId)) },
                onViewEvents = { nav.navigate(Routes.EVENTS) },
                onCreateByVoice = { nav.navigate(Routes.CREATE_VOICE) },
            )
        }
        composable(Routes.EVENTS) {
            EventsRoute(
                container = container,
                onNewEvent = { nav.navigate(Routes.CREATE_VOICE) },
                onOpenEvent = { eventId -> nav.navigate(Routes.eventDetail(eventId)) },
                onRoutines = { nav.navigate(Routes.ROUTINES) },
                onSummary = { nav.navigate(Routes.SUMMARY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.EVENT_DETAIL) { entry ->
            val eventId = entry.arguments?.getString(Routes.ARG_EVENT_ID)?.toLongOrNull() ?: return@composable
            EventDetailRoute(
                container = container,
                eventId = eventId,
                onWhyThisTime = { id -> nav.navigate(Routes.explain(id)) },
                // Provisional: la sesión de preparación se crea en la tarea 3.6; por ahora va el id del evento.
                onStartNow = { id -> nav.navigate(Routes.prep(id)) },
            )
        }
        composable(Routes.CREATE_VOICE) {
            CreateVoiceRoute(container, onParsed = {
                nav.navigate(Routes.CREATE_CONFIRM) { popUpTo(Routes.CREATE_VOICE) { inclusive = true } }
            })
        }
        composable(Routes.CREATE_CONFIRM) {
            // Tras guardar se vuelve a la lista. Si se llegó desde Inicio vacío, la lista no está atrás:
            // se abre encima de Inicio.
            val toEvents = {
                if (!nav.popBackStack(Routes.EVENTS, inclusive = false)) {
                    nav.navigate(Routes.EVENTS) { popUpTo(Routes.HOME) }
                }
            }
            ConfirmEventRoute(
                container = container,
                onEdit = { field ->
                    when (field) {
                        ConfirmField.DATE -> nav.navigate(Routes.CONFIRM_DATE)
                        ConfirmField.TIME -> nav.navigate(Routes.CONFIRM_TIME)
                        ConfirmField.ROUTINE -> nav.navigate(Routes.CONFIRM_ROUTINE)
                        ConfirmField.PLACE -> nav.navigate(Routes.CONFIRM_PLACE)
                        ConfirmField.TITLE -> Unit // se edita con la entrada de texto, en la misma pantalla
                    }
                },
                onSaved = { toEvents() },
                onNothingToConfirm = { toEvents() },
            )
        }
        composable(Routes.CONFIRM_DATE) { ConfirmDateRoute(container, onDone = { nav.popBackStack() }) }
        composable(Routes.CONFIRM_TIME) { ConfirmTimeRoute(container, onDone = { nav.popBackStack() }) }
        composable(Routes.CONFIRM_ROUTINE) { ConfirmRoutineRoute(container, onDone = { nav.popBackStack() }) }
        composable(Routes.CONFIRM_PLACE) { ConfirmPlaceRoute(container, onDone = { nav.popBackStack() }) }
        composable(Routes.PREP) {
            PrepScreen(onLeft = {
                nav.navigate(Routes.prepDone(SAMPLE_ID)) { popUpTo(Routes.PREP) { inclusive = true } }
            })
        }
        composable(Routes.PREP_DONE) {
            PrepDoneScreen(onDone = { nav.popBackStack(Routes.HOME, inclusive = false) })
        }
        composable(Routes.EXPLAIN) {
            ExplainScreen(onNotRight = { nav.navigate(Routes.routineEdit(SAMPLE_ID)) })
        }
        composable(Routes.ROUTINES) {
            RoutinesScreen(onEditRoutine = { nav.navigate(Routes.routineEdit(SAMPLE_ID)) })
        }
        composable(Routes.ROUTINE_EDIT) { RoutineEditScreen() }
        composable(Routes.SUMMARY) { SummaryScreen() }
        composable(Routes.SETTINGS) {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            SettingsScreen(themeMode = mode, onThemeModeChange = { scope.launch { container.settings.setThemeMode(it) } })
        }
    }
}
