package com.twinspace.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinspace.app.core.AppEntry
import com.twinspace.app.core.IconCache
import com.twinspace.app.ui.components.ActionRow
import com.twinspace.app.ui.components.AppIcon
import com.twinspace.app.ui.theme.LocalExtraColors
import kotlinx.coroutines.launch

enum class AppAction { OPEN, RENAME, SHORTCUT, FREEZE, INFO, REMOVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    entry: AppEntry,
    name: String,
    icons: IconCache,
    shortcutsSupported: Boolean,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current

    fun pick(action: AppAction) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            onAction(action)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainer,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(entry, icons, size = 52.dp, showBadge = true, dimmed = entry.frozen)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (entry.frozen) "Frozen · ${entry.packageName}" else entry.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.outlineVariant)
            Spacer(Modifier.height(4.dp))
            ActionRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open", subtitle = "Launch the clone", onClick = { pick(AppAction.OPEN) }, enabled = !entry.frozen)
            ActionRow(Icons.Rounded.DriveFileRenameOutline, "Rename", subtitle = "Use your own name for this clone", tint = colors.tertiary, onClick = { pick(AppAction.RENAME) })
            ActionRow(
                Icons.AutoMirrored.Rounded.AddToHomeScreen,
                "Add to home screen",
                subtitle = if (shortcutsSupported) "Create a shortcut with the clone badge" else "Not supported by your launcher",
                tint = extra.success,
                enabled = shortcutsSupported,
                onClick = { pick(AppAction.SHORTCUT) },
            )
            ActionRow(
                if (entry.frozen) Icons.Rounded.WbSunny else Icons.Rounded.AcUnit,
                if (entry.frozen) "Unfreeze" else "Freeze",
                subtitle = if (entry.frozen) "Make the clone usable again" else "Stop it running in the background. Data is kept.",
                tint = extra.warning,
                onClick = { pick(AppAction.FREEZE) },
            )
            ActionRow(Icons.Rounded.Info, "App info", subtitle = "Storage, permissions and notifications", tint = colors.secondary, enabled = !entry.frozen, onClick = { pick(AppAction.INFO) })
            ActionRow(
                Icons.Rounded.DeleteOutline,
                "Remove clone",
                subtitle = "Deletes only this clone and its data",
                tint = colors.error,
                titleColor = colors.error,
                onClick = { pick(AppAction.REMOVE) },
            )
        }
    }
}

@Composable
fun RenameDialog(
    title: String,
    current: String,
    original: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val valid = value.text.isNotBlank() && value.text.length <= 40

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    isError = value.text.length > 40,
                    supportingText = { Text("${value.text.length}/40") },
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(value.text) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (original != null && current != original) {
                    TextButton(onClick = { onConfirm(null) }) { Text("Reset to \"$original\"") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    destructive: Boolean = true,
    requireCheck: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var checked by rememberSaveable { mutableStateOf(requireCheck == null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(body, style = MaterialTheme.typography.bodyMedium)
                if (requireCheck != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = { checked = it })
                        Text(requireCheck, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = checked,
                colors = if (destructive) {
                    androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    androidx.compose.material3.ButtonDefaults.textButtonColors()
                },
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
