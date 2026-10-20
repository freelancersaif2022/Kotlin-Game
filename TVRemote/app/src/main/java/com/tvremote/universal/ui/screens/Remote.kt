package com.tvremote.universal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Input
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.tvremote.universal.ui.components.DPad
import com.tvremote.universal.ui.components.Direction
import com.tvremote.universal.ui.components.IconKey
import com.tvremote.universal.ui.components.KeyButton
import com.tvremote.universal.ui.components.Rocker
import com.tvremote.universal.ui.components.rememberHaptic
import com.tvremote.universal.ui.theme.AppTheme
import kotlin.math.abs

private enum class RemoteMode(val label: String, val icon: ImageVector, val premium: Boolean) {
    DPAD("Remote", Icons.Rounded.SettingsRemote, false),
    TOUCHPAD("Touchpad", Icons.Rounded.TouchApp, true),
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

    LifecycleResumeEffect(deviceId) {
        vm.onForeground()
        onPauseOrDispose { vm.onBackground() }
    }
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(Unit) {
        vm.events.collect { snackbar.showSnackbar(it) }
    }

    val tv = device
    if (tv == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val capabilities = caps ?: return

    fun repair() = onRepair(DiscoveredTv(tv.platform, tv.host, tv.name, tv.model, tv.mac))

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // ---------------------------------------------------------------- top bar
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "My TVs") }
                Column(Modifier.weight(1f).clickable(onClick = onSwitchTv).padding(horizontal = 4.dp)) {
                    Text(tv.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(state, waking)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            statusText(state, waking),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (capabilities.text) {
                    Box {
                        IconButton(onClick = { if (premium) showKeyboard = true else onPremium() }) {
                            Icon(Icons.Rounded.Keyboard, contentDescription = "Keyboard")
                        }
                        if (!premium) Box(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)) { SmallLock() }
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Switch TV") },
                            leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) },
                            onClick = { menuOpen = false; onSwitchTv() },
                        )
                        DropdownMenuItem(
                            text = { Text("Reconnect") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            onClick = { menuOpen = false; vm.connect() },
                        )
                        DropdownMenuItem(
                            text = { Text("TV settings") },
                            leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                            onClick = { menuOpen = false; onDeviceSettings() },
                        )
                        DropdownMenuItem(
                            text = { Text("Pairing & power-on help") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, null) },
                            onClick = { menuOpen = false; onHelp(tv.platform) },
                        )
                    }
                }
            }

            // ---------------------------------------------------------------- status banner
            StatusBanner(
                state = state,
                waking = waking,
                canWake = vm.canWake,
                onRetry = vm::connect,
                onRepair = ::repair,
                onWake = { if (premium) vm.wake() else onPremium() },
                premium = premium,
                onSettings = onDeviceSettings,
            )

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val compact = maxHeight < 560.dp
                val keySize: Dp = if (compact) 52.dp else 58.dp
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ------------------------------------------------------------ power row
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
                            background = Brush.verticalGradient(listOf(AppTheme.extra.power, AppTheme.extra.power.copy(alpha = 0.85f))),
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

                    // ------------------------------------------------------------ mode tabs
                    ModeTabs(
                        modes = RemoteMode.entries.filter { it != RemoteMode.NUMBERS || capabilities.numbers },
                        selected = mode,
                        premium = premium,
                        onSelect = { m -> if (m.premium && !premium) onPremium() else mode = m },
                    )
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

                    // ------------------------------------------------------------ main area
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val padSize = minOf(maxWidth * 0.78f, maxHeight * 0.98f, 270.dp)
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

                    // ------------------------------------------------------------ bottom cluster
                    BottomCluster(vm, keySize = if (compact) 48.dp else 54.dp, rockerHeight = if (compact) 124.dp else 146.dp)
                    Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }

    if (showKeyboard) {
        KeyboardSheet(
            capabilities = capabilities,
            onDismiss = { showKeyboard = false },
            onSend = vm::sendText,
            onBackspace = vm::backspace,
            onEnter = vm::enter,
        )
    }
}

@Composable
private fun SmallLock() {
    Box(
        Modifier.size(14.dp).clip(CircleShape).background(AppTheme.extra.gold),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(9.dp))
    }
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
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
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
                .clip(shape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f), shape)
                .padding(14.dp),
        ) {
            Text(f.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (f.reason) {
                    FailReason.NEEDS_PAIRING, FailReason.CERT_CHANGED -> {
                        TextButton(onClick = onRepair) { Text("Pair again") }
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                    else -> {
                        TextButton(onClick = onRetry) { Text("Retry") }
                        if (canWake) {
                            TextButton(onClick = onWake) { Text(if (premium) "Turn on TV" else "Turn on TV (Premium)") }
                        } else {
                            TextButton(onClick = onSettings) { Text("Set up power on") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTabs(modes: List<RemoteMode>, selected: RemoteMode, premium: Boolean, onSelect: (RemoteMode) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        modes.forEach { m ->
            val isSelected = m == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(m) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        m.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        m.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (m.premium && !premium) {
                        Spacer(Modifier.width(3.dp))
                        SmallLock()
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
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(extra.keyTop, extra.keyBottom)))
            .border(1.dp, extra.keyBorder, shape)
            .pointerInput(Unit) {
                val threshold = 44.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var acc = Offset.Zero
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!moved) {
                                haptic()
                                onKey(RemoteKey.OK)
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
                            acc = Offset.Zero
                        }
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Swipe to move · Tap to select", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NumberPad(onKey: (RemoteKey) -> Unit, compact: Boolean) {
    val size = if (compact) 50.dp else 58.dp
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { n -> DigitKey(n, size) { onKey(RemoteKey.number(n)) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            IconKey(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onPress = { onKey(RemoteKey.BACK) }, size = size)
            DigitKey(0, size) { onKey(RemoteKey.number(0)) }
            KeyButton(onPress = { onKey(RemoteKey.OK) }, contentDescription = "OK", size = size) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DigitKey(n: Int, size: Dp, onPress: () -> Unit) {
    KeyButton(onPress = onPress, contentDescription = "$n", size = size) {
        Text("$n", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppsGrid(apps: List<StreamingApp>, onLaunch: (StreamingApp) -> Unit) {
    val haptic = rememberHaptic()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        apps.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { app ->
                    val shape = RoundedCornerShape(22.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(shape)
                            .background(Brush.linearGradient(listOf(app.color, app.color.copy(alpha = 0.82f))))
                            .clickable {
                                haptic()
                                onLaunch(app)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(app.title, color = app.textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Text(
            "Opens the app on your TV if it's installed.",
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
    capabilities: Capabilities,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding()) {
            Text("Type on your TV", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Select a search box or text field on the TV first, then type here and tap Send.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Search or type…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (text.isNotEmpty()) {
                        onSend(text)
                        text = ""
                    }
                }),
                trailingIcon = {
                    IconButton(onClick = {
                        if (text.isNotEmpty()) {
                            onSend(text)
                            text = ""
                        }
                    }) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (capabilities.backspace) {
                    IconKey(Icons.AutoMirrored.Rounded.Backspace, "Delete", onPress = onBackspace, size = 54.dp, repeat = true)
                }
                IconKey(Icons.AutoMirrored.Rounded.KeyboardReturn, "Enter", onPress = onEnter, size = 54.dp)
            }
        }
    }
}

