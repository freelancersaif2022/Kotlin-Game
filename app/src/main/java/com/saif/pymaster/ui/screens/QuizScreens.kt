package com.saif.pymaster.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.QuizQuestion
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.MainViewModel
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.TrackStudyTime
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.CodeBlock
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.components.ProgressRing
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.components.SecondaryButton
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.LocalHaptics

/** Plays through [questions] one by one and reports the chosen answers at the end. */
@Composable
private fun QuizFlow(
    questions: List<QuizQuestion>,
    onFinished: (answers: List<Int>, correct: Int) -> Unit,
) {
    val haptics = LocalHaptics.current
    val colors = LocalAppColors.current
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val answers = remember { mutableStateListOf<Int>() }
    val q = questions.getOrNull(index) ?: return

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Question ${index + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Brand.Cyan,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${answers.withIndex().count { (i, a) -> questions[i].answer == a }} correct",
                    style = MaterialTheme.typography.labelMedium,
                    color = Brand.TextSecondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            GradientProgressBar((index + if (selected != null) 1f else 0f) / questions.size, height = 6.dp)
        }
        AnimatedContent(
            targetState = index,
            transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "question",
        ) { qi ->
            val question = questions[qi]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                RichText(question.q, Brand.White, style = MaterialTheme.typography.titleLarge)
                if (question.code.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    CodeBlock(question.code)
                }
                Spacer(Modifier.height(18.dp))
                question.options.forEachIndexed { i, option ->
                    val reveal = selected != null
                    val isCorrect = i == question.answer
                    val isChosen = i == selected
                    val border = when {
                        reveal && isCorrect -> Brand.Success
                        reveal && isChosen -> Brand.Danger
                        else -> colors.stroke
                    }
                    val bg = when {
                        reveal && isCorrect -> Brand.Success.copy(alpha = 0.14f)
                        reveal && isChosen -> Brand.Danger.copy(alpha = 0.14f)
                        else -> colors.surface
                    }
                    Row(
                        Modifier
                            .padding(vertical = 6.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .border(1.5.dp, border, RoundedCornerShape(16.dp))
                            .clickable(enabled = !reveal) {
                                selected = i
                                answers.add(i)
                                if (isCorrect) haptics?.success() else haptics?.error()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                reveal && isCorrect -> Icon(Icons.Rounded.CheckCircle, null, tint = Brand.Success)
                                reveal && isChosen -> Icon(Icons.Rounded.Cancel, null, tint = Brand.Danger)
                                else -> Text(('A' + i).toString(), style = MaterialTheme.typography.labelLarge, color = Brand.White)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        RichText(option, Brand.White, Modifier.weight(1f))
                    }
                }
                if (selected != null && question.explain.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brand.PythonBlue.copy(alpha = 0.16f))
                            .padding(14.dp)
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = Brand.PythonYellow, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        RichText(question.explain, Brand.White.copy(alpha = 0.9f))
                    }
                }
            }
        }
        PrimaryButton(
            text = if (index == questions.lastIndex) "See results" else "Next question",
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            enabled = selected != null,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (index == questions.lastIndex) {
                val list = answers.toList()
                onFinished(list, list.withIndex().count { (i, a) -> questions[i].answer == a })
            } else {
                index++
                selected = null
            }
        }
    }
}

@Composable
private fun ResultView(
    title: String,
    message: String,
    correct: Int,
    total: Int,
    passed: Boolean,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String?,
    onSecondary: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ProgressRing(
                correct.toFloat() / total.coerceAtLeast(1), size = 156.dp, stroke = 14.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$correct/$total", style = MaterialTheme.typography.headlineMedium, color = Brand.White)
                    Text("correct", style = MaterialTheme.typography.labelMedium, color = Brand.TextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            Tag(if (passed) "PASSED" else "NOT PASSED YET", if (passed) Brand.Success else Brand.PythonYellow)
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Brand.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Brand.TextSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(primary, modifier = Modifier.fillMaxWidth(), onClick = onPrimary)
        if (secondary != null) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton(secondary, modifier = Modifier.fillMaxWidth(), onClick = onSecondary)
        }
    }
}

@Composable
fun ModuleQuizScreen(state: AppState?, moduleId: String, vm: MainViewModel, nav: Navigator) {
    val s = state ?: return
    val m = Content.module(moduleId) ?: return
    val colors = LocalAppColors.current
    var attempt by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val questions = remember(attempt) { m.quiz.shuffled() }
    TrackStudyTime(vm)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar("Module ${m.number} quiz", subtitle = m.title, onBack = nav::back)
        val r = result
        if (r == null) {
            QuizFlow(questions) { _, correct ->
                vm.submitModuleQuiz(m.id, correct) { passed -> result = correct to passed }
            }
        } else {
            val (correct, passed) = r
            val nextModule = Content.modules.getOrNull(m.number)
            val courseDone = Rules.courseComplete(s)
            ResultView(
                title = if (passed) "Module ${m.number} complete!" else "Almost there!",
                message = if (passed) {
                    if (courseDone) "You finished the whole Python course! 15 real-world projects are now unlocked."
                    else "Great job. ${nextModule?.let { "Module ${it.number}: ${it.title} is now unlocked." } ?: ""}"
                } else "You need ${Rules.passMark(m)} correct answers to pass. Review the lessons and try again — you've got this.",
                correct = correct,
                total = questions.size,
                passed = passed,
                primary = when {
                    !passed -> "Try again"
                    courseDone -> "Open projects"
                    nextModule != null -> "Start Module ${nextModule.number}"
                    else -> "Back to course"
                },
                onPrimary = {
                    when {
                        !passed -> {
                            result = null
                            attempt++
                        }
                        courseDone -> {
                            nav.back(); nav.to(Routes.PROJECTS)
                        }
                        nextModule != null -> {
                            nav.back(); nav.to(Routes.module(nextModule.id))
                        }
                        else -> nav.back()
                    }
                },
                secondary = if (passed) "Back to module" else "Review lessons",
                onSecondary = { nav.back() },
            )
        }
    }
}

@Composable
fun DailyQuizScreen(state: AppState?, vm: MainViewModel, nav: Navigator) {
    val s = state ?: return
    val colors = LocalAppColors.current
    val today = Rules.today()
    val questions = remember(today) { Rules.dailyQuestions(today) }
    val doneToday = s.progress.dailyQuiz.day == today && s.progress.dailyQuiz.completed
    var justFinished by remember { mutableStateOf<Pair<List<Int>, Int>?>(null) }
    TrackStudyTime(vm)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar("Daily quiz", subtitle = "${questions.size} new questions every day", onBack = nav::back)
        val finished = justFinished ?: if (doneToday) s.progress.dailyQuiz.answers to s.progress.dailyQuiz.score else null
        if (finished == null) {
            QuizFlow(questions) { answers, correct ->
                justFinished = answers to correct
                vm.submitDailyQuiz(answers, correct)
            }
        } else {
            val (answers, correct) = finished
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppCard(Modifier.fillMaxWidth(), border = Brand.Cyan.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(correct.toFloat() / questions.size.coerceAtLeast(1), size = 104.dp, stroke = 11.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$correct/${questions.size}", style = MaterialTheme.typography.titleLarge, color = Brand.White)
                                Text("today", style = MaterialTheme.typography.labelSmall, color = Brand.TextSecondary)
                            }
                        }
                        Spacer(Modifier.width(18.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Daily quiz complete ⚡", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tag("+${correct * Rules.XP_DAILY_CORRECT} XP", Brand.PythonYellow)
                                Tag("${s.progress.streak} day streak", Brand.Warning)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("New questions arrive tomorrow.", style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    "Review your answers",
                    style = MaterialTheme.typography.titleMedium,
                    color = Brand.White,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                questions.forEachIndexed { i, q ->
                    val chosen = answers.getOrNull(i)
                    val ok = chosen == q.answer
                    AppCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        border = if (ok) Brand.Success.copy(alpha = 0.4f) else Brand.Danger.copy(alpha = 0.4f),
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null,
                                tint = if (ok) Brand.Success else Brand.Danger,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                RichText(q.q, Brand.White)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Answer: " + q.options[q.answer].replace("`", ""),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = Brand.Success,
                                )
                                if (!ok && chosen != null) {
                                    Text(
                                        "Your answer: " + q.options[chosen].replace("`", ""),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = Brand.Danger,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
