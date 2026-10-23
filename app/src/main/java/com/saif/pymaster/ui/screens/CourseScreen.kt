package com.saif.pymaster.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Module
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.GoldGradient
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.LockBadge
import com.saif.pymaster.ui.components.ProgressRing
import com.saif.pymaster.ui.components.SectionHeader
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors

@Composable
fun CourseScreen(state: AppState?, nav: Navigator) {
    val s = state ?: return
    val colors = LocalAppColors.current
    val modules = Content.modules
    val courseFraction = Rules.courseFraction(s)
    val modulesDone = modules.count { Rules.moduleComplete(s, it) }
    val running = modules.withIndex()
        .filter { (i, m) -> Rules.moduleUnlocked(s, i) && !Rules.moduleComplete(s, m) }
        .map { it.value }
        .take(3)
    val scores = s.progress.quizScores
    val avgScore = if (scores.isEmpty()) 0 else scores.entries.sumOf { (id, v) ->
        val total = Content.module(id)?.quiz?.size ?: 1
        v * 100 / total
    } / scores.size

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Banner
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Image(
                painterResource(R.drawable.course_header), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to Color(0x660B0F17), 0.55f to Color(0x330B0F17), 1f to colors.background))
            )
            Column(
                Modifier
                    .statusBarsPadding()
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text("Python Course", style = MaterialTheme.typography.headlineMedium, color = Brand.White)
                Text(
                    "${modules.size} modules  •  ${Content.totalLessons} lessons  •  ${modules.sumOf { it.quiz.size }} quiz questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.TextSecondary,
                )
            }
        }

        // Overview with donut chart
        AppCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(courseFraction, size = 112.dp, stroke = 12.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(courseFraction * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = Brand.White)
                        Text("course", style = MaterialTheme.typography.labelSmall, color = Brand.TextSecondary)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Legend(Brand.PythonBlueBright, "Modules", "$modulesDone / ${modules.size}")
                    Legend(Brand.Cyan, "Lessons", "${s.progress.completedLessons.size} / ${Content.totalLessons}")
                    Legend(Brand.PythonYellow, "Quizzes passed", "${s.progress.passedQuizzes.size} / ${modules.size}")
                    Legend(Brand.Success, "Avg. quiz score", "$avgScore%")
                }
            }
        }

        // Weekly chart
        SectionHeader("Study time this week", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
        WeeklyChart(s)

        // Module chart
        SectionHeader("Module progress", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
        ModuleChart(s, modules)

        // Running courses
        if (running.isNotEmpty()) {
            SectionHeader("Running now", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                running.forEach { m ->
                    AppCard(
                        Modifier.width(250.dp),
                        onClick = { nav.to(Routes.module(m.id)) },
                        border = Brand.Cyan.copy(alpha = 0.35f),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(Content.imageRes(m.image)), null, Modifier.size(52.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("MODULE ${m.number}", style = MaterialTheme.typography.labelSmall, color = Brand.Cyan)
                                Text(m.title, style = MaterialTheme.typography.titleSmall, color = Brand.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        GradientProgressBar(Rules.moduleFraction(s, m), height = 6.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${Rules.lessonsDone(s, m)}/${m.lessons.size} lessons  •  ${(Rules.moduleFraction(s, m) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.TextSecondary,
                        )
                    }
                }
            }
        }

        // All modules
        SectionHeader("All modules", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
        modules.forEachIndexed { i, m ->
            ModuleRow(s, m, i, nav)
        }

        // Projects
        SectionHeader("Real-world projects", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
        val projectsOpen = Rules.projectsUnlocked(s)
        AppCard(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            onClick = { nav.to(Routes.PROJECTS) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(if (projectsOpen) R.drawable.success_celebration else R.drawable.projects_locked), null,
                    Modifier.size(88.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("15 Python projects", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                    Text(
                        if (projectsOpen) "${s.progress.completedProjects.size}/${Content.projects.size} completed — keep building!"
                        else "Locked. Complete all ${modules.size} modules to unlock real-world projects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    GradientProgressBar(if (projectsOpen) Rules.projectFraction(s) else courseFraction, height = 6.dp)
                }
            }
        }

        // Certificate
        Spacer(Modifier.height(12.dp))
        val certOpen = Rules.certificateUnlocked(s)
        AppCard(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            onClick = { nav.to(Routes.CERTIFICATE) },
            border = Brand.PythonYellow.copy(alpha = 0.35f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(painterResource(R.drawable.certificate_seal), null, Modifier.size(72.dp).alpha(if (certOpen) 1f else 0.5f))
                    if (!certOpen) LockBadge(size = 24.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Professional certificate", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                    Text(
                        if (certOpen) "Your certificate is ready to view and share."
                        else "Earn it by completing the course and all 15 projects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                    )
                }
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Brand.PythonYellow)
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Brand.White)
    }
}

@Composable
private fun WeeklyChart(s: AppState) {
    val week = Rules.lastWeek(s)
    val maxSeconds = (week.maxOf { it.second }).coerceAtLeast(600L)
    val total = week.sumOf { it.second }
    AppCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Rules.formatDuration(total), style = MaterialTheme.typography.titleLarge, color = Brand.White)
                Text("total in the last 7 days", style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
            }
            Tag("${s.progress.streak} day streak 🔥", Brand.Warning)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            week.forEachIndexed { i, (label, secs) ->
                val isToday = i == week.lastIndex
                val target = secs.toFloat() / maxSeconds
                val animated by animateFloatAsState(target, tween(900), label = "bar$i")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        if (secs >= 60) "${secs / 60}m" else if (secs > 0) "<1m" else "",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Brand.TextSecondary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .fillMaxHeight(animated.coerceAtLeast(0.03f) * 0.82f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(
                                if (isToday) Brush.verticalGradient(listOf(Brand.PythonYellow, Brand.Warning))
                                else Brush.verticalGradient(listOf(Brand.Cyan, Brand.PythonBlue))
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isToday) "Today" else label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isToday) Brand.PythonYellow else Brand.TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleChart(s: AppState, modules: List<Module>) {
    AppCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            modules.forEach { m ->
                val f = Rules.moduleFraction(s, m)
                val animated by animateFloatAsState(f, tween(900), label = "m${m.number}")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x12FFFFFF))
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animated.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (f >= 1f) GoldGradient
                                    else Brush.verticalGradient(listOf(Brand.Cyan, Brand.PythonBlue))
                                )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "M${m.number}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Brand.TextMuted,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Brand.Cyan))
                Spacer(Modifier.width(6.dp))
                Text("In progress", style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Brand.PythonYellow))
                Spacer(Modifier.width(6.dp))
                Text("Completed", style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
            }
        }
    }
}

@Composable
private fun ModuleRow(s: AppState, m: Module, index: Int, nav: Navigator) {
    val unlocked = Rules.moduleUnlocked(s, index)
    val complete = Rules.moduleComplete(s, m)
    val colors = LocalAppColors.current
    AppCard(
        Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth(),
        onClick = { nav.to(Routes.module(m.id)) },
        padding = PaddingValues(14.dp),
        border = if (complete) Brand.Success.copy(alpha = 0.35f) else colors.stroke,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceHigh)
                        .border(1.dp, colors.stroke, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(Content.imageRes(m.image)), null,
                        Modifier.size(54.dp).alpha(if (unlocked) 1f else 0.35f),
                    )
                }
                if (!unlocked) LockBadge(size = 22.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "MODULE ${m.number}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = if (unlocked) Brand.Cyan else Brand.TextMuted,
                )
                Text(m.title, style = MaterialTheme.typography.titleSmall, color = Brand.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    m.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Brand.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GradientProgressBar(
                        Rules.moduleFraction(s, m),
                        Modifier.weight(1f),
                        height = 5.dp,
                        brush = if (complete) GoldGradient else Brush.horizontalGradient(listOf(Brand.PythonBlue, Brand.Cyan)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${Rules.lessonsDone(s, m)}/${m.lessons.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand.TextSecondary,
                    )
                }
            }
            if (complete) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.CheckCircle, "Completed", tint = Brand.Success)
            }
        }
    }
}

