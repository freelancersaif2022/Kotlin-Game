package com.flashshare.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.container
import com.flashshare.app.net.BatchStatus
import com.flashshare.app.transfer.Mode
import com.flashshare.app.transfer.Phase
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.ui.withPerms
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed

@Composable
fun ReceiveScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val phase by c.transfers.phase.collectAsStateWithLifecycle()
    val settings by c.prefs.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val p = c.transfers.phase.value
        if (p is Phase.Idle || p is Phase.Error || c.transfers.mode != Mode.RECEIVE) c.transfers.startHosting(Mode.RECEIVE)
    }
    LaunchedEffect(phase) {
        if (phase is Phase.Connected) nav.navigate(Routes.TRANSFER) { popUpTo(Routes.HOME) }
    }
    val leave = {
        c.transfers.disconnect()
        nav.popBackStack()
        Unit
    }
    BackHandler(onBack = leave)

    AppScaffold("Receive", onBack = leave) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            AnimatedContent(phase, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut() },
                contentKey = { it::class }, label = "phase") { p ->
                when (p) {
                    is Phase.Hosting -> HostCard(p.qr, p.band, p.pin, "Scan with FlashShare on the sending phone")
                    is Phase.Error -> ErrorCard(p.message) { c.transfers.startHosting(Mode.RECEIVE) }
                    else -> PreparingCard((p as? Phase.Preparing)?.text ?: "Starting…")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(settings?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Text(
                if (phase is Phase.Hosting) "Ready to receive" else "Getting ready…",
                color = LocalExtra.current.success, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            HowTo(
                listOf(
                    "Open FlashShare on the other phone",
                    "Tap Send and pick your files",
                    "Scan this QR code – transfer starts instantly",
                )
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {
                c.transfers.disconnect()
                nav.popBackStack()
                nav.withPerms(ctx, PermFlow.JOIN, Routes.scan("join"))
            }) {
                Icon(Icons.Rounded.Groups, null)
                Spacer(Modifier.width(8.dp))
                Text("Join a group share instead")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun HostCard(qr: String, band: String, pin: String?, hint: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        RadarPulse(MaterialTheme.colorScheme.primary, Modifier.size(360.dp))
        Column(
            Modifier.shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = Brand.Blue, spotColor = Brand.Blue)
                .clip(RoundedCornerShape(32.dp)).background(Color.White).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
                QrCode(qr, Modifier.fillMaxSize())
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White).padding(4.dp)) { MeAvatar(48.dp) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(band.ifEmpty { "Wi-Fi Direct" }, Icons.Rounded.Wifi, if (band == "2.4 GHz") Brand.Orange else Brand.Blue)
                if (pin != null) InfoChip("Encrypted", Icons.Rounded.Lock, Brand.Mint)
                else InfoChip("Offline", Icons.Rounded.CloudOff, Brand.Violet)
            }
            Spacer(Modifier.height(8.dp))
            Text(hint, color = Color(0xFF5B6478), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 240.dp))
        }
    }
    if (pin != null) {
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PAIRING PIN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(pin.chunked(3).joinToString(" "), style = MaterialTheme.typography.headlineMedium, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun PreparingCard(text: String) {
    Box(Modifier.fillMaxWidth().height(380.dp), contentAlignment = Alignment.Center) {
        RadarPulse(MaterialTheme.colorScheme.primary, Modifier.size(320.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Brand.main), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WifiTethering, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Locking to the fastest Wi-Fi band", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WifiOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Connection problem", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            GradientButton("Try again", onClick = onRetry, icon = Icons.Rounded.Refresh, height = 48.dp)
        }
    }
}

@Composable
fun HowTo(steps: List<String>) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            steps.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(s, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun GroupHostScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val phase by c.transfers.phase.collectAsStateWithLifecycle()
    val peers by c.transfers.peers.collectAsStateWithLifecycle()
    val batches by c.transfers.batches.collectAsStateWithLifecycle()
    var confirmExit by remember { mutableStateOf(false) }

    val leave = {
        if (batches.any { it.status == BatchStatus.RUNNING }) confirmExit = true
        else { c.transfers.disconnect(); nav.popBackStack(Routes.HOME, false) }
        Unit
    }
    BackHandler(onBack = leave)

    AppScaffold("Group Share", onBack = leave) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                when (val p = phase) {
                    is Phase.Hosting -> HostCard(p.qr, p.band, p.pin, "On each receiving phone: Receive › Join a group share, then scan")
                    is Phase.Error -> ErrorCard(p.message) { nav.popBackStack() }
                    else -> PreparingCard((p as? Phase.Preparing)?.text ?: "Starting…")
                }
            }
            item {
                SectionTitle(if (peers.isEmpty()) "Waiting for phones to join…" else "${peers.size} phone${if (peers.size == 1) "" else "s"} joined")
            }
            if (peers.isEmpty()) {
                item {
                    Text("Up to 8 phones can join. Each one receives all your selected files automatically.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            items(peers, key = { it.key }) { peer ->
                val b = batches.lastOrNull { it.peer.key == peer.key }
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(peer.avatarId, peer.avatar, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(peer.name, style = MaterialTheme.typography.titleSmall)
                            val status = when (b?.status) {
                                null, BatchStatus.WAITING -> "Preparing…"
                                BatchStatus.RUNNING -> "${(b.progress * 100).toInt()}% • ${formatSpeed(b.speed)}"
                                BatchStatus.COMPLETED -> "Done • ${formatSize(b.total)}"
                                BatchStatus.CANCELLED -> "Cancelled"
                                BatchStatus.FAILED -> b.error ?: "Failed"
                            }
                            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { b?.progress ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (b?.status == BatchStatus.COMPLETED) LocalExtra.current.success else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                drawStopIndicator = {},
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                GradientButton("Finish", onClick = leave, icon = Icons.Rounded.Check, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Stop group share?") },
            text = { Text("Transfers still in progress will be cancelled.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; c.transfers.disconnect(); nav.popBackStack(Routes.HOME, false) }) { Text("Stop") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Keep sharing") } },
        )
    }
}
