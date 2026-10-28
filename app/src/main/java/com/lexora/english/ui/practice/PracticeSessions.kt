package com.lexora.english.ui.practice

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexora.english.content.PracticeContent
import com.lexora.english.content.WordCard
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.StudyTimer
import com.lexora.english.ui.components.CircleIconButton
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.LinearProgress
import com.lexora.english.ui.components.Pill
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.QuizResult
import com.lexora.english.ui.components.QuizRunner
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.components.SecondaryButton
import com.lexora.english.ui.components.WritingWorkspace
import com.lexora.english.ui.screens.practiceModes
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.Speaker
import com.lexora.english.util.rememberHaptics
import kotlinx.coroutines.delay

@Composable
fun PracticeSessionScreen(mode: String, state: AppState, nav: Nav) {
    val c = Lex.colors
    val info = practiceModes.firstOrNull { it.key == mode }
    StudyTimer()
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .imePadding()
    ) {
        when (mode) {
            "test" -> GrammarTest(nav)
            "write" -> WritingLab(nav)
            "speak" -> SpeakingPractice(nav)
            "fill" -> FillTheGap(nav)
            "build" -> SentenceBuilder(nav)
            "listen" -> ListeningPractice(nav)
            "cards" -> Flashcards(state, nav)
            else -> ScreenHeader(info?.title ?: "Practice", { nav.back() })
        }
    }
}

/** Common end-of-session screen that also records the result. */
@Composable
private fun SessionResult(mode: String, title: String, score: Int, total: Int, nav: Nav, onRestart: () -> Unit) {
    val repo = AppRepository.get(LocalContext.current)
    LaunchedEffect(Unit) { repo.recordPractice(mode, title, score, total) }
    val percent = if (total == 0) 0 else score * 100 / total
    QuizResult(
        correct = score, total = total, passed = percent >= 60,
        title = when {
            percent >= 90 -> "Outstanding!"
            percent >= 70 -> "Great work!"
            percent >= 50 -> "Good effort"
            else -> "Keep practising"
        },
        message = "Your result has been saved to your practice history.",
        primaryText = "Practice again",
        onPrimary = onRestart,
        secondaryText = "Back to practice",
        onSecondary = { nav.back() },
    )
}

// ---------------------------------------------------------------- Grammar test

@Composable
private fun GrammarTest(nav: Nav) {
    var round by rememberSaveable { mutableIntStateOf(0) }
    var result by rememberSaveable(round) { mutableStateOf<IntArray?>(null) }
    val questions = remember(round) { PracticeContent.testQuestions(10) }
    var seconds by rememberSaveable(round) { mutableLongStateOf(0L) }
    LaunchedEffect(round, result) {
        while (result == null) {
            delay(1000)
            seconds++
        }
    }
    ScreenHeader("Grammar Test", { nav.back() }, subtitle = "Time: %d:%02d".format(seconds / 60, seconds % 60))
    val r = result
    if (r == null) {
        QuizRunner(questions, Modifier.fillMaxSize()) { correct, total, _ -> result = intArrayOf(correct, total) }
    } else {
        SessionResult("test", "Grammar Test · ${seconds / 60}m ${seconds % 60}s", r[0], r[1], nav) { round++ }
    }
}

// ---------------------------------------------------------------- Writing lab

@Composable
private fun WritingLab(nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    if (selected < 0) {
        ScreenHeader("Writing Lab", { nav.back() }, subtitle = "Choose a topic")
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(PracticeContent.writingPrompts.size) { i ->
                val p = PracticeContent.writingPrompts[i]
                LexCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    onClick = { selected = i },
                    padding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.title, style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text(p.prompt, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 2)
                        }
                        Spacer(Modifier.width(8.dp))
                        Pill("${p.minWords}+ words")
                    }
                }
            }
        }
    } else {
        val p = PracticeContent.writingPrompts[selected]
        ScreenHeader(p.title, { selected = -1 }, subtitle = "Writing Lab")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            WritingWorkspace(
                title = p.title,
                prompt = p.prompt,
                minWords = p.minWords,
                keywords = p.keywords,
                stateKey = "lab_$selected",
            ) { report, _ ->
                if (report.words > 0) repo.recordPractice("write", "Writing: ${p.title}", report.score, 100)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------- Speaking

@Composable
private fun SpeakingPractice(nav: Nav) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    var round by rememberSaveable { mutableIntStateOf(0) }
    val sentences = remember(round) { PracticeContent.speaking.shuffled().take(5) }
    var index by rememberSaveable(round) { mutableIntStateOf(0) }
    val scores = remember(round) { mutableStateListOf<Int>() }
    var heard by rememberSaveable(round, index) { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val target = sentences.getOrNull(index)

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (text != null && target != null) {
                heard = text
                val s = PracticeContent.similarity(target, text)
                if (scores.size > index) scores[index] = maxOf(scores[index], s) else scores.add(s)
                if (s >= 70) haptics.success() else haptics.error()
            }
        }
    }

    ScreenHeader("Speaking", { nav.back() }, subtitle = "Sentence ${minOf(index + 1, sentences.size)} of ${sentences.size}")
    if (target == null) {
        val passed = scores.count { it >= 70 }
        SessionResult("speak", "Speaking practice", passed, sentences.size, nav) { round++ }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgress(index / sentences.size.toFloat())
        Spacer(Modifier.height(24.dp))
        LexCard(Modifier.fillMaxWidth()) {
            Text("Say this sentence:", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
            Text(target, style = MaterialTheme.typography.headlineSmall, color = c.text)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Listen", { Speaker.speak(target) }, Modifier.weight(1f), icon = Icons.AutoMirrored.Rounded.VolumeUp, height = 46.dp)
                SecondaryButton(
                    "Slow",
                    {
                        val old = Speaker.rate
                        Speaker.rate = 0.6f
                        Speaker.speak(target)
                        Speaker.rate = old
                    },
                    Modifier.weight(1f),
                    icon = Icons.Rounded.SlowMotionVideo,
                    height = 46.dp,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        val pulse by animateFloatAsState(if (heard == null) 1f else 0.92f, tween(300), label = "mic")
        Box(
            Modifier
                .size(112.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Brand.Pink, Brand.Indigo)))
                .clickable {
                    error = null
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, target)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    }
                    try {
                        recognizer.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        error = "Speech recognition isn't available on this phone. Install or enable Google voice typing."
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Mic, "Speak", tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Tap the microphone and speak", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.error, textAlign = TextAlign.Center)
        }
        heard?.let { said ->
            val s = scores.getOrNull(index) ?: 0
            Spacer(Modifier.height(24.dp))
            LexCard(Modifier.fillMaxWidth(), color = if (s >= 70) c.successSoft else c.accentSoft, elevated = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$s%", style = MaterialTheme.typography.headlineMedium, color = if (s >= 70) c.success else Color(0xFFB7791F))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(if (s >= 90) "Excellent pronunciation!" else if (s >= 70) "Good match!" else "Try again for a better match", style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text("We heard: \"$said\"", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            if (index == sentences.lastIndex) "Finish" else if (heard == null) "Skip" else "Next sentence",
            {
                if (scores.size <= index) scores.add(0)
                index++
            },
            Modifier.fillMaxWidth(),
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
        )
    }
}

// ---------------------------------------------------------------- Fill the gap

@Composable
private fun FillTheGap(nav: Nav) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    val focus = LocalFocusManager.current
    var round by rememberSaveable { mutableIntStateOf(0) }
    val items = remember(round) { PracticeContent.fillBlanks.shuffled().take(8) }
    var index by rememberSaveable(round) { mutableIntStateOf(0) }
    var correct by rememberSaveable(round) { mutableIntStateOf(0) }
    var input by rememberSaveable(round, index) { mutableStateOf("") }
    var checked by rememberSaveable(round, index) { mutableStateOf<Boolean?>(null) }

    ScreenHeader("Fill the Gap", { nav.back() }, subtitle = "Question ${minOf(index + 1, items.size)} of ${items.size}")
    val item = items.getOrNull(index)
    if (item == null) {
        SessionResult("fill", "Fill the Gap", correct, items.size, nav) { round++ }
        return
    }
    fun check() {
        if (input.isBlank() || checked != null) return
        val ok = item.answers.any { PracticeContent.normalize(it) == PracticeContent.normalize(input) }
        checked = ok
        if (ok) {
            correct++
            haptics.success()
        } else haptics.error()
        focus.clearFocus()
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        LinearProgress(index / items.size.toFloat())
        Spacer(Modifier.height(24.dp))
        LexCard(Modifier.fillMaxWidth()) {
            Text("Complete the sentence", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            Spacer(Modifier.height(10.dp))
            Text(
                "${item.before} ____ ${item.after}",
                style = MaterialTheme.typography.headlineSmall,
                color = c.text,
            )
            Spacer(Modifier.height(10.dp))
            Pill("Hint: ${item.hint}", color = c.textSecondary, background = c.cardAlt)
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { if (checked == null && it.length <= 40) input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type the missing word(s)", color = c.textMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(color = c.text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { check() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = when (checked) { true -> c.success; false -> c.error; null -> c.primary },
                unfocusedBorderColor = when (checked) { true -> c.success; false -> c.error; null -> c.border },
                focusedContainerColor = c.card,
                unfocusedContainerColor = c.card,
                cursorColor = c.primary,
            ),
        )
        checked?.let { ok ->
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (ok) c.successSoft else c.errorSoft)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel, null, tint = if (ok) c.success else c.error)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (ok) "Correct!" else "Not quite", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text(
                        "${item.before} ${item.answers.first()} ${item.after}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        if (checked == null) {
            PrimaryButton("Check answer", { check() }, Modifier.fillMaxWidth(), enabled = input.isNotBlank())
        } else {
            PrimaryButton(if (index == items.lastIndex) "See results" else "Next", { index++ }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        }
    }
}

// ---------------------------------------------------------------- Sentence builder

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceBuilder(nav: Nav) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    var round by rememberSaveable { mutableIntStateOf(0) }
    val items = remember(round) { PracticeContent.sentences.shuffled().take(6) }
    var index by rememberSaveable(round) { mutableIntStateOf(0) }
    var correct by rememberSaveable(round) { mutableIntStateOf(0) }
    val item = items.getOrNull(index)

    ScreenHeader("Sentence Builder", { nav.back() }, subtitle = "Sentence ${minOf(index + 1, items.size)} of ${items.size}")
    if (item == null) {
        SessionResult("build", "Sentence Builder", correct, items.size, nav) { round++ }
        return
    }
    val words = remember(round, index) { item.sentence.split(" ").withIndex().toList().shuffled() }
    val chosen = remember(round, index) { mutableStateListOf<Int>() }
    var checked by remember(round, index) { mutableStateOf<Boolean?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        LinearProgress(index / items.size.toFloat())
        Spacer(Modifier.height(20.dp))
        Text("Build the sentence", style = MaterialTheme.typography.headlineSmall, color = c.text)
        Text(item.meaning, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        Spacer(Modifier.height(16.dp))
        // Answer area
        FlowRow(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    when (checked) {
                        true -> c.successSoft
                        false -> c.errorSoft
                        null -> c.cardAlt
                    }
                )
                .border(1.dp, c.border, RoundedCornerShape(18.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (chosen.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TouchApp, null, tint = c.textMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tap the words below in the right order", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
            }
            chosen.forEach { wi ->
                WordChip(words.first { it.index == wi }.value, selected = true) {
                    if (checked == null) {
                        chosen.remove(wi)
                        haptics.tick()
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            words.forEach { w ->
                val used = w.index in chosen
                WordChip(w.value, selected = false, dimmed = used) {
                    if (!used && checked == null) {
                        chosen.add(w.index)
                        haptics.tick()
                    }
                }
            }
        }
        if (checked == false) {
            Spacer(Modifier.height(16.dp))
            Text("Correct order: ${item.sentence}.", style = MaterialTheme.typography.bodyMedium, color = c.text)
        }
        Spacer(Modifier.height(28.dp))
        when (checked) {
            null -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Reset", { chosen.clear() }, Modifier.weight(1f), icon = Icons.Rounded.Refresh)
                PrimaryButton(
                    "Check",
                    {
                        val built = chosen.joinToString(" ") { wi -> words.first { it.index == wi }.value }
                        val ok = PracticeContent.normalize(built) == PracticeContent.normalize(item.sentence)
                        checked = ok
                        if (ok) {
                            correct++
                            haptics.success()
                            Speaker.speak(item.sentence)
                        } else haptics.error()
                    },
                    Modifier.weight(1.4f),
                    enabled = chosen.size == words.size,
                )
            }
            else -> PrimaryButton(if (index == items.lastIndex) "See results" else "Next sentence", { index++ }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        }
    }
}

@Composable
private fun WordChip(text: String, selected: Boolean, dimmed: Boolean = false, onClick: () -> Unit) {
    val c = Lex.colors
    Box(
        Modifier
            .graphicsLayer { alpha = if (dimmed) 0.3f else 1f }
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.primary else c.card)
            .border(1.dp, if (selected) c.primary else c.border, RoundedCornerShape(12.dp))
            .clickable(enabled = !dimmed, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = if (selected) Color.White else c.text)
    }
}

// ---------------------------------------------------------------- Listening (dictation)

@Composable
private fun ListeningPractice(nav: Nav) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    val focus = LocalFocusManager.current
    var round by rememberSaveable { mutableIntStateOf(0) }
    val items = remember(round) { PracticeContent.listening.shuffled().take(5) }
    var index by rememberSaveable(round) { mutableIntStateOf(0) }
    var correct by rememberSaveable(round) { mutableIntStateOf(0) }
    var input by rememberSaveable(round, index) { mutableStateOf("") }
    var score by rememberSaveable(round, index) { mutableStateOf<Int?>(null) }
    val target = items.getOrNull(index)

    ScreenHeader("Listening", { nav.back() }, subtitle = "Sentence ${minOf(index + 1, items.size)} of ${items.size}")
    if (target == null) {
        SessionResult("listen", "Listening dictation", correct, items.size, nav) { round++ }
        return
    }
    LaunchedEffect(round, index) {
        delay(500)
        Speaker.speak(target)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgress(index / items.size.toFloat())
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                Icons.AutoMirrored.Rounded.VolumeUp, "Play", { Speaker.speak(target) },
                tint = Color.White, background = Brand.Blue, size = 84.dp,
            )
            CircleIconButton(
                Icons.Rounded.SlowMotionVideo, "Play slowly",
                {
                    val old = Speaker.rate
                    Speaker.rate = 0.6f
                    Speaker.speak(target)
                    Speaker.rate = old
                },
                tint = Brand.Blue, background = c.primarySoft, size = 60.dp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Listen and type exactly what you hear", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { if (score == null && it.length <= 200) input = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp),
            placeholder = { Text("Type the sentence…", color = c.textMuted) },
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.primary,
                unfocusedBorderColor = c.border,
                focusedContainerColor = c.card,
                unfocusedContainerColor = c.card,
                cursorColor = c.primary,
            ),
        )
        score?.let { s ->
            Spacer(Modifier.height(14.dp))
            LexCard(Modifier.fillMaxWidth(), color = if (s >= 80) c.successSoft else c.accentSoft, elevated = false) {
                Text("$s% match", style = MaterialTheme.typography.titleMedium, color = if (s >= 80) c.success else Color(0xFFB7791F), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(target, style = MaterialTheme.typography.bodyLarge, color = c.text)
            }
        }
        Spacer(Modifier.height(24.dp))
        if (score == null) {
            PrimaryButton(
                "Check",
                {
                    focus.clearFocus()
                    val s = PracticeContent.similarity(target, input)
                    score = s
                    if (s >= 80) {
                        correct++
                        haptics.success()
                    } else haptics.error()
                },
                Modifier.fillMaxWidth(),
                enabled = input.isNotBlank(),
            )
        } else {
            PrimaryButton(if (index == items.lastIndex) "See results" else "Next sentence", { index++ }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        }
    }
}

// ---------------------------------------------------------------- Flashcards

@Composable
private fun Flashcards(state: AppState, nav: Nav) {
    val c = Lex.colors
    val repo = AppRepository.get(LocalContext.current)
    val haptics = rememberHaptics()
    var round by rememberSaveable { mutableIntStateOf(0) }
    val deck = remember(round) {
        val known = state.progress.knownWords
        val (unknown, knownCards) = PracticeContent.words.partition { it.word !in known }
        (unknown.shuffled() + knownCards.shuffled()).take(10)
    }
    var index by rememberSaveable(round) { mutableIntStateOf(0) }
    var knownCount by rememberSaveable(round) { mutableIntStateOf(0) }
    var flipped by rememberSaveable(round, index) { mutableStateOf(false) }

    ScreenHeader(
        "Flashcards", { nav.back() },
        subtitle = "${state.progress.knownWords.size}/${PracticeContent.words.size} words known",
    )
    val card = deck.getOrNull(index)
    if (card == null) {
        SessionResult("cards", "Flashcards", knownCount, deck.size, nav) { round++ }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgress(index / deck.size.toFloat())
        Spacer(Modifier.height(10.dp))
        Text("Card ${index + 1} of ${deck.size}", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
        Spacer(Modifier.height(16.dp))
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
            label = "card",
        ) { i ->
            val current = deck.getOrNull(i) ?: card
            FlipCard(current, flipped && i == index) { flipped = !flipped; haptics.tick() }
        }
        Spacer(Modifier.height(12.dp))
        Text("Tap the card to flip it", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                "Still learning",
                {
                    repo.setWordKnown(card.word, false)
                    haptics.tick()
                    index++
                },
                Modifier.weight(1f),
            )
            PrimaryButton(
                "I know it",
                {
                    repo.setWordKnown(card.word, true)
                    knownCount++
                    haptics.success()
                    index++
                },
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FlipCard(card: WordCard, flipped: Boolean, onClick: () -> Unit) {
    val c = Lex.colors
    val density = LocalDensity.current
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(450), label = "flip")
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            }
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (rotation <= 90f) androidx.compose.ui.graphics.Brush.linearGradient(listOf(Brand.Blue, Brand.Indigo))
                else androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.card, c.card))
            )
            .border(1.dp, c.border, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rotation <= 90f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.word, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(card.type, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.VolumeUp, "Listen", { Speaker.speak(card.word) },
                    tint = Color.White, background = Color.White.copy(alpha = 0.2f),
                )
            }
        } else {
            Column(
                Modifier.graphicsLayer { rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(card.meaning, style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text("\"${card.example}\"", style = MaterialTheme.typography.bodyLarge, color = c.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.VolumeUp, "Listen", { Speaker.speak(card.example) },
                    tint = c.primary, background = c.primarySoft,
                )
            }
        }
    }
}
