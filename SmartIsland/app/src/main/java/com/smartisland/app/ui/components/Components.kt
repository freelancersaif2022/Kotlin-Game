package com.smartisland.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartisland.app.ui.theme.Palette

/** Press-to-shrink feedback used by every tappable card and button. */
@Composable
fun Modifier.pressable(onClick: () -> Unit, enabled: Boolean = true): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 900f), label = "press")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Palette.Primary,
    enabled: Boolean = true,
) {
    val brush = if (color == Palette.Primary) Palette.PrimaryGradient else SolidColor(color)
    Row(
        modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) brush else SolidColor(Palette.SurfaceHigh))
            .pressable(onClick, enabled)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) Color.White else Palette.TextTertiary)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, color: Color = Palette.TextPrimary) {
    Row(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.SurfaceHigh)
            .pressable(onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush ?: SolidColor(Palette.Surface))
            .border(1.dp, Palette.Outline, shape)
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 38.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.GoldGradient)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF3A2600), modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(2.dp))
        Text("PRO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF3A2600))
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = {
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.TextTertiary)
    },
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, iconColor)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    locked: Boolean = false,
    onLockedClick: () -> Unit = {},
) {
    SettingRow(
        icon = icon,
        iconColor = iconColor,
        title = title,
        subtitle = subtitle,
        onClick = { if (locked) onLockedClick() else onCheckedChange(!checked) },
        trailing = {
            if (locked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProBadge()
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Lock, null, tint = Palette.TextTertiary, modifier = Modifier.size(18.dp))
                }
            } else {
                AppSwitch(checked, onCheckedChange)
            }
        },
    )
}

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Palette.Primary,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color(0xFFB0B0BA),
            uncheckedTrackColor = Palette.SurfaceHigh,
            uncheckedBorderColor = Color(0x33FFFFFF),
        ),
    )
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
    icon: ImageVector? = null,
    color: Color = Palette.Primary,
    steps: Int = 0,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Palette.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
            val chipColor by animateColorAsState(color.copy(alpha = 0.16f), label = "chip")
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = color,
                inactiveTrackColor = Palette.SurfaceHigh,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)?, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Palette.SurfaceHigh)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Palette.TextPrimary)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun TabHeader(title: String, subtitle: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Palette.TextPrimary)
        }
        trailing?.invoke()
    }
}

@Composable
fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(Palette.Outline),
    )
}
