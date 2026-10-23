package com.saif.pymaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Block
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.MainViewModel
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.TrackStudyTime
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.CodeBlock
import com.saif.pymaster.ui.components.GradientProgressBar
import com.saif.pymaster.ui.components.PrimaryButton
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.components.SecondaryButton
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.Fonts
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.personalize
import androidx.compose.runtime.remember

@Composable
fun LessonScreen(
    state: AppState?,
    lessonId: String,
    vm: MainViewModel,
    nav: Navigator,
    onRunCode: (String) -> Unit,
) {
    val s = state ?: return
    val lesson = Content.lesson(lessonId) ?: return
    val module = Content.moduleOfLesson(lessonId) ?: return
    val index = module.lessons.indexOf(lesson)
    val colors = LocalAppColors.current
    val done = Rules.lessonDone(s, lesson.id)
    val nextLesson = module.lessons.getOrNull(index + 1)

    TrackStudyTime(vm)
    LaunchedEffect(lessonId) { vm.openLesson(lessonId) }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar(
            title = lesson.title,
            subtitle = "Module ${module.number}  •  Lesson ${index + 1} of ${module.lessons.size}",
            onBack = nav::back,
        )
        GradientProgressBar((index + 1f) / module.lessons.size, Modifier.padding(horizontal = 20.dp), height = 4.dp)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag("${lesson.minutes} MIN READ", Brand.Cyan)
                Spacer(Modifier.width(8.dp))
                if (done) Tag("COMPLETED", Brand.Success)
            }
            Spacer(Modifier.height(12.dp))
            Text(lesson.summary, style = MaterialTheme.typography.bodyLarge, color = Brand.TextSecondary)
            Spacer(Modifier.height(8.dp))

            lesson.blocks.forEach { block -> LessonBlock(block, s.profile?.name, onRunCode) }

            if (lesson.keyPoints.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                AppCard(Modifier.fillMaxWidth(), border = Brand.PythonYellow.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = Brand.PythonYellow)
                        Spacer(Modifier.width(8.dp))
                        Text("Key points", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    lesson.keyPoints.forEach { point ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Brand.Success, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(10.dp))
                            RichText(point, Brand.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            if (lesson.practice.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                AppCard(Modifier.fillMaxWidth(), border = Brand.Cyan.copy(alpha = 0.35f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Terminal, null, tint = Brand.Cyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Practice challenge", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    RichText(lesson.practice, Brand.TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Open terminal", icon = Icons.Rounded.Terminal, modifier = Modifier.fillMaxWidth()) {
                        onRunCode("# Practice: ${lesson.title}\n# ${lesson.practice.replace("`", "").replace("\n", "\n# ")}\n\n")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Bottom action
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val label: String
            val icon = if (nextLesson == null) Icons.Rounded.Quiz else Icons.AutoMirrored.Rounded.ArrowForward
            label = when {
                !done && nextLesson != null -> "Complete & next lesson"
                !done -> "Complete & take the quiz"
                nextLesson != null -> "Next lesson"
                else -> "Go to module quiz"
            }
            PrimaryButton(label, icon = icon, modifier = Modifier.fillMaxWidth()) {
                if (!done) vm.completeLesson(lesson.id)
                if (nextLesson != null) {
                    nav.back()
                    nav.to(Routes.lesson(nextLesson.id))
                } else {
                    nav.back()
                    nav.to(Routes.quiz(module.id))
                }
            }
        }
    }
}

@Composable
private fun LessonBlock(block: Block, userName: String?, onRunCode: (String) -> Unit) {
    when (block.type) {
        "heading" -> {
            Spacer(Modifier.height(20.dp))
            Text(block.text, style = MaterialTheme.typography.titleLarge, color = Brand.White)
            Spacer(Modifier.height(6.dp))
        }
        "text" -> {
            Spacer(Modifier.height(8.dp))
            RichText(block.text, Brand.White.copy(alpha = 0.9f))
        }
        "list" -> {
            Spacer(Modifier.height(8.dp))
            block.items.forEach { item ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Brand.Cyan)
                    )
                    Spacer(Modifier.width(12.dp))
                    RichText(item, Brand.White.copy(alpha = 0.9f))
                }
            }
        }
        "code" -> {
            Spacer(Modifier.height(14.dp))
            if (block.text.isNotBlank()) {
                Text(block.text, style = MaterialTheme.typography.labelMedium, color = Brand.TextSecondary)
                Spacer(Modifier.height(6.dp))
            }
            val code = remember(block.code, userName) { personalize(block.code, userName) }
            val output = remember(block.output, userName) { personalize(block.output, userName) }
            CodeBlock(code = code, output = output, onRun = { onRunCode(code) })
        }
        "tip" -> {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand.PythonBlue.copy(alpha = 0.16f))
                    .border(1.dp, Brand.PythonBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Rounded.TipsAndUpdates, null, tint = Brand.PythonYellow, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                RichText(block.text, Brand.White)
            }
        }
    }
}

/** Renders text where `backticks` become inline code and **double stars** become bold. */
@Composable
fun RichText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val annotated = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end < 0) {
                        append(text.substring(i)); i = text.length
                    } else {
                        withStyle(SpanStyle(fontFamily = Fonts.Mono, color = Brand.PythonYellow, background = Color(0x1AFFD43B))) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    }
                }
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end < 0) {
                        append(text.substring(i)); i = text.length
                    } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Brand.White)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    }
                }
                else -> {
                    val nextTick = text.indexOf('`', i).let { if (it < 0) text.length else it }
                    val nextBold = text.indexOf("**", i).let { if (it < 0) text.length else it }
                    val end = minOf(nextTick, nextBold)
                    append(text.substring(i, end))
                    i = end
                }
            }
        }
    }
    Text(annotated, style = style, color = color, modifier = modifier)
}
