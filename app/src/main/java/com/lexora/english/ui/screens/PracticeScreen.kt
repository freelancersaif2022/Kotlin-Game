package com.lexora.english.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexora.english.R
import com.lexora.english.data.AppState
import com.lexora.english.data.PracticeResult
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.IconTile
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.Pill
import com.lexora.english.ui.components.PrimaryButton
import com.lexora.english.ui.components.SectionTitle
import com.lexora.english.ui.components.StatBlock
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PracticeMode(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val image: Int? = null,
)

val practiceModes = listOf(
    PracticeMode("test", "Grammar Test", "10 mixed questions · timed", Icons.Rounded.Timer, Brand.Blue, R.drawable.practice_test),
    PracticeMode("write", "Writing Lab", "Write & get instant feedback", Icons.Rounded.Edit, Brand.Indigo, R.drawable.practice_writing),
    PracticeMode("speak", "Speaking", "Speak & check pronunciation", Icons.Rounded.Mic, Brand.Pink, R.drawable.practice_speaking),
    PracticeMode("fill", "Fill the Gap", "Type the missing word", Icons.Rounded.Spellcheck, Brand.Teal),
    PracticeMode("build", "Sentence Builder", "Put words in the right order", Icons.Rounded.Extension, Brand.Orange),
    PracticeMode("listen", "Listening", "Hear it, then type it", Icons.Rounded.Headphones, Brand.Sky),
    PracticeMode("cards", "Flashcards", "Learn 40 essential words", Icons.Rounded.Style, Brand.Emerald),
)

@Composable
fun PracticeScreen(state: AppState, nav: Nav, listState: LazyListState) {
    val c = Lex.colors
    val history = state.progress.practiceHistory
    val featured = practiceModes.first()
    val withImages = practiceModes.filter { it.image != null && it != featured }
    val compact = practiceModes.filter { it.image == null }

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
                Text("Practice", style = MaterialTheme.typography.headlineLarge, color = c.text)
                Text("Train every skill like a professional test", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
        }
        item(key = "stats") {
            val avg = if (history.isEmpty()) 0 else history.map { it.score * 100 / it.total.coerceAtLeast(1) }.average().toInt()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatBlock("${history.size}", "Sessions", Icons.Rounded.Timer, Brand.Blue, Modifier.weight(1f))
                StatBlock(if (history.isEmpty()) "—" else "$avg%", "Avg. score", Icons.Rounded.Spellcheck, Brand.Emerald, Modifier.weight(1f))
                StatBlock("${state.progress.knownWords.size}", "Words known", Icons.Rounded.Style, Brand.Indigo, Modifier.weight(1f))
            }
        }
        item(key = "featured") {
            LexCard(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                onClick = { nav.practice(featured.key) },
                padding = PaddingValues(0.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f)
                        .background(c.primarySoft)
                ) {
                    AppImage(featured.image!!, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Pill(
                        "Recommended", color = Color.White, background = Brand.Blue,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    )
                }
                Column(Modifier.padding(16.dp)) {
                    Text(featured.title, style = MaterialTheme.typography.titleLarge, color = c.text)
                    Text("10 questions covering grammar, vocabulary, idioms and spelling. Your time is recorded.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Start test", { nav.practice(featured.key) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward, height = 48.dp)
                }
            }
        }
        item(key = "imageModes") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                withImages.forEach { m ->
                    LexCard(Modifier.weight(1f), onClick = { nav.practice(m.key) }, padding = PaddingValues(0.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                                .background(c.primarySoft)
                        ) {
                            AppImage(m.image!!, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Column(Modifier.padding(12.dp)) {
                            Text(m.title, style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text(m.subtitle, style = MaterialTheme.typography.labelMedium, color = c.textMuted, maxLines = 2)
                        }
                    }
                }
            }
        }
        item(key = "moreTitle") {
            SectionTitle("More practice", Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp))
        }
        items(compact, key = { it.key }) { m ->
            LexCard(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp),
                onClick = { nav.practice(m.key) },
                padding = PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(m.icon, m.color, size = 48.dp, iconSize = 24.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.title, style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    val best = history.filter { it.mode == m.key }.maxOfOrNull { it.score * 100 / it.total.coerceAtLeast(1) }
                    if (best != null) Pill("Best $best%", color = c.success, background = c.successSoft)
                    else Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = c.textMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
        item(key = "historyTitle") {
            SectionTitle("Recent results", Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp))
        }
        if (history.isEmpty()) {
            item(key = "noHistory") {
                Text(
                    "Your practice results will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        } else {
            items(history.take(8), key = { "h" + it.at }) { r -> HistoryRow(r) }
        }
    }
}

@Composable
private fun HistoryRow(r: PracticeResult) {
    val c = Lex.colors
    val mode = practiceModes.firstOrNull { it.key == r.mode }
    val percent = r.score * 100 / r.total.coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.cardAlt)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(mode?.icon ?: Icons.Rounded.Timer, mode?.color ?: Brand.Blue, size = 38.dp, iconSize = 19.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, style = MaterialTheme.typography.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.US).format(Instant.ofEpochMilli(r.at).atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted,
            )
        }
        Text(
            "${r.score}/${r.total}",
            style = MaterialTheme.typography.titleSmall,
            color = when {
                percent >= 80 -> c.success
                percent >= 50 -> c.primary
                else -> c.error
            },
        )
    }
}
