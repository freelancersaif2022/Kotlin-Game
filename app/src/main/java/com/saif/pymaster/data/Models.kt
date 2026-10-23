package com.saif.pymaster.data

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Persisted user state (stored as JSON in DataStore, on the device only)
// ---------------------------------------------------------------------------

@Serializable
data class Profile(
    val name: String = "",
    /** 0..4 = built-in avatar, -1 = custom photo at [customAvatarPath]. */
    val avatarIndex: Int = 0,
    val customAvatarPath: String? = null,
    val level: String = "Beginner",
    val goal: String = "Get a developer job",
    val dailyGoalMinutes: Int = 20,
    val createdAt: Long = 0L,
)

@Serializable
data class Settings(
    val theme: String = "navy",
    val fontScale: Float = 1f,
    val terminalFontSize: Int = 14,
    val vibration: Boolean = true,
    val notifications: Boolean = true,
    val dailyQuizReminder: Boolean = true,
    val autoIndent: Boolean = true,
    val devUnlocked: Boolean = false,
)

@Serializable
data class DailyQuizState(
    val day: Long = -1,
    val answers: List<Int> = emptyList(),
    val score: Int = 0,
    val completed: Boolean = false,
)

@Serializable
data class Progress(
    val completedLessons: Set<String> = emptySet(),
    /** module id -> best quiz score (number of correct answers). */
    val quizScores: Map<String, Int> = emptyMap(),
    val passedQuizzes: Set<String> = emptySet(),
    val completedProjects: Set<String> = emptySet(),
    /** project id -> indexes of finished steps. */
    val projectSteps: Map<String, Set<Int>> = emptyMap(),
    val xp: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = -1,
    /** epoch day (as string) -> seconds studied that day. */
    val studySeconds: Map<String, Long> = emptyMap(),
    val totalStudySeconds: Long = 0,
    val dailyQuiz: DailyQuizState = DailyQuizState(),
    val dailyQuizzesDone: Int = 0,
    val lastDailyNotifiedDay: Long = -1,
    val lastLessonId: String? = null,
    val codeRuns: Int = 0,
    val courseCompletedAt: Long = 0,
    val certificateId: String? = null,
    val certificateIssuedAt: Long = 0,
)

@Serializable
data class AppNotification(
    val id: Long,
    val title: String,
    val body: String,
    val type: String,
    val time: Long,
    val read: Boolean = false,
)

@Serializable
data class AppState(
    val onboarded: Boolean = false,
    val profile: Profile? = null,
    val settings: Settings = Settings(),
    val progress: Progress = Progress(),
    val notifications: List<AppNotification> = emptyList(),
    val terminalCode: String = DEFAULT_TERMINAL_CODE,
)

const val DEFAULT_TERMINAL_CODE = """# Welcome to the PyMaster terminal
# Write Python code here and tap Run

name = input("What is your name? ")
print(f"Hello, {name}! Welcome to Python.")

for i in range(1, 4):
    print("Step", i, "complete")
"""

object NotificationType {
    const val LESSON = "lesson"
    const val MODULE = "module"
    const val COURSE = "course"
    const val PROJECT = "project"
    const val CERTIFICATE = "certificate"
    const val QUIZ = "quiz"
    const val QUIZ_UNLOCK = "quiz_unlock"
    const val INFO = "info"
}

// ---------------------------------------------------------------------------
// Static learning content (loaded from JSON files in assets/)
// ---------------------------------------------------------------------------

@Serializable
data class Block(
    /** "text", "code", "tip", "list" or "heading". */
    val type: String,
    val text: String = "",
    val code: String = "",
    val output: String = "",
    val items: List<String> = emptyList(),
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val minutes: Int = 6,
    val blocks: List<Block>,
    val keyPoints: List<String> = emptyList(),
    val practice: String = "",
)

@Serializable
data class QuizQuestion(
    val q: String,
    val code: String = "",
    val options: List<String>,
    val answer: Int,
    val explain: String = "",
)

@Serializable
data class Module(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    val image: String,
    val lessons: List<Lesson>,
    val quiz: List<QuizQuestion>,
)

@Serializable
data class ProjectStep(
    val title: String,
    val text: String,
    val code: String = "",
)

@Serializable
data class Project(
    val id: String,
    val number: Int,
    val title: String,
    val tagline: String,
    val image: String,
    val difficulty: String,
    val minutes: Int,
    val overview: String,
    val features: List<String>,
    val concepts: List<String>,
    val steps: List<ProjectStep>,
    val code: String,
    val sampleOutput: String = "",
)

@Serializable
data class CourseContent(
    val modules: List<Module>,
)

@Serializable
data class ProjectContent(
    val projects: List<Project>,
)

@Serializable
data class QuizBank(
    val questions: List<QuizQuestion>,
)
