package com.zyropay.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.zyropay.app.R
import com.zyropay.app.ui.theme.Z

/** Custom numeric keypad. `extraKey` is "." for amounts or a biometric icon for passcode entry. */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    @DrawableRes extraIcon: Int? = null,
    onExtra: (() -> Unit)? = null,
    keyHeight: androidx.compose.ui.unit.Dp = 62.dp,
) {
    val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                r.forEach { d -> Key(Modifier.weight(1f), keyHeight, onClick = { onDigit(d) }) { Text(d.toString(), style = Z.type.h2, color = Z.colors.textPrimary) } }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                decimal -> Key(Modifier.weight(1f), keyHeight, onClick = { onDigit('.') }) { Text("•", style = Z.type.h2, color = Z.colors.textPrimary) }
                extraIcon != null && onExtra != null -> Key(Modifier.weight(1f), keyHeight, onClick = onExtra) { ZIcon(extraIcon, size = 28.dp, tint = Z.colors.primary) }
                else -> Box(Modifier.weight(1f).height(keyHeight))
            }
            Key(Modifier.weight(1f), keyHeight, onClick = { onDigit('0') }) { Text("0", style = Z.type.h2, color = Z.colors.textPrimary) }
            Key(Modifier.weight(1f), keyHeight, onClick = onDelete) { ZIcon(R.drawable.ic_arrow_left, size = 24.dp, tint = Z.colors.textPrimary) }
        }
    }
}

@Composable
private fun Key(modifier: Modifier, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if (pressed) Z.colors.keypadPressed else Color.Transparent, tween(90), label = "key")
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(stiffness = 1200f), label = "keyScale")
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(interactionSource = source, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Passcode dots with fill animation and error shake (shakeKey changes trigger a shake). */
@Composable
fun PasscodeDots(filled: Int, modifier: Modifier = Modifier, length: Int = 6, error: Boolean = false, shakeKey: Int = 0) {
    val c = Z.colors
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            for (x in listOf(18f, -16f, 12f, -9f, 5f, 0f)) shake.animateTo(x, tween(55))
        }
    }
    Row(
        modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) },
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        repeat(length) { i ->
            val on = i < filled
            val scale by animateFloatAsState(if (on) 1f else 0.82f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "dot")
            val color by animateColorAsState(
                when {
                    error -> c.error
                    on -> c.primary
                    else -> Color.Transparent
                },
                tween(120), label = "dotColor",
            )
            Box(
                Modifier
                    .size(16.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(color)
                    .border(1.6.dp, if (on || error) Color.Transparent else c.textTertiary, CircleShape),
            )
        }
    }
}

/** Big amount entry display used with the keypad. */
fun appendAmount(current: String, key: Char, decimals: Int): String {
    if (key == '.') {
        if (decimals == 0 || current.contains('.')) return current
        return if (current.isEmpty()) "0." else "$current."
    }
    if (current == "0") return key.toString()
    val dot = current.indexOf('.')
    if (dot >= 0 && current.length - dot - 1 >= decimals) return current
    if (dot < 0 && current.length >= 9) return current
    return current + key
}

@Composable
fun KeyHint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Z.type.caption, color = Z.colors.textTertiary, modifier = modifier.padding(4.dp))
}
