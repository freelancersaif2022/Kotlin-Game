package com.saif.pymaster.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.data.Content
import com.saif.pymaster.data.Profile
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.Fonts
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.ImageUtils
import com.saif.pymaster.util.LocalHaptics
import com.saif.pymaster.util.PythonHighlighter

val CardShape = RoundedCornerShape(20.dp)
val PrimaryGradient = Brush.horizontalGradient(listOf(Brand.PythonBlue, Color(0xFF1E9BD7), Brand.Cyan))
val GoldGradient = Brush.horizontalGradient(listOf(Color(0xFFFFC233), Brand.PythonYellow, Color(0xFFFFE89A)))

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    border: Color = LocalAppColors.current.stroke,
    background: Color = LocalAppColors.current.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHaptics.current
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(background)
            .border(1.dp, border, CardShape)
            .then(if (onClick != null) Modifier.clickable { haptics?.tick(); onClick() } else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    brush: Brush = PrimaryGradient,
    textColor: Color = Brand.White,
    onClick: () -> Unit,
) {
    val haptics = LocalHaptics.current
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) brush else Brush.horizontalGradient(listOf(Color(0xFF2A3242), Color(0xFF2A3242))))
            .clickable(enabled = enabled) { haptics?.click(); onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, null, tint = if (enabled) textColor else Brand.TextMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) textColor else Brand.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val haptics = LocalHaptics.current
    Surface(
        onClick = { haptics?.tick(); onClick() },
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        color = LocalAppColors.current.surfaceHigh,
        border = BorderStroke(1.dp, LocalAppColors.current.stroke),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = Brand.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) Brand.White else Brand.TextMuted, maxLines = 1)
        }
    }
}

@Composable
fun ScreenTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Brand.White)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Brand.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Brand.White, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                color = Brand.Cyan,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp,
    track: Color = Color(0x1FFFFFFF),
    brush: Brush = Brush.sweepGradient(listOf(Brand.Cyan, Brand.PythonBlueBright, Brand.PythonYellow, Brand.Cyan)),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(900), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            if (animated > 0f) {
                drawArc(brush, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(s, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

@Composable
fun GradientProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    brush: Brush = PrimaryGradient,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color(0x1FFFFFFF))
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(brush)
            )
        }
    }
}

@Composable
fun AvatarImage(profile: Profile?, size: Dp, modifier: Modifier = Modifier, ring: Boolean = true) {
    val custom = profile?.takeIf { it.avatarIndex < 0 }?.customAvatarPath
    val bitmap = remember(custom) { ImageUtils.loadBitmap(custom)?.asImageBitmap() }
    val ringMod = if (ring) Modifier.border(2.dp, Brush.linearGradient(listOf(Brand.Cyan, Brand.PythonYellow)), CircleShape) else Modifier
    Box(modifier.size(size).then(ringMod).padding(if (ring) 3.dp else 0.dp).clip(CircleShape)) {
        if (bitmap != null) {
            Image(bitmap, "Avatar", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            val index = (profile?.avatarIndex ?: 0).coerceIn(0, Content.avatars.lastIndex)
            Image(painterResource(Content.avatars[index]), "Avatar", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun StatTile(icon: ImageVector, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    AppCard(modifier, padding = PaddingValues(14.dp)) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Brand.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Tag(text: String, color: Color = Brand.Cyan, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun LockBadge(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xCC000000))
            .border(1.dp, Color(0x33FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Lock, "Locked", tint = Brand.PythonYellow, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun EmptyState(image: Int, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(image), null, Modifier.size(180.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Brand.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Brand.TextSecondary, textAlign = TextAlign.Center)
    }
}

/** A code snippet styled like an editor window, with optional output and a "Run" button. */
@Composable
fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    output: String = "",
    title: String = "main.py",
    onRun: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val highlighted = remember(code) { PythonHighlighter.highlight(code) }
    val colors = LocalAppColors.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.terminal)
            .border(1.dp, Color(0x2600D1FF), RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0x0FFFFFFF))
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(Color(0xFFFF5F57), Color(0xFFFEBC2E), Color(0xFF28C840)).forEach {
                Box(Modifier.size(10.dp).clip(CircleShape).background(it))
                Spacer(Modifier.width(6.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = Brand.TextSecondary, modifier = Modifier.weight(1f))
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.ContentCopy, "Copy", tint = Brand.TextSecondary, modifier = Modifier.size(18.dp))
            }
            if (onRun != null) {
                IconButton(onClick = onRun, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.PlayArrow, "Run in terminal", tint = Brand.Success, modifier = Modifier.size(22.dp))
                }
            }
        }
        SelectionContainer {
            Text(
                highlighted,
                style = TextStyle(fontFamily = Fonts.Mono, fontSize = 13.sp, lineHeight = 20.sp),
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp),
            )
        }
        if (output.isNotBlank()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text("OUTPUT", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp), color = Brand.Success)
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        output.trimEnd('\n'),
                        style = TextStyle(fontFamily = Fonts.Mono, fontSize = 13.sp, lineHeight = 20.sp, color = Color(0xFFD7E3F1)),
                        softWrap = false,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

@Composable
fun IconCircle(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Brand.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = Brand.White)
    }
}
