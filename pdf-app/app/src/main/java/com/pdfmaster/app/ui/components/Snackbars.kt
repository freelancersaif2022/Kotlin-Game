package com.pdfmaster.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pdfmaster.app.ui.theme.Brand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class SnackType(val icon: ImageVector, val accent: Color) {
    SUCCESS(Icons.Rounded.CheckCircle, Color(0xFF22C55E)),
    ERROR(Icons.Rounded.ErrorOutline, Color(0xFFFF5A5F)),
    INFO(Icons.Rounded.Info, Color(0xFF60A5FA)),
    WARNING(Icons.Rounded.WarningAmber, Color(0xFFFBBF24)),
    PREMIUM(Icons.Rounded.WorkspacePremium, Brand.Gold),
}

class AppSnackVisuals(
    override val message: String,
    val type: SnackType,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    val title: String? = null,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = actionLabel == null
}

@Stable
class SnackbarController(val hostState: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(
        message: String,
        type: SnackType = SnackType.INFO,
        title: String? = null,
        action: String? = null,
        long: Boolean = false,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                AppSnackVisuals(
                    message = message,
                    type = type,
                    actionLabel = action,
                    duration = when {
                        action != null -> SnackbarDuration.Long
                        long -> SnackbarDuration.Long
                        else -> SnackbarDuration.Short
                    },
                    title = title,
                ),
            )
            if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
        }
    }

    fun success(message: String, title: String? = null) = show(message, SnackType.SUCCESS, title)
    fun error(message: String, title: String? = null) = show(message, SnackType.ERROR, title, long = true)
    fun info(message: String) = show(message, SnackType.INFO)
}

val LocalSnackbar = staticCompositionLocalOf<SnackbarController> { error("Snackbar not provided") }

@Composable
fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> AppSnackbar(data) }
}

@Composable
private fun AppSnackbar(data: SnackbarData) {
    val visuals = data.visuals as? AppSnackVisuals
    val type = visuals?.type ?: SnackType.INFO
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E2029),
        contentColor = Color.White,
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(type.accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(type.icon, contentDescription = null, tint = type.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                visuals?.title?.let {
                    Text(it, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Text(
                    data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = if (visuals?.title != null) 0.78f else 0.95f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val action = data.visuals.actionLabel
            if (action != null) {
                TextButton(onClick = { data.performAction() }) {
                    Text(action.uppercase(), color = if (type == SnackType.PREMIUM) Brand.Gold else Color(0xFFFF7A7E), fontWeight = FontWeight.Bold)
                }
            } else {
                IconButton(onClick = { data.dismiss() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
