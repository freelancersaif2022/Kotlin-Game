package com.saif.pymaster.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Project
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.MainViewModel
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.TrackStudyTime
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.CodeBlock
import com.saif.pymaster.ui.components.GoldGradient
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.LockBadge
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.components.SecondaryButton
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.LocalHaptics

private fun difficultyColor(d: String) = when (d) {
    "Beginner" -> Brand.Success
    "Intermediate" -> Brand.PythonYellow
    else -> Brand.Danger
}

@Composable
fun ProjectsScreen(state: AppState?, nav: Navigator) {
    val s = state ?: return
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val unlockedAll = Rules.projectsUnlocked(s)
    val done = s.progress.completedProjects.size

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar("Real-world projects", subtitle = "$done of ${Content.projects.size} completed", onBack = nav::back)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                if (!unlockedAll) {
                    AppCard(Modifier.fillMaxWidth(), border = Brand.PythonYellow.copy(alpha = 0.4f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.projects_locked), null, Modifier.size(96.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Projects are locked", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                                Text(
                                    "Complete all ${Content.modules.size} course modules to unlock 15 real-world Python projects.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Brand.TextSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        GradientProgressBar(Rules.courseFraction(s), height = 6.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Course progress: ${(Rules.courseFraction(s) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Brand.TextSecondary,
                        )
                    }
                } else {
                    AppCard(Modifier.fillMaxWidth(), border = Brand.Cyan.copy(alpha = 0.35f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Build like a pro", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                                Text(
                                    "Each project explains the idea, walks you through every step and gives you the complete working code to run in the terminal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Brand.TextSecondary,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Image(painterResource(R.drawable.certificate_badge), null, Modifier.size(64.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        GradientProgressBar(Rules.projectFraction(s), height = 6.dp, brush = GoldGradient)
                    }
                }
            }
            itemsIndexed(Content.projects, key = { _, p -> p.id }) { i, p ->
                val open = Rules.projectUnlocked(s, i)
                val complete = p.id in s.progress.completedProjects
                ProjectCard(s, p, open, complete) {
                    if (open) nav.to(Routes.project(p.id))
                    else Toast.makeText(
                        context,
                        if (!unlockedAll) "Finish the course to unlock projects" else "Complete project ${i} first",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun ProjectCard(s: AppState, p: Project, open: Boolean, complete: Boolean, onClick: () -> Unit) {
    val stepsDone = s.progress.projectSteps[p.id]?.size ?: 0
    AppCard(
        Modifier.fillMaxWidth(),
        onClick = onClick,
        padding = PaddingValues(0.dp),
        border = if (complete) Brand.Success.copy(alpha = 0.45f) else LocalAppColors.current.stroke,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Image(
                painterResource(Content.imageRes(p.image)), p.title,
                Modifier.fillMaxSize().alpha(if (open) 1f else 0.35f),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC0B0F17)))
            )
            Text(
                "PROJECT ${p.number.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = Brand.White,
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            if (!open) {
                LockBadge(Modifier.align(Alignment.Center), size = 52.dp)
            }
            if (complete) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand.Success)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Brand.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("DONE", style = MaterialTheme.typography.labelSmall, color = Brand.White)
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(p.title, style = MaterialTheme.typography.titleMedium, color = Brand.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(p.tagline, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(p.difficulty, difficultyColor(p.difficulty))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.Schedule, null, tint = Brand.TextMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${p.minutes} min", style = MaterialTheme.typography.labelSmall, color = Brand.TextMuted)
                Spacer(Modifier.weight(1f))
                if (open && !complete) {
                    Text("$stepsDone/${p.steps.size} steps", style = MaterialTheme.typography.labelSmall, color = Brand.Cyan)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    state: AppState?,
    projectId: String,
    vm: MainViewModel,
    nav: Navigator,
    onRunCode: (String) -> Unit,
) {
    val s = state ?: return
    val p = Content.project(projectId) ?: return
    val index = Content.projects.indexOf(p)
    val colors = LocalAppColors.current
    val haptics = LocalHaptics.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val open = Rules.projectUnlocked(s, index)
    val complete = p.id in s.progress.completedProjects
    val stepsDone = s.progress.projectSteps[p.id] ?: emptySet()
    val allSteps = stepsDone.size >= p.steps.size
    TrackStudyTime(vm)

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar("Project ${p.number}", subtitle = p.title, onBack = nav::back)
        if (!open) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(painterResource(R.drawable.projects_locked), null, Modifier.size(200.dp))
                Text("This project is locked", style = MaterialTheme.typography.titleLarge, color = Brand.White)
                Text(
                    "Finish the course and the previous projects to unlock it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Brand.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x3300D1FF), RoundedCornerShape(20.dp))
            ) {
                Image(painterResource(Content.imageRes(p.image)), p.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(p.title, style = MaterialTheme.typography.headlineSmall, color = Brand.White)
                Spacer(Modifier.height(4.dp))
                Text(p.tagline, style = MaterialTheme.typography.bodyMedium, color = Brand.TextSecondary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(p.difficulty, difficultyColor(p.difficulty))
                    Tag("${p.minutes} min", Brand.Cyan)
                    Tag("${p.steps.size} steps", Brand.Purple)
                }
            }
            // Tabs
            Row(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.stroke, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                listOf("Overview", "Build steps", "Full code").forEachIndexed { i, label ->
                    val selected = tab == i
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Brand.PythonBlue.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { haptics?.tick(); tab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Brand.White else Brand.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                when (tab) {
                    0 -> {
                        SectionTitle("About this project")
                        RichText(p.overview, Brand.White.copy(alpha = 0.9f))
                        SectionTitle("Features you will build")
                        p.features.forEach { f ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Rounded.Star, null, tint = Brand.PythonYellow, modifier = Modifier.size(18.dp).padding(top = 3.dp))
                                Spacer(Modifier.width(10.dp))
                                RichText(f, Brand.White.copy(alpha = 0.9f))
                            }
                        }
                        SectionTitle("Python concepts used")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.concepts.forEach { Tag(it, Brand.Cyan) }
                        }
                        if (p.sampleOutput.isNotBlank()) {
                            SectionTitle("Sample run")
                            CodeBlock(code = p.sampleOutput.trimEnd(), title = "terminal — sample output")
                        }
                        Spacer(Modifier.height(16.dp))
                        SecondaryButton("Start building", modifier = Modifier.fillMaxWidth()) { tab = 1 }
                    }
                    1 -> {
                        Text(
                            "Work through every step, then tick it off. Tap ▶ on any snippet to try it in the terminal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.TextSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        GradientProgressBar(stepsDone.size.toFloat() / p.steps.size, height = 6.dp)
                        p.steps.forEachIndexed { i, step ->
                            val checked = i in stepsDone
                            AppCard(
                                Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth(),
                                border = if (checked) Brand.Success.copy(alpha = 0.45f) else colors.stroke,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(if (checked) Brand.Success else Brand.PythonBlue.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("${i + 1}", style = MaterialTheme.typography.labelLarge, color = Brand.White)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(step.title, style = MaterialTheme.typography.titleSmall, color = Brand.White, modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                RichText(step.text, Brand.White.copy(alpha = 0.88f))
                                if (step.code.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    CodeBlock(step.code, title = "step_${i + 1}.py", onRun = { onRunCode(step.code) })
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptics?.tick()
                                            vm.setProjectStep(p.id, i, !checked)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null,
                                        tint = if (checked) Brand.Success else Brand.TextSecondary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (checked) "Step completed" else "Mark step as done",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (checked) Brand.Success else Brand.White,
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            "The complete program. Run it, read it, change it — that's how developers learn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.TextSecondary,
                        )
                        Spacer(Modifier.height(10.dp))
                        CodeBlock(p.code, title = "${p.id}.py", onRun = { onRunCode(p.code) })
                        Spacer(Modifier.height(12.dp))
                        PrimaryButton("Run in terminal", icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth()) {
                            onRunCode(p.code)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (complete) {
                Row(
                    Modifier.fillMaxWidth().height(54.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Brand.PythonYellow)
                    Spacer(Modifier.width(8.dp))
                    Text("Project completed", style = MaterialTheme.typography.titleSmall, color = Brand.White)
                    val nextProject = Content.projects.getOrNull(index + 1)
                    if (nextProject != null) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Next →",
                            style = MaterialTheme.typography.labelLarge,
                            color = Brand.Cyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { nav.back(); nav.to(Routes.project(nextProject.id)) }
                                .padding(8.dp),
                        )
                    } else {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Certificate →",
                            style = MaterialTheme.typography.labelLarge,
                            color = Brand.PythonYellow,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { nav.to(Routes.CERTIFICATE) }
                                .padding(8.dp),
                        )
                    }
                }
            } else {
                PrimaryButton(
                    if (allSteps) "Complete project" else "Finish all steps (${stepsDone.size}/${p.steps.size})",
                    icon = if (allSteps) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                    enabled = allSteps,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    vm.completeProject(p.id)
                    haptics?.success()
                    if (index == Content.projects.lastIndex) nav.to(Routes.CERTIFICATE)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = Brand.White)
    Spacer(Modifier.height(8.dp))
}
