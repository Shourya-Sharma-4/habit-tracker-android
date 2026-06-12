package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Habit entity representing a user's trackable habit.
 *
 * This represents the design for DB tracking of our habits.
 * It is structured to support features like streaks, difficulty levels,
 * student-focused categorization, and custom user definitions.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g., "Coding", "Study", "Fitness", "Health", "Reading", "Personal Development"
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val createdTimestamp: Long = System.currentTimeMillis(),
    val streakCount: Int = 0,
    val longestStreak: Int = 0,
    val isStudentMode: Boolean = false,
    val isActive: Boolean = true
) : Serializable
