package com.smartisland.app.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.smartisland.app.AppContainer
import com.smartisland.app.container
import com.smartisland.app.system.ActionResult
import com.smartisland.app.system.BatterySnapshot
import com.smartisland.app.system.PermissionStatus
import com.smartisland.app.system.Permissions
import com.smartisland.app.ui.components.SnackController

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun appContainer(): AppContainer = LocalContext.current.container

/** Permission state that refreshes whenever the user comes back from a settings screen. */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(Permissions.status(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = Permissions.status(context) }
    return status
}

/** Subscribes to a system broadcast for as long as the composable is on screen. */
@Composable
fun SystemBroadcastEffect(vararg actions: String, onReceive: (Intent) -> Unit) {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onReceive)
    DisposableEffect(*actions) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = callback(intent)
        }
        val filter = IntentFilter().apply { actions.forEach { addAction(it) } }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
}

@Composable
fun rememberBattery(): BatterySnapshot {
    val context = LocalContext.current
    var battery by remember { mutableStateOf(BatterySnapshot.from(context)) }
    SystemBroadcastEffect(Intent.ACTION_BATTERY_CHANGED) { battery = BatterySnapshot.from(context, it) }
    return battery
}

/** Turns the result of a power-key action into user feedback. */
fun reportAction(result: ActionResult, snack: SnackController, openSetup: () -> Unit) {
    when (result) {
        ActionResult.DONE -> Unit
        ActionResult.NEEDS_ACCESSIBILITY ->
            snack.error("Turn on the Smart Island accessibility service first", "Enable", openSetup)
        ActionResult.UNSUPPORTED -> snack.error("This action needs Android 9 or newer")
        ActionResult.FAILED -> snack.error("Your device blocked this action")
    }
}
