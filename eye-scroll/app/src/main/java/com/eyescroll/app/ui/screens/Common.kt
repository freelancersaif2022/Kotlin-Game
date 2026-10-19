package com.eyescroll.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyescroll.app.Readiness
import kotlinx.coroutines.delay

data class SetupState(
    val camera: Boolean,
    val notifications: Boolean,
    val accessibility: Boolean,
) {
    val requiredDone: Boolean get() = camera && accessibility
    val doneCount: Int get() = listOf(camera, notifications, accessibility).count { it }

    companion object {
        fun read(context: Context) = SetupState(
            camera = Readiness.hasCamera(context),
            notifications = Readiness.hasNotifications(context),
            accessibility = Readiness.isAccessibilityEnabled(context),
        )
    }
}

class SetupStateHolder(private val context: Context, val state: MutableState<SetupState>) {
    fun refresh() {
        state.value = SetupState.read(context)
    }
}

/** Setup state that refreshes whenever the user comes back from a system settings screen. */
@Composable
fun rememberSetupState(): SetupStateHolder {
    val context = LocalContext.current
    val holder = remember { SetupStateHolder(context, mutableStateOf(SetupState.read(context))) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) holder.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return holder
}

/** Staggered fade + slide-up entrance. */
@Composable
fun Modifier.enterAnimation(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    val offset = with(LocalDensity.current) { 36.dp.toPx() }
    LaunchedEffect(Unit) {
        delay(60L * index)
        progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offset
    }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
