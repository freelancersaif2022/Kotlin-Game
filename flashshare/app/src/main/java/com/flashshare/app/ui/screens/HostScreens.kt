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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
            AnimatedContent(phase, transitionSpec = { (fadeIn(tween(400)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(200)) },
                contentKey = { it::class }, label = "phase") { p ->
                when (p) {
                    is Phase.Hosting -> HostCard(p.qr, p.band, p.pin, "Scan with FlashShare on the sending phone")
                    is Phase.Error -> ErrorCard(p.message) { c.transfers.startHosting(Mode.RECEIVE) }
                    else -> HostingAnimation(Modifier.fillMaxWidth().padding(top = 12.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            if (phase is Phase.Hosting) {
                JoinGroupButton {
                    c.transfers.disconnect()
                    nav.popBackStack()
                    nav.withPerms(ctx, PermFlow.JOIN, Routes.scan("join"))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun JoinGroupButton(onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(Icons.Rounded.Groups, IconBrushes.violet, 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Join a group share", style = MaterialTheme.typography.titleSmall)
                Text("Someone is sending to many phones? Scan their code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** QR card + device identity + band/security chips + PIN. Shared by Receive, Group Share, Phone Clone and the transfer screen. */
@Composable
fun HostCard(qr: String, band: String, pin: String?, hint: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    val ctx = LocalContext.current
    val settings by ctx.container.prefs.state.collectAsStateWithLifecycle()
    val t = rememberInfiniteTransition(label = "dot")
    val dot by t.animateFloat(0.6f, 1.2f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "d")
    QrCard(
        qr,
        center = { MeAvatar(54.dp, showCrown = false) },
        footer = {
            Spacer(Modifier.height(14.dp))
            Text(settings?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF111827))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).scale(dot).clip(CircleShape).background(Color(0xFF00C48C)))
                Spacer(Modifier.width(6.dp))
                Text("Ready to receive", color = Color(0xFF00A877), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(band.ifEmpty { "Wi-Fi Direct" }, Icons.Rounded.Wifi, if (band == "2.4 GHz") Brand.Orange else Brand.Blue)
                if (pin != null) InfoChip("Encrypted", Icons.Rounded.Lock, Brand.Mint)
                else InfoChip("Offline", Icons.Rounded.CloudOff, Brand.Violet)
            }
            Spacer(Modifier.height(8.dp))
            Text(hint, color = Color(0xFF5B6478), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 240.dp))
        },
    )
    if (pin != null) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.clip(RoundedCornerShape(22.dp)).background(Brand.main).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Lock, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("PAIRING PIN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.8f))
                Text(pin.chunked(3).joinToString("  "), style = MaterialTheme.typography.headlineSmall, letterSpacing = 4.sp, color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlossyIcon(Icons.Rounded.WifiOff, IconBrushes.red, 72.dp)
            Spacer(Modifier.height(16.dp))
            Text("Connection problem", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            GradientButton("Try again", onClick = onRetry, icon = Icons.Rounded.Refresh, height = 50.dp)
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
                AnimatedContent(phase, transitionSpec = { (fadeIn(tween(400)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(200)) },
                    contentKey = { it::class }, label = "gphase") { p ->
                    when (p) {
                        is Phase.Hosting -> HostCard(p.qr, p.band, p.pin, "On each phone: Receive › Join a group share, then scan")
                        is Phase.Error -> ErrorCard(p.message) { nav.popBackStack() }
                        else -> HostingAnimation(Modifier.fillMaxWidth())
                    }
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
            itemsIndexed(peers, key = { _, p -> p.key }) { i, peer ->
                val b = batches.lastOrNull { it.peer.key == peer.key }
                AppCard(Modifier.fillMaxWidth().staggerIn(i)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(peer.avatarId, peer.avatar, 46.dp)
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
                        if (b?.status == BatchStatus.COMPLETED) {
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Rounded.CheckCircle, null, tint = LocalExtra.current.success)
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
        AppDialog(
            onDismiss = { confirmExit = false }, icon = Icons.Rounded.Groups, title = "Stop group share?",
            message = "Transfers still in progress will be cancelled.", confirmText = "Stop sharing", destructive = true,
            dismissText = "Keep sharing",
            onConfirm = { confirmExit = false; c.transfers.disconnect(); nav.popBackStack(Routes.HOME, false) },
        )
    }
}
