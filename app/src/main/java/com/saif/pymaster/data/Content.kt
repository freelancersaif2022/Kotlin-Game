package com.saif.pymaster.data

import android.content.Context
import com.saif.pymaster.R

/** Read-only learning content bundled in assets/. */
object Content {
    @Volatile
    private var course: CourseContent? = null

    @Volatile
    private var projectContent: ProjectContent? = null

    @Volatile
    private var bank: QuizBank? = null

    fun init(context: Context) {
        if (course != null) return
        synchronized(this) {
            if (course != null) return
            val assets = context.applicationContext.assets
            fun read(name: String) = assets.open(name).bufferedReader().use { it.readText() }
            bank = Repository.json.decodeFromString(QuizBank.serializer(), read("quiz_bank.json"))
            projectContent = Repository.json.decodeFromString(ProjectContent.serializer(), read("projects.json"))
            course = Repository.json.decodeFromString(CourseContent.serializer(), read("course.json"))
        }
    }

    val modules: List<Module> get() = course?.modules ?: emptyList()
    val projects: List<Project> get() = projectContent?.projects ?: emptyList()
    val quizBank: List<QuizQuestion> get() = bank?.questions ?: emptyList()

    val allLessons: List<Lesson> get() = modules.flatMap { it.lessons }
    val totalLessons: Int get() = allLessons.size

    fun module(id: String) = modules.firstOrNull { it.id == id }
    fun project(id: String) = projects.firstOrNull { it.id == id }
    fun moduleOfLesson(lessonId: String) = modules.firstOrNull { m -> m.lessons.any { it.id == lessonId } }
    fun lesson(id: String) = allLessons.firstOrNull { it.id == id }

    fun imageRes(name: String): Int = when (name) {
        "module_01_basics" -> R.drawable.module_01_basics
        "module_02_variables" -> R.drawable.module_02_variables
        "module_03_operators" -> R.drawable.module_03_operators
        "module_04_conditions" -> R.drawable.module_04_conditions
        "module_05_loops" -> R.drawable.module_05_loops
        "module_06_functions" -> R.drawable.module_06_functions
        "module_07_collections" -> R.drawable.module_07_collections
        "module_08_strings" -> R.drawable.module_08_strings
        "module_09_files" -> R.drawable.module_09_files
        "module_10_errors" -> R.drawable.module_10_errors
        "module_11_oop" -> R.drawable.module_11_oop
        "module_12_advanced" -> R.drawable.module_12_advanced
        "project_01_calculator" -> R.drawable.project_01_calculator
        "project_02_todo" -> R.drawable.project_02_todo
        "project_03_quiz_game" -> R.drawable.project_03_quiz_game
        "project_04_password_generator" -> R.drawable.project_04_password_generator
        "project_05_weather" -> R.drawable.project_05_weather
        "project_06_expense_tracker" -> R.drawable.project_06_expense_tracker
        "project_07_contact_book" -> R.drawable.project_07_contact_book
        "project_08_number_guess" -> R.drawable.project_08_number_guess
        "project_09_file_organizer" -> R.drawable.project_09_file_organizer
        "project_10_web_scraper" -> R.drawable.project_10_web_scraper
        "project_11_chatbot" -> R.drawable.project_11_chatbot
        "project_12_library_system" -> R.drawable.project_12_library_system
        "project_13_bank_system" -> R.drawable.project_13_bank_system
        "project_14_student_grades" -> R.drawable.project_14_student_grades
        "project_15_snake_game" -> R.drawable.project_15_snake_game
        else -> R.drawable.logo
    }

    val avatars = listOf(
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
    )
}
