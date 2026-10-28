package com.lexora.english.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.content.Course
import com.lexora.english.content.DailyQuiz
import com.lexora.english.content.Module
import com.lexora.english.content.Project
import com.lexora.english.content.Projects
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.data.Progress
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.CircleIconButton
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.components.Pill
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.ProfileAvatar
import com.lexora.english.ui.components.ProgressRing
import com.lexora.english.ui.components.SecondaryButton
import com.lexora.english.ui.components.SectionTitle
import com.lexora.english.ui.components.StatBlock
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun HomeScreen(state: AppState, nav: Nav, listState: LazyListState, onOpenTab: (Int) -> Unit) {
    val c = Lex.colors
    val p = state.progress
    val repo = AppRepository.get(LocalContext.current)
    val unread = state.notifications.count { !it.read }
    val todayKey = LocalDate.now().toString()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(state.profile, 48.dp, ring = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    Text(
                        state.profile?.name ?: "Learner",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.tutorialTarget("bell")) {
                    CircleIconButton(Icons.Rounded.Notifications, "Notifications", { nav.notifications() }, size = 46.dp)
                    if (unread > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(if (unread > 9) 22.dp else 19.dp)
                                .clip(CircleShape)
                                .background(c.error)
                                .border(2.dp, c.background, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (unread > 9) "9+" else unread.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        item(key = "hero") { HeroCard(state, nav, onOpenTab) }

        item(key = "stats") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatBlock("${repo.displayStreak(p)} days", "Streak", Icons.Rounded.LocalFireDepartment, Brand.Orange, Modifier.weight(1f))
                StatBlock(AppRepository.formatMinutes(p.studySeconds.values.sum()), "Course time", Icons.Rounded.Schedule, Brand.Blue, Modifier.weight(1f))
                StatBlock("${p.xp}", "XP earned", Icons.Rounded.Bolt, Brand.Indigo, Modifier.weight(1f))
            }
        }

        item(key = "goal") {
            val goalMin = state.profile?.dailyMinutes ?: 15
            val todayMin = ((p.studySeconds[todayKey] ?: 0L) / 60).toInt()
            LexCard(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Today's goal", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(
                            if (todayMin >= goalMin) "Goal reached — amazing work! 🎉" else "${goalMin - todayMin} min left to reach your goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                        )
                    }
                    Text(
                        "${todayMin.coerceAtMost(999)}/$goalMin min",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (todayMin >= goalMin) c.success else c.primary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LinearProgress(todayMin / goalMin.toFloat(), color = if (todayMin >= goalMin) c.success else c.primary)
            }
        }

        item(key = "daily") { DailyCard(p, nav) }

        item(key = "modulesTitle") {
            SectionTitle(
                "Your course",
                Modifier.padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 10.dp),
                action = "See all",
                onAction = { onOpenTab(1) },
            )
        }
        item(key = "modules") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(Course.modules, key = { it.id }) { m -> ModuleMiniCard(m, p) { nav.module(m.id) } }
            }
        }

        item(key = "projectsTitle") {
            SectionTitle(
                "Real-life projects",
                Modifier.padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
                action = "See all",
                onAction = { onOpenTab(1) },
            )
            Text(
                if (Course.isCourseComplete(p)) "${p.completedProjects.size} of ${Projects.all.size} projects completed"
                else "Complete the full course to unlock these projects",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
            )
        }
        item(key = "projects") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(Projects.all, key = { it.id }) { pr -> ProjectMiniCard(pr, p) { nav.project(pr.id) } }
            }
        }

        item(key = "certificate") { CertificateCard(state, nav) }
    }
}

private fun greeting(): String {
    val h = LocalTime.now().hour
    return when {
        h < 5 -> "Good night 🌙"
        h < 12 -> "Good morning ☀️"
        h < 17 -> "Good afternoon 👋"
        else -> "Good evening 🌆"
    }
}

@Composable
private fun HeroCard(state: AppState, nav: Nav, onOpenTab: (Int) -> Unit) {
    val c = Lex.colors
    val p = state.progress
    val courseProgress = Course.courseProgress(p)
    val courseDone = Course.isCourseComplete(p)
    val next = Course.nextStep(p)
    val nextProject = Projects.all.firstOrNull { it.id !in p.completedProjects }
    val started = p.completedLessons.isNotEmpty()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .tutorialTarget("progress")
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, c.border, RoundedCornerShape(26.dp))
    ) {
        AppImage(
            if (c.isDark) R.drawable.home_header_bg_dark else R.drawable.home_header_bg,
            Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            crossfade = false,
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Pill(
                        when {
                            p.certificateIssuedAt != null -> "Certified 🎓"
                            courseDone -> "Projects phase"
                            else -> state.profile?.level ?: "Beginner"
                        },
                        color = c.primary,
                        background = c.card.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            p.certificateIssuedAt != null -> "You did it!"
                            courseDone -> "Course complete"
                            started -> "Keep going!"
                            else -> "Start your journey"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.text,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            p.certificateIssuedAt != null -> "All lessons and projects completed."
                            courseDone && nextProject != null -> "Next project: ${nextProject.title}"
                            next != null && next.second != null -> "Module ${next.first.number} · ${next.second!!.title}"
                            next != null -> "Module ${next.first.number} quiz is ready"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                val ringValue = if (courseDone) p.completedProjects.size / Projects.all.size.toFloat() else courseProgress
                ProgressRing(
                    ringValue,
                    size = 92.dp,
                    stroke = 10.dp,
                    track = c.card.copy(alpha = 0.7f),
                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Brand.Blue, Brand.Indigo, Brand.Blue)),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(ringValue * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = c.text, fontWeight = FontWeight.Bold)
                        Text(if (courseDone) "projects" else "course", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${p.completedLessons.size}/${Course.totalLessons} lessons", color = c.text, background = c.card.copy(alpha = 0.8f))
                Pill("${p.passedModules.size}/${Course.modules.size} modules", color = c.text, background = c.card.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = when {
                    p.certificateIssuedAt != null -> "View certificate"
                    courseDone -> "Continue projects"
                    started -> "Continue learning"
                    else -> "Start learning"
                },
                icon = if (started || courseDone) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.PlayArrow,
                onClick = {
                    when {
                        p.certificateIssuedAt != null -> nav.certificate()
                        courseDone && nextProject != null -> nav.project(nextProject.id)
                        next != null && next.second != null -> nav.lesson(next.second!!.id)
                        next != null -> nav.moduleQuiz(next.first.id)
                        else -> onOpenTab(1)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 50.dp,
            )
        }
    }
}

@Composable
private fun DailyCard(p: Progress, nav: Nav) {
    val c = Lex.colors
    val today = LocalDate.now()
    val questions = remember(today) { DailyQuiz.questionsFor(today) }
    val answers = if (p.dailyQuizDate == today.toString()) p.dailyAnswers else emptyList()
    val answered = answers.count { it >= 0 }
    val done = answered == DailyQuiz.SIZE
    val correct = answers.indices.count { answers[it] >= 0 && answers[it] == questions.getOrNull(it)?.answer }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val remaining = remember(now) {
        val mins = java.time.Duration.between(LocalDateTime.now(), today.plusDays(1).atStartOfDay()).toMinutes()
        "${mins / 60}h ${mins % 60}m"
    }

    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .tutorialTarget("daily"),
        onClick = { nav.daily() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                AppImage(R.drawable.quiz_daily, Modifier.size(66.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily Challenge", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(6.dp))
                    if (done) Pill("$correct/${DailyQuiz.SIZE}", color = c.success, background = c.successSoft)
                    else Pill("NEW", color = Color(0xFF8A5A00), background = c.accentSoft)
                }
                Text(
                    if (done) "Done for today · new in $remaining" else "${DailyQuiz.SIZE} questions · ${DailyQuiz.topicLine(today)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    maxLines = 2,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until DailyQuiz.SIZE) {
                        val a = answers.getOrNull(i) ?: -1
                        val color = when {
                            a < 0 -> c.track
                            a == questions.getOrNull(i)?.answer -> c.success
                            else -> c.error
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (done) {
            SecondaryButton("Review answers", { nav.daily() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.CheckCircle, height = 46.dp)
        } else {
            PrimaryButton(if (answered > 0) "Continue · $answered/${DailyQuiz.SIZE}" else "Start challenge", { nav.daily() }, Modifier.fillMaxWidth(), height = 46.dp)
        }
    }
}

@Composable
fun ModuleMiniCard(m: Module, p: Progress, onClick: () -> Unit) {
    val c = Lex.colors
    val unlocked = Course.isModuleUnlocked(m, p)
    val progress = Course.moduleProgress(m, p)
    LexCard(Modifier.width(220.dp), onClick = onClick, padding = PaddingValues(0.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.78f)
                .background(c.primarySoft)
        ) {
            AppImage(m.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = m.title)
            if (!unlocked) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, "Locked", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            if (m.id in p.passedModules) {
                Icon(
                    Icons.Rounded.CheckCircle, "Completed", tint = c.success,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
        Column(Modifier.padding(14.dp)) {
            Text("MODULE ${m.number}", style = MaterialTheme.typography.labelSmall, color = c.primary)
            Text(m.title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            LinearProgress(progress, height = 6.dp, color = if (progress >= 1f) c.success else c.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                if (!unlocked) "Locked" else "${(progress * 100).toInt()}% · ${m.lessons.size} lessons",
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
            )
        }
    }
}

@Composable
fun ProjectMiniCard(pr: Project, p: Progress, onClick: () -> Unit) {
    val c = Lex.colors
    val unlocked = Projects.isUnlocked(pr, p)
    val done = pr.id in p.completedProjects
    LexCard(Modifier.width(220.dp), onClick = onClick, padding = PaddingValues(0.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .background(c.primarySoft)
        ) {
            AppImage(pr.image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = pr.title)
            if (!unlocked) {
                LockedOverlay()
            }
            if (done) {
                Icon(
                    Icons.Rounded.CheckCircle, "Completed", tint = c.success,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
        Column(Modifier.padding(14.dp)) {
            Text("PROJECT ${pr.number}", style = MaterialTheme.typography.labelSmall, color = Brand.Indigo)
            Text(pr.title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (!unlocked) "🔒 Locked" else if (done) "Completed" else pr.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CertificateCard(state: AppState, nav: Nav) {
    val c = Lex.colors
    val p = state.progress
    val issued = p.certificateIssuedAt != null
    LexCard(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        onClick = { nav.certificate() },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppImage(R.drawable.trophy_course_complete, Modifier.size(76.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Brand.Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Professional Certificate", style = MaterialTheme.typography.titleMedium, color = c.text)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (issued) "Issued to ${state.profile?.name}. Tap to view, save or share."
                    else "Complete all ${Course.modules.size} modules and ${Projects.all.size} projects to earn it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                )
                if (!issued) {
                    Spacer(Modifier.height(10.dp))
                    val total = Course.modules.size + Projects.all.size
                    val done = p.passedModules.size + p.completedProjects.size
                    LinearProgress(done / total.toFloat(), height = 6.dp, color = Brand.Gold)
                }
            }
        }
    }
}

/** Dims a locked project image and shows the frosted padlock badge in the middle. */
@Composable
fun LockedOverlay(badgeWidth: Float = 0.5f) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        AppImage(
            R.drawable.project_locked_overlay,
            Modifier
                .fillMaxWidth(badgeWidth)
                .aspectRatio(1.8f),
            contentScale = ContentScale.Fit,
            contentDescription = "Locked",
        )
    }
}
