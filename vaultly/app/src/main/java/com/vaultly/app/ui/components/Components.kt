package com.vaultly.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultly.app.data.Category
import com.vaultly.app.data.Item
import com.vaultly.app.domain.DeadlineKind
import com.vaultly.app.domain.Deadlines
import com.vaultly.app.domain.Urgency
import com.vaultly.app.ui.theme.Palette
import com.vaultly.app.ui.theme.VaultlyTheme
import com.vaultly.app.util.Format

fun Category.icon(): ImageVector = when (this) {
    Category.ELECTRONICS -> Icons.Outlined.Devices
    Category.APPLIANCES -> Icons.Outlined.Kitchen
    Category.FASHION -> Icons.Outlined.Checkroom
    Category.HOME -> Icons.Outlined.Weekend
    Category.BEAUTY -> Icons.Outlined.Spa
    Category.SPORTS -> Icons.Outlined.SportsTennis
    Category.TOOLS -> Icons.Outlined.Handyman
    Category.TOYS -> Icons.Outlined.ChildCare
    Category.GROCERY -> Icons.Outlined.LocalGroceryStore
    Category.OTHER -> Icons.Outlined.Inventory2
}

@Composable
fun urgencyColor(urgency: Urgency): Color {
    val c = VaultlyTheme.colors
    return when (urgency) {
        Urgency.URGENT -> c.danger
        Urgency.SOON -> c.warning
        Urgency.SAFE -> c.success
        Urgency.EXPIRED, Urgency.CLOSED -> c.muted
    }
}

@Composable
fun daysColor(days: Long): Color {
    val c = VaultlyTheme.colors
    return when {
        days < 0 -> c.muted
        days <= Deadlines.URGENT_DAYS -> c.danger
        days <= Deadlines.SOON_DAYS -> c.warning
        else -> c.success
    }
}

/** Surface card with a hairline border – the base building block of every screen. */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(VaultlyTheme.colors.card)
            .border(1.dp, VaultlyTheme.colors.hairline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun CategoryBadge(category: Category, size: Dp = 46.dp, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon(), null, Modifier.size(size * 0.48f), tint = tint)
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

/** Thin rounded progress track. */
@Composable
fun Track(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 5.dp) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "track")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Circular countdown showing how much of a window remains. */
@Composable
fun CountdownRing(
    fraction: Float,
    color: Color,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 86.dp,
    stroke: Dp = 7.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(trackColor, -90f, 360f, false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = MaterialTheme.colorScheme.onSurface)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = VaultlyTheme.colors.muted)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = VaultlyTheme.colors.muted,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** Main call to action: ink in light mode, champagne gold in dark mode. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    gold: Boolean = false,
    danger: Boolean = false,
) {
    val colors = VaultlyTheme.colors
    val brush: Brush = when {
        danger -> Brush.linearGradient(listOf(Color(0xFFE5484D), Color(0xFFC62F35)))
        gold || colors.dark -> colors.goldBrush
        else -> colors.inkBrush
    }
    val content = if (danger) Color.White else if (gold || colors.dark) Palette.Ink else Color.White
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .height(58.dp)
            .clip(shape)
            .background(brush, alpha = if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = content, strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge, color = content)
            }
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .height(58.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(VaultlyTheme.colors.goldBrush)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink, letterSpacing = 1.sp)
    }
}

/** A purchase in the vault list: category, title, store, next deadline track and a status pill. */
@Composable
fun ItemCard(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val urgency = Deadlines.urgency(item)
    val color = urgencyColor(urgency)
    val next = Deadlines.next(item)
    VaultCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(item.categoryEnum)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${item.store} · ${Format.shortDate(Deadlines.purchaseDate(item))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = VaultlyTheme.colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                item.price?.let {
                    Text(Format.money(it, item.currency), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                }
                val label = when (urgency) {
                    Urgency.CLOSED -> item.statusEnum.name.lowercase().replaceFirstChar { it.uppercase() }
                    Urgency.EXPIRED -> "Expired"
                    else -> next?.let { Deadlines.shortLeft(it.daysLeft()) } ?: "Kept"
                }
                StatusPill(label, color)
            }
        }
        if (next != null && urgency != Urgency.CLOSED) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (next.kind == DeadlineKind.RETURN) "Return window" else "Warranty",
                    style = MaterialTheme.typography.labelMedium,
                    color = VaultlyTheme.colors.muted,
                    modifier = Modifier.width(98.dp),
                )
                Track(next.remainingFraction(), color, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                Text(
                    Format.shortDate(next.end),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
