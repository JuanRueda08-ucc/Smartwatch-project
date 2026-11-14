package com.atiempo.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.atiempo.wear.ui.explain.ExplainRoute
import com.atiempo.wear.ui.home.HomeRoute
import com.atiempo.wear.ui.prep.PrepDoneRoute
import com.atiempo.wear.ui.prep.PrepRoute
import com.atiempo.wear.ui.routines.RoutineEditRoute
import com.atiempo.wear.ui.routines.RoutinesRoute
import com.atiempo.wear.ui.settings.SettingsScreen
import com.atiempo.wear.ui.summary.SummaryScreen
import com.atiempo.wear.domain.PrepPhase
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Grafo de navegación con deslizar para volver. Los enlaces siguen el prototipo aprobado
 * (`design/prototipo/index.html`).
 */
@Composable
fun AppNavHost(container: AppContainer, openPrepRequest: Int = 0) {
    val nav = rememberSwipeDismissableNavController()
    val scope = rememberCoroutineScope()

    // Cuando una preparación arranca sola (llegó la hora del primer aviso), se abre su pantalla,
    // como hará el aviso a pantalla completa (tarea 3.8).
    LaunchedEffect(Unit) {
        container.prep.autoStarted.collect { sessionId ->
            if (nav.currentDestination?.route != Routes.PREP) nav.navigate(Routes.prep(sessionId))
        }
    }
    // Se tocó el aviso (o se abrió a pantalla completa): ir a la preparación en curso. Si la app estaba
    // cerrada, la sesión se recupera en un momento; se espera hasta 3 s.
    LaunchedEffect(openPrepRequest) {
        if (openPrepRequest == 0) return@LaunchedEffect
        val session = withTimeoutOrNull(3_000) {
            container.prep.active.filterNotNull().first { it.state.phase == PrepPhase.ACTIVE }
        } ?: return@LaunchedEffect
        if (nav.currentDestination?.route != Routes.PREP) nav.navigate(Routes.prep(session.sessionId))
    }

    SwipeDismissableNavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                container = container,
                onViewPlan = { eventId -> nav.navigate(Routes.eventDetail(eventId)) },
                onViewEvents = { nav.navigate(Routes.EVENTS) },
                onCreateByVoice = { nav.navigate(Routes.CREATE_VOICE) },
                onBackToAlert = { sessionId -> nav.navigate(Routes.prep(sessionId)) },
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
                onStartNow = { id ->
                    scope.launch { container.prep.start(id)?.let { sessionId -> nav.navigate(Routes.prep(sessionId)) } }
                },
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
        composable(Routes.PREP) { entry ->
            val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID)?.toLongOrNull() ?: return@composable
            PrepRoute(
                container = container,
                sessionId = sessionId,
                onFinished = {
                    nav.navigate(Routes.prepDone(sessionId)) { popUpTo(Routes.PREP) { inclusive = true } }
                },
                onMissing = { if (!nav.popBackStack()) nav.navigate(Routes.HOME) },
            )
        }
        composable(Routes.PREP_DONE) { entry ->
            val sessionId = entry.arguments?.getString(Routes.ARG_SESSION_ID)?.toLongOrNull() ?: return@composable
            PrepDoneRoute(container, sessionId, onDone = {
                if (!nav.popBackStack(Routes.HOME, inclusive = false)) nav.navigate(Routes.HOME)
            })
        }
        composable(Routes.EXPLAIN) { entry ->
            val eventId = entry.arguments?.getString(Routes.ARG_EVENT_ID)?.toLongOrNull() ?: return@composable
            ExplainRoute(container, eventId, onNotRight = { routineId -> nav.navigate(Routes.routineEdit(routineId)) })
        }
        composable(Routes.ROUTINES) {
            RoutinesRoute(container, onEditRoutine = { routineId -> nav.navigate(Routes.routineEdit(routineId)) })
        }
        composable(Routes.ROUTINE_EDIT) { entry ->
            val routineId = entry.arguments?.getString(Routes.ARG_ROUTINE_ID)?.toLongOrNull() ?: return@composable
            RoutineEditRoute(container, routineId)
        }
        composable(Routes.SUMMARY) { SummaryScreen() }
        composable(Routes.SETTINGS) {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
            SettingsScreen(themeMode = mode, onThemeModeChange = { scope.launch { container.settings.setThemeMode(it) } })
        }
    }
}
