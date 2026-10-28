package com.lexora.english.data

import android.content.Context
import com.lexora.english.content.Course
import com.lexora.english.content.DailyQuiz
import com.lexora.english.content.Projects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random

/**
 * Single source of truth for everything the app stores on the phone. The whole state is a small
 * JSON document in internal storage; writes are batched on a background thread.
 */
class AppRepository private constructor(private val context: Context) {

    private val file = File(context.filesDir, "lexora_state.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveSignal = Channel<Unit>(Channel.CONFLATED)
    private val writeLock = Any()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()
    val current: AppState get() = _state.value

    init {
        scope.launch {
            for (signal in saveSignal) {
                writeNow()
                delay(250)
            }
        }
    }

    private fun load(): AppState = try {
        if (file.exists()) json.decodeFromString(AppState.serializer(), file.readText()) else AppState()
    } catch (e: Exception) {
        AppState()
    }

    private fun writeNow() = synchronized(writeLock) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(AppState.serializer(), _state.value))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }

    /** Persists immediately; used by background workers before the process may die. */
    fun flush() = writeNow()

    fun update(transform: (AppState) -> AppState) {
        _state.update(transform)
        saveSignal.trySend(Unit)
    }

    private fun updateProgress(transform: (Progress) -> Progress) =
        update { it.copy(progress = transform(it.progress)) }

    // ---------------------------------------------------------------- profile & settings

    fun completeOnboarding() = update { it.copy(onboardingDone = true) }

    fun saveProfile(profile: UserProfile) {
        val firstTime = current.profile == null
        update { it.copy(profile = profile) }
        if (firstTime) {
            notify(
                "Welcome to Lexora, ${profile.name.substringBefore(' ')}!",
                "Your learning journey starts now. Begin with Alphabet & Phonics and take today's 5-question challenge.",
                "welcome",
                system = false,
            )
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) =
        update { it.copy(settings = transform(it.settings)) }

    fun setTutorialDone(done: Boolean) = update { it.copy(tutorialDone = done) }

    fun setNotificationPermissionAsked() = update { it.copy(notificationPermissionAsked = true) }

    fun resetProgress() {
        update { it.copy(progress = Progress(), notifications = emptyList()) }
    }

    // ---------------------------------------------------------------- study time & streak

    fun addStudyTime(seconds: Long) {
        if (seconds <= 0) return
        val today = today()
        updateProgress { p ->
            val map = p.studySeconds.toMutableMap()
            map[today] = (map[today] ?: 0L) + seconds
            // Keep the last 120 days only.
            val trimmed = if (map.size > 120) map.toSortedMap().entries.toList().takeLast(120).associate { it.key to it.value } else map
            p.copy(studySeconds = trimmed)
        }
        markActive()
    }

    private fun markActive() {
        val today = LocalDate.now()
        val p = current.progress
        if (p.lastActiveDay == today.toString()) return
        val newStreak = if (p.lastActiveDay == today.minusDays(1).toString()) p.streak + 1 else 1
        updateProgress { it.copy(streak = newStreak, bestStreak = maxOf(it.bestStreak, newStreak), lastActiveDay = today.toString()) }
        if (newStreak in setOf(3, 7, 14, 30, 50, 100)) {
            notify("$newStreak-day streak! 🔥", "You have learned $newStreak days in a row. Keep the fire burning!", "streak")
        }
    }

    /** Streak shown to the user: it is broken if the last active day is older than yesterday. */
    fun displayStreak(p: Progress): Int {
        val today = LocalDate.now()
        return if (p.lastActiveDay == today.toString() || p.lastActiveDay == today.minusDays(1).toString()) p.streak else 0
    }

    // ---------------------------------------------------------------- course

    fun completeLesson(lessonId: String) {
        val p = current.progress
        if (lessonId in p.completedLessons) {
            updateProgress { it.copy(lastLessonId = lessonId) }
            return
        }
        val lesson = Course.lesson(lessonId) ?: return
        val module = Course.moduleOfLesson(lessonId) ?: return
        updateProgress { it.copy(completedLessons = it.completedLessons + lessonId, xp = it.xp + 20, lastLessonId = lessonId) }
        markActive()
        val done = module.lessons.count { it.id in current.progress.completedLessons }
        val body = if (done == module.lessons.size) {
            "All ${module.lessons.size} lessons of ${module.title} are done. Take the module quiz to unlock the next module."
        } else {
            "+20 XP · ${module.title}: $done of ${module.lessons.size} lessons complete."
        }
        notify("Lesson complete: ${lesson.title}", body, "lesson")
    }

    /** Returns true when the attempt passed. */
    fun submitModuleQuiz(moduleId: String, correct: Int, total: Int): Boolean {
        val module = Course.module(moduleId) ?: return false
        val percent = if (total == 0) 0 else correct * 100 / total
        val passed = percent >= Course.PASS_PERCENT
        markActive()
        if (!passed) return false
        val firstPass = moduleId !in current.progress.passedModules
        updateProgress {
            val best = maxOf(it.passedModules[moduleId] ?: 0, percent)
            it.copy(passedModules = it.passedModules + (moduleId to best), xp = it.xp + if (firstPass) 50 + correct * 5 else 0)
        }
        if (firstPass) {
            val next = Course.modules.getOrNull(module.number)
            notify(
                "Module ${module.number} passed with $percent%",
                if (next != null) "Great work on ${module.title}! Module ${next.number}: ${next.title} is now unlocked." else "You finished the final module.",
                "module",
            )
            if (Course.isCourseComplete(current.progress) && current.progress.courseCompletedAt == null) {
                updateProgress { it.copy(courseCompletedAt = System.currentTimeMillis(), xp = it.xp + 300) }
                notify(
                    "Course complete! 🎓",
                    "You completed the full A to Z English course. 15 real-life projects are now unlocked. Finish them to earn your certificate.",
                    "course",
                )
            }
        }
        return true
    }

    // ---------------------------------------------------------------- projects

    fun completeProjectTask(projectId: String, task: String) {
        val project = Projects.byId(projectId) ?: return
        val tasks = current.progress.projectTasks[projectId].orEmpty()
        if (task in tasks) return
        val newTasks = tasks + task
        updateProgress { it.copy(projectTasks = it.projectTasks + (projectId to newTasks), xp = it.xp + 15) }
        markActive()
        if (newTasks.containsAll(Projects.TASKS) && projectId !in current.progress.completedProjects) {
            updateProgress { it.copy(completedProjects = it.completedProjects + (projectId to System.currentTimeMillis()), xp = it.xp + 100) }
            val doneCount = current.progress.completedProjects.size
            val next = Projects.all.getOrNull(project.number)
            notify(
                "Project ${project.number} complete: ${project.title}",
                if (next != null) "$doneCount of ${Projects.all.size} projects done. Next up: ${next.title}." else "All ${Projects.all.size} real-life projects are complete!",
                "project",
            )
            if (doneCount == Projects.all.size && current.progress.certificateIssuedAt == null) {
                issueCertificate()
            }
        }
    }

    private fun issueCertificate() {
        val id = "LX-" + LocalDate.now().year + "-" + (100000 + Random.nextInt(900000))
        updateProgress { it.copy(certificateIssuedAt = System.currentTimeMillis(), certificateId = id, xp = it.xp + 500) }
        notify(
            "Your certificate is ready! 🏆",
            "Congratulations ${current.profile?.name ?: ""}! You completed the course and all real-life projects. Open your professional certificate now.",
            "certificate",
        )
    }

    // ---------------------------------------------------------------- daily quiz

    /** Makes sure today's daily quiz exists; posts an in-app notice the first time a new day is seen. */
    fun ensureDailyQuiz(postSystemNotification: Boolean) {
        val today = today()
        val p = current.progress
        if (p.dailyQuizDate == today) return
        updateProgress { it.copy(dailyQuizDate = today, dailyAnswers = List(DailyQuiz.SIZE) { -1 }) }
        if (current.profile != null) {
            notify(
                "5 new daily questions are ready",
                "Today's challenge: ${DailyQuiz.topicLine(LocalDate.now())}. Keep your streak alive!",
                "daily",
                system = postSystemNotification,
            )
        }
    }

    fun answerDaily(index: Int, choice: Int, correct: Boolean) {
        ensureDailyQuiz(postSystemNotification = false)
        val p = current.progress
        if (p.dailyAnswers.getOrNull(index) != -1) return
        val answers = p.dailyAnswers.toMutableList().also { it[index] = choice }
        val finished = answers.none { it == -1 }
        updateProgress {
            it.copy(
                dailyAnswers = answers,
                xp = it.xp + if (correct) 10 else 2,
                dailyCorrectTotal = it.dailyCorrectTotal + if (correct) 1 else 0,
                dailyQuizzesCompleted = it.dailyQuizzesCompleted + if (finished) 1 else 0,
            )
        }
        markActive()
        if (finished) {
            val qs = DailyQuiz.questionsFor(LocalDate.now())
            val score = answers.indices.count { answers[it] == qs[it].answer }
            notify(
                "Daily challenge complete: $score/${DailyQuiz.SIZE}",
                if (score == DailyQuiz.SIZE) "Perfect score! Come back tomorrow for 5 new questions." else "Nice effort! New questions arrive tomorrow.",
                "daily",
            )
        }
    }

    // ---------------------------------------------------------------- practice

    fun recordPractice(mode: String, title: String, score: Int, total: Int) {
        val result = PracticeResult(mode, title, score, total, System.currentTimeMillis())
        updateProgress {
            it.copy(practiceHistory = (listOf(result) + it.practiceHistory).take(60), xp = it.xp + score * 3 + 5)
        }
        markActive()
        notify(
            "Practice session complete",
            "$title: $score/$total. +${score * 3 + 5} XP added to your profile.",
            "practice",
        )
    }

    fun setWordKnown(word: String, known: Boolean) = updateProgress {
        it.copy(knownWords = if (known) it.knownWords + word else it.knownWords - word)
    }

    // ---------------------------------------------------------------- notifications

    fun notify(title: String, body: String, type: String, system: Boolean = true) {
        val n = AppNotification(
            id = System.currentTimeMillis() * 10 + Random.nextInt(10),
            title = title,
            body = body,
            type = type,
            time = System.currentTimeMillis(),
        )
        update { it.copy(notifications = (listOf(n) + it.notifications).take(100)) }
        if (!system) return
        val s = current.settings
        val allowed = if (type == "daily") s.dailyQuizAlerts else s.achievementAlerts
        if (allowed) Notifier.show(context, n)
    }

    fun markNotificationRead(id: Long) = update { st ->
        st.copy(notifications = st.notifications.map { if (it.id == id) it.copy(read = true) else it })
    }

    fun markAllRead() = update { st -> st.copy(notifications = st.notifications.map { it.copy(read = true) }) }

    fun deleteNotification(id: Long) = update { st -> st.copy(notifications = st.notifications.filterNot { it.id == id }) }

    fun clearNotifications() = update { it.copy(notifications = emptyList()) }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun get(context: Context): AppRepository =
            instance ?: synchronized(this) {
                instance ?: AppRepository(context.applicationContext).also { instance = it }
            }

        fun today(): String = LocalDate.now().toString()

        fun formatMinutes(seconds: Long): String {
            val m = seconds / 60
            return if (m < 60) "${m}m" else String.format(Locale.US, "%dh %02dm", m / 60, m % 60)
        }
    }
}
