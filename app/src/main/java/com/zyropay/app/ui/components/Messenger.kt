package com.zyropay.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zyropay.app.R
import com.zyropay.app.ui.theme.Z
import kotlinx.coroutines.delay

enum class ToastType { SUCCESS, ERROR, WARNING, INFO }

data class TopMessage(
    val id: Long,
    val type: ToastType,
    val title: String,
    val message: String?,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/** App-wide feedback: rich top banners (snackbars) and compact bottom toasts ("Copied"). */
@Stable
class Messenger {
    var top by mutableStateOf<TopMessage?>(null)
        private set
    var toast by mutableStateOf<Pair<Long, String>?>(null)
        private set

    fun show(type: ToastType, title: String, message: String? = null, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        top = TopMessage(System.nanoTime(), type, title, message, actionLabel, onAction)
    }

    fun success(title: String, message: String? = null) = show(ToastType.SUCCESS, title, message)
    fun error(title: String, message: String? = null) = show(ToastType.ERROR, title, message)
    fun warning(title: String, message: String? = null) = show(ToastType.WARNING, title, message)
    fun info(title: String, message: String? = null) = show(ToastType.INFO, title, message)

    fun toast(text: String) {
        toast = System.nanoTime() to text
    }

    fun dismissTop() {
        top = null
    }

    internal fun clearToast(id: Long) {
        if (toast?.first == id) toast = null
    }
}

val LocalMessenger = staticCompositionLocalOf { Messenger() }

@Composable
fun MessengerHost(messenger: Messenger) {
    val c = Z.colors
    Box(Modifier.fillMaxSize()) {
        // ---- top banner
        val msg = messenger.top
        LaunchedEffect(msg?.id) {
            if (msg != null) {
                delay(if (msg.actionLabel != null) 5_000 else 3_400)
                if (messenger.top?.id == msg.id) messenger.dismissTop()
            }
        }
        var last by androidx.compose.runtime.remember { mutableStateOf<TopMessage?>(null) }
        if (msg != null) last = msg
        AnimatedVisibility(
            visible = msg != null,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) { -it * 2 } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(220)) { -it * 2 } + fadeOut(tween(200)),
        ) {
            val m = last ?: return@AnimatedVisibility
            val (accent, soft, icon) = when (m.type) {
                ToastType.SUCCESS -> Triple(c.success, c.successSoft, R.drawable.ic_check_circle)
                ToastType.ERROR -> Triple(c.error, c.errorSoft, R.drawable.ic_status_failed)
                ToastType.WARNING -> Triple(c.warning, c.warningSoft, R.drawable.ic_warning)
                ToastType.INFO -> Triple(c.primary, c.primarySoft, R.drawable.ic_info)
            }
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.25f))
                    .background(c.surfaceElevated, RoundedCornerShape(18.dp))
                    .border(1.dp, c.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .pointerInput(m.id) {
                        detectVerticalDragGestures { _, drag -> if (drag < -6) messenger.dismissTop() }
                    }
                    .clickable(interactionSource = null, indication = null) { messenger.dismissTop() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(38.dp).background(soft, CircleShape), contentAlignment = Alignment.Center) {
                    ZIcon(icon, size = 20.dp, tint = accent)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(m.title, style = Z.type.bodySmallMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (m.message != null) Text(m.message, style = Z.type.caption, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (m.actionLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        m.actionLabel,
                        style = Z.type.label,
                        color = c.primary,
                        modifier = Modifier
                            .clickable { m.onAction?.invoke(); messenger.dismissTop() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }

        // ---- compact bottom toast
        val t = messenger.toast
        LaunchedEffect(t?.first) {
            if (t != null) {
                delay(1_800)
                messenger.clearToast(t.first)
            }
        }
        var lastToast by androidx.compose.runtime.remember { mutableStateOf("") }
        if (t != null) lastToast = t.second
        AnimatedVisibility(
            visible = t != null,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f),
        ) {
            Row(
                Modifier
                    .widthIn(max = 300.dp)
                    .background(Color(0xE6141620), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZIcon(R.drawable.ic_check_circle, size = 16.dp, tint = Color(0xFF24D18C))
                Spacer(Modifier.width(8.dp))
                Text(lastToast, style = Z.type.bodySmallMedium, color = Color.White, maxLines = 1)
            }
        }
    }
}
