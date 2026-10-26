package com.zyropay.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zyropay.app.R
import com.zyropay.app.ui.theme.Z

@Composable
fun ZTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    error: String? = null,
    helper: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = Z.type.body,
    minHeight: androidx.compose.ui.unit.Dp = 54.dp,
    onClick: (() -> Unit)? = null,
) {
    val c = Z.colors
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        when {
            error != null -> c.error
            focused -> c.primary
            else -> Color.Transparent
        },
        tween(160), label = "fieldBorder",
    )
    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = Z.type.label, color = c.textSecondary)
            Spacer(Modifier.height(8.dp))
        }
        val shape = RoundedCornerShape(14.dp)
        Box {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled && onClick == null,
                readOnly = readOnly,
                singleLine = singleLine,
                textStyle = textStyle.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.primary),
                visualTransformation = visualTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(shape)
                    .background(if (focused) c.surface else c.surfaceVariant)
                    .border(1.4.dp, borderColor, shape)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                        if (leading != null) {
                            leading(); Spacer(Modifier.width(10.dp))
                        } else if (leadingIcon != null) {
                            ZIcon(leadingIcon, size = 20.dp, tint = if (focused) c.primary else c.textTertiary)
                            Spacer(Modifier.width(10.dp))
                        }
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty()) Text(placeholder, style = textStyle, color = c.textTertiary, maxLines = if (singleLine) 1 else 4)
                            inner()
                        }
                        if (trailing != null) {
                            Spacer(Modifier.width(8.dp)); trailing()
                        }
                    }
                },
            )
            if (onClick != null) {
                Box(Modifier.matchParentSize().clip(shape).clickable(onClick = onClick))
            }
        }
        AnimatedVisibility(error != null || helper != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (error != null) {
                    ZIcon(R.drawable.ic_info, size = 14.dp, tint = c.error)
                    Spacer(Modifier.width(4.dp))
                }
                Text(error ?: helper ?: "", style = Z.type.caption, color = if (error != null) c.error else c.textTertiary)
            }
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "Password",
    placeholder: String = "Enter password",
    error: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    ZTextField(
        value, onValueChange, modifier, label = label, placeholder = placeholder, error = error,
        leadingIcon = R.drawable.ic_lock, keyboardType = KeyboardType.Password, imeAction = imeAction, onImeAction = onImeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation('•'),
        trailing = {
            ZIcon(
                if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye, size = 20.dp, tint = Z.colors.textTertiary,
                modifier = Modifier.clip(CircleShape).clickable { visible = !visible },
            )
        },
    )
}

/** Password requirement checklist with live validation. */
@Composable
fun PasswordStrength(password: String, modifier: Modifier = Modifier) {
    val rules = listOf(
        "8–32 characters" to (password.length in 8..32),
        "At least one uppercase letter" to password.any { it.isUpperCase() },
        "At least one number" to password.any { it.isDigit() },
        "At least one special character" to password.any { !it.isLetterOrDigit() },
    )
    val score = rules.count { it.second }
    val c = Z.colors
    Column(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                val col by animateColorAsState(
                    if (i < score) when (score) { 1 -> c.error; 2 -> c.warning; 3 -> c.info; else -> c.success } else c.outline,
                    label = "strength",
                )
                Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(col))
            }
        }
        Spacer(Modifier.height(10.dp))
        rules.forEach { (text, ok) ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                val col by animateColorAsState(if (ok) c.success else c.textTertiary, label = "rule")
                ZIcon(if (ok) R.drawable.ic_check_circle else R.drawable.ic_status_pending, size = 15.dp, tint = col)
                Spacer(Modifier.width(8.dp))
                Text(text, style = Z.type.caption, color = if (ok) c.textPrimary else c.textSecondary)
            }
        }
    }
}

fun isStrongPassword(p: String) = p.length in 8..32 && p.any { it.isUpperCase() } && p.any { it.isDigit() } && p.any { !it.isLetterOrDigit() }

fun isValidEmail(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e.trim()).matches()

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Search") {
    ZTextField(
        value, onValueChange, modifier, placeholder = placeholder, leadingIcon = R.drawable.ic_search, imeAction = ImeAction.Search,
        minHeight = 46.dp, textStyle = Z.type.bodySmall,
        trailing = if (value.isNotEmpty()) {
            { ZIcon(R.drawable.ic_close, size = 18.dp, tint = Z.colors.textTertiary, modifier = Modifier.clip(CircleShape).clickable { onValueChange("") }) }
        } else null,
    )
}

/** Six separate boxes for OTP entry, backed by one hidden text field. */
@Composable
fun OtpInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, length: Int = 6, error: Boolean = false) {
    val c = Z.colors
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(length)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(length) { i ->
                    val active = focused && i == value.length.coerceAtMost(length - 1)
                    val border by animateColorAsState(
                        when {
                            error -> c.error
                            active -> c.primary
                            i < value.length -> c.outline
                            else -> Color.Transparent
                        },
                        label = "otp",
                    )
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (active) c.surface else c.surfaceVariant)
                            .border(1.4.dp, border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(value.getOrNull(i)?.toString() ?: "", style = Z.type.h2, color = c.textPrimary)
                        if (active && i >= value.length) {
                            BlinkingCursor()
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun BlinkingCursor() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "cursor")
    val a by t.animateFloat(
        1f, 0f,
        androidx.compose.animation.core.infiniteRepeatable(tween(520), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Box(Modifier.size(width = 2.dp, height = 24.dp).background(Z.colors.primary.copy(alpha = a)))
}
