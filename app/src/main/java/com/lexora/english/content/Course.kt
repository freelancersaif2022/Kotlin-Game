package com.lexora.english.content

import com.lexora.english.data.Progress

internal fun q(prompt: String, options: List<String>, answer: Int, explanation: String) =
    Question(prompt, options, answer, explanation)

internal fun ex(emoji: String, sentence: String, note: String) = Example(emoji, sentence, note)

internal fun sec(heading: String, body: String, vararg points: String) =
    LessonSection(heading, body, points.toList())

object Course {
    const val PASS_PERCENT = 70

    val modules: List<Module> by lazy { CourseFoundations.modules + CourseAdvanced.modules }

    private val lessonIndex: Map<String, Pair<Module, Lesson>> by lazy {
        modules.flatMap { m -> m.lessons.map { it.id to (m to it) } }.toMap()
    }

    val totalLessons: Int get() = lessonIndex.size

    fun module(id: String): Module? = modules.firstOrNull { it.id == id }
    fun lesson(id: String): Lesson? = lessonIndex[id]?.second
    fun moduleOfLesson(id: String): Module? = lessonIndex[id]?.first

    fun isModuleUnlocked(module: Module, p: Progress): Boolean =
        module.number == 1 || modules[module.number - 2].id in p.passedModules

    fun isLessonUnlocked(module: Module, index: Int, p: Progress): Boolean {
        if (!isModuleUnlocked(module, p)) return false
        return index == 0 || module.lessons[index - 1].id in p.completedLessons
    }

    fun lessonsDone(module: Module, p: Progress): Int = module.lessons.count { it.id in p.completedLessons }

    fun isQuizUnlocked(module: Module, p: Progress): Boolean = lessonsDone(module, p) == module.lessons.size

    /** 0f..1f: lessons count as 80% of a module, passing its quiz as the remaining 20%. */
    fun moduleProgress(module: Module, p: Progress): Float {
        val lessons = lessonsDone(module, p).toFloat() / module.lessons.size
        return lessons * 0.8f + if (module.id in p.passedModules) 0.2f else 0f
    }

    fun courseProgress(p: Progress): Float = modules.map { moduleProgress(it, p) }.average().toFloat()

    fun isCourseComplete(p: Progress): Boolean = modules.all { it.id in p.passedModules }

    /** The next thing to study: first unfinished lesson, or the quiz of a module whose lessons are done. */
    fun nextStep(p: Progress): Pair<Module, Lesson?>? {
        for (m in modules) {
            if (m.id in p.passedModules) continue
            val lesson = m.lessons.firstOrNull { it.id !in p.completedLessons }
            return m to lesson
        }
        return null
    }

    fun allQuestions(): List<Question> = modules.flatMap { it.quiz }
}
