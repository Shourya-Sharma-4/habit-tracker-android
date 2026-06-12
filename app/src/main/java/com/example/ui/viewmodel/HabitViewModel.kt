package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * HabitViewModel manages the UI state and interfaces with the repository.
 * Adhering to the MVVM Architecture:
 * - We read database changes using StateFlows for live UI updates.
 * - We perform background tasks (like DB queries, streak calculations, and API calls) via Co-routines.
 * - Written clearly with beginner-friendly comments explain each method.
 */
class HabitViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HabitRepository
    
    // Simple Date formats used throughout the app
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 1. Reactive Databinding flows from Room
    val habits: StateFlow<List<Habit>>
    val completions: StateFlow<List<HabitCompletion>>

    // 2. UI Intermediary States (for filtering, calendar and prompts)
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _studentModeOnly = MutableStateFlow(false)
    val studentModeOnly: StateFlow<Boolean> = _studentModeOnly.asStateFlow()

    // AI Coach advice states
    private val _coachAdvice = MutableStateFlow<String>("")
    val coachAdvice: StateFlow<String> = _coachAdvice.asStateFlow()

    private val _isCoachLoading = MutableStateFlow(false)
    val isCoachLoading: StateFlow<Boolean> = _isCoachLoading.asStateFlow()

    init {
        val database = HabitDatabase.getDatabase(application)
        val habitDao = database.habitDao()
        repository = HabitRepository(habitDao)

        // Read habits from database of active items
        habits = repository.allHabits.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Read completions from database
        completions = repository.allCompletions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Auto-generate coach advice on launch with a local starter fallback
        refreshCoachAdvice()
    }

    // --- Helper Date APIs ---

    fun getTodayDateString(): String {
        return dateFormatter.format(Date())
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return dateFormatter.format(cal.time)
    }

    // Helper to get formatted week labels (e.g., "Mon 8") for calendar display
    fun getWeekDates(): List<CalendarDateInfo> {
        val list = mutableListOf<CalendarDateInfo>()
        val cal = Calendar.getInstance()
        
        // Go back to find Monday of the current week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
        val numFormatter = SimpleDateFormat("d", Locale.getDefault())
        
        for (i in 0..6) {
            val dateStr = dateFormatter.format(cal.time)
            list.add(
                CalendarDateInfo(
                    dateString = dateStr,
                    dayOfWeek = dayFormatter.format(cal.time),
                    dayNumber = numFormatter.format(cal.time)
                )
            )
            cal.add(Calendar.DATE, 1)
        }
        return list
    }

    data class CalendarDateInfo(
        val dateString: String,
        val dayOfWeek: String,
        val dayNumber: String
    )

    // --- Database Operations ---

    fun addHabit(name: String, category: String, difficulty: String, isStudentMode: Boolean) {
        viewModelScope.launch {
            val newHabit = Habit(
                name = name,
                category = category,
                difficulty = difficulty,
                isStudentMode = isStudentMode,
                createdTimestamp = System.currentTimeMillis()
            )
            repository.insertHabit(newHabit)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    /**
     * Toggles whether a habit is marked complete for a specific date.
     * Recalculates streak records dynamically to prevent drift or desync.
     */
    fun toggleHabitCompletion(habitId: Int, dateString: String) {
        viewModelScope.launch {
            val existing = completions.value.find { it.habitId == habitId && it.dateString == dateString }
            if (existing != null && !existing.isMissed) {
                // If completed, delete completion record
                repository.deleteCompletionForDate(habitId, dateString)
            } else {
                // If not completed (or marked missed), make it completed (remove missed reason)
                if (existing != null && existing.isMissed) {
                    repository.deleteCompletionForDate(habitId, dateString)
                }
                val newCompletion = HabitCompletion(
                    habitId = habitId,
                    dateString = dateString,
                    isMissed = false
                )
                repository.saveCompletion(newCompletion)
            }
            // Trigger streak updater on the Habit model
            triggerStreakRecalculation(habitId)
        }
    }

    /**
     * Logs a reason for why a habit was missed.
     */
    fun logHabitMiss(habitId: Int, dateString: String, reason: String, notes: String?) {
        viewModelScope.launch {
            repository.deleteCompletionForDate(habitId, dateString)
            val missedCompletion = HabitCompletion(
                habitId = habitId,
                dateString = dateString,
                isMissed = true,
                missReason = reason,
                notes = notes
            )
            repository.saveCompletion(missedCompletion)
            triggerStreakRecalculation(habitId)
        }
    }

    /**
     * Re-scans all completed logs to update current/longest streak on Habit Entity.
     */
    private suspend fun triggerStreakRecalculation(habitId: Int) {
        val habit = repository.getHabitById(habitId) ?: return
        val allCompletedDatesForHabit = completions.value
            .filter { it.habitId == habitId && !it.isMissed }
            .map { it.dateString }
            .distinct()
            .sortedDescending() // newest first

        val (currentStreak, longestStreak) = repository.calculateStreakFromDates(
            allCompletedDatesForHabit,
            getTodayDateString(),
            getYesterdayDateString()
        )

        val updatedHabit = habit.copy(
            streakCount = currentStreak,
            longestStreak = maxOf(habit.longestStreak, longestStreak)
        )
        repository.updateHabit(updatedHabit)
    }

    // --- Dynamic Stats Computations ---

    /**
     * Calculates the dynamically updated Consistency Score (0 to 100).
     */
    fun getConsistencyScore(): Int {
        val currentHabits = habits.value
        if (currentHabits.isEmpty()) return 100 // default 100 for blank slate
        
        // Take last 7 days of logs
        val lastSevenDays = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0..6) {
            lastSevenDays.add(dateFormatter.format(cal.time))
            cal.add(Calendar.DATE, -1)
        }

        val totalPossibleEvents = currentHabits.size * 7
        val actualSuccessCompletions = completions.value.count {
            it.dateString in lastSevenDays && !it.isMissed
        }

        val completionRate = (actualSuccessCompletions.toFloat() / totalPossibleEvents.toFloat()) * 100f
        val highestStreak = currentHabits.maxOfOrNull { it.streakCount } ?: 0
        val streakFactor = (highestStreak * 10f).coerceAtMost(100f)

        return (completionRate * 0.7f + streakFactor * 0.3f).toInt().coerceIn(0, 100)
    }

    fun getTotalCompletionsCount(): Int {
        return completions.value.count { !it.isMissed }
    }

    fun getCompletionsTodayCount(): Int {
        val today = getTodayDateString()
        return completions.value.count { it.dateString == today && !it.isMissed }
    }

    fun getMissedTodayCount(): Int {
        val today = getTodayDateString()
        return completions.value.count { it.dateString == today && it.isMissed }
    }

    fun getLongestStreak(): Int {
        return habits.value.maxOfOrNull { it.longestStreak } ?: 0
    }

    fun getCurrentStreak(): Int {
        return habits.value.maxOfOrNull { it.streakCount } ?: 0
    }

    // --- State Filters & Controls ---

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleStudentMode(enabled: Boolean) {
        _studentModeOnly.value = enabled
    }

    // --- AI accountable Coach Triggers ---

    fun refreshCoachAdvice() {
        viewModelScope.launch {
            _isCoachLoading.value = true
            val todayCompletions = completions.value.filter { it.dateString == getTodayDateString() }
            val completedIds = todayCompletions.filter { !it.isMissed }.map { it.habitId }
            
            // Collect missed names
            val missedNames = habits.value
                .filter { it.id !in completedIds }
                .map { it.name }

            val score = getConsistencyScore()
            val currentMaxStreak = getCurrentStreak()
            val longestMaxStreak = getLongestStreak()

            val suggestion = GeminiClient.generateCoachSuggestion(
                missedHabits = missedNames,
                consistencyScore = score,
                currentStreak = currentMaxStreak,
                longestStreak = longestMaxStreak
            )
            _coachAdvice.value = suggestion
            _isCoachLoading.value = false
        }
    }

    // --- Recovery advice logic for specific broken streaks ---

    fun getStreakRecoveryHabit(): Habit? {
        // Find a habit whose yesterday was not completed (which means streak is 0), but longestStreak > 3 (was building momentum)
        val habitsList = habits.value
        val completionsList = completions.value

        for (h in habitsList) {
            val completedYesterday = completionsList.any { it.habitId == h.id && it.dateString == getYesterdayDateString() && !it.isMissed }
            if (!completedYesterday && h.streakCount == 0 && h.longestStreak >= 3) {
                return h
            }
        }
        return null
    }

    // --- Achievements Calculation ---

    fun getAchievements(): List<Achievement> {
        val totalCompletions = getTotalCompletionsCount()
        val maxStreak = getLongestStreak()
        val score = getConsistencyScore()
        val activeHabitCount = habits.value.size

        return listOf(
            Achievement(
                title = "Launch Day",
                description = "Complete your first ever habit.",
                isUnlocked = totalCompletions >= 1,
                icon = "🔥"
            ),
            Achievement(
                title = "Consistent Student",
                description = "Maintain a 7-day streak on any target.",
                isUnlocked = maxStreak >= 7,
                icon = "⚡"
            ),
            Achievement(
                title = "Iron Mind",
                description = "Build a formidable 30-day streak.",
                isUnlocked = maxStreak >= 30,
                icon = "🛡️"
            ),
            Achievement(
                title = "Centurion Elite",
                description = "Log 100 total habit successful sessions.",
                isUnlocked = totalCompletions >= 100,
                icon = "👑"
            ),
            Achievement(
                title = "Consistency Master",
                description = "Secure consistency of 90+ across >=3 habits.",
                isUnlocked = score >= 90 && activeHabitCount >= 3,
                icon = "💎"
            )
        )
    }

    data class Achievement(
        val title: String,
        val description: String,
        val isUnlocked: Boolean,
        val icon: String
    )
}
