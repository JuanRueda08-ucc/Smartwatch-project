package com.atiempo.wear.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.atiempo.wear.domain.model.PrepStep
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY id")
    fun observeAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    fun observe(id: Long): Flow<Routine?>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun get(id: Long): Routine?

    @Upsert
    suspend fun upsertAll(routines: List<Routine>)

    @Update
    suspend fun update(routine: Routine)
}

@Dao
interface EventDao {
    /** Eventos que empiezan desde `from` (epoch ms), en orden. */
    @Query("SELECT * FROM events WHERE startAt >= :from ORDER BY startAt")
    fun observeUpcoming(from: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observe(id: Long): Flow<Event?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): Event?

    @Insert
    suspend fun insert(event: Event): Long

    @Insert
    suspend fun insertAll(events: List<Event>): List<Long>

    @Update
    suspend fun update(event: Event)

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int
}

@Dao
interface PrepSessionDao {
    @Insert
    suspend fun insert(session: PrepSession): Long

    @Update
    suspend fun update(session: PrepSession)

    @Query("SELECT * FROM prep_sessions WHERE id = :id")
    suspend fun get(id: Long): PrepSession?

    /** La sesión de un evento, si ya se empezó a preparar. */
    @Query("SELECT * FROM prep_sessions WHERE eventId = :eventId ORDER BY id DESC LIMIT 1")
    suspend fun forEvent(eventId: Long): PrepSession?

    /** La preparación más reciente que no terminó con "Ya salí" (para recuperarla al reiniciar). */
    @Query("SELECT * FROM prep_sessions WHERE leftAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun latestOpen(): PrepSession?

    /** Eventos cuya preparación ya terminó con "Ya salí". */
    @Query("SELECT DISTINCT eventId FROM prep_sessions WHERE leftAt IS NOT NULL")
    fun observeLeftEventIds(): Flow<List<Long>>

    /** Sesiones iniciadas entre `from` y `to` (epoch ms), para el resumen semanal. */
    @Query("SELECT * FROM prep_sessions WHERE startedAt >= :from AND startedAt < :to ORDER BY startedAt")
    fun observeBetween(from: Long, to: Long): Flow<List<PrepSession>>

    @Query("SELECT COUNT(*) FROM prep_sessions")
    suspend fun count(): Int
}

@Dao
interface StepRecordDao {
    @Insert
    suspend fun insert(record: StepRecord): Long

    @Insert
    suspend fun insertAll(records: List<StepRecord>)

    /** Historial de un paso de una rutina, del más antiguo al más reciente (para la EMA). */
    @Query("SELECT * FROM step_records WHERE routineId = :routineId AND step = :step ORDER BY id")
    suspend fun history(routineId: Long, step: PrepStep): List<StepRecord>

    @Query("SELECT * FROM step_records WHERE sessionId = :sessionId ORDER BY id")
    suspend fun forSession(sessionId: Long): List<StepRecord>

    @Query("SELECT COUNT(*) FROM step_records")
    suspend fun count(): Int
}
