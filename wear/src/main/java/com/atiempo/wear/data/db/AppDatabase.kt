package com.atiempo.wear.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Routine::class, Event::class, PrepSession::class, StepRecord::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routineDao(): RoutineDao
    abstract fun eventDao(): EventDao
    abstract fun prepSessionDao(): PrepSessionDao
    abstract fun stepRecordDao(): StepRecordDao

    companion object {
        const val NAME = "atiempo.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
