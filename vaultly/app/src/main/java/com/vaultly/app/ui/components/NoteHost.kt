package com.vaultly.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultly.app.Note
import com.vaultly.app.NoteKind
import com.vaultly.app.Notifier
import com.vaultly.app.ui.theme.Palette
import kotlinx.coroutines.delay

/** Shows [Notifier] messages as a floating ink snackbar with a status icon. Tap to dismiss. */
@Composable
fun NoteHost(notifier: Notifier, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<Note?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(notifier) {
        notifier.notes.collect { note ->
            if (visible) { visible = false; delay(160) }
            current = note
            visible = true
        }
    }
    LaunchedEffect(current?.id, visible) {
        if (visible) { delay(3200); visible = false }
    }
    AnimatedVisibility(
        visible = visible && current != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.8f)) { it } + fadeIn(tween(160)),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(200)),
    ) {
        val note = current ?: return@AnimatedVisibility
        val (icon, tint) = when (note.kind) {
            NoteKind.SUCCESS -> Icons.Outlined.Check to Color(0xFF3CC28C)
            NoteKind.INFO -> Icons.Outlined.Info to Palette.GoldLight
            NoteKind.ERROR -> Icons.Outlined.ErrorOutline to Color(0xFFFF6B6B)
        }
        val shape = RoundedCornerShape(20.dp)
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .shadow(18.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(shape)
                    .background(Color(0xFF111A2C))
                    .border(1.dp, Palette.Gold.copy(alpha = 0.22f), shape)
                    .clickable { visible = false }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, Modifier.size(18.dp), tint = tint) }
                Spacer(Modifier.width(12.dp))
                Text(note.message, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
            }
        }
    }
}
