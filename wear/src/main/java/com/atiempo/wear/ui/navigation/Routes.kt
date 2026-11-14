package com.atiempo.wear.ui.navigation

/** Rutas de DISENO_UI §3. Las que llevan argumentos tienen una función para construirlas. */
object Routes {
    const val ARG_EVENT_ID = "eventId"
    const val ARG_SESSION_ID = "sessionId"
    const val ARG_ROUTINE_ID = "routineId"

    const val HOME = "home"
    const val EVENTS = "events"
    const val EVENT_DETAIL = "event/{$ARG_EVENT_ID}"
    const val CREATE_VOICE = "create/voice"
    const val CREATE_CONFIRM = "create/confirm"
    // Selectores de la confirmación: cada uno edita el borrador y vuelve.
    const val CONFIRM_DATE = "create/confirm/date"
    const val CONFIRM_TIME = "create/confirm/time"
    const val CONFIRM_ROUTINE = "create/confirm/routine"
    const val CONFIRM_PLACE = "create/confirm/place"
    const val PREP = "prep/{$ARG_SESSION_ID}"
    const val PREP_DONE = "prep/{$ARG_SESSION_ID}/done"
    const val EXPLAIN = "explain/{$ARG_EVENT_ID}"
    const val ROUTINE_EDIT = "routine/{$ARG_ROUTINE_ID}/edit"
    const val ROUTINES = "routines"
    const val SUMMARY = "summary"
    const val SETTINGS = "settings"

    fun eventDetail(eventId: Long) = "event/$eventId"
    fun prep(sessionId: Long) = "prep/$sessionId"
    fun prepDone(sessionId: Long) = "prep/$sessionId/done"
    fun explain(eventId: Long) = "explain/$eventId"
    fun routineEdit(routineId: Long) = "routine/$routineId/edit"
}
