package com.saif.pymaster.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.GoldGradient
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors

@Composable
fun ModuleScreen(state: AppState?, moduleId: String, nav: Navigator) {
    val s = state ?: return
    val m = Content.module(moduleId) ?: return
    val index = Content.modules.indexOf(m)
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val unlocked = Rules.moduleUnlocked(s, index)
    val fraction = Rules.moduleFraction(s, m)
    val complete = Rules.moduleComplete(s, m)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar("Module ${m.number}", subtitle = m.title, onBack = nav::back)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header card
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Brand.PythonBlue.copy(alpha = 0.45f), colors.surface, colors.surface)))
                    .border(1.dp, Color(0x3300D1FF), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Tag(if (complete) "COMPLETED" else if (unlocked) "IN PROGRESS" else "LOCKED", if (complete) Brand.Success else if (unlocked) Brand.Cyan else Brand.PythonYellow)
                            Spacer(Modifier.height(10.dp))
                            Text(m.title, style = MaterialTheme.typography.headlineSmall, color = Brand.White)
                            Spacer(Modifier.height(4.dp))
                            Text(m.subtitle, style = MaterialTheme.typography.bodyMedium, color = Brand.TextSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        Image(painterResource(Content.imageRes(m.image)), null, Modifier.size(96.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    GradientProgressBar(fraction, brush = if (complete) GoldGradient else Brush.horizontalGradient(listOf(Brand.PythonBlue, Brand.Cyan)))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${Rules.lessonsDone(s, m)} of ${m.lessons.size} lessons  •  ${m.lessons.sumOf { it.minutes }} min  •  ${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                }
            }

            if (!unlocked) {
                AppCard(Modifier.padding(20.dp).fillMaxWidth(), border = Brand.PythonYellow.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = Brand.PythonYellow)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Complete Module ${m.number - 1} (all lessons + quiz) to unlock this module.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brand.White,
                        )
                    }
                }
            }

            Text(
                "Lessons",
                style = MaterialTheme.typography.titleMedium,
                color = Brand.White,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
            )
            m.lessons.forEachIndexed { i, lesson ->
                val done = Rules.lessonDone(s, lesson.id)
                val open = Rules.lessonUnlocked(s, m, i)
                val current = open && !done
                AppCard(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .fillMaxWidth(),
                    onClick = {
                        if (open) nav.to(Routes.lesson(lesson.id))
                        else Toast.makeText(context, "Finish the previous lesson first", Toast.LENGTH_SHORT).show()
                    },
                    padding = PaddingValues(14.dp),
                    border = if (current) Brand.Cyan.copy(alpha = 0.5f) else colors.stroke,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        done -> Brand.Success.copy(alpha = 0.18f)
                                        current -> Brand.Cyan.copy(alpha = 0.18f)
                                        else -> colors.surfaceHigh
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                done -> Icon(Icons.Rounded.Check, "Done", tint = Brand.Success)
                                current -> Icon(Icons.Rounded.PlayArrow, "Start", tint = Brand.Cyan)
                                else -> Icon(Icons.Rounded.Lock, "Locked", tint = Brand.TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "LESSON ${i + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = Brand.TextMuted,
                            )
                            Text(
                                lesson.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (open) Brand.White else Brand.TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Schedule, null, tint = Brand.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${lesson.minutes}m", style = MaterialTheme.typography.labelSmall, color = Brand.TextMuted)
                        }
                    }
                }
            }

            // Quiz
            val quizOpen = Rules.quizUnlocked(s, m) && unlocked
            val passed = Rules.quizPassed(s, m)
            val best = s.progress.quizScores[m.id]
            AppCard(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (quizOpen) nav.to(Routes.quiz(m.id))
                    else Toast.makeText(context, "Finish all lessons to unlock the quiz", Toast.LENGTH_SHORT).show()
                },
                border = if (passed) Brand.Success.copy(alpha = 0.5f) else Brand.PythonYellow.copy(alpha = if (quizOpen) 0.6f else 0.2f),
                background = colors.surface,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brand.PythonYellow.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (quizOpen) Icons.Rounded.Quiz else Icons.Rounded.Lock, null, tint = Brand.PythonYellow)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Module quiz", style = MaterialTheme.typography.titleSmall, color = Brand.White)
                        Text(
                            when {
                                passed -> "Passed  •  best score $best/${m.quiz.size}"
                                best != null -> "Best $best/${m.quiz.size}  •  need ${Rules.passMark(m)} to pass"
                                else -> "${m.quiz.size} questions  •  pass with ${Rules.passMark(m)}/${m.quiz.size}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.TextSecondary,
                        )
                    }
                    if (passed) Icon(Icons.Rounded.Check, null, tint = Brand.Success)
                }
            }
        }
    }
}
