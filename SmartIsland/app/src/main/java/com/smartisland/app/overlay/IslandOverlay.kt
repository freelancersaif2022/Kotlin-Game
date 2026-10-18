package com.smartisland.app.overlay

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.smartisland.app.MainActivity
import com.smartisland.app.data.IslandSettings
import com.smartisland.app.island.IslandEngine
import com.smartisland.app.island.MediaCommands
import com.smartisland.app.island.ui.IslandActions
import com.smartisland.app.island.ui.IslandOuterPadding
import com.smartisland.app.island.ui.IslandView
import com.smartisland.app.system.PendingIntents
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow

/** The always-on island window anchored at the top of the screen. */
class IslandOverlay(
    private val context: Context,
    private val settings: StateFlow<IslandSettings>,
) {
    private val expanded = mutableStateOf(false)
    private val density = context.resources.displayMetrics.density

    private val actions = object : IslandActions {
        override val media: MediaCommands? get() = IslandEngine.mediaCommands
        override fun send(intent: PendingIntent?) {
            PendingIntents.send(context, intent)
            expanded.value = false
        }
        override fun dismissTransient() = IslandEngine.dismissTransient()
        override fun openApp() {
            runCatching {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private val window = OverlayWindow(
        context = context,
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
        onOutsideTouch = { expanded.value = false },
    ) { Content() }

    @Composable
    private fun Content() {
        val s by settings.collectAsState()
        val activity by IslandEngine.current.collectAsState()
        // A new kind of activity always starts collapsed; updates of the same kind keep the state.
        LaunchedEffect(activity::class) { expanded.value = false }
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        IslandView(
            activity = activity,
            settings = s,
            expanded = expanded.value,
            onExpandedChange = { expanded.value = it },
            actions = actions,
            maxWidth = screenWidth - 16.dp,
        )
    }

    fun show() = window.show()

    fun hide() = window.hide()

    val isShowing: Boolean get() = window.isShowing

    fun setVisible(visible: Boolean) = window.setVisible(visible)

    fun applyGeometry(s: IslandSettings) {
        val pad = IslandOuterPadding.value
        window.move(px(s.xOffsetDp), px(s.topOffsetDp - pad))
    }

    private fun px(dp: Float): Int = (dp * density).roundToInt()
}
