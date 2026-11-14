package com.atiempo.wear.data.repo

import com.atiempo.wear.data.db.Routine
import com.atiempo.wear.domain.model.PrepStep

/* Acceso por paso a las duraciones de una rutina. El paso LEAVE corresponde al trayecto. */

fun Routine.defaultFor(step: PrepStep): Int = when (step) {
    PrepStep.FINISH -> defaultFinishMin
    PrepStep.PREPARE -> defaultPrepareMin
    PrepStep.CHECK -> defaultCheckMin
    PrepStep.LEAVE -> defaultTravelMin
}

fun Routine.manualFor(step: PrepStep): Int? = when (step) {
    PrepStep.FINISH -> manualFinishMin
    PrepStep.PREPARE -> manualPrepareMin
    PrepStep.CHECK -> manualCheckMin
    PrepStep.LEAVE -> manualTravelMin
}

/** La rutina con el ajuste manual de un paso cambiado (null = quitar el ajuste). */
fun Routine.withManual(step: PrepStep, minutes: Int?): Routine = when (step) {
    PrepStep.FINISH -> copy(manualFinishMin = minutes)
    PrepStep.PREPARE -> copy(manualPrepareMin = minutes)
    PrepStep.CHECK -> copy(manualCheckMin = minutes)
    PrepStep.LEAVE -> copy(manualTravelMin = minutes)
}

/** Duración de cada paso sin evento concreto: el ajuste manual o el valor por defecto. */
fun Routine.baseMinutes(step: PrepStep): Int = manualFor(step) ?: defaultFor(step)
