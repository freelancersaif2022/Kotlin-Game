package com.pulse.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulse.messenger.ui.theme.Danger
import com.pulse.messenger.ui.theme.Mint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SnackType { INFO, SUCCESS, ERROR, UNDO }

private class PulseSnackVisuals(
    override val message: String,
    override val actionLabel: String?,
    val type: SnackType,
    override val duration: SnackbarDuration,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

/** App-wide snackbar entry point, available through [LocalSnackbar]. */
@Stable
class SnackbarController(val hostState: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(
        message: String,
        type: SnackType = SnackType.INFO,
        action: String? = null,
        duration: SnackbarDuration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(PulseSnackVisuals(message, action, type, duration))
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun undo(message: String, onUndo: () -> Unit) = show(message, SnackType.UNDO, "Undo", onAction = onUndo)
}

val LocalSnackbar = staticCompositionLocalOf<SnackbarController> { error("No snackbar controller") }

@Composable
fun PulseSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> PulseSnackbar(data) }
}

@Composable
private fun PulseSnackbar(data: SnackbarData) {
    val visuals = data.visuals as? PulseSnackVisuals
    val type = visuals?.type ?: SnackType.INFO
    val (icon, tint) = when (type) {
        SnackType.SUCCESS -> Icons.Filled.Check to Mint
        SnackType.ERROR -> Icons.Filled.ErrorOutline to Danger
        SnackType.UNDO -> Icons.Filled.Undo to Mint
        SnackType.INFO -> Icons.Filled.Info to Color(0xFF8AB4F8)
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1F2C33),
            contentColor = Color(0xFFE9EDEF),
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                SnackIcon(icon, tint)
                Spacer(Modifier.width(12.dp))
                Text(
                    data.visuals.message,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                )
                val label = data.visuals.actionLabel
                if (label != null) {
                    TextButton(onClick = { data.performAction() }) {
                        Text(label, color = Mint, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    IconButton(onClick = { data.dismiss() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, "Dismiss", tint = Color(0xFF8696A0), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SnackIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier.size(28.dp).background(tint.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)) }
}
