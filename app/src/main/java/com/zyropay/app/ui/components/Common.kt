package com.zyropay.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zyropay.app.R
import com.zyropay.app.data.KycStatus
import com.zyropay.app.data.MockData
import com.zyropay.app.data.TxStatus
import com.zyropay.app.ui.theme.Z

@Composable
fun ZIcon(@DrawableRes res: Int, modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = Z.colors.textPrimary) {
    Icon(painterResource(res), contentDescription = null, modifier = modifier.size(size), tint = tint)
}

@Composable
fun ZImage(@DrawableRes res: Int, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    Image(painterResource(res), contentDescription = null, modifier = modifier, contentScale = contentScale)
}

/** Click with a subtle press-scale and the theme ripple. */
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = 0.97f,
    haptic: Boolean = false,
    ripple: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) scaleDown else 1f, spring(stiffness = 900f), label = "press")
    val haptics = LocalHapticFeedback.current
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = source,
            indication = if (ripple) LocalIndication.current else null,
            enabled = enabled,
        ) {
            if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    radius: Dp = 20.dp,
    color: Color = Z.colors.surface,
    border: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Z.colors
    val shape = RoundedCornerShape(radius)
    var m = modifier.clip(shape).background(color)
    if (border && !c.isDark) m = m.border(1.dp, c.outline.copy(alpha = 0.55f), shape)
    if (border && c.isDark) m = m.border(1.dp, c.outline.copy(alpha = 0.35f), shape)
    if (onClick != null) m = m.pressable(scaleDown = 0.985f, onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Z.type.title, color = Z.colors.textPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action, style = Z.type.label, color = Z.colors.primary)
                ZIcon(R.drawable.ic_chevron_right, size = 16.dp, tint = Z.colors.primary)
            }
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(modifier.padding(start = inset).fillMaxWidth().height(1.dp).background(Z.colors.divider))
}

@Composable
fun IconCircle(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    background: Color = Z.colors.surfaceVariant,
    tint: Color = Z.colors.textPrimary,
) {
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        ZIcon(icon, size = iconSize, tint = tint)
    }
}

@Composable
fun CoinIcon(symbol: String, modifier: Modifier = Modifier, size: Dp = 40.dp, network: Int? = null) {
    Box(modifier.size(size)) {
        ZImage(MockData.coin(symbol).icon, Modifier.size(size).clip(CircleShape))
        if (network != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(size * 0.42f)
                    .background(Z.colors.surface, CircleShape).padding(1.5.dp),
            ) { ZImage(network, Modifier.fillMaxWidth().clip(CircleShape)) }
        }
    }
}

@Composable
fun Avatar(index: Int, modifier: Modifier = Modifier, size: Dp = 44.dp, ring: Boolean = false) {
    val res = MockData.avatars.getOrElse(index) { R.drawable.avatar_default }
    Box(
        modifier
            .size(size)
            .then(if (ring) Modifier.border(2.dp, Z.colors.surface, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(Z.colors.primarySoft),
    ) { ZImage(res, Modifier.size(size), ContentScale.Crop) }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Z.colors.primary,
    background: Color = Z.colors.primarySoft,
    @DrawableRes icon: Int? = null,
    style: TextStyle = Z.type.captionMedium,
) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ZIcon(icon, size = 13.dp, tint = color)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = style, color = color, maxLines = 1)
    }
}

@Composable
fun StatusPill(status: TxStatus, modifier: Modifier = Modifier) {
    val c = Z.colors
    when (status) {
        TxStatus.SUCCESS -> Pill("Completed", modifier, c.success, c.successSoft)
        TxStatus.PENDING -> Pill("Processing", modifier, c.warning, c.warningSoft)
        TxStatus.FAILED -> Pill("Failed", modifier, c.error, c.errorSoft)
    }
}

@Composable
fun KycPill(status: KycStatus, modifier: Modifier = Modifier) {
    val c = Z.colors
    when (status) {
        KycStatus.VERIFIED -> Pill("Verified", modifier, c.success, c.successSoft, R.drawable.ic_shield_check)
        KycStatus.PENDING -> Pill("Under review", modifier, c.warning, c.warningSoft, R.drawable.ic_clock)
        KycStatus.REJECTED -> Pill("Rejected", modifier, c.error, c.errorSoft, R.drawable.ic_warning)
        KycStatus.NOT_STARTED -> Pill("Unverified", modifier, c.textSecondary, c.surfaceVariant, R.drawable.ic_user_verify)
    }
}

@Composable
fun ChipButton(text: String, selected: Boolean, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, onClick: () -> Unit) {
    val c = Z.colors
    val bg by animateColorAsState(if (selected) c.textPrimary else c.surface, tween(180), label = "chipBg")
    val fg by animateColorAsState(if (selected) c.background else c.textSecondary, tween(180), label = "chipFg")
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, if (selected) Color.Transparent else c.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ZIcon(icon, size = 16.dp, tint = fg)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Z.type.label, color = fg)
    }
}

/** Pill-shaped segmented control with a sliding indicator. */
@Composable
fun SegmentedTabs(tabs: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = Z.colors
    BoxWithConstraints(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(c.surfaceVariant).padding(4.dp),
    ) {
        val w = maxWidth / tabs.size
        val x by animateDpAsState(w * selected, spring(dampingRatio = 0.85f, stiffness = 600f), label = "seg")
        Box(
            Modifier.offset(x = x).width(w).fillMaxHeight()
                .background(if (c.isDark) c.outline else c.surface, RoundedCornerShape(11.dp)),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            tabs.forEachIndexed { i, t ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp))
                        .clickable(interactionSource = null, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        t,
                        style = Z.type.label.copy(fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (i == selected) c.textPrimary else c.textSecondary,
                    )
                }
            }
        }
    }
}

/** Underlined text tabs (e.g. for transaction filters). */
@Composable
fun UnderlineTabs(tabs: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = Z.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        tabs.forEachIndexed { i, t ->
            val active = i == selected
            Column(
                Modifier.clickable(interactionSource = null, indication = null) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(t, style = Z.type.bodySmallMedium.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium), color = if (active) c.textPrimary else c.textTertiary)
                Spacer(Modifier.height(6.dp))
                val wAnim by animateDpAsState(if (active) 20.dp else 0.dp, label = "ul")
                Box(Modifier.width(wAnim).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.primary))
            }
        }
    }
}

@Composable
fun ZSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Z.colors
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = c.primary,
            checkedBorderColor = c.primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = if (c.isDark) c.outline else Color(0xFFD9DDE5),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Generic settings/list row. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = Z.colors.textPrimary,
    iconBg: Color = Z.colors.surfaceVariant,
    value: String? = null,
    valueColor: Color = Z.colors.textSecondary,
    chevron: Boolean = true,
    titleColor: Color = Z.colors.textPrimary,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Z.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> { leading(); Spacer(Modifier.width(12.dp)) }
            icon != null -> { IconCircle(icon, size = 38.dp, iconSize = 19.dp, background = iconBg, tint = iconTint); Spacer(Modifier.width(12.dp)) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Z.type.bodyMedium, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = Z.type.caption, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = Z.type.bodySmall, color = valueColor, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (chevron && onClick != null) {
            Spacer(Modifier.width(4.dp))
            ZIcon(R.drawable.ic_chevron_right, size = 18.dp, tint = c.textTertiary)
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    ListRow(
        title = title, subtitle = subtitle, icon = icon, modifier = modifier,
        trailing = { ZSwitch(checked, onChange, enabled = enabled) },
        onClick = { if (enabled) onChange(!checked) }, chevron = false,
    )
}

/** Key/value row used on detail & confirmation screens. */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Z.colors.textPrimary,
    copyable: Boolean = false,
    bold: Boolean = false,
    onCopy: (() -> Unit)? = null,
) {
    val c = Z.colors
    Row(modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
        Text(label, style = Z.type.bodySmall, color = c.textSecondary, modifier = Modifier.padding(end = 16.dp))
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = if (bold) Z.type.bodySmallMedium.copy(fontWeight = FontWeight.SemiBold) else Z.type.bodySmallMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f, fill = false),
        )
        if (copyable) {
            Spacer(Modifier.width(6.dp))
            ZIcon(R.drawable.ic_copy, size = 16.dp, tint = c.textTertiary, modifier = Modifier.clip(CircleShape).clickable { onCopy?.invoke() })
        }
    }
}

@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_info,
    color: Color = Z.colors.info,
    background: Color = Z.colors.infoSoft,
    title: String? = null,
) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(background).padding(12.dp)) {
        ZIcon(icon, size = 18.dp, tint = color)
        Spacer(Modifier.width(10.dp))
        Column {
            if (title != null) Text(title, style = Z.type.label.copy(fontWeight = FontWeight.SemiBold), color = Z.colors.textPrimary)
            Text(text, style = Z.type.caption, color = Z.colors.textSecondary)
        }
    }
}

@Composable
fun WarningBanner(text: String, modifier: Modifier = Modifier, title: String? = null) =
    InfoBanner(text, modifier, R.drawable.ic_warning, Z.colors.warning, Z.colors.warningSoft, title)

@Composable
fun EmptyState(
    @DrawableRes illustration: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ZImage(illustration, Modifier.size(170.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = Z.type.h3, color = Z.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = Z.type.bodySmall, color = Z.colors.textSecondary, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            ZButton(action, onClick = onAction, modifier = Modifier.width(200.dp), size = ButtonSize.Medium)
        }
    }
}

fun Modifier.shimmer(): Modifier = composed {
    val c = Z.colors
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "x")
    this.drawWithContent {
        val w = size.width
        drawRect(c.shimmer)
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = if (c.isDark) 0.06f else 0.55f), Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(w * x - w * 0.5f, 0f),
                end = androidx.compose.ui.geometry.Offset(w * x, size.height),
            ),
        )
    }
}

@Composable
fun ShimmerBox(modifier: Modifier, radius: Dp = 10.dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).shimmer())
}

@Composable
fun GrabHandle(modifier: Modifier = Modifier) {
    Box(modifier.padding(vertical = 10.dp).size(width = 38.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Z.colors.outline))
}

@Composable
fun BadgeDot(modifier: Modifier = Modifier, count: Int = 0) {
    if (count <= 0) return
    Box(
        modifier.size(if (count > 9) 18.dp else 16.dp).clip(CircleShape).background(Z.colors.error).border(1.5.dp, Z.colors.background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 9) "9+" else count.toString(), style = Z.type.tiny, color = Color.White)
    }
}

/** Masked balance text helper. */
fun maskIf(hidden: Boolean, text: String) = if (hidden) "••••••" else text
