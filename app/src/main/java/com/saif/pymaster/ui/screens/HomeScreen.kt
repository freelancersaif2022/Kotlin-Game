package com.saif.pymaster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.AvatarImage
import com.saif.pymaster.ui.components.GoldGradient
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.components.ProgressRing
import com.saif.pymaster.ui.components.SectionHeader
import com.saif.pymaster.ui.components.StatTile
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import java.util.Calendar

@Composable
fun HomeScreen(state: AppState?, nav: Navigator) {
    val s = state ?: return
    val colors = LocalAppColors.current
    val p = s.progress
    val unread = s.notifications.count { !it.read }
    val next = Rules.next(s)
    val overall = Rules.overallFraction(s)
    val lessonsDone = p.completedLessons.size
    val totalLessons = Content.totalLessons
    val today = Rules.today()
    val dailyDone = p.dailyQuiz.day == today && p.dailyQuiz.completed
    val dailyCount = Rules.dailyQuestions(today).size

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 24.dp)
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarImage(s.profile, 50.dp, Modifier.clickable { nav.to(Routes.EDIT_PROFILE) })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
                Text(
                    s.profile?.name ?: "Learner",
                    style = MaterialTheme.typography.titleLarge,
                    color = Brand.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(
                    onClick = { nav.to(Routes.NOTIFICATIONS) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.stroke, CircleShape),
                ) {
                    Icon(Icons.Rounded.Notifications, "Notifications", tint = Brand.White)
                }
                if (unread > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Brand.Danger)
                            .border(2.dp, colors.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (unread > 9) "9+" else unread.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                            color = Brand.White,
                        )
                    }
                }
            }
        }

        // Hero progress
        Box(
            Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x3300D1FF), RoundedCornerShape(24.dp))
        ) {
            Image(
                painterResource(R.drawable.home_hero), null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterEnd,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xF20B0F17), 0.5f to Color(0xB30B0F17), 1f to Color(0x000B0F17)
                        )
                    )
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "YOUR PYTHON JOURNEY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = Brand.Cyan,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${(overall * 100).toInt()}%",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 44.sp),
                            color = Brand.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brand.TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Text(
                        "$lessonsDone/$totalLessons lessons  •  ${p.completedProjects.size}/${Content.projects.size} projects",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                }
                GradientProgressBar(overall, Modifier.fillMaxWidth(0.6f))
            }
        }

        // Continue learning
        ContinueCard(s, next, nav)

        // Daily quiz
        AppCard(
            Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
            onClick = { nav.to(Routes.DAILY_QUIZ) },
            padding = PaddingValues(0.dp),
            border = Brand.PythonYellow.copy(alpha = 0.3f),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0x26FFD43B), Color(0x00FFD43B))))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.daily_quiz), null, Modifier.size(76.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily Quiz", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                        Spacer(Modifier.width(8.dp))
                        Tag(if (dailyDone) "DONE" else "NEW", if (dailyDone) Brand.Success else Brand.PythonYellow)
                    }
                    Text(
                        if (dailyDone) "You scored ${p.dailyQuiz.score}/${p.dailyQuiz.answers.size} today. New questions tomorrow!"
                        else "$dailyCount fresh questions are waiting. Earn up to ${dailyCount * Rules.XP_DAILY_CORRECT} XP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (dailyDone) Brand.Success.copy(alpha = 0.2f) else Brand.PythonYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (dailyDone) Icons.Rounded.CheckCircle else Icons.Rounded.PlayArrow, null,
                        tint = if (dailyDone) Brand.Success else Color(0xFF14110A),
                    )
                }
            }
        }

        // Stats
        SectionHeader("Your stats", Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 10.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Rounded.Timer, Rules.formatDuration(p.totalStudySeconds), "Learning time", Brand.Cyan, Modifier.weight(1f))
            StatTile(Icons.Rounded.LocalFireDepartment, "${p.streak} day${if (p.streak == 1) "" else "s"}", "Current streak", Brand.Warning, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.AutoMirrored.Rounded.MenuBook, "$lessonsDone / $totalLessons", "Lessons done", Brand.PythonBlueBright, Modifier.weight(1f))
            StatTile(Icons.Rounded.Star, "${p.xp} XP", "Total points", Brand.PythonYellow, Modifier.weight(1f))
        }

        // Today goal
        val todaySeconds = Rules.studySecondsToday(s)
        val goalSeconds = (s.profile?.dailyGoalMinutes ?: 20) * 60L
        AppCard(
            Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(todaySeconds.toFloat() / goalSeconds, size = 76.dp, stroke = 8.dp) {
                    Text(
                        "${(todaySeconds * 100 / goalSeconds).coerceAtMost(100)}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = Brand.White,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Today's goal", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                    Text(
                        "${Rules.formatDuration(todaySeconds)} of ${s.profile?.dailyGoalMinutes ?: 20} min studied",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (todaySeconds >= goalSeconds) "Goal reached — amazing work! 🎉" else "Lessons, quizzes and terminal time all count.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (todaySeconds >= goalSeconds) Brand.Success else Brand.TextMuted,
                    )
                }
            }
        }

        // Journey path
        SectionHeader("Learning path", Modifier.padding(start = 20.dp, end = 12.dp, bottom = 10.dp))
        val courseDone = Rules.courseComplete(s)
        val projectsUnlocked = Rules.projectsUnlocked(s)
        val certUnlocked = Rules.certificateUnlocked(s)
        PathStep(
            icon = Icons.Rounded.School, tint = Brand.PythonBlueBright,
            title = "Python Course A–Z", subtitle = "${Content.modules.size} modules  •  ${(Rules.courseFraction(s) * 100).toInt()}% complete",
            fraction = Rules.courseFraction(s), locked = false, done = courseDone,
            onClick = { nav.tab(Routes.COURSE) },
        )
        PathStep(
            icon = Icons.Rounded.Rocket, tint = Brand.Cyan,
            title = "15 Real-World Projects",
            subtitle = if (projectsUnlocked) "${p.completedProjects.size}/${Content.projects.size} projects built" else "Unlocks after you finish the course",
            fraction = Rules.projectFraction(s), locked = !projectsUnlocked, done = Rules.allProjectsDone(s),
            onClick = { nav.to(Routes.PROJECTS) },
        )
        PathStep(
            icon = Icons.Rounded.WorkspacePremium, tint = Brand.PythonYellow,
            title = "Professional Certificate",
            subtitle = if (certUnlocked) "Ready — view & share it" else "Unlocks after all 15 projects",
            fraction = if (certUnlocked) 1f else 0f, locked = !certUnlocked, done = certUnlocked,
            onClick = { nav.to(Routes.CERTIFICATE) },
            last = true,
        )
    }
}

@Composable
private fun ContinueCard(s: AppState, next: Rules.Next, nav: Navigator) {
    val (image, label, title, subtitle, action) = when (next) {
        is Rules.Next.LessonNext -> Quint(
            Content.imageRes(next.module.image), "CONTINUE LEARNING", next.lesson.title,
            "Module ${next.module.number}  •  ${next.module.title}",
        ) { nav.to(Routes.lesson(next.lesson.id)) }
        is Rules.Next.QuizNext -> Quint(
            Content.imageRes(next.module.image), "MODULE QUIZ READY", "${next.module.title} quiz",
            "Pass ${Rules.passMark(next.module)}/${next.module.quiz.size} to complete Module ${next.module.number}",
        ) { nav.to(Routes.quiz(next.module.id)) }
        is Rules.Next.ProjectNext -> Quint(
            Content.imageRes(next.project.image), "NEXT PROJECT", next.project.title,
            "Project ${next.project.number} of ${Content.projects.size}  •  ${next.project.difficulty}",
        ) {
            if (Rules.projectsUnlocked(s)) nav.to(Routes.project(next.project.id)) else nav.to(Routes.PROJECTS)
        }
        Rules.Next.CertificateNext -> Quint(
            R.drawable.certificate_badge, "ALL DONE", "Your certificate is ready",
            "View, save and share your achievement",
        ) { nav.to(Routes.CERTIFICATE) }
    }
    AppCard(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth(),
        onClick = action,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LocalAppColors.current.surfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(image), null, Modifier.size(56.dp), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp), color = Brand.Cyan)
                Text(
                    title, style = MaterialTheme.typography.titleMedium, color = Brand.White,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = when (next) {
                is Rules.Next.LessonNext -> if (s.progress.completedLessons.isEmpty()) "Start learning" else "Continue"
                is Rules.Next.QuizNext -> "Take the quiz"
                is Rules.Next.ProjectNext -> if (Rules.projectsUnlocked(s)) "Open project" else "View projects"
                Rules.Next.CertificateNext -> "View certificate"
            },
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            onClick = action,
        )
    }
}

private data class Quint(
    val image: Int,
    val label: String,
    val title: String,
    val subtitle: String,
    val action: () -> Unit,
)

@Composable
private fun PathStep(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    fraction: Float,
    locked: Boolean,
    done: Boolean,
    onClick: () -> Unit,
    last: Boolean = false,
) {
    val colors = LocalAppColors.current
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (locked) colors.surfaceHigh else tint.copy(alpha = 0.18f))
                    .border(1.5.dp, if (locked) colors.stroke else tint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        locked -> Icons.Rounded.Lock
                        done -> Icons.Rounded.CheckCircle
                        else -> icon
                    },
                    null,
                    tint = if (locked) Brand.TextMuted else if (done) Brand.Success else tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (!last) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(58.dp)
                        .background(if (done) Brand.Success.copy(alpha = 0.6f) else colors.stroke)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        AppCard(
            Modifier
                .weight(1f)
                .padding(bottom = if (last) 0.dp else 12.dp),
            onClick = onClick,
            padding = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Brand.White)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
                }
                if (done && !locked) Icon(Icons.Rounded.EmojiEvents, null, tint = Brand.PythonYellow)
            }
            if (!locked) {
                Spacer(Modifier.height(10.dp))
                GradientProgressBar(fraction, height = 6.dp, brush = if (done) GoldGradient else Brush.horizontalGradient(listOf(tint, Brand.Cyan)))
            }
        }
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "Burning the midnight oil 🌙"
        h < 12 -> "Good morning ☀️"
        h < 17 -> "Good afternoon 👋"
        else -> "Good evening 🌆"
    }
}
