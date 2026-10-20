package com.flashshare.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.flashshare.app.data.Category
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// region avatars
data class AvatarStyle(val emoji: String, val c1: Color, val c2: Color)

val Avatars = listOf(
    AvatarStyle("🦊", Color(0xFFFF9A44), Color(0xFFFF5E62)),
    AvatarStyle("🐼", Color(0xFF6A85FF), Color(0xFF9B6BFF)),
    AvatarStyle("🦁", Color(0xFFFFC233), Color(0xFFFF8A00)),
    AvatarStyle("🐯", Color(0xFFFF7A59), Color(0xFFFFB347)),
    AvatarStyle("🐨", Color(0xFF7FC8FF), Color(0xFF4F7BFF)),
    AvatarStyle("🐸", Color(0xFF4CD99A), Color(0xFF15A87A)),
    AvatarStyle("🦄", Color(0xFFFF8AD8), Color(0xFFA36BFF)),
    AvatarStyle("🐙", Color(0xFFFF6B9A), Color(0xFFFF4D6D)),
    AvatarStyle("🐧", Color(0xFF5BC0FF), Color(0xFF2F6BFF)),
    AvatarStyle("🐶", Color(0xFFFFB86B), Color(0xFFE8833A)),
    AvatarStyle("🐱", Color(0xFFB28BFF), Color(0xFF6D5BFF)),
    AvatarStyle("🚀", Color(0xFF3D7BFF), Color(0xFF00C2FF)),
)

/** Shows a custom photo when available, otherwise the selected emoji avatar on its gradient. */
@Composable
fun Avatar(avatarId: Int, image: ByteArray?, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val bmp = remember(image) { image?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() } }
    AvatarImpl(avatarId, bmp, size, modifier, ring)
}

@Composable
fun AvatarImpl(avatarId: Int, bmp: ImageBitmap?, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val style = Avatars[Math.floorMod(avatarId, Avatars.size)]
    val ringMod = if (ring) Modifier.border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape) else Modifier
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(ringMod)
            .background(Brush.linearGradient(listOf(style.c1, style.c2))),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (avatarId < 0) {
            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        } else {
            Text(style.emoji, fontSize = (size.value * 0.5f).sp)
        }
    }
}
// endregion

// region buttons & chips
fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 600f), label = "press")
    this.scale(s)
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = Brand.main,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(src)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(if (enabled) brush else Brush.linearGradient(listOf(Color(0xFFB8BFD0), Color(0xFFA3ABBE))))
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(color = Color.White), enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Brand.premium)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("PRO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun InfoChip(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val extra = LocalExtra.current
    val src = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressScale(src) else Modifier)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, extra.cardBorder, MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick) else Modifier),
        content = content,
    )
}
// endregion

// region scaffold
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = bottomBar,
        content = content,
    )
}
// endregion

// region file visuals
fun categoryIcon(c: Category): ImageVector = when (c) {
    Category.APP -> Icons.Rounded.Android
    Category.PHOTO -> Icons.Rounded.Image
    Category.VIDEO -> Icons.Rounded.Movie
    Category.MUSIC -> Icons.Rounded.MusicNote
    Category.CONTACT -> Icons.Rounded.Contacts
    Category.FILE -> Icons.Rounded.Description
}

fun categoryColor(c: Category): Color = when (c) {
    Category.APP -> Color(0xFF00B07A)
    Category.PHOTO -> Color(0xFF2F7BFF)
    Category.VIDEO -> Color(0xFFFF4D6D)
    Category.MUSIC -> Color(0xFFFF8A00)
    Category.CONTACT -> Color(0xFF00A3FF)
    Category.FILE -> Color(0xFF7B4DFF)
}

/** Thumbnail for photos/videos (Coil), real icon for apps, colored category icon for everything else. */
@Composable
fun FileThumb(uri: String?, category: Category, size: Dp, modifier: Modifier = Modifier, packageName: String? = null, corner: Dp = 12.dp) {
    val color = categoryColor(category)
    Box(
        modifier.size(size).clip(RoundedCornerShape(corner)).background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoryIcon(category), null, tint = color, modifier = Modifier.size(size * 0.48f))
        when {
            packageName != null -> AppIcon(packageName, size * 0.82f)
            uri != null && (category == Category.PHOTO || category == Category.VIDEO) -> {
                val ctx = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(Uri.parse(uri)).videoFrameMillis(1000).size(256).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private val iconCache = android.util.LruCache<String, ImageBitmap>(200)

@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val icon by produceState(iconCache.get(packageName), packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.packageManager.getApplicationIcon(packageName).toBitmap(144, 144).asImageBitmap()
                        .also { iconCache.put(packageName, it) }
                }.getOrNull()
            }
        }
    }
    icon?.let { Image(it, null, modifier.size(size).clip(RoundedCornerShape(size / 4))) }
}
// endregion

// region QR
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, dark: Color = Color(0xFF0E1426)) {
    val argb = dark.let { (0xFF shl 24) or ((it.red * 255).toInt() shl 16) or ((it.green * 255).toInt() shl 8) or (it.blue * 255).toInt() }
    val bmp by produceState<ImageBitmap?>(null, content) {
        value = withContext(Dispatchers.Default) { makeQr(content, 720, argb) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        bmp?.let { Image(it, "QR code", Modifier.fillMaxSize(), contentScale = ContentScale.Fit, filterQuality = androidx.compose.ui.graphics.FilterQuality.None) }
            ?: CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
    }
}

fun makeQr(content: String, size: Int, darkArgb: Int): ImageBitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 0, EncodeHintType.CHARACTER_SET to "UTF-8")
    val m = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val w = m.width; val h = m.height
    val px = IntArray(w * h)
    for (y in 0 until h) {
        val off = y * w
        for (x in 0 until w) px[off + x] = if (m.get(x, y)) darkArgb else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
}
// endregion

// region animations
/** Expanding rings behind an avatar while waiting/connecting. */
@Composable
fun RadarPulse(color: Color, modifier: Modifier = Modifier, rings: Int = 3) {
    val t = rememberInfiniteTransition(label = "radar")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "p")
    Canvas(modifier) {
        val maxR = size.minDimension / 2
        for (i in 0 until rings) {
            val f = (p + i.toFloat() / rings) % 1f
            drawCircle(color.copy(alpha = (1f - f) * 0.35f), radius = maxR * (0.3f + 0.7f * f))
            drawCircle(color.copy(alpha = (1f - f) * 0.6f), radius = maxR * (0.3f + 0.7f * f), style = Stroke(2.dp.toPx()))
        }
    }
}

/** Animated dotted beam between two avatars on the transfer screen. */
@Composable
fun TransferBeam(active: Boolean, reverse: Boolean, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val t = rememberInfiniteTransition(label = "beam")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "shift")
    Canvas(modifier) {
        val dots = 9
        val gap = size.width / dots
        val y = size.height / 2
        for (i in 0 until dots) {
            var x = (i + if (active) shift else 0f) * gap
            if (reverse) x = size.width - x
            val edge = 1f - kotlin.math.abs(x / size.width - 0.5f) * 2f
            drawCircle(color.copy(alpha = (0.25f + 0.75f * edge).coerceIn(0f, 1f)), radius = 3.dp.toPx() + 2.dp.toPx() * edge, center = Offset(x, y))
        }
    }
}

/** Big progress ring with gradient sweep. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 14.dp, brush: Brush = Brand.main) {
    val animated by animateFloatAsState(progress, tween(260, easing = FastOutSlowInEasing), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val s = stroke.toPx()
        val inset = s / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - s, size.height - s)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(s))
        drawArc(brush, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(s, cap = StrokeCap.Round))
    }
}

@Composable
fun Shimmer(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier.graphicsLayer { alpha = a }.background(MaterialTheme.colorScheme.surfaceVariant))
}
// endregion

@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(16.dp)); action() }
    }
}

@Composable
fun LoadingDialog(text: String) {
    androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun clickableNoRipple(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    return Modifier.clickable(interactionSource = src, indication = null, onClick = onClick)
}
