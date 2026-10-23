package com.saif.pymaster.data

import android.content.Context
import com.saif.pymaster.notify.Notifier
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.random.Random

/** Pure rules that decide what is unlocked and how far the learner is. */
object Rules {
    const val XP_LESSON = 10
    const val XP_MODULE = 50
    const val XP_DAILY_CORRECT = 5
    const val XP_PROJECT = 100

    fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    fun passMark(m: Module) = ceil(m.quiz.size * 0.6).toInt().coerceAtLeast(1)

    fun lessonDone(s: AppState, id: String) = id in s.progress.completedLessons
    fun lessonsDone(s: AppState, m: Module) = m.lessons.count { lessonDone(s, it.id) }
    fun allLessonsDone(s: AppState, m: Module) = m.lessons.all { lessonDone(s, it.id) }
    fun quizPassed(s: AppState, m: Module) = m.id in s.progress.passedQuizzes
    fun moduleComplete(s: AppState, m: Module) = allLessonsDone(s, m) && quizPassed(s, m)

    fun moduleUnlocked(s: AppState, index: Int): Boolean {
        if (s.settings.devUnlocked || index <= 0) return true
        val prev = Content.modules.getOrNull(index - 1) ?: return true
        return moduleComplete(s, prev)
    }

    fun lessonUnlocked(s: AppState, m: Module, lessonIndex: Int): Boolean {
        if (s.settings.devUnlocked) return true
        if (!moduleUnlocked(s, m.number - 1)) return false
        if (lessonIndex == 0) return true
        return lessonDone(s, m.lessons[lessonIndex - 1].id)
    }

    fun quizUnlocked(s: AppState, m: Module) = s.settings.devUnlocked || allLessonsDone(s, m)

    /** 0..1 progress inside one module (lessons + quiz). */
    fun moduleFraction(s: AppState, m: Module): Float {
        val total = m.lessons.size + 1
        val done = lessonsDone(s, m) + if (quizPassed(s, m)) 1 else 0
        return done.toFloat() / total
    }

    fun courseComplete(s: AppState) = Content.modules.isNotEmpty() && Content.modules.all { moduleComplete(s, it) }

    fun courseFraction(s: AppState): Float {
        val mods = Content.modules
        if (mods.isEmpty()) return 0f
        val total = mods.sumOf { it.lessons.size + 1 }
        val done = mods.sumOf { lessonsDone(s, it) + if (quizPassed(s, it)) 1 else 0 }
        return done.toFloat() / total
    }

    fun projectsUnlocked(s: AppState) = s.settings.devUnlocked || courseComplete(s)

    fun projectUnlocked(s: AppState, index: Int): Boolean {
        if (s.settings.devUnlocked) return true
        if (!courseComplete(s)) return false
        if (index == 0) return true
        val prev = Content.projects.getOrNull(index - 1) ?: return true
        return prev.id in s.progress.completedProjects
    }

    fun projectFraction(s: AppState): Float {
        val p = Content.projects
        if (p.isEmpty()) return 0f
        return p.count { it.id in s.progress.completedProjects }.toFloat() / p.size
    }

    fun allProjectsDone(s: AppState) = Content.projects.isNotEmpty() &&
        Content.projects.all { it.id in s.progress.completedProjects }

    fun certificateUnlocked(s: AppState) = s.progress.certificateId != null || allProjectsDone(s)

    /** Course is 70% of the journey, projects 30%. */
    fun overallFraction(s: AppState) = courseFraction(s) * 0.7f + projectFraction(s) * 0.3f

    sealed interface Next {
        data class LessonNext(val module: Module, val lesson: Lesson) : Next
        data class QuizNext(val module: Module) : Next
        data class ProjectNext(val project: Project) : Next
        data object CertificateNext : Next
    }

    fun next(s: AppState): Next {
        Content.modules.forEachIndexed { mi, m ->
            if (!moduleUnlocked(s, mi)) return@forEachIndexed
            m.lessons.forEach { l -> if (!lessonDone(s, l.id)) return Next.LessonNext(m, l) }
            if (!quizPassed(s, m)) return Next.QuizNext(m)
        }
        Content.projects.forEach { p ->
            if (p.id !in s.progress.completedProjects) return Next.ProjectNext(p)
        }
        return Next.CertificateNext
    }

    fun dailyQuestions(day: Long): List<QuizQuestion> {
        val bank = Content.quizBank
        if (bank.isEmpty()) return emptyList()
        val count = if (day % 2L == 0L) 4 else 3
        return bank.shuffled(Random(day * 7919L + 17L)).take(count)
    }

    fun studySecondsToday(s: AppState) = s.progress.studySeconds[today().toString()] ?: 0L

    /** Seconds per day for the last 7 days, oldest first. */
    fun lastWeek(s: AppState): List<Pair<String, Long>> {
        val today = today()
        val cal = Calendar.getInstance()
        return (6 downTo 0).map { back ->
            val day = today - back
            cal.timeInMillis = day * 86_400_000L
            val label = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH) ?: ""
            label to (s.progress.studySeconds[day.toString()] ?: 0L)
        }
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m"
            else -> "${seconds}s"
        }
    }

    fun touchStreak(s: AppState): AppState {
        val today = today()
        val p = s.progress
        if (p.lastActiveDay == today) return s
        val streak = if (p.lastActiveDay == today - 1) p.streak + 1 else 1
        return s.copy(progress = p.copy(streak = streak, bestStreak = maxOf(p.bestStreak, streak), lastActiveDay = today))
    }
}

/** State-changing actions. Each one saves to the phone and raises notifications. */
class Learning(private val context: Context) {
    private val repo = Repository.get(context)

    private class Pending {
        val items = mutableListOf<Triple<String, String, String>>()
        fun add(state: AppState, title: String, body: String, type: String): AppState {
            items += Triple(title, body, type)
            return Notifier.add(state, title, body, type)
        }
    }

    private suspend fun mutate(block: (AppState, Pending) -> AppState): AppState {
        val pending = Pending()
        val newState = repo.update { block(it, pending) }
        pending.items.forEach { (t, b, type) -> Notifier.show(context, newState, t, b, type) }
        return newState
    }

    suspend fun addStudySeconds(seconds: Long) {
        repo.update { s ->
            val key = Rules.today().toString()
            val p = s.progress
            val map = p.studySeconds.toMutableMap()
            map[key] = (map[key] ?: 0L) + seconds
            // keep roughly the last 60 days
            val trimmed = if (map.size > 60) map.entries.sortedByDescending { it.key.toLong() }.take(60).associate { it.toPair() } else map
            Rules.touchStreak(s.copy(progress = p.copy(studySeconds = trimmed, totalStudySeconds = p.totalStudySeconds + seconds)))
        }
    }

    suspend fun openLesson(lessonId: String) {
        repo.update { s -> s.copy(progress = s.progress.copy(lastLessonId = lessonId)) }
    }

    suspend fun completeLesson(lessonId: String) = mutate { s0, pending ->
        if (Rules.lessonDone(s0, lessonId)) return@mutate s0
        val lesson = Content.lesson(lessonId) ?: return@mutate s0
        val module = Content.moduleOfLesson(lessonId)
        var s = Rules.touchStreak(s0)
        s = s.copy(
            progress = s.progress.copy(
                completedLessons = s.progress.completedLessons + lessonId,
                xp = s.progress.xp + Rules.XP_LESSON,
                lastLessonId = lessonId,
            )
        )
        s = pending.add(s, "Session complete ✅", "You finished \"${lesson.title}\". +${Rules.XP_LESSON} XP", NotificationType.LESSON)
        if (module != null && Rules.allLessonsDone(s, module) && !Rules.quizPassed(s, module)) {
            s = pending.add(
                s, "Module quiz unlocked 🧠",
                "All lessons of \"${module.title}\" are done. Pass the quiz to finish the module.",
                NotificationType.QUIZ_UNLOCK
            )
        }
        s
    }

    /** Returns true when the quiz was passed. */
    suspend fun submitModuleQuiz(moduleId: String, correct: Int): Boolean {
        val module = Content.module(moduleId) ?: return false
        val passed = correct >= Rules.passMark(module)
        mutate { s0, pending ->
            var s = Rules.touchStreak(s0)
            val p = s.progress
            val best = maxOf(p.quizScores[moduleId] ?: 0, correct)
            val firstPass = passed && moduleId !in p.passedQuizzes
            s = s.copy(
                progress = p.copy(
                    quizScores = p.quizScores + (moduleId to best),
                    passedQuizzes = if (passed) p.passedQuizzes + moduleId else p.passedQuizzes,
                    xp = p.xp + if (firstPass) Rules.XP_MODULE else 0,
                )
            )
            if (firstPass) {
                s = pending.add(
                    s, "Module ${module.number} complete 🎉",
                    "You mastered \"${module.title}\" with $correct/${module.quiz.size}. +${Rules.XP_MODULE} XP",
                    NotificationType.MODULE
                )
                if (Rules.courseComplete(s) && s.progress.courseCompletedAt == 0L) {
                    s = s.copy(progress = s.progress.copy(courseCompletedAt = System.currentTimeMillis()))
                    s = pending.add(
                        s, "Course completed! 🏆",
                        "You finished the full Python course. 15 real-world projects are now unlocked.",
                        NotificationType.COURSE
                    )
                }
            }
            s
        }
        return passed
    }

    suspend fun setProjectStep(projectId: String, step: Int, done: Boolean) {
        repo.update { s ->
            val p = s.progress
            val current = p.projectSteps[projectId] ?: emptySet()
            val next = if (done) current + step else current - step
            s.copy(progress = p.copy(projectSteps = p.projectSteps + (projectId to next)))
        }
    }

    suspend fun completeProject(projectId: String) = mutate { s0, pending ->
        val project = Content.project(projectId) ?: return@mutate s0
        if (projectId in s0.progress.completedProjects) return@mutate s0
        var s = Rules.touchStreak(s0)
        s = s.copy(
            progress = s.progress.copy(
                completedProjects = s.progress.completedProjects + projectId,
                xp = s.progress.xp + Rules.XP_PROJECT,
            )
        )
        s = pending.add(
            s, "Project complete 🚀",
            "\"${project.title}\" is done (${s.progress.completedProjects.size}/${Content.projects.size}). +${Rules.XP_PROJECT} XP",
            NotificationType.PROJECT
        )
        if (Rules.allProjectsDone(s) && s.progress.certificateId == null) {
            s = issueCertificate(s)
            s = pending.add(
                s, "Certificate earned 🎓",
                "Congratulations ${s.profile?.name ?: ""}! Your PyMaster Python Developer certificate is ready.",
                NotificationType.CERTIFICATE
            )
        }
        s
    }

    suspend fun submitDailyQuiz(answers: List<Int>, correct: Int) = mutate { s0, pending ->
        val today = Rules.today()
        if (s0.progress.dailyQuiz.day == today && s0.progress.dailyQuiz.completed) return@mutate s0
        var s = Rules.touchStreak(s0)
        val gained = correct * Rules.XP_DAILY_CORRECT
        s = s.copy(
            progress = s.progress.copy(
                dailyQuiz = DailyQuizState(day = today, answers = answers, score = correct, completed = true),
                dailyQuizzesDone = s.progress.dailyQuizzesDone + 1,
                xp = s.progress.xp + gained,
            )
        )
        pending.add(s, "Daily quiz finished ⚡", "You scored $correct/${answers.size} today. +$gained XP", NotificationType.INFO)
    }

    /** Called on app start and by the background worker: announces today's quiz once per day. */
    suspend fun announceDailyQuiz() = mutate { s, pending ->
        val today = Rules.today()
        if (s.profile == null || s.progress.lastDailyNotifiedDay == today) return@mutate s
        val count = Rules.dailyQuestions(today).size
        val updated = s.copy(progress = s.progress.copy(lastDailyNotifiedDay = today))
        pending.add(
            updated, "New daily quiz is ready ⚡",
            "$count fresh Python questions are waiting for you. Keep your streak alive!",
            NotificationType.QUIZ
        )
    }

    suspend fun welcome(name: String) = mutate { s, pending ->
        pending.add(
            s, "Welcome to PyMaster, $name 👋",
            "Your profile is saved on this phone. Start Module 1 to begin your Python journey.",
            NotificationType.INFO
        )
    }

    suspend fun recordRun() {
        repo.update { s -> Rules.touchStreak(s.copy(progress = s.progress.copy(codeRuns = s.progress.codeRuns + 1))) }
    }

    suspend fun unlockCertificatePreview() {
        repo.update { s -> if (s.progress.certificateId == null) issueCertificate(s) else s }
    }

    private fun issueCertificate(s: AppState): AppState {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val code = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return s.copy(
            progress = s.progress.copy(
                certificateId = "PYM-$year-$code",
                certificateIssuedAt = System.currentTimeMillis(),
            )
        )
    }
}
