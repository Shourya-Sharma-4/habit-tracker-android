package com.example.data.local

import androidx.room.*
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

/**
 * HabitDao specifies the database query operations.
 * Following modern Android architectures:
 * - We return `Flow` for reactive, real-time UI updates.
 * - Non-flow operations use `suspend` to run safely in coroutines.
 */
@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdTimestamp DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habit_completions ORDER BY timestamp DESC")
    fun getAllCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId")
    fun getCompletionsForHabit(habitId: Int): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE dateString = :dateString")
    suspend fun getCompletionsForDate(dateString: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE dateString BETWEEN :startDate AND :endDate")
    fun getCompletionsBetweenDates(startDate: String, endDate: String): Flow<List<HabitCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Delete
    suspend fun deleteCompletion(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteCompletionForHabitAndDate(habitId: Int, dateString: String)
}
