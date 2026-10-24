package com.pulse.messenger.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pulse.messenger.ui.theme.PulseTheme

data class DialogButton(val text: String, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * Base dialog used across the app: rounded card, optional tinted icon badge, title, body slot
 * and right-aligned actions. Enters with a gentle scale/fade spring.
 */
@Composable
fun PulseDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    buttons: List<DialogButton> = emptyList(),
    stackedButtons: Boolean = false,
    dismissOnOutside: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = dismissOnOutside, usePlatformDefaultWidth = false),
    ) {
        val scale = remember { Animatable(0.92f) }
        LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value; scaleY = scale.value
                    alpha = ((scale.value - 0.92f) / 0.08f).coerceIn(0f, 1f)
                },
        ) {
            Column(Modifier.padding(top = 24.dp, bottom = 12.dp)) {
                if (icon != null) {
                    Box(
                        Modifier
                            .padding(horizontal = 24.dp)
                            .size(48.dp)
                            .background(iconTint.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp)) }
                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (message != null) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                        color = PulseTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                if (content != null) {
                    Spacer(Modifier.height(if (message != null || title != null) 12.dp else 0.dp))
                    content()
                }
                if (buttons.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    if (stackedButtons) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.End,
                        ) { buttons.forEach { DialogTextButton(it) } }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End,
                        ) { buttons.forEach { DialogTextButton(it); Spacer(Modifier.width(4.dp)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTextButton(b: DialogButton) {
    TextButton(onClick = b.onClick, shape = RoundedCornerShape(20.dp)) {
        Text(
            b.text,
            color = if (b.destructive) PulseTheme.colors.danger else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String?,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    icon: ImageVector? = null,
    checkboxLabel: String? = null,
    checkboxInitial: Boolean = false,
    onConfirmWithCheck: ((Boolean) -> Unit)? = null,
) {
    var checked by remember { mutableStateOf(checkboxInitial) }
    PulseDialog(
        onDismiss = onDismiss,
        title = title,
        message = message,
        icon = icon,
        iconTint = if (destructive) PulseTheme.colors.danger else MaterialTheme.colorScheme.primary,
        buttons = listOf(
            DialogButton("Cancel", onClick = onDismiss),
            DialogButton(confirmText, destructive = destructive) {
                onConfirmWithCheck?.invoke(checked) ?: onConfirm()
                onDismiss()
            },
        ),
    ) {
        if (checkboxLabel != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { checked = !checked }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked, onCheckedChange = { checked = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Text(checkboxLabel, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
    confirmMode: Boolean = false,
) {
    var current by remember { mutableStateOf(selected) }
    PulseDialog(
        onDismiss = onDismiss,
        title = title,
        message = description,
        buttons = if (confirmMode) listOf(
            DialogButton("Cancel", onClick = onDismiss),
            DialogButton("OK") { onSelect(current); onDismiss() },
        ) else emptyList(),
    ) {
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            current = option
                            if (!confirmMode) {
                                onSelect(option); onDismiss()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == current,
                        onClick = null,
                        modifier = Modifier.padding(12.dp),
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                    )
                    Text(label(option), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (!confirmMode) Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun MultiChoiceDialog(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    PulseDialog(
        onDismiss = onDismiss,
        title = title,
        buttons = listOf(
            DialogButton("Cancel", onClick = onDismiss),
            DialogButton("OK") { onConfirm(current); onDismiss() },
        ),
    ) {
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { current = if (option in current) current - option else current + option }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = option in current, onCheckedChange = null, modifier = Modifier.padding(12.dp),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Text(option, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun InputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String = "",
    maxLength: Int = 100,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    confirmText: String = "Save",
    allowEmpty: Boolean = false,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    PulseDialog(
        onDismiss = onDismiss,
        title = title,
        buttons = listOf(
            DialogButton("Cancel", onClick = onDismiss),
            DialogButton(confirmText) {
                if (allowEmpty || value.text.isNotBlank()) {
                    onConfirm(value.text.trim()); onDismiss()
                }
            },
        ),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.text.length <= maxLength) value = it },
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 5,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            supportingText = {
                Text(
                    "${maxLength - value.text.length}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .focusRequester(focus),
        )
    }
}

@Composable
fun LoadingDialog(message: String) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 10.dp) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(20.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
