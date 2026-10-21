package com.novacleaner.app.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Permissions

/** Permission state that refreshes whenever the screen resumes (e.g. back from Settings). */
@Composable
fun rememberPermissionState(permission: AppPermission): State<Boolean> {
    val context = LocalContext.current
    val state = remember(permission) { mutableStateOf(Permissions.has(context, permission)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.value = Permissions.has(context, permission) }
    return state
}

fun openPermissionSettings(context: Context, permission: AppPermission) {
    AppGraph.appOpen.skipNextResume()
    for (intent in Permissions.settingsIntents(context, permission)) {
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
}

/** Returns a function that asks for [permission] via dialog or the right settings page. */
@Composable
fun rememberPermissionRequester(permission: AppPermission, onResult: (Boolean) -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val runtime = Permissions.runtimePermissions(permission)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onResult(Permissions.has(context, permission))
    }
    return remember(permission, launcher) {
        {
            if (runtime != null) launcher.launch(runtime) else openPermissionSettings(context, permission)
        }
    }
}

/** Shows [content] when the permission is granted, otherwise a friendly explanation + CTA. */
@Composable
fun PermissionGate(
    permission: AppPermission,
    title: String,
    message: String,
    icon: ImageVector,
    colors: List<Color>,
    content: @Composable () -> Unit,
) {
    val granted = rememberPermissionState(permission)
    if (granted.value) {
        content()
        return
    }
    val request = rememberPermissionRequester(permission)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(24.dp))
        GlossyIcon(icon, colors, size = 96.dp)
        Spacer(Modifier.height(26.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        NovaCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            PrivacyLine(Icons.Rounded.Shield, "Everything is processed on your device")
            Spacer(Modifier.height(10.dp))
            PrivacyLine(Icons.Rounded.Lock, "Nothing is deleted without your confirmation")
        }
        Spacer(Modifier.height(28.dp))
        GradientButton("Allow access", request, Modifier.fillMaxWidth(), colors = colors)
    }
}

@Composable
private fun PrivacyLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
