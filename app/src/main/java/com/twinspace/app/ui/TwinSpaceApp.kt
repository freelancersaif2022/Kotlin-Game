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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import com.twinspace.app.ui.components.BusyCard
import com.twinspace.app.ui.components.DialogPoint
import com.twinspace.app.ui.components.DialogTone
import com.twinspace.app.ui.components.ProDialog
import com.twinspace.app.ui.screens.AddAppsScreen
import com.twinspace.app.ui.screens.AppDialogHeader
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
                UiEvent.OpenSystemSettings -> onOpenSystemSettings()
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
    var freezeFor by remember { mutableStateOf<AppEntry?>(null) }
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

        if (destination == "setup") {
            com.twinspace.app.ui.components.ProSnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            )
        }

        // A request is running inside the space (except on Add apps, which shows its own progress).
        val busy = state.busy
        if (busy != null && state.screen != Screen.AddApps && !state.locked) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                BusyCard(busy)
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
                    AppAction.FREEZE -> if (entry.frozen) vm.setFrozen(entry, false) else freezeFor = entry
                    AppAction.INFO -> vm.openAppInfo(entry)
                    AppAction.REMOVE -> removeFor = entry
                }
            },
        )
    }

    renameFor?.let { entry ->
        RenameDialog(
            title = "Rename clone",
            message = "Shown in Twin Space and on this clone\u2019s home-screen shortcut.",
            current = nameOf(entry),
            original = entry.label,
            header = { AppDialogHeader(entry, vm.icons) },
            onConfirm = { vm.rename(entry, it); renameFor = null },
            onDismiss = { renameFor = null },
        )
    }

    removeFor?.let { entry ->
        ConfirmDialog(
            title = "Remove ${nameOf(entry)}?",
            body = "The clone and everything signed in to it will be deleted. Your original app is not affected. Android will ask you to confirm once more.",
            confirm = "Remove clone",
            icon = Icons.Rounded.DeleteOutline,
            header = { AppDialogHeader(entry, vm.icons) },
            onConfirm = { removeFor = null; vm.remove(entry) },
            onDismiss = { removeFor = null },
        )
    }

    freezeFor?.let { entry ->
        ProDialog(
            title = "Freeze ${nameOf(entry)}?",
            message = "The clone is completely switched off until you unfreeze it.",
            header = { AppDialogHeader(entry, vm.icons) },
            tone = DialogTone.WARNING,
            confirmText = "Freeze",
            onConfirm = { freezeFor = null; vm.setFrozen(entry, true) },
            onDismiss = { freezeFor = null },
        ) {
            DialogPoint("No background activity, sync or notifications")
            DialogPoint("No mobile data or Wi-Fi use")
            DialogPoint("Hidden from your app drawer")
            DialogPoint("All its data and logins are kept")
        }
    }

    state.unfreezePrompt?.let { pkg ->
        val entry = state.cloned.firstOrNull { it.packageName == pkg }
        if (entry == null) {
            LaunchedEffect(pkg, state.loading) { if (!state.loading) vm.dismissUnfreezePrompt() }
        } else {
            ProDialog(
                title = "${nameOf(entry)} is frozen",
                message = "It\u2019s switched off right now. Unfreeze it to open it. Everything is just as you left it.",
                header = { AppDialogHeader(entry, vm.icons) },
                icon = Icons.Rounded.AcUnit,
                confirmText = "Unfreeze & open",
                onConfirm = { vm.dismissUnfreezePrompt(); vm.setFrozen(entry, hide = false, openAfter = true) },
                onDismiss = vm::dismissUnfreezePrompt,
            )
        }
    }

    if (renamingSpace) {
        RenameDialog(
            title = "Rename space",
            message = "Shown in Twin Space and in Android\u2019s settings.",
            current = spaceName,
            original = null,
            onConfirm = { if (it != null) vm.renameSpace(it); renamingSpace = false },
            onDismiss = { renamingSpace = false },
        )
    }

    if (deletingSpace) {
        ConfirmDialog(
            title = "Remove your space?",
            body = "All ${state.cloned.size} clone${if (state.cloned.size == 1) "" else "s"} and their accounts, chats and files will be permanently deleted. Your original apps stay exactly as they are.",
            confirm = "Remove space",
            icon = Icons.Rounded.DeleteForever,
            requireCheck = "I understand this can\u2019t be undone",
            onConfirm = { deletingSpace = false; vm.deleteSpace() },
            onDismiss = { deletingSpace = false },
        )
    }

    if (showHelp) {
        ProDialog(
            title = "How Twin Space works",
            message = "Twin Space uses Android\u2019s built-in work profile to create a second, fully separate space.",
            icon = Icons.Rounded.AutoAwesome,
            confirmText = "Got it",
            dismissText = null,
            onConfirm = { showHelp = false },
            onDismiss = { showHelp = false },
        ) {
            DialogPoint("Clones share the original app\u2019s code, so they use almost no extra storage, but have their own data and accounts.")
            DialogPoint("Clones update automatically with the original app.")
            DialogPoint("Freezing switches a clone off completely without deleting anything.")
            DialogPoint("Nothing is ever removed unless you remove it yourself.")
        }
    }
}
