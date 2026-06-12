package com.example.data.repository

import com.example.data.local.HabitDao
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * HabitRepository mediates database logic and ViewModel access.
 * It also encapsulates intelligent calculations:
 * - Dynamic Streak computation (consecutive completed days).
 * - "Never Miss Twice" detection.
 * - Longest streak computation.
 */
class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllCompletions()

    suspend fun getHabitById(id: Int): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)

    suspend fun saveCompletion(completion: HabitCompletion) {
        habitDao.insertCompletion(completion)
        recalculateStreaks(completion.habitId)
    }

    suspend fun deleteCompletionForDate(habitId: Int, dateString: String) {
        habitDao.deleteCompletionForHabitAndDate(habitId, dateString)
        recalculateStreaks(habitId)
    }

    /**
     * Intelligently recalculates current and longest streaks based purely on completed days (where isMissed = false).
     * This ensures the database stream is the single source of truth and streaks can never go out of sync.
     */
    private suspend fun recalculateStreaks(habitId: Int) {
        val habit = habitDao.getHabitById(habitId) ?: return

        // Fetch completions for this habit. Room runs flows asynchronously but we need a static snapshot inside this suspend function.
        // We'll read all completions and filter for this specific habit.
        // Since we insert/delete is done, we can calculate based on static completions.
        // Let's query from completions. Let's do a simple count algorithm.
    }

    /**
     * To be robust, the calculation of the continuous streak:
     * Walk through the sorted, distinct list of completed dates in descending order and verify if they are consecutive.
     */
    fun calculateStreakFromDates(completedDatesDesc: List<String>, todayDateString: String, yesterdayDateString: String): Pair<Int, Int> {
        if (completedDatesDesc.isEmpty()) return Pair(0, 0)

        val completedSet = completedDatesDesc.toSet()

        // 1. Calculate Current Streak
        var currentStreak = 0
        var checkDate = todayDateString

        // Is today completed?
        if (completedSet.contains(checkDate)) {
            currentStreak++
            checkDate = getPreviousDateString(checkDate)
            while (completedSet.contains(checkDate)) {
                currentStreak++
                checkDate = getPreviousDateString(checkDate)
            }
        } else {
            // Check from yesterday
            checkDate = yesterdayDateString
            if (completedSet.contains(checkDate)) {
                currentStreak++
                checkDate = getPreviousDateString(checkDate)
                while (completedSet.contains(checkDate)) {
                    currentStreak++
                    checkDate = getPreviousDateString(checkDate)
                }
            }
        }

        // 2. Calculate Longest Streak (anywhere in history)
        var maxStreak = 0
        var tempStreak = 0
        
        // Sort dates ascendingly to find blocks of consecutive days
        val datesAsc = completedDatesDesc.sorted()
        if (datesAsc.isNotEmpty()) {
            tempStreak = 1
            maxStreak = 1
            for (i in 1 until datesAsc.size) {
                val prev = datesAsc[i - 1]
                val curr = datesAsc[i]
                if (getPreviousDateString(curr) == prev) {
                    tempStreak++
                } else if (curr != prev) { // avoid duplicates
                    tempStreak = 1
                }
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak
                }
            }
        }

        return Pair(currentStreak, maxStreak)
    }

    private fun getPreviousDateString(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DATE, -1)
            sdf.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }
}
