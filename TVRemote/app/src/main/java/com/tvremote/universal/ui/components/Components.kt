package com.tvremote.universal.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.theme.AppTheme
import com.tvremote.universal.ui.theme.GoldDeep
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ haptics

private object Haptics {
    private var vibrator: Vibrator? = null

    fun tick(context: Context) {
        val v = vibrator ?: run {
            val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            vibrator = created
            created
        } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                v.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}

@Composable
fun rememberHaptic(): () -> Unit {
    val context = LocalContext.current.applicationContext
    val enabled by App.container.settings.haptics.collectAsStateWithLifecycle()
    return remember(enabled) { { if (enabled) Haptics.tick(context) } }
}

// ------------------------------------------------------------------ brushes

val goldBrush: Brush get() = Brush.linearGradient(listOf(Color(0xFFFFD86B), Color(0xFFF5B400), GoldDeep))

@Composable
fun brandBrush(): Brush {
    val s = MaterialTheme.colorScheme
    return Brush.linearGradient(listOf(s.primary, s.secondary))
}

@Composable
fun heroBrush(): Brush {
    val e = AppTheme.extra
    return Brush.linearGradient(listOf(e.heroStart, e.heroEnd))
}

// ------------------------------------------------------------------ press handling

/** Fires [onPress] on touch-down (like a physical remote) and auto-repeats while held if [repeat]. */
@Composable
fun Modifier.remotePress(
    repeat: Boolean = false,
    enabled: Boolean = true,
    onPressedChange: (Boolean) -> Unit = {},
    onPress: () -> Unit,
): Modifier {
    val haptic = rememberHaptic()
    val action by rememberUpdatedState(onPress)
    val pressedChange by rememberUpdatedState(onPressedChange)
    return this.pointerInput(enabled, repeat) {
        if (!enabled) return@pointerInput
        detectTapGestures(onPress = {
            pressedChange(true)
            haptic()
            action()
            if (repeat) {
                coroutineScope {
                    val job = launch {
                        delay(420)
                        while (isActive) {
                            action()
                            haptic()
                            delay(115)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                }
            } else {
                tryAwaitRelease()
            }
            pressedChange(false)
        })
    }
}

// ------------------------------------------------------------------ remote keys

/** A physical-looking key: soft gradient body, top sheen, rim and drop shadow that compresses on press. */
@Composable
fun KeyButton(
    onPress: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    shape: Shape = CircleShape,
    repeat: Boolean = false,
    enabled: Boolean = true,
    tint: Color = AppTheme.extra.keyIcon,
    background: Brush? = null,
    content: @Composable () -> Unit,
) {
    val extra = AppTheme.extra
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.91f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "keyScale")
    val bg = background ?: Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom))
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.38f
            }
            .shadow(if (pressed) 1.dp else 6.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(shape)
            .background(bg)
            .background(Brush.verticalGradient(0f to extra.keyHighlight, 0.45f to Color.Transparent))
            .border(1.dp, extra.keyBorder, shape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            }
            .remotePress(repeat = repeat, enabled = enabled, onPressedChange = { pressed = it }, onPress = onPress),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) { content() }
    }
}

@Composable
fun IconKey(
    icon: ImageVector,
    contentDescription: String,
    onPress: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    repeat: Boolean = false,
    enabled: Boolean = true,
    tint: Color = AppTheme.extra.keyIcon,
    background: Brush? = null,
    shape: Shape = CircleShape,
    badge: (@Composable () -> Unit)? = null,
) {
    Box(modifier) {
        KeyButton(
            onPress = onPress,
            contentDescription = contentDescription,
            size = size,
            repeat = repeat,
            enabled = enabled,
            tint = tint,
            background = background,
            shape = shape,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.42f))
        }
        if (badge != null) Box(Modifier.align(Alignment.TopEnd)) { badge() }
    }
}

@Composable
fun SmallLock(size: Dp = 16.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(goldBrush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = "Premium", tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** Circular D-pad with an illuminated rim, like the app icon. */
@Composable
fun DPad(
    onDirection: (Direction) -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
) {
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // Outer glow halo
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(extra.glow.copy(alpha = if (extra.isDark) 0.35f else 0.18f), Color.Transparent),
                    center = center,
                    radius = this.size.minDimension / 2,
                ),
            )
        }
        val pad = size * 0.9f
        Box(
            Modifier
                .size(pad)
                .shadow(14.dp, CircleShape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
                .background(Brush.verticalGradient(0f to extra.keyHighlight, 0.4f to Color.Transparent))
                .border(2.dp, Brush.sweepGradient(listOf(scheme.primary, scheme.secondary, scheme.primary)), CircleShape),
        ) {
            val zone = pad * 0.32f
            DirectionZone(Modifier.align(Alignment.TopCenter), zone, Icons.Rounded.KeyboardArrowUp, "Up") { onDirection(Direction.UP) }
            DirectionZone(Modifier.align(Alignment.BottomCenter), zone, Icons.Rounded.KeyboardArrowDown, "Down") { onDirection(Direction.DOWN) }
            @Suppress("DEPRECATION")
            DirectionZone(Modifier.align(Alignment.CenterStart), zone, Icons.Rounded.KeyboardArrowLeft, "Left") { onDirection(Direction.LEFT) }
            @Suppress("DEPRECATION")
            DirectionZone(Modifier.align(Alignment.CenterEnd), zone, Icons.Rounded.KeyboardArrowRight, "Right") { onDirection(Direction.RIGHT) }
            KeyButton(
                onPress = onOk,
                contentDescription = "OK",
                modifier = Modifier.align(Alignment.Center),
                size = pad * 0.4f,
                tint = Color.White,
                background = brandBrush(),
            ) {
                Text("OK", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun DirectionZone(modifier: Modifier, size: Dp, icon: ImageVector, label: String, onPress: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = if (pressed) 0.16f else 0f)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(highlight)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .remotePress(repeat = true, onPressedChange = { pressed = it }, onPress = onPress),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (pressed) MaterialTheme.colorScheme.primary else AppTheme.extra.keyIcon,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

@Composable
fun Rocker(
    label: String,
    upIcon: ImageVector,
    downIcon: ImageVector,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 148.dp,
) {
    val extra = AppTheme.extra
    val shape = RoundedCornerShape(50)
    Column(
        modifier
            .width(width)
            .height(height)
            .shadow(6.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
            .background(Brush.verticalGradient(0f to extra.keyHighlight, 0.3f to Color.Transparent))
            .border(1.dp, extra.keyBorder, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RockerHalf(Modifier.weight(1f), upIcon, "$label up", onUp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RockerHalf(Modifier.weight(1f), downIcon, "$label down", onDown)
    }
}

@Composable
private fun RockerHalf(modifier: Modifier, icon: ImageVector, label: String, onPress: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (pressed) 0.14f else 0f))
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .remotePress(repeat = true, onPressedChange = { pressed = it }, onPress = onPress),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (pressed) MaterialTheme.colorScheme.primary else AppTheme.extra.keyIcon)
    }
}

// ------------------------------------------------------------------ buttons

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    brush: Brush? = null,
    contentColor: Color = Color.White,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val active = enabled && !loading
    val glow = scheme.primary.copy(alpha = 0.45f)
    Box(
        modifier
            .height(56.dp)
            .shadow(if (active) 10.dp else 0.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(if (enabled) brush ?: brandBrush() else SolidColor(scheme.outline))
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.18f), 0.5f to Color.Transparent))
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(color = contentColor, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, color = contentColor, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .height(52.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(shape)
            .background(AppTheme.extra.panel)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

// ------------------------------------------------------------------ layout pieces

@Composable
fun AppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) { actions() }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val extra = AppTheme.extra
    val stroke = border ?: BorderStroke(1.dp, extra.panelBorder)
    val m = modifier.shadow(3.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = m, shape = shape, color = extra.panel, contentColor = MaterialTheme.colorScheme.onSurface, border = stroke, content = content)
    } else {
        Surface(modifier = m, shape = shape, color = extra.panel, contentColor = MaterialTheme.colorScheme.onSurface, border = stroke, content = content)
    }
}

@Composable
fun platformTextColor(platform: TvPlatform): Color {
    val c = platform.color
    return if (AppTheme.extra.isDark) {
        Color(c.red + (1 - c.red) * 0.5f, c.green + (1 - c.green) * 0.5f, c.blue + (1 - c.blue) * 0.5f)
    } else {
        Color(c.red * 0.72f, c.green * 0.72f, c.blue * 0.72f)
    }
}

@Composable
fun PlatformBadge(platform: TvPlatform, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(platform.color.copy(alpha = if (AppTheme.extra.isDark) 0.22f else 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(platform.short, style = MaterialTheme.typography.labelSmall, color = platformTextColor(platform), maxLines = 1)
    }
}

/** A drawn, glossy TV icon tinted with the platform's color. */
@Composable
fun TvGlyph(platform: TvPlatform, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val c = platform.color
    val dark = Color(c.red * 0.55f, c.green * 0.55f, c.blue * 0.55f)
    Box(
        modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(size * 0.3f), ambientColor = c, spotColor = c)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(c, dark))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            // top sheen
            drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.5f to Color.Transparent))
            // body
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.17f, h * 0.24f),
                size = Size(w * 0.66f, h * 0.44f),
                cornerRadius = CornerRadius(w * 0.07f),
            )
            // screen
            drawRoundRect(
                brush = Brush.linearGradient(listOf(dark, c), start = Offset(w * 0.2f, h * 0.27f), end = Offset(w * 0.8f, h * 0.65f)),
                topLeft = Offset(w * 0.21f, h * 0.28f),
                size = Size(w * 0.58f, h * 0.36f),
                cornerRadius = CornerRadius(w * 0.04f),
            )
            // screen glare
            val glare = Path().apply {
                moveTo(w * 0.42f, h * 0.28f)
                lineTo(w * 0.58f, h * 0.28f)
                lineTo(w * 0.38f, h * 0.64f)
                lineTo(w * 0.22f, h * 0.64f)
                close()
            }
            drawPath(glare, Color.White.copy(alpha = 0.18f))
            // stand
            val stand = Path().apply {
                moveTo(w * 0.43f, h * 0.68f)
                lineTo(w * 0.57f, h * 0.68f)
                lineTo(w * 0.63f, h * 0.77f)
                lineTo(w * 0.37f, h * 0.77f)
                close()
            }
            drawPath(stand, Color.White.copy(alpha = 0.92f))
        }
    }
}

/** Layered glowing circles behind an icon — status screens, empty states. */
@Composable
fun HeroIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    colors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(colors.first().copy(alpha = 0.07f)))
        Box(Modifier.size(size * 0.74f).clip(CircleShape).background(colors.first().copy(alpha = 0.11f)))
        Box(
            Modifier
                .size(size * 0.5f)
                .shadow(18.dp, CircleShape, ambientColor = colors.first(), spotColor = colors.first())
                .clip(CircleShape)
                .background(Brush.linearGradient(colors))
                .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.5f to Color.Transparent)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.24f))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun NumberedStep(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(brandBrush()),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
fun IconTile(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun PremiumChip(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(goldBrush).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("PRO", style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

// ------------------------------------------------------------------ snackbars

enum class SnackKind { INFO, SUCCESS, ERROR, PREMIUM }

data class AppSnack(
    override val message: String,
    val kind: SnackKind = SnackKind.INFO,
    override val actionLabel: String? = null,
) : SnackbarVisuals {
    override val withDismissAction: Boolean get() = false
    override val duration: SnackbarDuration get() = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
}

suspend fun SnackbarHostState.show(message: String, kind: SnackKind = SnackKind.INFO) {
    currentSnackbarData?.dismiss()
    showSnackbar(AppSnack(message, kind))
}

@Composable
fun AppSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        val visuals = data.visuals
        val kind = (visuals as? AppSnack)?.kind ?: SnackKind.INFO
        val extra = AppTheme.extra
        val (icon, color) = when (kind) {
            SnackKind.SUCCESS -> Icons.Rounded.CheckCircle to extra.success
            SnackKind.ERROR -> Icons.Rounded.ErrorOutline to MaterialTheme.colorScheme.error
            SnackKind.PREMIUM -> Icons.Rounded.WorkspacePremium to extra.gold
            SnackKind.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.primary
        }
        val shape = RoundedCornerShape(18.dp)
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(16.dp, shape)
                .clip(shape)
                .background(extra.snackbar)
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = extra.onSnackbar,
                modifier = Modifier.weight(1f),
            )
            visuals.actionLabel?.let { label ->
                TextButton(onClick = { data.performAction() }) { Text(label, color = color) }
            }
        }
    }
}

// ------------------------------------------------------------------ dialogs

@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    dismissText: String? = "Cancel",
    destructive: Boolean = false,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, shape)
                .clip(shape)
                .background(AppTheme.extra.panel)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (extraContent != null) {
                Spacer(Modifier.height(16.dp))
                extraContent()
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                confirmText,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                brush = if (destructive) Brush.linearGradient(listOf(Color(0xFFF04438), Color(0xFFD92D20))) else null,
            )
            if (dismissText != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(dismissText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun ClickableRow(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
