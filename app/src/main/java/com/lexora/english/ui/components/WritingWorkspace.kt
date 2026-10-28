package com.lexora.english.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.WritingCoach
import com.lexora.english.util.rememberHaptics

/** Text editor with live word count and offline feedback. [onChecked] receives each analysis. */
@Composable
fun WritingWorkspace(
    title: String,
    prompt: String,
    minWords: Int,
    keywords: List<String>,
    stateKey: String,
    modifier: Modifier = Modifier,
    submitLabel: String = "Check my writing",
    onChecked: (WritingCoach.Report, String) -> Unit,
) {
    val c = Lex.colors
    val haptics = rememberHaptics()
    val focus = LocalFocusManager.current
    var text by rememberSaveable(stateKey) { mutableStateOf("") }
    var report by remember(stateKey) { mutableStateOf<WritingCoach.Report?>(null) }
    val words = remember(text) { Regex("[A-Za-z']+").findAll(text).count() }

    Column(modifier) {
        LexCard(Modifier.fillMaxWidth(), color = c.primarySoft, elevated = false) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.height(4.dp))
            Text(prompt, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            if (keywords.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    keywords.forEach { k ->
                        val used = text.lowercase().contains(k.lowercase())
                        Pill(
                            k,
                            color = if (used) c.success else c.primary,
                            background = if (used) c.successSoft else c.card,
                            icon = if (used) Icons.Rounded.CheckCircle else null,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 3000) text = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            placeholder = { Text("Start writing here…", color = c.textMuted) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text),
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.primary,
                unfocusedBorderColor = c.border,
                focusedContainerColor = c.card,
                unfocusedContainerColor = c.card,
                cursorColor = c.primary,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$words / $minWords words",
                style = MaterialTheme.typography.labelLarge,
                color = if (words >= minWords) c.success else c.textMuted,
                modifier = Modifier.weight(1f),
            )
            LinearProgress(words / minWords.toFloat(), Modifier.width(120.dp), height = 6.dp, color = if (words >= minWords) c.success else c.primary)
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            submitLabel,
            onClick = {
                focus.clearFocus()
                val r = WritingCoach.analyze(text, minWords, keywords)
                report = r
                if (r.meetsMinimum && r.score >= 50) haptics.success() else haptics.error()
                onChecked(r, text)
            },
            enabled = words > 0,
            icon = Icons.Rounded.AutoAwesome,
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(report != null, enter = fadeIn() + expandVertically()) {
            report?.let { r -> WritingReportCard(r, Modifier.padding(top = 14.dp)) }
        }
    }
}

@Composable
fun WritingReportCard(r: WritingCoach.Report, modifier: Modifier = Modifier) {
    val c = Lex.colors
    val color = when {
        r.score >= 80 -> c.success
        r.score >= 50 -> c.accent
        else -> c.error
    }
    LexCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(r.score / 100f, 72.dp, 8.dp, color = color) {
                Text("${r.score}", style = MaterialTheme.typography.titleLarge, color = c.text)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        r.score >= 85 -> "Excellent writing!"
                        r.score >= 70 -> "Great job"
                        r.score >= 50 -> "Good effort"
                        else -> "Keep practising"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text,
                )
                Text(
                    "${r.words} words · ${r.sentences} sentences · ${r.uniqueRatio}% unique words",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        r.feedback.forEach { f ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Icon(
                    if (f.good) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    null,
                    tint = if (f.good) c.success else c.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(f.text, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        }
        if (r.score < 50 || !r.meetsMinimum) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Reach the word count and a score of 50+ to submit.",
                style = MaterialTheme.typography.labelMedium,
                color = c.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.errorSoft)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
