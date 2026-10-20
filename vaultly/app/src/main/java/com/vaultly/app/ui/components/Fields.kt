package com.vaultly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vaultly.app.ui.theme.VaultlyTheme

/** Labelled input in a soft card; the border turns gold while focused. */
@Composable
fun VaultField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    singleLine: Boolean = true,
    minHeight: Int = 0,
    isError: Boolean = false,
    leading: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        isError -> colors.danger
        focused -> colors.gold
        else -> colors.hairline
    }
    Column(
        modifier
            .clip(shape)
            .background(colors.card)
            .border(if (focused || isError) 1.5.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (isError) colors.danger else colors.muted)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Text(leading, style = MaterialTheme.typography.titleMedium, color = colors.muted)
                Spacer(Modifier.width(6.dp))
            }
            Box(Modifier.weight(1f).defaultMinSize(minHeight = minHeight.dp)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.titleMedium, color = colors.muted.copy(alpha = 0.6f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(colors.gold),
                    keyboardOptions = KeyboardOptions(
                        capitalization = capitalization,
                        keyboardType = keyboardType,
                        imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
                    ),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                )
            }
        }
    }
}

/** Read-only field that opens a picker. */
@Composable
fun PickerField(label: String, value: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaultlyTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.muted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
        Icon(icon, null, Modifier.size(20.dp), tint = colors.muted)
    }
}

/** Pill-shaped choice used for presets (return window, warranty, category). */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val colors = VaultlyTheme.colors
    val bg = if (selected) MaterialTheme.colorScheme.primary else colors.card
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else colors.hairline, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(16.dp), tint = fg)
            Spacer(Modifier.width(6.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}
