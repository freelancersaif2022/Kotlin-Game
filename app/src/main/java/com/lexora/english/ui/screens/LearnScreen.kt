package com.lexora.english.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexora.english.content.Course
import com.lexora.english.content.Module
import com.lexora.english.content.Project
import com.lexora.english.content.Projects
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.data.Progress
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.BarChart
import com.lexora.english.ui.components.BarDatum
import com.lexora.english.ui.components.DonutChart
import com.lexora.english.ui.components.DonutSlice
import com.lexora.english.ui.components.LegendRow
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.components.Pill
import com.lexora.english.ui.components.PillSelector
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LearnScreen(state: AppState, nav: Nav, listState: LazyListState) {
    val c = Lex.colors
    val p = state.progress
    var segment by rememberSaveable { mutableIntStateOf(if (Course.isCourseComplete(p)) 1 else 0) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "title") {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Learn", style = MaterialTheme.typography.headlineLarge, color = c.text)
                Text("Your complete English path from A to Z", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
        }
        item(key = "overview") { OverviewCard(p) }
        item(key = "weekly") { WeeklyCard(state) }
        item(key = "segment") {
            PillSelector(
                listOf(0, 1), segment, { if (it == 0) "Course · ${Course.modules.size} modules" else "Projects · ${Projects.all.size}" }, { segment = it },
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        if (segment == 0) {
            items(Course.modules, key = { it.id }) { m -> ModuleRow(m, p) { nav.module(m.id) } }
        } else {
            if (!Course.isCourseComplete(p)) {
                item(key = "lockedInfo") {
                    LexCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        color = c.accentSoft,
                        elevated = false,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, tint = Color(0xFF8A5A00))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Projects unlock after you pass all ${Course.modules.size} module quizzes. ${Course.modules.size - p.passedModules.size} to go!",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.text,
                            )
                        }
                    }
                }
            }
            items(Projects.all, key = { it.id }) { pr -> ProjectRow(pr, p) { nav.project(pr.id) } }
        }
    }
}

@Composable
private fun OverviewCard(p: Progress) {
    val c = Lex.colors
    val lessons = p.completedLessons.size
    val total = Course.totalLessons
    val quizAvg = if (p.passedModules.isEmpty()) 0 else p.passedModules.values.average().toInt()
    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text("Course overview", style = MaterialTheme.typography.titleMedium, color = c.text)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                slices = listOf(
                    DonutSlice("Completed", lessons.toFloat(), Brand.Blue),
                    DonutSlice("Remaining", (total - lessons).toFloat(), Color.Transparent),
                ),
                centerTitle = "${(Course.courseProgress(p) * 100).toInt()}%",
                centerSubtitle = "complete",
                size = 128.dp,
                stroke = 16.dp,
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendRow(Brand.Blue, "Lessons", "$lessons/$total")
                LegendRow(Brand.Indigo, "Modules passed", "${p.passedModules.size}/${Course.modules.size}")
                LegendRow(Brand.Emerald, "Projects", "${p.completedProjects.size}/${Projects.all.size}")
                LegendRow(Brand.Amber, "Quiz average", if (quizAvg == 0) "—" else "$quizAvg%")
            }
        }
    }
}

@Composable
private fun WeeklyCard(state: AppState) {
    val c = Lex.colors
    val p = state.progress
    val today = LocalDate.now()
    val data = remember(p.studySeconds, today) {
        (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            BarDatum(
                d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).take(3),
                ((p.studySeconds[d.toString()] ?: 0L) / 60f),
                highlight = back == 0,
            )
        }
    }
    val weekMinutes = data.sumOf { it.value.toDouble() }.toInt()
    val activeDays = data.count { it.value >= 1f }
    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Weekly activity", style = MaterialTheme.typography.titleMedium, color = c.text)
                Text("Minutes studied per day", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(AppRepository.formatMinutes(weekMinutes * 60L), style = MaterialTheme.typography.titleLarge, color = c.primary)
                Text("$activeDays/7 active days", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        BarChart(data, goal = (state.profile?.dailyMinutes ?: 15).toFloat(), height = 130.dp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(16.dp)
                    .height(3.dp)
                    .background(Brand.Amber)
            )
            Spacer(Modifier.width(6.dp))
            Text("Daily goal: ${state.profile?.dailyMinutes ?: 15} min", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        }
    }
}

@Composable
private fun ModuleRow(m: Module, p: Progress, onClick: () -> Unit) {
    val c = Lex.colors
    val unlocked = Course.isModuleUnlocked(m, p)
    val passed = m.id in p.passedModules
    val progress = Course.moduleProgress(m, p)
    val quizReady = !passed && Course.isQuizUnlocked(m, p)
    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        onClick = onClick,
        padding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(104.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.primarySoft)
            ) {
                AppImage(m.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = m.title)
                if (!unlocked) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Lock, "Locked", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MODULE ${m.number}", style = MaterialTheme.typography.labelSmall, color = c.primary, modifier = Modifier.weight(1f))
                    when {
                        passed -> Icon(Icons.Rounded.CheckCircle, "Passed", tint = c.success, modifier = Modifier.size(18.dp))
                        quizReady -> Pill("Quiz ready", icon = Icons.Rounded.Quiz, color = Color(0xFF8A5A00), background = c.accentSoft)
                    }
                }
                Text(m.title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${m.lessons.size} lessons · ${m.lessons.sumOf { it.minutes }} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgress(progress, height = 6.dp, color = if (passed) c.success else c.primary)
            }
        }
    }
}

@Composable
private fun ProjectRow(pr: Project, p: Progress, onClick: () -> Unit) {
    val c = Lex.colors
    val unlocked = Projects.isUnlocked(pr, p)
    val done = pr.id in p.completedProjects
    val progress = Projects.progressOf(pr, p)
    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        onClick = onClick,
        padding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(104.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.primarySoft)
            ) {
                AppImage(pr.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = pr.title)
                if (!unlocked) LockedOverlay(0.7f)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PROJECT ${pr.number}", style = MaterialTheme.typography.labelSmall, color = Brand.Indigo, modifier = Modifier.weight(1f))
                    if (done) Icon(Icons.Rounded.CheckCircle, "Completed", tint = c.success, modifier = Modifier.size(18.dp))
                }
                Text(pr.title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pr.subtitle, style = MaterialTheme.typography.labelMedium, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                LinearProgress(progress, height = 6.dp, color = if (done) c.success else Brand.Indigo)
            }
        }
    }
}
