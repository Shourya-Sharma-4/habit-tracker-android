package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * HabitCompletion entity representing a logged event of a habit.
 *
 * This tracking is crucial for:
 * 1. Mark habits as complete or missed.
 * 2. Save reasons for missing ("Why Did You Miss Today?") such as:
 *    "Procrastination", "Busy Schedule", "Lack of Motivation", "Forgot", "Sick", "Other".
 * 3. Never Miss Twice system tracking.
 */
@Entity(tableName = "habit_completions")
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val dateString: String, // format "YYYY-MM-DD" representing the calendar day
    val timestamp: Long = System.currentTimeMillis(),
    val isMissed: Boolean = false, // True if the user declared they skipped it with a reason
    val missReason: String? = null, // e.g., "Procrastination", "Busy Schedule", "Lack of Motivation", "Forgot", "Sick", "Other"
    val notes: String? = null
) : Serializable
