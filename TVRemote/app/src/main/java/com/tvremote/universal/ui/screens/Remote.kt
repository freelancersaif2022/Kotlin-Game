package com.tvremote.universal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvremote.universal.App
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.RemoteKey
import com.tvremote.universal.data.StreamingApp
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.tv.Capabilities
import com.tvremote.universal.tv.ConnectionState
import com.tvremote.universal.tv.FailReason
import com.tvremote.universal.ui.components.AppSnackbarHost
import com.tvremote.universal.ui.components.DPad
import com.tvremote.universal.ui.components.Direction
import com.tvremote.universal.ui.components.IconKey
import com.tvremote.universal.ui.components.IconTile
import com.tvremote.universal.ui.components.KeyButton
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.Rocker
import com.tvremote.universal.ui.components.SmallLock
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.components.brandBrush
import com.tvremote.universal.ui.components.rememberHaptic
import com.tvremote.universal.ui.components.show
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class RemoteMode(val label: String, val icon: ImageVector, val premium: Boolean) {
    DPAD("Remote", Icons.Rounded.SettingsRemote, false),
    TOUCHPAD("Touch", Icons.Rounded.TouchApp, true),
    NUMBERS("Numbers", Icons.Rounded.Dialpad, false),
    APPS("Apps", Icons.Rounded.Apps, true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    deviceId: String,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onDeviceSettings: () -> Unit,
    onRepair: (DiscoveredTv) -> Unit,
    onHelp: (TvPlatform) -> Unit,
    onSwitchTv: () -> Unit,
) {
    val vm: RemoteViewModel = viewModel(key = "remote-$deviceId") { RemoteViewModel(deviceId) }
    val container = App.container
    val device by vm.device.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val caps by vm.capabilities.collectAsStateWithLifecycle()
    val waking by vm.waking.collectAsStateWithLifecycle()
    val premium by container.premium.isPremium.collectAsStateWithLifecycle()
    val keepOn by container.settings.keepScreenOn.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var mode by rememberSaveable { mutableStateOf(RemoteMode.DPAD) }
    var showKeyboard by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val view = LocalView.current
    val scheme = MaterialTheme.colorScheme

    LifecycleResumeEffect(deviceId) {
        vm.onForeground()
        onPauseOrDispose { vm.onBackground() }
    }
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        vm.events.collect { snackbar.show(it.first, it.second) }
    }

    val tv = device
    if (tv == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val capabilities = caps ?: return

    fun repair() = onRepair(DiscoveredTv(tv.platform, tv.host, tv.name, tv.model, tv.mac))

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Box(
            Modifier.fillMaxWidth().height(320.dp)
                .background(Brush.verticalGradient(listOf(AppTheme.extra.glow.copy(alpha = if (AppTheme.extra.isDark) 0.16f else 0.07f), Color.Transparent))),
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // ---------------------------------------------------------------- top bar
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "My TVs", tint = scheme.onBackground) }
                TvGlyph(tv.platform, size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).clickable(onClick = onSwitchTv)) {
                    Text(tv.name, style = MaterialTheme.typography.titleMedium, color = scheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(state, waking)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            statusText(state, waking),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (capabilities.text) {
                    Box {
                        IconButton(onClick = { if (premium) showKeyboard = true else onPremium() }) {
                            Icon(Icons.Rounded.Keyboard, contentDescription = "Keyboard", tint = scheme.onBackground)
                        }
                        if (!premium) Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) { SmallLock(14.dp) }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = scheme.onBackground) }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = AppTheme.extra.panel,
                    ) {
                        MenuItem(Icons.Rounded.SwapHoriz, "Switch TV") { menuOpen = false; onSwitchTv() }
                        MenuItem(Icons.Rounded.Refresh, "Reconnect") { menuOpen = false; vm.connect() }
                        MenuItem(Icons.Rounded.Settings, "TV settings") { menuOpen = false; onDeviceSettings() }
                        MenuItem(Icons.AutoMirrored.Rounded.HelpOutline, "Pairing & power-on help") { menuOpen = false; onHelp(tv.platform) }
                    }
                }
            }

            StatusBanner(
                state = state,
                waking = waking,
                canWake = vm.canWake,
                premium = premium,
                onRetry = vm::connect,
                onRepair = ::repair,
                onWake = { if (premium) vm.wake() else onPremium() },
                onSettings = onDeviceSettings,
            )

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val compact = maxHeight < 560.dp
                val keySize: Dp = if (compact) 52.dp else 58.dp
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconKey(
                            Icons.Rounded.PowerSettingsNew, "Power",
                            onPress = { if (!vm.power(premium)) onPremium() },
                            size = keySize,
                            tint = Color.White,
                            background = Brush.verticalGradient(listOf(Color(0xFFFF6B5F), AppTheme.extra.power, Color(0xFFC4320A))),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (RemoteKey.INPUT in capabilities.keys) {
                                IconKey(Icons.Rounded.Input, "Input / Source", onPress = { vm.press(RemoteKey.INPUT) }, size = keySize)
                            }
                            IconKey(Icons.AutoMirrored.Rounded.VolumeOff, "Mute", onPress = { vm.press(RemoteKey.MUTE) }, size = keySize)
                            IconKey(Icons.Rounded.Info, "Info", onPress = { vm.press(RemoteKey.INFO) }, size = keySize)
                        }
                    }
                    Spacer(Modifier.height(if (compact) 10.dp else 16.dp))

                    ModeTabs(
                        modes = RemoteMode.entries.filter { it != RemoteMode.NUMBERS || capabilities.numbers },
                        selected = mode,
                        premium = premium,
                        onSelect = { m -> if (m.premium && !premium) onPremium() else mode = m },
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val padSize = minOf(maxWidth * 0.84f, maxHeight * 1.0f, 290.dp)
                        AnimatedContent(
                            targetState = mode,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                            label = "mode",
                        ) { m ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                when (m) {
                                    RemoteMode.DPAD -> DPad(
                                        onDirection = { d ->
                                            vm.press(
                                                when (d) {
                                                    Direction.UP -> RemoteKey.UP
                                                    Direction.DOWN -> RemoteKey.DOWN
                                                    Direction.LEFT -> RemoteKey.LEFT
                                                    Direction.RIGHT -> RemoteKey.RIGHT
                                                },
                                            )
                                        },
                                        onOk = { vm.press(RemoteKey.OK) },
                                        size = padSize,
                                    )
                                    RemoteMode.TOUCHPAD -> Touchpad(onKey = vm::press, modifier = Modifier.fillMaxSize())
                                    RemoteMode.NUMBERS -> NumberPad(onKey = vm::press, compact = compact)
                                    RemoteMode.APPS -> AppsGrid(
                                        apps = StreamingApp.entries.filter { it in capabilities.apps },
                                        onLaunch = vm::launch,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                    BottomCluster(vm, keySize = if (compact) 48.dp else 54.dp, rockerHeight = if (compact) 124.dp else 146.dp)
                    Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                }
            }
        }
        AppSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    if (showKeyboard) {
        KeyboardSheet(
            tvName = tv.name,
            capabilities = capabilities,
            onDismiss = { showKeyboard = false },
            onSend = vm::sendText,
            onBackspace = vm::backspace,
            onEnter = vm::enter,
        )
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
    )
}

private fun statusText(state: ConnectionState, waking: Boolean): String = when {
    waking -> "Turning on your TV…"
    state is ConnectionState.Connected && state.tvOn -> "Connected"
    state is ConnectionState.Connected -> "Connected · TV in standby"
    state is ConnectionState.Connecting -> state.hint ?: "Connecting…"
    state is ConnectionState.Failed -> "Not connected"
    else -> "Connecting…"
}

@Composable
private fun StatusDot(state: ConnectionState, waking: Boolean) {
    val extra = AppTheme.extra
    val color = when {
        waking -> extra.warning
        state is ConnectionState.Connected -> extra.success
        state is ConnectionState.Failed -> MaterialTheme.colorScheme.error
        else -> extra.warning
    }
    Box(Modifier.size(8.dp).shadow(4.dp, CircleShape, ambientColor = color, spotColor = color).clip(CircleShape).background(color))
}

@Composable
private fun StatusBanner(
    state: ConnectionState,
    waking: Boolean,
    canWake: Boolean,
    premium: Boolean,
    onRetry: () -> Unit,
    onRepair: () -> Unit,
    onWake: () -> Unit,
    onSettings: () -> Unit,
) {
    val failed = state as? ConnectionState.Failed
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    AnimatedVisibility(
        visible = failed != null && !waking,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        val f = failed ?: return@AnimatedVisibility
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(4.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
                .clip(shape)
                .background(extra.panel)
                .border(1.dp, scheme.error.copy(alpha = 0.35f), shape)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                IconTile(Icons.Rounded.WifiOff, scheme.error, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(f.message, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (f.reason) {
                    FailReason.NEEDS_PAIRING, FailReason.CERT_CHANGED -> {
                        BannerAction("Pair again", primary = true, onClick = onRepair)
                        BannerAction("Retry", onClick = onRetry)
                    }
                    else -> {
                        BannerAction("Retry", onClick = onRetry)
                        if (canWake) BannerAction(if (premium) "Turn on TV" else "Turn on TV · PRO", primary = true, onClick = onWake)
                        else BannerAction("Set up power on", onClick = onSettings)
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerAction(text: String, primary: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) brandBrush() else Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.1f), scheme.primary.copy(alpha = 0.1f))))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (primary) Color.White else scheme.primary)
    }
}

@Composable
private fun ModeTabs(modes: List<RemoteMode>, selected: RemoteMode, premium: Boolean, onSelect: (RemoteMode) -> Unit) {
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val haptic = rememberHaptic()
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(5.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(extra.keyBottom, extra.keyTop)))
            .border(1.dp, extra.keyBorder, shape)
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / modes.size
        val index = modes.indexOf(selected).coerceAtLeast(0)
        val offset by animateDpAsState(tabWidth * index, spring(dampingRatio = 0.8f, stiffness = 500f), label = "tab")
        Box(
            Modifier
                .offset(x = offset)
                .width(tabWidth)
                .fillMaxSize()
                .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = extra.glow, spotColor = extra.glow)
                .clip(RoundedCornerShape(16.dp))
                .background(brandBrush())
                .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.2f), 0.5f to Color.Transparent)),
        )
        Row(Modifier.fillMaxSize()) {
            modes.forEach { m ->
                val isSelected = m == selected
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic()
                            onSelect(m)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        m.icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (isSelected) Color.White else scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        m.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (m.premium && !premium) {
                        Spacer(Modifier.width(3.dp))
                        SmallLock(13.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomCluster(vm: RemoteViewModel, keySize: Dp, rockerHeight: Dp) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rocker(
            label = "VOL",
            upIcon = Icons.Rounded.Add,
            downIcon = Icons.Rounded.Remove,
            onUp = { vm.press(RemoteKey.VOLUME_UP) },
            onDown = { vm.press(RemoteKey.VOLUME_DOWN) },
            height = rockerHeight,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onPress = { vm.press(RemoteKey.BACK) }, size = keySize)
                IconKey(Icons.Rounded.Home, "Home", onPress = { vm.press(RemoteKey.HOME) }, size = keySize)
                IconKey(Icons.Rounded.Menu, "Menu", onPress = { vm.press(RemoteKey.MENU) }, size = keySize)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconKey(Icons.Rounded.FastRewind, "Rewind", onPress = { vm.press(RemoteKey.REWIND) }, size = keySize, repeat = true)
                IconKey(Icons.Rounded.PlayArrow, "Play / Pause", onPress = { vm.press(RemoteKey.PLAY_PAUSE) }, size = keySize)
                IconKey(Icons.Rounded.FastForward, "Fast forward", onPress = { vm.press(RemoteKey.FAST_FORWARD) }, size = keySize, repeat = true)
            }
        }
        Rocker(
            label = "CH",
            upIcon = Icons.Rounded.KeyboardArrowUp,
            downIcon = Icons.Rounded.KeyboardArrowDown,
            onUp = { vm.press(RemoteKey.CHANNEL_UP) },
            onDown = { vm.press(RemoteKey.CHANNEL_DOWN) },
            height = rockerHeight,
        )
    }
}

@Composable
private fun Touchpad(onKey: (RemoteKey) -> Unit, modifier: Modifier = Modifier) {
    val haptic = rememberHaptic()
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(30.dp)
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    var pulse by remember { mutableIntStateOf(0) }
    val ripple = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pulse) {
        if (pulse > 0) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(420))
        }
    }
    Box(
        modifier
            .shadow(8.dp, shape, ambientColor = extra.keyShadow, spotColor = extra.keyShadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
            .border(1.5.dp, Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.6f), scheme.secondary.copy(alpha = 0.3f))), shape)
            .pointerInput(Unit) {
                val threshold = 42.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touch = down.position
                    var acc = Offset.Zero
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        touch = change.position
                        if (!change.pressed) {
                            if (!moved) {
                                haptic()
                                onKey(RemoteKey.OK)
                                pulse++
                            }
                            break
                        }
                        acc += change.positionChange()
                        if (abs(acc.x) > threshold || abs(acc.y) > threshold) {
                            moved = true
                            val key = if (abs(acc.x) > abs(acc.y)) {
                                if (acc.x > 0) RemoteKey.RIGHT else RemoteKey.LEFT
                            } else {
                                if (acc.y > 0) RemoteKey.DOWN else RemoteKey.UP
                            }
                            haptic()
                            onKey(key)
                            scope.launch { pulse++ }
                            acc = Offset.Zero
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 22.dp.toPx()
            var y = step
            while (y < size.height) {
                var x = step
                while (x < size.width) {
                    drawCircle(scheme.onSurfaceVariant.copy(alpha = 0.16f), 1.4.dp.toPx(), Offset(x, y))
                    x += step
                }
                y += step
            }
            if (touch.isSpecified && ripple.value < 1f) {
                drawCircle(
                    scheme.primary.copy(alpha = (1f - ripple.value) * 0.35f),
                    radius = 20.dp.toPx() + ripple.value * 60.dp.toPx(),
                    center = touch,
                )
                drawCircle(
                    scheme.primary.copy(alpha = 1f - ripple.value),
                    radius = 20.dp.toPx() + ripple.value * 60.dp.toPx(),
                    center = touch,
                    style = Stroke(2.dp.toPx()),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Swipe to move", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text("Tap anywhere to select", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NumberPad(onKey: (RemoteKey) -> Unit, compact: Boolean) {
    val size = if (compact) 52.dp else 62.dp
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { n -> DigitKey(n, size) { onKey(RemoteKey.number(n)) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onPress = { onKey(RemoteKey.BACK) }, size = size)
            DigitKey(0, size) { onKey(RemoteKey.number(0)) }
            KeyButton(onPress = { onKey(RemoteKey.OK) }, contentDescription = "OK", size = size, tint = Color.White, background = brandBrush()) {
                Text("OK", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun DigitKey(n: Int, size: Dp, onPress: () -> Unit) {
    KeyButton(onPress = onPress, contentDescription = "$n", size = size) {
        Text("$n", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppsGrid(apps: List<StreamingApp>, onLaunch: (StreamingApp) -> Unit) {
    val haptic = rememberHaptic()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        apps.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { app ->
                    val shape = RoundedCornerShape(24.dp)
                    Row(
                        Modifier
                            .weight(1f)
                            .height(92.dp)
                            .shadow(8.dp, shape, ambientColor = app.color, spotColor = app.color)
                            .clip(shape)
                            .background(Brush.linearGradient(listOf(app.color, app.color.copy(alpha = 0.78f))))
                            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.18f), 0.5f to Color.Transparent))
                            .clickable {
                                haptic()
                                onLaunch(app)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(app.title.take(1), fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (app == StreamingApp.NETFLIX) app.textColor else app.color)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(app.title, color = app.textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                            Text("Open on TV", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text(
            "Opens the app on your TV when it's installed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyboardSheet(
    tvName: String,
    capabilities: Capabilities,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    fun send() {
        if (text.isNotEmpty()) {
            onSend(text)
            text = ""
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = extra.panel,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Keyboard, scheme.primary, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Type on your TV", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                    Text("Sending to $tvName", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Close", tint = scheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Open a search box or text field on the TV first, then type here.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Search movies, apps, passwords…") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (capabilities.backspace) {
                    IconKey(Icons.AutoMirrored.Rounded.Backspace, "Delete on TV", onPress = onBackspace, size = 52.dp, repeat = true)
                }
                IconKey(Icons.Rounded.SpaceBar, "Space", onPress = { onSend(" ") }, size = 52.dp)
                IconKey(Icons.AutoMirrored.Rounded.KeyboardReturn, "Enter", onPress = onEnter, size = 52.dp)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                "Send to TV",
                icon = Icons.AutoMirrored.Rounded.Send,
                enabled = text.isNotEmpty(),
                onClick = { send() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
