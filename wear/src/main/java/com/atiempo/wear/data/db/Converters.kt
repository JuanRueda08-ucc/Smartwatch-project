package com.atiempo.wear.data.db

import androidx.room.TypeConverter
import com.atiempo.wear.domain.model.PrepStep

/** Conversores de Room. Las listas se guardan con un separador que no aparece en textos normales. */
class Converters {

    @TypeConverter
    fun fromItems(items: List<String>): String = items.joinToString(SEPARATOR)

    @TypeConverter
    fun toItems(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    @TypeConverter
    fun fromStep(step: PrepStep): String = step.name

    @TypeConverter
    fun toStep(value: String): PrepStep = PrepStep.valueOf(value)

    private companion object {
        /** Separador de unidades ASCII (U+001F). */
        const val SEPARATOR = "\u001F"
    }
}
