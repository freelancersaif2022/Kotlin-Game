package com.flashshare.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.data.Category
import com.flashshare.app.net.BatchStatus
import com.flashshare.app.transfer.BatchUi
import com.flashshare.app.transfer.FileUi
import com.flashshare.app.transfer.Phase
import com.flashshare.app.transfer.PeerUi
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.util.formatEta
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed
import com.flashshare.app.util.openFile

@Composable
fun TransferScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val phase by c.transfers.phase.collectAsStateWithLifecycle()
    val peers by c.transfers.peers.collectAsStateWithLifecycle()
    val batches by c.transfers.batches.collectAsStateWithLifecycle()
    val band by c.transfers.band.collectAsStateWithLifecycle()
    val encrypted by c.transfers.encrypted.collectAsStateWithLifecycle()
    val settings by c.prefs.state.collectAsStateWithLifecycle()
    val premium = settings?.premium == true
    var confirmLeave by remember { mutableStateOf(false) }
    var everConnected by remember { mutableStateOf(false) }
    LaunchedEffect(peers) { if (peers.isNotEmpty()) everConnected = true }

    // Keep the screen awake while on this screen (setting).
    val view = LocalView.current
    DisposableEffect(settings?.keepScreenOn) {
        view.keepScreenOn = settings?.keepScreenOn != false
        onDispose { view.keepScreenOn = false }
    }

    val leave = {
        c.transfers.disconnect()
        nav.popBackStack(Routes.HOME, false)
        Unit
    }
    val anyRunning = batches.any { it.status == BatchStatus.RUNNING || it.status == BatchStatus.WAITING }
    BackHandler { if (peers.isNotEmpty()) confirmLeave = true else leave() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (peers.isNotEmpty()) {
                Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { confirmLeave = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                        ) {
                            Icon(Icons.Rounded.LinkOff, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Disconnect")
                        }
                        GradientButton(
                            "Send more", onClick = { nav.navigate(Routes.picker("more")) }, icon = Icons.Rounded.Add,
                            brush = Brand.send, modifier = Modifier.weight(1f), height = 52.dp,
                        )
                    }
                }
            }
        },
    ) { pad ->
        when (val p = phase) {
            is Phase.Joining -> ConnectingView(p.peerName, p.peerAvatarId, p.text, Modifier.padding(pad)) { leave() }
            is Phase.Preparing -> ConnectingView("", 0, p.text, Modifier.padding(pad)) { leave() }
            is Phase.Error -> Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), verticalArrangement = Arrangement.Center) {
                var pinError by remember(p) { mutableStateOf(p.needPin) }
                ErrorCard(p.message) { if (!c.transfers.retry(null)) leave() }
                TextButton(onClick = leave, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to home") }
                if (pinError && p.payload != null) {
                    PinDialog(p.payload.name, p.payload.avatarId, error = p.message, onDismiss = { pinError = false }) { pin ->
                        pinError = false
                        c.transfers.retry(pin)
                    }
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (peers.isNotEmpty()) confirmLeave = true else leave() }) { Icon(Icons.Rounded.Close, "Close") }
                        Text(
                            when {
                                peers.isEmpty() && everConnected -> "Disconnected"
                                anyRunning -> "Transferring"
                                else -> "Connected"
                            },
                            style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    val active = batches.lastOrNull { it.status == BatchStatus.RUNNING } ?: batches.lastOrNull()
                    PeersHeader(peers.firstOrNull() ?: active?.peer, active?.status == BatchStatus.RUNNING, active?.outgoing ?: true)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                        if (band.isNotEmpty()) InfoChip(band, Icons.Rounded.Wifi, if (band == "2.4 GHz") Brand.Orange else Brand.Blue)
                        if (encrypted) InfoChip("PIN encrypted", Icons.Rounded.Lock, Brand.Mint)
                        if (premium) InfoChip("Max speed", Icons.Rounded.Bolt, Brand.Orange)
                        else InfoChip("Free • 25 MB/s", Icons.Rounded.Speed, Brand.Violet)
                    }
                }
                item { SummaryRing(batches, peers.isEmpty() && everConnected) { nav.navigate(Routes.PREMIUM) } }
                if (batches.isEmpty()) {
                    item {
                        Text(
                            if (peers.isEmpty()) "Waiting for connection…" else "Connected! Tap “Send more” to share files, or wait for files from the other phone.",
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                batches.asReversed().forEach { b ->
                    item(key = b.key + "h") { BatchHeader(b) { c.transfers.cancel(b.key) } }
                    itemsIndexed(b.files.take(500), key = { i, _ -> b.key + "#" + i }) { _, f -> FileRow(f, b) }
                    if (b.files.size > 500) item(key = b.key + "more") {
                        Text("+ ${b.files.size - 500} more files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            icon = { Icon(Icons.Rounded.LinkOff, null) },
            title = { Text("Disconnect?") },
            text = { Text(if (anyRunning) "A transfer is still running. Disconnecting will cancel it." else "You can reconnect any time by scanning the QR code again.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; leave() }) { Text("Disconnect", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Stay") } },
        )
    }
}

@Composable
private fun ConnectingView(name: String, avatarId: Int, text: String, modifier: Modifier, onCancel: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                RadarPulse(MaterialTheme.colorScheme.primary, Modifier.size(150.dp))
                MeAvatar(76.dp, ring = true)
            }
            TransferBeam(true, false, Modifier.width(60.dp).height(20.dp))
            Box(contentAlignment = Alignment.Center) {
                RadarPulse(MaterialTheme.colorScheme.secondary, Modifier.size(150.dp))
                AvatarImpl(avatarId, null, 76.dp, ring = true)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (name.isNotEmpty()) "Connecting to $name" else "Connecting", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Keep the phones close for the fastest 5 GHz link", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(24.dp)) { Text("Cancel") }
    }
}

@Composable
private fun PeersHeader(peer: PeerUi?, active: Boolean, outgoing: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            MeAvatar(64.dp, ring = true)
            Spacer(Modifier.height(6.dp))
            Text("You", style = MaterialTheme.typography.labelLarge)
        }
        TransferBeam(active, !outgoing, Modifier.weight(1f).height(24.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (peer != null) Avatar(peer.avatarId, peer.avatar, 64.dp, ring = true) else AvatarImpl(-1, null, 64.dp)
            Spacer(Modifier.height(6.dp))
            Text(peer?.name ?: "—", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (peer != null && peer.model.isNotBlank()) {
                Text(peer.model, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SummaryRing(batches: List<BatchUi>, disconnected: Boolean, onUpgrade: () -> Unit) {
    val running = batches.filter { it.status == BatchStatus.RUNNING || it.status == BatchStatus.WAITING }
    val shown = running.ifEmpty { listOfNotNull(batches.lastOrNull()) }
    val total = shown.sumOf { it.total }
    val done = shown.sumOf { it.transferred }
    val progress = if (total > 0) (done.toDouble() / total).toFloat() else if (shown.any { it.status == BatchStatus.COMPLETED }) 1f else 0f
    val speed = if (running.isNotEmpty()) running.sumOf { it.speed } else shown.firstOrNull()?.speed ?: 0.0
    val eta = running.maxOfOrNull { it.etaSec } ?: -1
    val completed = running.isEmpty() && shown.isNotEmpty() && shown.all { it.status == BatchStatus.COMPLETED }
    val failed = running.isEmpty() && shown.any { it.status == BatchStatus.FAILED || it.status == BatchStatus.CANCELLED }
    val extra = LocalExtra.current

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                ProgressRing(progress, Modifier.fillMaxSize(), stroke = 16.dp, brush = if (completed) Brand.mint else Brand.main)
                AnimatedVisibility(completed, enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(), exit = fadeOut()) {
                    val t = rememberInfiniteTransition(label = "ok")
                    val s by t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
                    Box(Modifier.size(120.dp).scale(s).clip(CircleShape).background(extra.success), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(72.dp))
                    }
                }
                if (!completed) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (failed) Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                        Text(formatSpeed(speed), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val statusText = when {
                completed -> "Transfer complete"
                failed -> shown.firstOrNull { it.error != null }?.error ?: "Transfer stopped"
                disconnected -> "Disconnected"
                running.isNotEmpty() -> if (running.first().outgoing) "Sending…" else "Receiving…"
                else -> "Ready"
            }
            Text(statusText, style = MaterialTheme.typography.titleMedium, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("Transferred", "${formatSize(done)} / ${formatSize(total)}")
                Stat(if (completed) "Avg speed" else "Time left", if (completed) formatSpeed(speed) else formatEta(eta))
                Stat("Files", "${shown.sumOf { it.filesDone }}/${shown.sumOf { it.files.size }}")
            }
            if (!(LocalContext.current.container.prefs.current.premium) && running.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Brand.premium).padding(horizontal = 14.dp, vertical = 7.dp)
                        .then(clickableNoRipple(onUpgrade)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove the 25 MB/s cap with Premium", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BatchHeader(b: BatchUi, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (b.outgoing) Brand.send else Brand.receive),
            contentAlignment = Alignment.Center,
        ) { Icon(if (b.outgoing) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (b.outgoing) "Sent to " else "From ") + b.peer.name,
                style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text("${b.files.size} files • ${formatSize(b.total)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (b.status) {
            BatchStatus.RUNNING, BatchStatus.WAITING -> TextButton(onClick = onCancel) { Text("Cancel", color = MaterialTheme.colorScheme.error) }
            BatchStatus.COMPLETED -> InfoChip("Done", Icons.Rounded.CheckCircle, LocalExtra.current.success)
            BatchStatus.CANCELLED -> InfoChip("Cancelled", Icons.Rounded.Block, MaterialTheme.colorScheme.onSurfaceVariant)
            BatchStatus.FAILED -> InfoChip("Failed", Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FileRow(f: FileUi, b: BatchUi) {
    val ctx = LocalContext.current
    val extra = LocalExtra.current
    val progress = if (f.size > 0) (f.transferred.toDouble() / f.size).toFloat().coerceIn(0f, 1f) else if (f.done) 1f else 0f
    val animated by animateFloatAsState(progress, tween(250), label = "fp")
    val barColor by animateColorAsState(if (f.done) extra.success else MaterialTheme.colorScheme.primary, label = "bc")
    val canOpen = f.done && !b.outgoing && f.uri != null
    AppCard(Modifier.fillMaxWidth(), onClick = if (canOpen) ({ ctx.openFile(f.uri, f.mime) }) else null) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            FileThumb(if (b.outgoing) f.sourceUri else f.uri, f.category, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (f.done) formatSize(f.size) else "${formatSize(f.transferred)} / ${formatSize(f.size)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!f.done && b.status == BatchStatus.RUNNING) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = barColor, trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {},
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                canOpen && f.category == Category.APP -> FilledTonalButton(onClick = { ctx.openFile(f.uri, f.mime) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Install") }
                canOpen && f.category == Category.CONTACT -> FilledTonalButton(onClick = { ctx.openFile(f.uri, "text/x-vcard") }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Import") }
                canOpen -> FilledTonalButton(onClick = { ctx.openFile(f.uri, f.mime) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Open") }
                f.done -> Icon(Icons.Rounded.CheckCircle, null, tint = extra.success)
                b.status == BatchStatus.FAILED || b.status == BatchStatus.CANCELLED -> Icon(Icons.Rounded.Cancel, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
