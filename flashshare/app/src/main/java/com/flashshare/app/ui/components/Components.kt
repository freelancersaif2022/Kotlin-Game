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
import androidx.compose.ui.res.painterResource
import com.flashshare.app.R
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
/** Illustrated avatars (Avataaars by Pablo Stanley – free for personal and commercial use). */
val AvatarRes = intArrayOf(
    R.drawable.avatar_00, R.drawable.avatar_01, R.drawable.avatar_02, R.drawable.avatar_03,
    R.drawable.avatar_04, R.drawable.avatar_05, R.drawable.avatar_06, R.drawable.avatar_07,
    R.drawable.avatar_08, R.drawable.avatar_09, R.drawable.avatar_10, R.drawable.avatar_11,
    R.drawable.avatar_12, R.drawable.avatar_13, R.drawable.avatar_14, R.drawable.avatar_15,
)
val AvatarCount: Int get() = AvatarRes.size

/** Shows a custom photo when available, otherwise the selected illustrated avatar. */
@Composable
fun Avatar(avatarId: Int, image: ByteArray?, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false, crown: Boolean = false) {
    val bmp = remember(image) { image?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() } }
    AvatarImpl(avatarId, bmp, size, modifier, ring, crown)
}

@Composable
fun AvatarImpl(avatarId: Int, bmp: ImageBitmap?, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false, crown: Boolean = false) {
    Box(modifier.size(size)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFDDE6FF), Color(0xFFEDE3FF)))),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bmp != null -> Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                avatarId < 0 -> Box(Modifier.fillMaxSize().background(Brand.main), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
                }
                else -> Image(
                    painterResource(AvatarRes[Math.floorMod(avatarId, AvatarRes.size)]), null,
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                )
            }
            if (ring) Box(Modifier.fillMaxSize().border(size * 0.045f, Color.White, CircleShape))
        }
        if (crown) CrownBadge(size * 0.38f, Modifier.align(Alignment.BottomEnd).offset(x = size * 0.04f, y = size * 0.04f))
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

fun categoryBrush(c: Category): Brush = when (c) {
    Category.APP -> IconBrushes.mint
    Category.PHOTO -> IconBrushes.cyan
    Category.VIDEO -> IconBrushes.pink
    Category.MUSIC -> IconBrushes.orange
    Category.CONTACT -> IconBrushes.blue
    Category.FILE -> IconBrushes.violet
}

private val thumbCache = android.util.LruCache<Int, ImageBitmap>(300)

/**
 * The real look of a file: sender-provided preview > saved preview > installed app / APK icon >
 * photo/video frame > a colored tile with the file type.
 */
@Composable
fun FileThumb(
    uri: String?,
    category: Category,
    size: Dp,
    modifier: Modifier = Modifier,
    packageName: String? = null,
    corner: Dp = 12.dp,
    thumb: ByteArray? = null,
    thumbPath: String? = null,
    name: String? = null,
) {
    val isAppLike = category == Category.APP
    Box(
        modifier.size(size).clip(RoundedCornerShape(corner))
            .background(if (isAppLike && (thumb != null || packageName != null)) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = remember(thumb) {
            thumb?.let { b ->
                val key = java.util.Arrays.hashCode(b)
                thumbCache.get(key) ?: runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull()?.also { thumbCache.put(key, it) }
            }
        }
        val ctx = LocalContext.current
        when {
            bmp != null -> Image(bmp, null, Modifier.fillMaxSize(), contentScale = if (isAppLike) ContentScale.Fit else ContentScale.Crop)
            thumbPath != null -> AsyncImage(
                model = ImageRequest.Builder(ctx).data(java.io.File(thumbPath)).crossfade(true).build(), contentDescription = null,
                contentScale = if (isAppLike) ContentScale.Fit else ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            packageName != null -> AppIcon(packageName, size)
            isAppLike && uri != null && uri.startsWith("file:") && uri.endsWith(".apk", true) -> ApkIcon(uri, size)
            uri != null && (category == Category.PHOTO || category == Category.VIDEO) -> {
                TypeTile(category, name, size)
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(Uri.parse(uri)).videoFrameMillis(1000).size(256).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> TypeTile(category, name, size)
        }
        if (category == Category.VIDEO && (bmp != null || thumbPath != null || uri != null) && size >= 40.dp) {
            Box(Modifier.size(size * 0.36f).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size * 0.26f))
            }
        }
    }
}

/** Colored tile with the category icon and, for documents, the file extension. */
@Composable
private fun TypeTile(category: Category, name: String?, size: Dp) {
    val ext = name?.substringAfterLast('.', "")?.uppercase()?.take(4).orEmpty()
    Box(Modifier.fillMaxSize().background(categoryBrush(category)), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(categoryIcon(category), null, tint = Color.White, modifier = Modifier.size(size * if (category == Category.FILE && ext.isNotEmpty() && size >= 40.dp) 0.38f else 0.5f))
            if (category == Category.FILE && ext.isNotEmpty() && size >= 40.dp) {
                Text(ext, color = Color.White, fontSize = (size.value * 0.17f).sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

private val apkCache = android.util.LruCache<String, ImageBitmap>(100)

@Composable
fun ApkIcon(uri: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val icon by produceState(apkCache.get(uri), uri) {
        if (value == null) value = withContext(Dispatchers.IO) {
            runCatching { com.flashshare.app.transfer.Thumbnailer.appIcon(ctx, null, Uri.parse(uri))?.asImageBitmap()?.also { apkCache.put(uri, it) } }.getOrNull()
        }
    }
    val i = icon
    if (i != null) Image(i, null, modifier.size(size)) else TypeTile(Category.APP, null, size)
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
        val t = rememberInfiniteTransition(label = "empty")
        val y by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)))
            GlossyIcon(icon, Brand.main, 72.dp, Modifier.graphicsLayer { translationY = y })
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
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, strokeCap = StrokeCap.Round)
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
