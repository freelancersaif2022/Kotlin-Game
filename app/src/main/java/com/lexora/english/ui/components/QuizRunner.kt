package com.lexora.english.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.content.Question
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.rememberHaptics

/**
 * Multiple-choice quiz flow: one question at a time, instant feedback with explanation, then
 * [onFinished]. [initialAnswers] lets a quiz resume (the daily challenge), -1 = unanswered.
 */
@Composable
fun QuizRunner(
    questions: List<Question>,
    modifier: Modifier = Modifier,
    initialAnswers: List<Int> = emptyList(),
    onAnswer: (index: Int, choice: Int, correct: Boolean) -> Unit = { _, _, _ -> },
    onFinished: (correct: Int, total: Int, answers: List<Int>) -> Unit,
) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    val answers = remember(questions) {
        mutableStateListOf<Int>().apply { addAll(List(questions.size) { initialAnswers.getOrNull(it) ?: -1 }) }
    }
    var index by rememberSaveable(questions) {
        mutableIntStateOf(answers.indexOfFirst { it < 0 }.let { if (it < 0) questions.lastIndex else it })
    }
    val q = questions[index]
    val chosen = answers[index]
    val answered = chosen >= 0

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Question ${index + 1} of ${questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textMuted,
                    modifier = Modifier.weight(1f),
                )
                val correctSoFar = answers.indices.count { answers[it] >= 0 && answers[it] == questions[it].answer }
                Pill("✓ $correctSoFar", color = c.success, background = c.successSoft)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                questions.indices.forEach { i ->
                    val a = answers[i]
                    val col = when {
                        a < 0 && i == index -> c.primary.copy(alpha = 0.45f)
                        a < 0 -> c.track
                        a == questions[i].answer -> c.success
                        else -> c.error
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(col)
                    )
                }
            }
        }

        AnimatedContent(
            targetState = index,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)))
                    .togetherWith(slideOutHorizontally(tween(250)) { -it / 3 } + fadeOut(tween(200)))
            },
            label = "question",
        ) { qi ->
            val question = questions[qi]
            val sel = answers[qi]
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(question.prompt, style = MaterialTheme.typography.headlineSmall, color = c.text)
                Spacer(Modifier.height(20.dp))
                question.options.forEachIndexed { oi, option ->
                    OptionCard(
                        letter = ('A' + oi).toString(),
                        text = option,
                        state = when {
                            sel < 0 -> OptionState.Idle
                            oi == question.answer -> OptionState.Correct
                            oi == sel -> OptionState.Wrong
                            else -> OptionState.Dimmed
                        },
                        onClick = {
                            if (answers[qi] < 0) {
                                answers[qi] = oi
                                val ok = oi == question.answer
                                if (ok) haptics.success() else haptics.error()
                                onAnswer(qi, oi, ok)
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                AnimatedVisibility(sel >= 0 && question.explanation.isNotBlank(), enter = fadeIn() + expandVertically()) {
                    val ok = sel == question.answer
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (ok) c.successSoft else c.accentSoft)
                            .padding(14.dp)
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = if (ok) c.success else Color(0xFFB7791F), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (ok) "Correct!" else "Not quite", style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text(question.explanation, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                    }
                }
            }
        }

        PrimaryButton(
            text = when {
                !answered -> "Choose an answer"
                index < questions.lastIndex -> "Next question"
                else -> "See results"
            },
            icon = if (answered) Icons.AutoMirrored.Rounded.ArrowForward else null,
            enabled = answered,
            onClick = {
                haptics.tick()
                if (index < questions.lastIndex) {
                    index++
                } else {
                    val correct = answers.indices.count { answers[it] == questions[it].answer }
                    onFinished(correct, questions.size, answers.toList())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

enum class OptionState { Idle, Correct, Wrong, Dimmed }

@Composable
fun OptionCard(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val c = Lex.colors
    val border by animateColorAsState(
        when (state) {
            OptionState.Correct -> c.success
            OptionState.Wrong -> c.error
            else -> c.border
        }, label = "ob"
    )
    val bg by animateColorAsState(
        when (state) {
            OptionState.Correct -> c.successSoft
            OptionState.Wrong -> c.errorSoft
            else -> c.card
        }, label = "obg"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(if (state == OptionState.Idle || state == OptionState.Dimmed) 1.dp else 2.dp, border, RoundedCornerShape(16.dp))
            .clickable(enabled = state == OptionState.Idle, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when (state) {
                        OptionState.Correct -> c.success
                        OptionState.Wrong -> c.error
                        else -> c.cardAlt
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                OptionState.Correct -> Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                OptionState.Wrong -> Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                else -> Text(letter, style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (state == OptionState.Dimmed) c.textMuted else c.text,
            fontWeight = if (state == OptionState.Correct) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Result screen shared by every quiz type. */
@Composable
fun QuizResult(
    correct: Int,
    total: Int,
    passed: Boolean,
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String?,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Lex.colors
    val percent = if (total == 0) 0 else correct * 100 / total
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppImage(if (passed) R.drawable.quiz_success else R.drawable.quiz_fail, Modifier.size(200.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        ProgressRing(percent / 100f, 132.dp, 12.dp, color = if (passed) c.success else c.error) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$percent%", style = MaterialTheme.typography.headlineMedium, color = c.text)
                Text("$correct of $total", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(32.dp))
        PrimaryButton(primaryText, onPrimary, Modifier.fillMaxWidth())
        if (secondaryText != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(secondaryText, onSecondary, Modifier.fillMaxWidth())
        }
    }
}
