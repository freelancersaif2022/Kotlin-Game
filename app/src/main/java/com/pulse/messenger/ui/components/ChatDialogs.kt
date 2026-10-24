package com.pulse.messenger.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Report
import androidx.compose.runtime.Composable
import com.pulse.messenger.ui.theme.PulseTheme

private val muteOptions = listOf("8 hours", "1 week", "Always")

@Composable
fun MuteDialog(onMute: (String) -> Unit, onDismiss: () -> Unit) {
    ChoiceDialog(
        title = "Mute notifications",
        description = "Other members won't see that you muted this chat. You'll still be notified if you're mentioned.",
        options = muteOptions,
        selected = muteOptions.first(),
        label = { it },
        onSelect = onMute,
        onDismiss = onDismiss,
        confirmMode = true,
    )
}

@Composable
fun BlockDialog(name: String, onBlock: () -> Unit, onDismiss: () -> Unit, onBlockAndReport: (() -> Unit)? = null) {
    PulseDialog(
        onDismiss = onDismiss,
        icon = Icons.Outlined.Block,
        iconTint = PulseTheme.colors.danger,
        title = "Block $name?",
        message = "Blocked contacts will no longer be able to call you or send you messages. This contact will not be notified.",
        stackedButtons = onBlockAndReport != null,
        buttons = buildList {
            if (onBlockAndReport != null) add(DialogButton("Report and block", destructive = true) { onBlockAndReport(); onDismiss() })
            add(DialogButton("Block", destructive = true) { onBlock(); onDismiss() })
            add(DialogButton("Cancel", onClick = onDismiss))
        },
    )
}

@Composable
fun ReportDialog(name: String, isGroup: Boolean, onReport: (alsoBlockOrExit: Boolean) -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "Report $name?",
        message = "The last 5 messages from this ${if (isGroup) "group" else "contact"} will be forwarded to Pulse. " +
            "If you ${if (isGroup) "exit this group and delete the chat" else "block this contact and delete the chat"}, messages will only be removed from this device.",
        confirmText = "Report",
        destructive = true,
        icon = Icons.Outlined.Report,
        checkboxLabel = if (isGroup) "Exit group and delete chat" else "Block contact and delete chat",
        checkboxInitial = true,
        onConfirm = {},
        onConfirmWithCheck = onReport,
        onDismiss = onDismiss,
    )
}
