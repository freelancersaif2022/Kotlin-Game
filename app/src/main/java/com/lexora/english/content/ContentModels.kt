package com.lexora.english.content

import androidx.annotation.DrawableRes

data class Question(
    val prompt: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String = "",
)

data class Example(
    val emoji: String,
    val sentence: String,
    val note: String,
)

data class LessonSection(
    val heading: String,
    val body: String,
    val points: List<String> = emptyList(),
)

data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val minutes: Int,
    val sections: List<LessonSection>,
    val examples: List<Example>,
    val tip: String,
)

data class Module(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    @DrawableRes val image: Int,
    val lessons: List<Lesson>,
    val quiz: List<Question>,
)

data class Phrase(val text: String, val meaning: String)

data class DialogueLine(val speaker: String, val line: String)

data class Project(
    val id: String,
    val number: Int,
    val title: String,
    val subtitle: String,
    @DrawableRes val image: Int,
    val overview: String,
    val goals: List<String>,
    val phrases: List<Phrase>,
    val dialogue: List<DialogueLine>,
    val vocabulary: List<Phrase>,
    val quiz: List<Question>,
    val writingTask: String,
    val writingKeywords: List<String>,
    val minWords: Int = 40,
)
