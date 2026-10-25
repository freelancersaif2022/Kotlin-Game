package com.twinspace.app.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinspace.app.MainViewModel
import com.twinspace.app.Screen
import com.twinspace.app.UiEvent
import com.twinspace.app.core.AppEntry
import com.twinspace.app.core.ShortcutHelper
import com.twinspace.app.core.SpaceStatus
import com.twinspace.app.ui.screens.AddAppsScreen
import com.twinspace.app.ui.screens.AppAction
import com.twinspace.app.ui.screens.AppActionsSheet
import com.twinspace.app.ui.screens.ConfirmDialog
import com.twinspace.app.ui.screens.HomeScreen
import com.twinspace.app.ui.screens.LockScreen
import com.twinspace.app.ui.screens.RenameDialog
import com.twinspace.app.ui.screens.SettingsScreen
import com.twinspace.app.ui.screens.SetupScreen
import kotlinx.coroutines.launch

@Composable
fun TwinSpaceApp(
    vm: MainViewModel,
    versionName: String,
    onStartForResult: (Intent, UiEvent.Kind) -> Unit,
    onAuthenticate: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Message -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = event.text,
                        actionLabel = event.actionLabel,
                        duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) event.action?.invoke()
                }
                is UiEvent.StartForResult -> onStartForResult(event.intent, event.kind)
                UiEvent.Authenticate -> onAuthenticate()
            }
        }
    }

    val aliases = settings.aliases
    val nameOf: (AppEntry) -> String = remember(aliases) { { entry -> aliases[entry.packageName] ?: entry.label } }
    val spaceName = settings.spaceName.ifBlank { "Twin Space" }
    val hasSpace = state.status == SpaceStatus.ACTIVE || state.status == SpaceStatus.PAUSED

    var sheetFor by remember { mutableStateOf<AppEntry?>(null) }
    var renameFor by remember { mutableStateOf<AppEntry?>(null) }
    var removeFor by remember { mutableStateOf<AppEntry?>(null) }
    var renamingSpace by rememberSaveable { mutableStateOf(false) }
    var deletingSpace by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val shortcutsSupported = remember { ShortcutHelper.isSupported(context) }

    val destination = when {
        state.locked -> "lock"
        state.loading -> "loading"
        !hasSpace -> "setup"
        else -> state.screen.name
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                val forward = targetState == Screen.AddApps.name || targetState == Screen.Settings.name
                val back = initialState == Screen.AddApps.name || initialState == Screen.Settings.name
                when {
                    forward -> (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(240))) togetherWith fadeOut(tween(180))
                    back -> fadeIn(tween(240)) togetherWith (slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200)))
                    else -> fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                }
            },
            label = "screen",
        ) { target ->
            when (target) {
                "lock" -> LockScreen(onUnlock = vm::requestUnlock)
                "loading" -> Box(Modifier.fillMaxSize())
                "setup" -> SetupScreen(state.status, state.canCreate, state.settingUp, vm::createSpace)
                Screen.AddApps.name -> AddAppsScreen(
                    apps = state.installable,
                    loaded = state.installableLoaded,
                    busy = state.busy != null,
                    icons = vm.icons,
                    snackbar = snackbar,
                    onClone = vm::clone,
                    onBack = { vm.navigate(Screen.Home) },
                )
                Screen.Settings.name -> SettingsScreen(
                    status = state.status,
                    spaceName = spaceName,
                    cloneCount = state.cloned.size,
                    lockEnabled = settings.lockEnabled,
                    versionName = versionName,
                    snackbar = snackbar,
                    onToggleLock = vm::setLockEnabled,
                    onRenameSpace = { renamingSpace = true },
                    onTogglePause = { vm.setPaused(state.status != SpaceStatus.PAUSED) },
                    onOpenSystemSettings = onOpenSystemSettings,
                    onHowItWorks = { showHelp = true },
                    onDeleteSpace = { deletingSpace = true },
                    onBack = { vm.navigate(Screen.Home) },
                )
                else -> HomeScreen(
                    status = state.status,
                    spaceName = spaceName,
                    apps = state.cloned,
                    loading = state.loading,
                    icons = vm.icons,
                    nameOf = nameOf,
                    snackbar = snackbar,
                    onLaunch = vm::launch,
                    onLongPress = { sheetFor = it },
                    onAddApps = { vm.navigate(Screen.AddApps) },
                    onSettings = { vm.navigate(Screen.Settings) },
                    onTogglePause = { vm.setPaused(state.status != SpaceStatus.PAUSED) },
                )
            }
        }

        // A request is running inside the space (except on Add apps, which shows its own progress).
        val busy = state.busy
        if (busy != null && state.screen != Screen.AddApps && !state.locked) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp) {
                    Row(Modifier.padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(busy, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    sheetFor?.let { entry ->
        AppActionsSheet(
            entry = entry,
            name = nameOf(entry),
            icons = vm.icons,
            shortcutsSupported = shortcutsSupported,
            onDismiss = { sheetFor = null },
            onAction = { action ->
                when (action) {
                    AppAction.OPEN -> vm.launch(entry)
                    AppAction.RENAME -> renameFor = entry
                    AppAction.SHORTCUT -> vm.addShortcut(entry)
                    AppAction.FREEZE -> vm.setFrozen(entry, !entry.frozen)
                    AppAction.INFO -> vm.openAppInfo(entry)
                    AppAction.REMOVE -> removeFor = entry
                }
            },
        )
    }

    renameFor?.let { entry ->
        RenameDialog(
            title = "Rename clone",
            current = nameOf(entry),
            original = entry.label,
            onConfirm = { vm.rename(entry, it); renameFor = null },
            onDismiss = { renameFor = null },
        )
    }

    removeFor?.let { entry ->
        ConfirmDialog(
            title = "Remove ${nameOf(entry)}?",
            body = "This deletes the clone and everything signed in to it. The original app on your phone is not affected.",
            confirm = "Remove",
            onConfirm = { vm.remove(entry); removeFor = null },
            onDismiss = { removeFor = null },
        )
    }

    if (renamingSpace) {
        RenameDialog(
            title = "Space name",
            current = spaceName,
            original = null,
            onConfirm = { if (it != null) vm.renameSpace(it); renamingSpace = false },
            onDismiss = { renamingSpace = false },
        )
    }

    if (deletingSpace) {
        ConfirmDialog(
            title = "Remove your space?",
            body = "All ${state.cloned.size} clones and their accounts, chats and files will be permanently deleted. Your original apps stay exactly as they are.",
            confirm = "Remove space",
            requireCheck = "I understand this can't be undone",
            onConfirm = { deletingSpace = false; vm.deleteSpace() },
            onDismiss = { deletingSpace = false },
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("How Twin Space works") },
            text = {
                Text(
                    "Twin Space uses Android's built-in work profile to create a second, fully separate space on your phone.\n\n" +
                        "• Cloning makes the same app available inside the space. It shares the app's code with the original, so it takes almost no extra storage, but has its own data and accounts.\n\n" +
                        "• Clones update automatically together with the original app.\n\n" +
                        "• Clones show a work badge in your app drawer and a \"2\" badge in Twin Space.\n\n" +
                        "• Nothing is ever removed unless you remove it yourself.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } },
        )
    }
}
