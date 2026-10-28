package com.inkly.notes.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import com.inkly.notes.ui.components.AnimatedDialog
import com.inkly.notes.ui.components.DialogIcon
import com.inkly.notes.BuildConfig
import com.inkly.notes.data.ThemeMode
import com.inkly.notes.drive.DriveFile
import com.inkly.notes.ui.components.BrandGradient
import com.inkly.notes.ui.components.ConfirmDialog
import com.inkly.notes.ui.components.InklyLogo
import com.inkly.notes.ui.components.ProgressDialog
import com.inkly.notes.ui.components.formatRelative
import com.inkly.notes.ui.components.pressScale
import com.inkly.notes.ui.theme.Brand

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val prefs by vm.prefs.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDisconnect by remember { mutableStateOf(false) }

    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        vm.onAuthResult(it.resultCode, it.data)
    }
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is SettingsEvent.Resolve -> authLauncher.launch(IntentSenderRequest.Builder(e.intentSender).build())
                is SettingsEvent.Message -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(e.text)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Text("Settings", style = MaterialTheme.typography.titleLarge)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionTitle("Backup & sync")
                DriveCard(
                    connected = prefs.driveConnected,
                    name = prefs.accountName,
                    email = prefs.accountEmail,
                    autoBackup = prefs.autoBackup,
                    lastBackupAt = prefs.lastBackupAt,
                    lastBackupCount = prefs.lastBackupCount,
                    lastError = prefs.lastBackupError,
                    needsReauth = prefs.needsReauth,
                    connecting = vm.busy == Busy.Connecting,
                    onConnect = vm::connect,
                    onBackup = vm::backupNow,
                    onRestore = vm::restore,
                    onAutoBackup = vm::setAutoBackup,
                    onDisconnect = { confirmDisconnect = true },
                )

                SectionTitle("Appearance")
                Card {
                    Text("Theme", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                    ThemeSelector(prefs.themeMode, vm::setTheme)
                }

                SectionTitle("General")
                Card(padding = 6.dp) {
                    ActionRow(Icons.Rounded.Lock, "Privacy", "Notes stay on your device. Backups go to a hidden folder only Inkly can access in your own Google Drive.")
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ActionRow(Icons.Rounded.Info, "Version", "Inkly ${BuildConfig.VERSION_NAME}")
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InklyLogo(20.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Inkly — thoughts, beautifully kept.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
        ) {
            Snackbar(
                it,
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }

    val busy = vm.busy
    var lastBusy by remember { mutableStateOf(Busy.Checking) }
    if (busy != null && busy != Busy.Connecting) lastBusy = busy
    ProgressDialog(
        visible = busy != null && busy != Busy.Connecting,
        icon = when (lastBusy) {
            Busy.BackingUp -> Icons.Rounded.CloudUpload
            Busy.Restoring -> Icons.Rounded.CloudDownload
            else -> Icons.Rounded.Sync
        },
        title = when (lastBusy) {
            Busy.BackingUp -> "Backing up to Google Drive"
            Busy.Restoring -> "Restoring your notes"
            Busy.Disconnecting -> "Disconnecting"
            else -> "Checking Google Drive"
        },
        message = when (lastBusy) {
            Busy.BackingUp -> "Uploading a secure copy of all your notes…"
            Busy.Restoring -> "Downloading and merging your backup…"
            Busy.Disconnecting -> "Revoking Drive access…"
            else -> "Looking for your latest backup…"
        },
    )

    var lastOffer by remember { mutableStateOf<DriveFile?>(null) }
    vm.restoreOffer?.let { lastOffer = it }
    val offer = lastOffer
    ConfirmDialog(
        visible = vm.restoreOffer != null,
        icon = Icons.Rounded.CloudDownload,
        title = if (vm.offerAfterConnect) "Backup found" else "Restore backup?",
        message = buildString {
            append("Backup from ")
            append(formatRelative(offer?.modifiedTime ?: 0))
            if ((offer?.noteCount ?: -1) >= 0) append(" with ${offer?.noteCount} notes")
            append(". It will be merged with the notes on this device — nothing is deleted.")
        },
        confirmText = "Restore now",
        dismissText = if (vm.offerAfterConnect) "Not now" else "Cancel",
        onConfirm = vm::confirmRestore,
        onDismiss = vm::dismissRestore,
    )

    ConfirmDialog(
        visible = confirmDisconnect,
        icon = Icons.AutoMirrored.Rounded.Logout,
        title = "Disconnect Google Drive?",
        message = "Automatic backups will stop. Your existing backups stay safe in Google Drive and your notes stay on this device.",
        confirmText = "Disconnect",
        destructive = true,
        onConfirm = { confirmDisconnect = false; vm.disconnect() },
        onDismiss = { confirmDisconnect = false },
    )

    var lastHelp by remember { mutableStateOf(SetupHelp(null)) }
    vm.setupHelp?.let { lastHelp = it }
    AnimatedDialog(visible = vm.setupHelp != null, onDismiss = vm::dismissSetupHelp) {
        SetupHelpContent(
            statusCode = lastHelp.statusCode,
            packageName = vm.packageName,
            sha1 = vm.signingSha1,
            onRetry = vm::retryConnect,
            onClose = vm::dismissSetupHelp,
        )
    }
}

@Composable
private fun ColumnScope.SetupHelpContent(
    statusCode: Int?,
    packageName: String,
    sha1: String,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    DialogIcon(Icons.Rounded.Cloud, MaterialTheme.colorScheme.primary, Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(16.dp))
    Text(
        "Google didn't finish connecting",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "If you didn't close the Google window yourself, Google refused this app build" +
            (if (statusCode != null) " (code $statusCode)" else "") +
            ". Check these in Google Cloud Console, then try again:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    HelpStep(1, "Google Drive API is enabled for the project")
    HelpStep(2, "OAuth consent screen: your Google account is added under Test users (or the app is published)")
    HelpStep(3, "An Android OAuth client exists with exactly these values:")
    Spacer(Modifier.height(8.dp))
    CopyField("Package name", packageName)
    Spacer(Modifier.height(8.dp))
    CopyField("SHA-1 fingerprint", sha1)
    Spacer(Modifier.height(22.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.material3.OutlinedButton(
            onClick = onClose,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Close", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) }
        Button(
            onClick = onRetry,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Try again", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun HelpStep(n: Int, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text("$n", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CopyField(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1500); copied = false }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable { clipboard.setText(AnnotatedString(value)); copied = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(8.dp))
        AnimatedContent(copied, label = "copy") { done ->
            Icon(
                if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                if (done) "Copied" else "Copy",
                tint = if (done) Brand.Success else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 10.dp),
    )
}

@Composable
private fun Card(padding: androidx.compose.ui.unit.Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
private fun DriveCard(
    connected: Boolean,
    name: String?,
    email: String?,
    autoBackup: Boolean,
    lastBackupAt: Long,
    lastBackupCount: Int,
    lastError: String?,
    needsReauth: Boolean,
    connecting: Boolean,
    onConnect: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onAutoBackup: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
) {
    Card(padding = 6.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(BrandGradient),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Cloud, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Google Drive backup", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dot by animateColorAsState(if (connected) Brand.Success else MaterialTheme.colorScheme.outline, label = "dot")
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (connected) "Connected" else "Not connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        AnimatedContent(
            connected,
            transitionSpec = { (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically()) },
            label = "drive",
        ) { isConnected ->
            if (!isConnected) {
                Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    Text(
                        "Back up your notes to a private folder in your Google Drive. Inkly then backs up automatically every day in the background, and you can restore anytime on any device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    val interaction = remember { MutableInteractionSource() }
                    Button(
                        onClick = onConnect,
                        enabled = !connecting,
                        interactionSource = interaction,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp).pressScale(interaction),
                    ) {
                        AnimatedContent(connecting, label = "btn") { busy ->
                            if (busy) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Connecting…")
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GoogleBadge()
                                    Spacer(Modifier.width(10.dp))
                                    Text("Connect Google Drive", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            } else {
                Column {
                    // account
                    Row(
                        Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val initial = (name ?: email ?: "G").trim().firstOrNull()?.uppercaseChar() ?: 'G'
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) { Text(initial.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name ?: "Google account", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (email != null) {
                                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    AnimatedVisibility(needsReauth || lastError != null) {
                        Row(
                            Modifier
                                .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clickable(enabled = needsReauth, onClick = onConnect)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (needsReauth) "Google access expired. Tap to reconnect and resume backups."
                                else lastError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    ActionRow(
                        Icons.Rounded.CloudUpload, "Back up now",
                        if (lastBackupAt > 0) "Last backup: ${formatRelative(lastBackupAt)} • $lastBackupCount ${if (lastBackupCount == 1) "note" else "notes"}"
                        else "No backup yet",
                        onClick = onBackup,
                    )
                    ActionRow(Icons.Rounded.CloudDownload, "Restore from Drive", "Bring back notes from your latest backup", onClick = onRestore)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAutoBackup(!autoBackup) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RowIcon(Icons.Rounded.Schedule, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Automatic daily backup", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (autoBackup) "Runs in the background once a day on Wi-Fi or mobile data" else "Off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = autoBackup,
                            onCheckedChange = onAutoBackup,
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                        )
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ActionRow(
                        Icons.AutoMirrored.Rounded.Logout, "Disconnect", null,
                        tint = MaterialTheme.colorScheme.error, chevron = false, onClick = onDisconnect,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleBadge() {
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RowIcon(icon: ImageVector, tint: Color) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: Color = MaterialTheme.colorScheme.primary,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null && chevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.LIGHT, Icons.Rounded.LightMode, "Light"),
        Triple(ThemeMode.DARK, Icons.Rounded.DarkMode, "Dark"),
        Triple(ThemeMode.SYSTEM, Icons.Rounded.SettingsBrightness, "System"),
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected.ordinal, spring(dampingRatio = 0.72f, stiffness = 420f), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .shadow(3.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { (mode, icon, label) ->
                val active = mode == selected
                val color by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "c",
                )
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(mode) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
