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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.theme.AppTheme
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
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "keyScale")
    val bg = background ?: Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom))
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .shadow(if (pressed) 1.dp else 5.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(shape)
            .background(bg)
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
fun LockBadge() {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AppTheme.extra.gold, Color(0xFFFF8A00)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = "Premium",
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
fun DPad(
    onDirection: (Direction) -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
) {
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .size(size)
            .shadow(10.dp, CircleShape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
            .border(1.dp, extra.keyBorder, CircleShape),
    ) {
        val zone = size * 0.32f
        DirectionZone(Modifier.align(Alignment.TopCenter), zone, Icons.Rounded.KeyboardArrowUp, "Up") { onDirection(Direction.UP) }
        DirectionZone(Modifier.align(Alignment.BottomCenter), zone, Icons.Rounded.KeyboardArrowDown, "Down") { onDirection(Direction.DOWN) }
        DirectionZone(Modifier.align(Alignment.CenterStart), zone, Icons.Rounded.KeyboardArrowLeft, "Left") { onDirection(Direction.LEFT) }
        DirectionZone(Modifier.align(Alignment.CenterEnd), zone, Icons.Rounded.KeyboardArrowRight, "Right") { onDirection(Direction.RIGHT) }
        KeyButton(
            onPress = onOk,
            contentDescription = "OK",
            modifier = Modifier.align(Alignment.Center),
            size = size * 0.38f,
            tint = Color.White,
            background = Brush.linearGradient(listOf(scheme.primary, scheme.secondary)),
        ) {
            Text("OK", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
        }
    }
}

enum class Direction { UP, DOWN, LEFT, RIGHT }

@Composable
private fun DirectionZone(modifier: Modifier, size: Dp, icon: ImageVector, label: String, onPress: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = if (pressed) 0.14f else 0f)
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
            modifier = Modifier.size(size * 0.5f),
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
            .shadow(5.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (pressed) 0.12f else 0f))
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

// ------------------------------------------------------------------ general UI

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val active = enabled && !loading
    Box(
        modifier
            .height(56.dp)
            .shadow(if (active) 8.dp else 0.dp, shape, ambientColor = scheme.primary.copy(alpha = 0.4f), spotColor = scheme.primary.copy(alpha = 0.4f))
            .clip(shape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))
                else SolidColor(scheme.outline),
            )
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .height(52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

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
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        actions()
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val extra = AppTheme.extra
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.shadow(2.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow),
            shape = shape,
            color = extra.panel,
            border = BorderStroke(1.dp, extra.panelBorder),
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.shadow(2.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow),
            shape = shape,
            color = extra.panel,
            border = BorderStroke(1.dp, extra.panelBorder),
            content = content,
        )
    }
}

@Composable
fun PlatformBadge(platform: TvPlatform, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(platform.color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(platform.short, style = MaterialTheme.typography.labelSmall, color = platformTextColor(platform))
    }
}

@Composable
fun platformTextColor(platform: TvPlatform): Color {
    val dark = MaterialTheme.colorScheme.background.luminanceApprox() < 0.5f
    return if (dark) platform.color.lighten() else platform.color
}

private fun Color.luminanceApprox() = 0.299f * red + 0.587f * green + 0.114f * blue
private fun Color.lighten() = Color(red + (1 - red) * 0.45f, green + (1 - green) * 0.45f, blue + (1 - blue) * 0.45f, alpha)

@Composable
fun TvGlyph(platform: TvPlatform, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(platform.color, platform.color.copy(alpha = 0.7f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

/** Layered circles behind an icon — used for onboarding, empty states and status screens. */
@Composable
fun HeroIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    colors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(colors.first().copy(alpha = 0.07f)))
        Box(Modifier.size(size * 0.74f).clip(CircleShape).background(colors.first().copy(alpha = 0.10f)))
        Box(
            Modifier
                .size(size * 0.5f)
                .shadow(14.dp, CircleShape, ambientColor = colors.first(), spotColor = colors.first())
                .clip(CircleShape)
                .background(Brush.linearGradient(colors)),
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
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
    }
}
