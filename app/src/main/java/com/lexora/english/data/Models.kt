package com.lexora.english.data

import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val onboardingDone: Boolean = false,
    val profile: UserProfile? = null,
    val settings: AppSettings = AppSettings(),
    val progress: Progress = Progress(),
    val notifications: List<AppNotification> = emptyList(),
    val tutorialDone: Boolean = false,
    val notificationPermissionAsked: Boolean = false,
)

@Serializable
data class UserProfile(
    val name: String,
    /** 0..9 picks a built-in avatar, -1 means [photoPath] is used. */
    val avatarIndex: Int = 0,
    val photoPath: String? = null,
    val level: String = "Beginner",
    val goal: String = "Career",
    val dailyMinutes: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class AppSettings(
    /** "light", "dark" or "system". */
    val themeMode: String = "light",
    val fontScale: Float = 1f,
    val vibration: Boolean = true,
    val dailyQuizAlerts: Boolean = true,
    val achievementAlerts: Boolean = true,
    val speechRate: Float = 0.95f,
    val autoNextQuestion: Boolean = false,
)

@Serializable
data class Progress(
    val completedLessons: Set<String> = emptySet(),
    /** moduleId -> best quiz score in percent; present only once passed. */
    val passedModules: Map<String, Int> = emptyMap(),
    /** projectId -> set of finished task keys ("read", "quiz", "write"). */
    val projectTasks: Map<String, Set<String>> = emptyMap(),
    val completedProjects: Map<String, Long> = emptyMap(),
    val xp: Int = 0,
    /** ISO date -> seconds studied that day. */
    val studySeconds: Map<String, Long> = emptyMap(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: String = "",
    val dailyQuizDate: String = "",
    val dailyAnswers: List<Int> = emptyList(),
    val dailyQuizzesCompleted: Int = 0,
    val dailyCorrectTotal: Int = 0,
    val practiceHistory: List<PracticeResult> = emptyList(),
    val knownWords: Set<String> = emptySet(),
    val lastLessonId: String? = null,
    val courseCompletedAt: Long? = null,
    val certificateIssuedAt: Long? = null,
    val certificateId: String? = null,
)

@Serializable
data class PracticeResult(
    val mode: String,
    val title: String,
    val score: Int,
    val total: Int,
    val at: Long,
)

@Serializable
data class AppNotification(
    val id: Long,
    val title: String,
    val body: String,
    /** lesson, module, course, project, certificate, daily, practice, streak, welcome */
    val type: String,
    val time: Long,
    val read: Boolean = false,
)
