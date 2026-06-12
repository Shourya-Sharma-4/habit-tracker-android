package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Moshi Data Classes ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Gemini Client Singleton Singleton ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // In Android Gradle, BuildConfig fields are strings
    // API Key is injected via Secrets map and properties.
    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Calls Gemini 3.5 Flash to generate accountability coach suggestions.
     * Includes a robust fallback mechanism in case the key is invalid or absent.
     */
    suspend fun generateCoachSuggestion(
        missedHabits: List<String>,
        consistencyScore: Int,
        currentStreak: Int,
        longestStreak: Int
    ): String {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return generateLocalFallbackSuggestion(missedHabits, consistencyScore, currentStreak)
        }

        val prompt = StringBuilder()
        prompt.append("You are 'Socrates', an encouraging, direct, and slightly serious AI Accountability Coach for busy students or software developers.\n")
        prompt.append("Context:\n")
        prompt.append("- Consistency Score: $consistencyScore/100\n")
        prompt.append("- Current Streak: $currentStreak days\n")
        prompt.append("- Longest Streak: $longestStreak days\n")
        if (missedHabits.isNotEmpty()) {
            prompt.append("- Habits missed today/recently: ${missedHabits.joinToString(", ")}\n")
        } else {
            prompt.append("- All habits completed today! Outstanding!\n")
        }
        prompt.append("\nCreate a short, punchy (max 3 sentences), highly actionable advice tailored specifically to this performance. Be practical, empathetic but firm. Focus on starting small and the 'Never Miss Twice' principle.")

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt.toString())))
            ),
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = "You are Socrates, a professional, inspiring AI accountability coach for students and developers. Deliver compact, actionable advice under 3 sentences. Never use generic introductory phrases or placeholder texts.")))
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: generateLocalFallbackSuggestion(missedHabits, consistencyScore, currentStreak)
        } catch (e: Exception) {
            // Log or print the error
            e.printStackTrace()
            generateLocalFallbackSuggestion(missedHabits, consistencyScore, currentStreak)
        }
    }

    /**
     * High-quality fallback messages based on actual habit performance if API is unreachable.
     */
    private fun generateLocalFallbackSuggestion(
        missedHabits: List<String>,
        consistencyScore: Int,
        currentStreak: Int
    ): String {
        return when {
            missedHabits.isEmpty() && currentStreak > 0 -> {
                "Socrates says: You are building strong momentum with a $currentStreak-day streak! Keep protecting it by showing up tomorrow."
            }
            missedHabits.isNotEmpty() && consistencyScore > 75 -> {
                "Socrates says: You missed ${missedHabits.first()} today, but your general consistency is strong. Remember the 'Never Miss Twice' rule—make tomorrow a non-negotiable success."
            }
            missedHabits.isNotEmpty() -> {
                "Socrates says: Friction is part of the process. Break down ${missedHabits.first()} into a tiny 5-minute action tomorrow. Motivation follows action, not the other way around."
            }
            else -> {
                "Socrates says: Consistency is the quiet engine of self-improvement. Standardize before you optimize; execute small habits every single day."
            }
        }
    }
}
