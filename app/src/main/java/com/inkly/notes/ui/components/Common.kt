package com.inkly.notes.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inkly.notes.R
import com.inkly.notes.ui.theme.Brand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val BrandGradient = Brush.linearGradient(listOf(Brand.IndigoLight, Brand.Indigo, Brand.IndigoDeep))

/** The Inkly app mark (same artwork as the launcher icon). */
@Composable
fun InklyLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(BrandGradient)
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().scale(1.45f),
        )
    }
}

/** Subtle press-down scale used by cards and buttons. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "press",
    )
    return this.scale(scale)
}

fun formatRelative(ms: Long): String {
    if (ms <= 0) return "Never"
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    return when {
        DateUtils.isToday(ms) -> "Today, $time"
        DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS) -> "Yesterday, $time"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))
    }
}

fun formatShort(ms: Long): String = when {
    DateUtils.isToday(ms) -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
}
