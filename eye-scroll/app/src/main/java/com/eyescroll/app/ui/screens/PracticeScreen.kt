package com.eyescroll.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyescroll.app.Readiness
import com.eyescroll.app.gesture.TrackingBus
import com.eyescroll.app.service.GestureTrackingService
import com.eyescroll.app.ui.components.GlassCard
import com.eyescroll.app.ui.components.GradientButton
import com.eyescroll.app.ui.components.StatusPill
import com.eyescroll.app.ui.theme.EsColors

private data class Post(val author: String, val title: String, val body: String, val colors: List<Color>)

private val samplePosts: List<Post> = run {
    val authors = listOf("Travel Daily", "Tech Weekly", "Food Lab", "Nature Mag", "Design Notes", "Fitness Hub", "Space News", "City Guide")
    val titles = listOf(
        "10 hidden beaches you have to see",
        "The new wave of foldable phones",
        "A 15-minute pasta that tastes like a restaurant",
        "Why forests are getting quieter",
        "Minimal desk setups for focus",
        "A morning stretch routine that sticks",
        "What the latest Mars images tell us",
        "The best night markets this season",
    )
    val palettes = listOf(
        listOf(Color(0xFF3DDCFF), Color(0xFF4F8CFF)),
        listOf(Color(0xFF7C5CFF), Color(0xFFFF7AC6)),
        listOf(Color(0xFFFFB547), Color(0xFFFF5C7A)),
        listOf(Color(0xFF2EE6A6), Color(0xFF3DDCFF)),
    )
    List(40) { i ->
        Post(
            author = authors[i % authors.size],
            title = "${i + 1}. ${titles[i % titles.size]}",
            body = "Read the text, then flick your finger in the air in front of the camera to move on. Point & hold mode keeps the feed moving while your finger stays up or down.",
            colors = palettes[i % palettes.size],
        )
    }
}

/** A realistic feed to practise air scrolling without leaving the app. */
@Composable
fun PracticeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val status by TrackingBus.status.collectAsStateWithLifecycle()
    val setup = rememberSetupState().state.value

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = EsColors.Text) }
                Text("Practice feed", style = MaterialTheme.typography.titleLarge, color = EsColors.Text, modifier = Modifier.weight(1f))
                StatusPill(
                    if (status.isActive) "Air scroll ON" else "Off",
                    if (status.isActive) EsColors.Success else EsColors.TextMuted,
                    pulsing = status.isActive,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 40.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                item {
                    GlassCard(Modifier.fillMaxWidth(), highlighted = true, padding = 16.dp) {
                        Text("How to practise", style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (!setup.accessibility) "Turn on the AirScroll accessibility service first (Setup), otherwise scrolling can't happen."
                            else "Tap Start below, then flick your finger up or down in the air in front of the front camera.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (!setup.accessibility) EsColors.Warning else EsColors.TextMuted,
                        )
                    }
                }
                items(samplePosts) { post -> PostCard(post) }
            }
        }

        GradientButton(
            text = if (status.isActive) "Stop air scrolling" else "Start air scrolling",
            onClick = {
                if (status.isActive) GestureTrackingService.stop(context)
                else if (Readiness.canStartTracking(context)) GestureTrackingService.start(context)
            },
            enabled = status.isActive || setup.camera,
            icon = Icons.Filled.PlayArrow,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun PostCard(post: Post) {
    GlassCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(post.colors)),
                contentAlignment = Alignment.Center,
            ) { Text(post.author.take(1), style = MaterialTheme.typography.titleMedium, color = Color.White) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(post.author, style = MaterialTheme.typography.titleMedium, color = EsColors.Text)
                Text("2h ago", style = MaterialTheme.typography.labelSmall, color = EsColors.TextMuted)
            }
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(
                Brush.linearGradient(post.colors.map { it.copy(alpha = 0.75f) }),
            ),
        ) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(14.dp).clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 6.dp),
            ) { Text("Photo", style = MaterialTheme.typography.labelSmall, color = Color.White) }
        }
        Column(Modifier.padding(14.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleLarge, color = EsColors.Text)
            Spacer(Modifier.height(6.dp))
            Text(post.body, style = MaterialTheme.typography.bodyMedium, color = EsColors.TextMuted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Icon(Icons.Filled.Favorite, null, tint = EsColors.Danger.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                Icon(Icons.Filled.Share, null, tint = EsColors.TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}
