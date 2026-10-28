package com.lexora.english.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.content.DailyQuiz
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.StudyTimer
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.ProgressRing
import com.lexora.english.ui.components.QuizRunner
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.theme.Lex
import java.time.LocalDate

@Composable
fun DailyQuizScreen(state: AppState, nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val today = LocalDate.now()
    val questions = remember(today) { DailyQuiz.questionsFor(today) }
    LaunchedEffect(Unit) { repo.ensureDailyQuiz(postSystemNotification = false) }
    StudyTimer()

    val p = state.progress
    val answers = if (p.dailyQuizDate == today.toString()) p.dailyAnswers else emptyList()
    val complete = answers.size == DailyQuiz.SIZE && answers.all { it >= 0 }
    // Show the review only after "See results", so the last explanation stays visible.
    var showReview by rememberSaveable { mutableStateOf(complete) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader("Daily Challenge", { nav.back() }, subtitle = DailyQuiz.topicLine(today).replaceFirstChar { it.uppercaseChar() })
        if (!complete || !showReview) {
            QuizRunner(
                questions,
                Modifier.weight(1f),
                initialAnswers = answers,
                onAnswer = { i, choice, ok -> repo.answerDaily(i, choice, ok) },
                onFinished = { _, _, _ -> showReview = true },
            )
        } else {
            val correct = questions.indices.count { answers[it] == questions[it].answer }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    LexCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppImage(if (correct >= 3) R.drawable.quiz_success else R.drawable.quiz_fail, Modifier.size(96.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (correct == DailyQuiz.SIZE) "Perfect score!" else if (correct >= 3) "Well done!" else "Good try!",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = c.text,
                                )
                                Text(
                                    "Come back tomorrow for 5 new questions. We'll notify you when they arrive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textMuted,
                                )
                            }
                            ProgressRing(correct / DailyQuiz.SIZE.toFloat(), 64.dp, 7.dp, color = if (correct >= 3) c.success else c.error) {
                                Text("$correct/${DailyQuiz.SIZE}", style = MaterialTheme.typography.titleSmall, color = c.text)
                            }
                        }
                    }
                }
                itemsIndexed(questions) { i, q ->
                    val a = answers[i]
                    val ok = a == q.answer
                    LexCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                null,
                                tint = if (ok) c.success else c.error,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${i + 1}. ${q.prompt}", style = MaterialTheme.typography.titleSmall, color = c.text)
                                Spacer(Modifier.height(6.dp))
                                if (!ok && a >= 0) {
                                    Text("Your answer: ${q.options[a]}", style = MaterialTheme.typography.bodySmall, color = c.error)
                                }
                                Text("Correct: ${q.options[q.answer]}", style = MaterialTheme.typography.bodySmall, color = c.success)
                                if (q.explanation.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(c.cardAlt)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            PrimaryButton(
                "Done",
                { nav.back() },
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
