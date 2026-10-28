package com.lexora.english.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.content.Course
import com.lexora.english.content.Project
import com.lexora.english.content.Projects
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.StudyTimer
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.CircleIconButton
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.PillSelector
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.QuizResult
import com.lexora.english.ui.components.QuizRunner
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.components.SecondaryButton
import com.lexora.english.ui.components.WritingWorkspace
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.Speaker
import com.lexora.english.util.rememberHaptics

private val sections = listOf("Brief", "Dialogue", "Quiz", "Writing")

@Composable
fun ProjectScreen(projectId: String, state: AppState, nav: Nav) {
    val c = Lex.colors
    val pr = Projects.byId(projectId)
    if (pr == null) {
        NotFoundState("Project", nav)
        return
    }
    val p = state.progress
    if (!Projects.isUnlocked(pr, p)) {
        LockedProject(pr, state, nav)
        return
    }
    StudyTimer()
    val tasks = p.projectTasks[pr.id].orEmpty()
    val done = pr.id in p.completedProjects
    var section by rememberSaveable(pr.id) { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .imePadding()
    ) {
        ScreenHeader("Project ${pr.number} of ${Projects.all.size}", { nav.back() }, subtitle = pr.title)
        TaskStrip(tasks)
        PillSelector(sections.indices.toList(), section, { sections[it] }, { section = it }, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        when (section) {
            0 -> BriefSection(pr, done) { section = 1 }
            1 -> DialogueSection(pr, "read" in tasks) { section = 2 }
            2 -> QuizSection(pr, "quiz" in tasks) { section = 3 }
            else -> WritingSection(pr, state, nav)
        }
    }
}

@Composable
private fun TaskStrip(tasks: Set<String>) {
    val c = Lex.colors
    val items = listOf("read" to "Study dialogue", "quiz" to "Pass quiz", "write" to "Writing task")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (key, label) ->
            val ok = key in tasks
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ok) c.successSoft else c.cardAlt)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Flag,
                    null,
                    tint = if (ok) c.success else c.textMuted,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (ok) c.success else c.textSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ColumnScopeScroll(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) { content() }
}

@Composable
private fun BriefSection(pr: Project, done: Boolean, onNext: () -> Unit) {
    val c = Lex.colors
    ColumnScopeScroll {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.75f)
                .clip(RoundedCornerShape(24.dp))
                .background(c.primarySoft)
        ) {
            AppImage(pr.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = pr.title)
            if (done) {
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(c.success)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Completed", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(pr.title, style = MaterialTheme.typography.headlineMedium, color = c.text)
        Text(pr.subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        Spacer(Modifier.height(12.dp))
        Text(pr.overview, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)

        Spacer(Modifier.height(18.dp))
        Text("Your goals", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(8.dp))
        pr.goals.forEach { g ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Brand.Indigo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(g, style = MaterialTheme.typography.bodyMedium, color = c.text)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Key phrases", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(8.dp))
        pr.phrases.forEach { ph ->
            LexCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), padding = androidx.compose.foundation.layout.PaddingValues(12.dp), elevated = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ph.text, style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text(ph.meaning, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    CircleIconButton(Icons.AutoMirrored.Rounded.VolumeUp, "Listen", { Speaker.speak(ph.text.replace("…", "")) }, tint = c.primary, background = c.primarySoft, size = 38.dp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Vocabulary", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(8.dp))
        LexCard(Modifier.fillMaxWidth(), elevated = false) {
            pr.vocabulary.forEachIndexed { i, v ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                Row {
                    Text(v.text, style = MaterialTheme.typography.titleSmall, color = c.primary, modifier = Modifier.weight(0.42f))
                    Text(v.meaning, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(0.58f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Continue to dialogue", onNext, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DialogueSection(pr: Project, read: Boolean, onNext: () -> Unit) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val haptics = rememberHaptics()
    ColumnScopeScroll {
        Text("Real-life dialogue", style = MaterialTheme.typography.titleMedium, color = c.text)
        Text("Read it aloud, then tap the speaker to compare your pronunciation.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        Spacer(Modifier.height(12.dp))
        pr.dialogue.forEach { line ->
            val mine = line.speaker == "You"
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 300.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp, topEnd = 20.dp,
                                bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp,
                            )
                        )
                        .background(if (mine) c.primary else c.cardAlt)
                        .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
                ) {
                    Text(
                        line.speaker,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (mine) Color.White.copy(alpha = 0.8f) else Brand.Indigo,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            line.line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mine) Color.White else c.text,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.VolumeUp,
                            "Listen",
                            tint = if (mine) Color.White else c.primary,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickableNoRipple { Speaker.speak(line.line) }
                                .padding(5.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            if (read) "Continue to quiz" else "I've studied this dialogue",
            onClick = {
                if (!read) {
                    repo.completeProjectTask(pr.id, "read")
                    haptics.success()
                }
                onNext()
            },
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(bounded = false),
            onClick = onClick,
        )
    )

@Composable
private fun QuizSection(pr: Project, passedBefore: Boolean, onNext: () -> Unit) {
    val repo = AppRepository.get(LocalContext.current)
    var attempt by rememberSaveable(pr.id) { mutableIntStateOf(0) }
    var result by rememberSaveable(pr.id, attempt) { mutableStateOf<IntArray?>(null) }
    val questions = remember(pr.id, attempt) { pr.quiz.shuffled() }
    val r = result
    if (r == null) {
        QuizRunner(questions, Modifier.fillMaxSize()) { correct, total, _ ->
            if (correct >= Projects.QUIZ_PASS) repo.completeProjectTask(pr.id, "quiz")
            result = intArrayOf(correct, total)
        }
    } else {
        val passed = r[0] >= Projects.QUIZ_PASS
        QuizResult(
            correct = r[0], total = r[1], passed = passed || passedBefore,
            title = if (passed) "Quiz passed!" else "Try once more",
            message = if (passed) "Great! Now finish the writing task to complete this project." else "You need ${Projects.QUIZ_PASS} of ${r[1]} correct. Review the brief and dialogue, then try again.",
            primaryText = if (passed || passedBefore) "Go to writing task" else "Try again",
            onPrimary = { if (passed || passedBefore) onNext() else attempt++ },
            secondaryText = if (passed) "Retake quiz" else null,
            onSecondary = { attempt++ },
        )
    }
}

@Composable
private fun WritingSection(pr: Project, state: AppState, nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val tasks = state.progress.projectTasks[pr.id].orEmpty()
    val done = pr.id in state.progress.completedProjects
    val nextProject = Projects.all.getOrNull(pr.number)
    ColumnScopeScroll {
        if (done) {
            LexCard(Modifier.fillMaxWidth(), color = c.successSoft, elevated = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppImage(R.drawable.trophy_course_complete, Modifier.size(64.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project complete!", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(
                            if (nextProject != null) "Next: Project ${nextProject.number} · ${nextProject.title}" else "You've completed every project. Your certificate is ready!",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (nextProject != null) {
                    PrimaryButton("Start next project", { nav.back(); nav.project(nextProject.id) }, Modifier.fillMaxWidth(), height = 48.dp)
                } else {
                    PrimaryButton("View my certificate", { nav.back(); nav.certificate() }, Modifier.fillMaxWidth(), height = 48.dp)
                }
            }
            Spacer(Modifier.height(16.dp))
        } else if ("quiz" !in tasks || "read" !in tasks) {
            LexCard(Modifier.fillMaxWidth(), color = c.accentSoft, elevated = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = Color(0xFF8A5A00), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "You can write now, but the project completes only after the dialogue and quiz are done too.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.text,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        WritingWorkspace(
            title = "Writing task",
            prompt = pr.writingTask,
            minWords = pr.minWords,
            keywords = pr.writingKeywords,
            stateKey = "project_" + pr.id,
            submitLabel = if ("write" in tasks) "Check again" else "Check & submit",
        ) { report, _ ->
            if (report.meetsMinimum && report.score >= 50) repo.completeProjectTask(pr.id, "write")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LockedProject(pr: Project, state: AppState, nav: Nav) {
    val c = Lex.colors
    val courseDone = Course.isCourseComplete(state.progress)
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader("Project ${pr.number}", { nav.back() }, subtitle = pr.title)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.75f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.primarySoft)
            ) {
                AppImage(pr.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                LockedOverlay(0.55f)
            }
            Spacer(Modifier.height(20.dp))
            Text(pr.title, style = MaterialTheme.typography.headlineMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            Text(
                if (!courseDone) "This project unlocks after you complete the full A to Z course (all ${Course.modules.size} module quizzes)."
                else "Complete Project ${pr.number - 1} first to unlock this one.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(pr.overview, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            if (!courseDone) {
                val next = Course.nextStep(state.progress)
                PrimaryButton(
                    "Continue the course",
                    {
                        nav.back()
                        val step = next
                        if (step != null) {
                            if (step.second != null) nav.lesson(step.second!!.id) else nav.moduleQuiz(step.first.id)
                        }
                    },
                    Modifier.fillMaxWidth(),
                )
            } else {
                val prev = Projects.all[pr.number - 2]
                PrimaryButton("Open Project ${prev.number}", { nav.back(); nav.project(prev.id) }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Go back", { nav.back() }, Modifier.fillMaxWidth())
        }
    }
}
