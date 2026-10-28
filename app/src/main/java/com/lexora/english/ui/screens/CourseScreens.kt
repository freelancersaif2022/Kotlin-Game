package com.lexora.english.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexora.english.content.Course
import com.lexora.english.content.Example
import com.lexora.english.content.Projects
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.StudyTimer
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.CircleIconButton
import com.lexora.english.ui.components.EmptyState
import com.lexora.english.ui.components.IconTile
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.components.Pill
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.QuizResult
import com.lexora.english.ui.components.QuizRunner
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.Speaker
import com.lexora.english.util.rememberHaptics

@Composable
fun ModuleScreen(moduleId: String, state: AppState, nav: Nav) {
    val c = Lex.colors
    val m = Course.module(moduleId)
    if (m == null) {
        Column(Modifier.fillMaxSize().background(c.background)) { ScreenHeader("Module", { nav.back() }) }
        return
    }
    val p = state.progress
    val unlocked = Course.isModuleUnlocked(m, p)
    val passed = m.id in p.passedModules
    val quizUnlocked = Course.isQuizUnlocked(m, p)

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader("Module ${m.number}", { nav.back() }, subtitle = m.title)
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Box(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .aspectRatio(1.78f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.primarySoft)
                ) {
                    AppImage(m.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = m.title)
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(m.title, style = MaterialTheme.typography.headlineMedium, color = c.text)
                    Text(m.subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("${m.lessons.size} lessons", icon = Icons.Rounded.PlayArrow)
                        Pill("${m.lessons.sumOf { it.minutes }} min", icon = Icons.Rounded.Schedule)
                        Pill("${m.quiz.size}-question quiz", icon = Icons.Rounded.Quiz)
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgress(Course.moduleProgress(m, p), color = if (passed) c.success else c.primary)
                    if (!unlocked) {
                        Spacer(Modifier.height(14.dp))
                        LexCard(color = c.accentSoft, elevated = false) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, null, tint = Color(0xFF8A5A00))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Pass the Module ${m.number - 1} quiz to unlock this module.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.text,
                                )
                            }
                        }
                    }
                }
            }
            itemsIndexed(m.lessons, key = { _, l -> l.id }) { i, lesson ->
                val done = lesson.id in p.completedLessons
                val open = Course.isLessonUnlocked(m, i, p)
                LexCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    onClick = if (open) ({ nav.lesson(lesson.id) }) else null,
                    padding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        done -> c.success
                                        open -> c.primary
                                        else -> c.track
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                done -> Icon(Icons.Rounded.Check, "Done", tint = Color.White)
                                open -> Icon(Icons.Rounded.PlayArrow, "Start", tint = Color.White)
                                else -> Icon(Icons.Rounded.Lock, "Locked", tint = c.textMuted, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Lesson ${i + 1} · ${lesson.minutes} min", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                            Text(lesson.title, style = MaterialTheme.typography.titleSmall, color = if (open) c.text else c.textMuted)
                            Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item {
                val best = p.passedModules[m.id]
                LexCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    color = if (quizUnlocked) c.primarySoft else c.card,
                    onClick = if (quizUnlocked) ({ nav.moduleQuiz(m.id) }) else null,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Quiz, if (quizUnlocked) Brand.Indigo else c.textMuted)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Module quiz", style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text(
                                when {
                                    best != null -> "Passed · best score $best%"
                                    quizUnlocked -> "Score ${Course.PASS_PERCENT}% or more to pass"
                                    else -> "Finish all lessons to unlock"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted,
                            )
                        }
                        if (best != null) Icon(Icons.Rounded.CheckCircle, null, tint = c.success)
                        else if (!quizUnlocked) Icon(Icons.Rounded.Lock, null, tint = c.textMuted)
                    }
                    if (quizUnlocked) {
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton(if (best != null) "Retake quiz" else "Start quiz", { nav.moduleQuiz(m.id) }, Modifier.fillMaxWidth(), height = 46.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun LessonScreen(lessonId: String, state: AppState, nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val haptics = rememberHaptics()
    val lesson = Course.lesson(lessonId)
    val module = Course.moduleOfLesson(lessonId)
    if (lesson == null || module == null) {
        Column(Modifier.fillMaxSize().background(c.background)) { ScreenHeader("Lesson", { nav.back() }) }
        return
    }
    StudyTimer()
    val index = module.lessons.indexOf(lesson)
    val nextLesson = module.lessons.getOrNull(index + 1)
    val done = lesson.id in state.progress.completedLessons

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader(
            "Lesson ${index + 1} of ${module.lessons.size}",
            { nav.back() },
            subtitle = "Module ${module.number} · ${module.title}",
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .aspectRatio(1.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.primarySoft)
                ) {
                    AppImage(module.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(lesson.title, style = MaterialTheme.typography.headlineMedium, color = c.text)
                    Spacer(Modifier.height(4.dp))
                    Text(lesson.summary, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("${lesson.minutes} min read", icon = Icons.Rounded.Schedule)
                        Pill("${lesson.examples.size} examples", icon = Icons.Rounded.Translate)
                        if (done) Pill("Completed", color = c.success, background = c.successSoft, icon = Icons.Rounded.Check)
                    }
                }
            }
            lesson.sections.forEachIndexed { si, section ->
                item(key = "s$si") {
                    LexCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c.primarySoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${si + 1}", style = MaterialTheme.typography.labelLarge, color = c.primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(section.heading, style = MaterialTheme.typography.titleMedium, color = c.text)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(section.body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                        if (section.points.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            section.points.forEach { pt ->
                                Row(Modifier.padding(vertical = 4.dp)) {
                                    Box(
                                        Modifier
                                            .padding(top = 9.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(c.primary)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(pt, style = MaterialTheme.typography.bodyMedium, color = c.text)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Examples",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.text,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                )
                Text(
                    "Tap the speaker to hear each sentence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            lesson.examples.forEachIndexed { ei, example ->
                item(key = "e$ei") { ExampleCard(example) }
            }
            item {
                Row(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.accentSoft)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFB7791F))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Pro tip", style = MaterialTheme.typography.titleSmall, color = c.text)
                        Spacer(Modifier.height(2.dp))
                        Text(lesson.tip, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    }
                }
            }
        }
        PrimaryButton(
            text = when {
                nextLesson != null -> if (done) "Next lesson" else "Complete & continue"
                else -> if (done) "Go to module quiz" else "Complete lesson"
            },
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            onClick = {
                repo.completeLesson(lesson.id)
                haptics.success()
                nav.back()
                if (nextLesson != null) nav.lesson(nextLesson.id) else nav.moduleQuiz(module.id)
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ExampleCard(example: Example) {
    val c = Lex.colors
    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(example.emoji, fontSize = 30.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(example.sentence, style = MaterialTheme.typography.titleSmall, color = c.text)
                Spacer(Modifier.height(2.dp))
                Text(example.note, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            Spacer(Modifier.width(8.dp))
            CircleIconButton(
                Icons.AutoMirrored.Rounded.VolumeUp, "Listen", { Speaker.speak(example.sentence) },
                tint = c.primary, background = c.primarySoft, size = 40.dp,
            )
        }
    }
}

@Composable
fun ModuleQuizScreen(moduleId: String, nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val m = Course.module(moduleId)
    if (m == null) {
        Column(Modifier.fillMaxSize().background(c.background)) { ScreenHeader("Quiz", { nav.back() }) }
        return
    }
    StudyTimer()
    var attempt by rememberSaveable { mutableStateOf(0) }
    var result by rememberSaveable { mutableStateOf<IntArray?>(null) }
    val questions = remember(attempt) { m.quiz.map { q -> q }.shuffled() }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader("Module ${m.number} quiz", { nav.back() }, subtitle = m.title)
        val r = result
        if (r == null) {
            QuizRunner(questions, Modifier.weight(1f)) { correct, total, _ ->
                repo.submitModuleQuiz(m.id, correct, total)
                result = intArrayOf(correct, total)
            }
        } else {
            val passed = r[1] > 0 && r[0] * 100 / r[1] >= Course.PASS_PERCENT
            val next = Course.modules.getOrNull(m.number)
            val courseDone = Course.isCourseComplete(repo.current.progress)
            QuizResult(
                correct = r[0], total = r[1], passed = passed,
                title = when {
                    passed && courseDone && next == null -> "Course complete! 🎓"
                    passed -> "Module passed!"
                    else -> "Almost there"
                },
                message = when {
                    passed && next == null -> "You finished all 12 modules. The 15 real-life projects are now unlocked."
                    passed -> "Great job! Module ${next?.number}: ${next?.title} is now unlocked."
                    else -> "You need ${Course.PASS_PERCENT}% to pass. Review the lessons and try again — you've got this!"
                },
                primaryText = when {
                    passed && next != null -> "Start Module ${next.number}"
                    passed -> "View projects"
                    else -> "Try again"
                },
                onPrimary = {
                    when {
                        passed && next != null -> { nav.back(); nav.module(next.id) }
                        passed -> { nav.back(); nav.project(Projects.all.first().id) }
                        else -> { attempt++; result = null }
                    }
                },
                secondaryText = if (passed) "Back to module" else "Review lessons",
                onSecondary = { nav.back() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun NotFoundState(title: String, nav: Nav) {
    val c = Lex.colors
    Column(Modifier.fillMaxSize().background(c.background)) {
        ScreenHeader(title, { nav.back() })
        EmptyState(com.lexora.english.R.drawable.empty_progress, "Not available", "This content could not be found.")
    }
}
