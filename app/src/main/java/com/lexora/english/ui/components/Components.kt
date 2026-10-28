package com.lexora.english.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lexora.english.R
import com.lexora.english.data.UserProfile
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import java.io.File

val Avatars = listOf(
    R.drawable.avatar_01, R.drawable.avatar_02, R.drawable.avatar_03, R.drawable.avatar_04, R.drawable.avatar_05,
    R.drawable.avatar_06, R.drawable.avatar_07, R.drawable.avatar_08, R.drawable.avatar_09, R.drawable.avatar_10,
)

val BrandGradient = Brush.linearGradient(listOf(Brand.Blue, Brand.Indigo))

/** Asynchronously decoded, size-aware image (keeps scrolling smooth). */
@Composable
fun AppImage(
    model: Any,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    crossfade: Boolean = true,
) {
    val context = LocalContext.current
    val request = remember(model, crossfade) {
        ImageRequest.Builder(context).data(model).crossfade(if (crossfade) 220 else 0).build()
    }
    AsyncImage(model = request, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
}

@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun LexCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Lex.colors.card,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Lex.colors
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    var m = modifier
    if (onClick != null) m = m.pressScale(interaction)
    if (elevated && !c.isDark) m = m.shadow(10.dp, shape, ambientColor = Color(0x14101828), spotColor = Color(0x1A101828))
    m = m.clip(shape).background(color).border(1.dp, c.border, shape)
    if (onClick != null) m = m.clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    val bg = if (enabled) BrandGradient else Brush.linearGradient(listOf(Lex.colors.track, Lex.colors.track))
    Box(
        modifier
            .height(height)
            .pressScale(interaction)
            .then(if (enabled && !Lex.colors.isDark) Modifier.shadow(12.dp, shape, spotColor = Brand.Blue.copy(alpha = 0.45f), ambientColor = Brand.Blue.copy(alpha = 0.2f)) else Modifier)
            .clip(shape)
            .background(bg)
            .clickable(interaction, androidx.compose.material3.ripple(color = Color.White), enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else Lex.colors.textMuted,
                maxLines = 1,
            )
            if (icon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(icon, null, tint = if (enabled) Color.White else Lex.colors.textMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 54.dp,
) {
    val c = Lex.colors
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(height)
            .pressScale(interaction)
            .clip(shape)
            .background(c.primarySoft)
            .clickable(interaction, androidx.compose.material3.ripple(), enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            if (icon != null) {
                Icon(icon, null, tint = c.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleSmall, color = if (enabled) c.primary else c.textMuted, maxLines = 1)
        }
    }
}

@Composable
fun LinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Lex.colors.primary,
    track: Color = Lex.colors.track,
    height: Dp = 8.dp,
    brush: Brush? = null,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "lp")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(height)
                    .clip(CircleShape)
                    .then(if (brush != null) Modifier.background(brush) else Modifier.background(color))
            )
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
    color: Color = Lex.colors.primary,
    track: Color = Lex.colors.track,
    brush: Brush? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val d = this.size.minDimension - sw
            val topLeft = Offset(sw / 2, sw / 2)
            drawArc(track, -90f, 360f, false, topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            if (animated > 0f) {
                if (brush != null) {
                    drawArc(brush, -90f, 360f * animated, false, topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                } else {
                    drawArc(color, -90f, 360f * animated, false, topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Lex.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.background)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Lex.colors.text,
    background: Color = Lex.colors.cardAlt,
    size: Dp = 44.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(background)
            .clickable(interaction, androidx.compose.material3.ripple(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Lex.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = c.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Lex.colors.primary,
    background: Color = Lex.colors.primarySoft,
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = if (Lex.colors.isDark) 0.18f else 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun ProfileAvatar(profile: UserProfile?, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val model: Any = when {
        profile == null -> Avatars[0]
        profile.avatarIndex < 0 && profile.photoPath != null -> File(profile.photoPath)
        else -> Avatars[profile.avatarIndex.coerceIn(0, Avatars.lastIndex)]
    }
    Box(
        modifier
            .size(size)
            .then(if (ring) Modifier.border(BorderStroke(2.dp, BrandGradient), CircleShape).padding(3.dp) else Modifier)
            .clip(CircleShape)
            .background(Lex.colors.primarySoft),
    ) {
        AppImage(model, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, contentDescription = "Profile picture")
    }
}

@Composable
fun EmptyState(@DrawableRes image: Int, title: String, body: String, modifier: Modifier = Modifier) {
    val c = Lex.colors
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AppImage(image, Modifier.size(180.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun <T> PillSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Lex.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.cardAlt)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSel) c.card else Color.Transparent)
                    .then(if (isSel) Modifier.border(1.dp, c.border, RoundedCornerShape(11.dp)) else Modifier)
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) c.primary else c.textSecondary,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun StatBlock(value: String, label: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    val c = Lex.colors
    LexCard(modifier, padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        IconTile(icon, tint, size = 36.dp, iconSize = 19.dp)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = c.text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textMuted, maxLines = 1)
    }
}
