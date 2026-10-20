package com.flashshare.app.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.MainActivity
import com.flashshare.app.container
import com.flashshare.app.data.HistoryEntry
import com.flashshare.app.data.StorageInfo
import com.flashshare.app.transfer.Mode
import com.flashshare.app.transfer.Phase
import com.flashshare.app.ui.LocalFeatureGate
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed
import com.flashshare.app.ui.withPerms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The local user's avatar (custom photo or emoji), refreshed whenever the profile is saved. */
@Composable
fun MeAvatar(size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val ctx = LocalContext.current
    val prefs = ctx.container.prefs
    val s by prefs.state.collectAsStateWithLifecycle()
    val st = s ?: return
    val bmp by produceState<ImageBitmap?>(null, st.customAvatar, st.avatarVersion) {
        value = if (st.customAvatar) withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(prefs.avatarFile.path)?.asImageBitmap() }.getOrNull()
        } else null
    }
    AvatarImpl(st.avatarId, bmp, size, modifier, ring)
}

@Composable
fun HomeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val c = ctx.container
    val s by c.prefs.state.collectAsStateWithLifecycle()
    val settings = s ?: return
    val history by c.history.entries.collectAsStateWithLifecycle()
    val phase by c.transfers.phase.collectAsStateWithLifecycle()
    val gate = LocalFeatureGate.current
    val pendingShare by MainActivity.pendingShare.collectAsStateWithLifecycle()
    var storage by remember { mutableStateOf(StorageInfo(0, 0)) }
    LaunchedEffect(Unit) { storage = withContext(Dispatchers.IO) { c.cleaner.storage() } }

    LaunchedEffect(pendingShare) {
        if (pendingShare) {
            MainActivity.pendingShare.value = false
            nav.withPerms(ctx, PermFlow.SEND, Routes.scan("send"))
        }
    }

    val active = phase !is Phase.Idle && phase !is Phase.Error

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 24.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeAvatar(46.dp, Modifier.clip(CircleShape).clickable { nav.navigate(Routes.profile(true)) })
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hi there 👋", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(settings.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (settings.premium) {
                        InfoChip("PREMIUM", Icons.Rounded.WorkspacePremium, Brand.Orange)
                    } else {
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(Brand.premium).clickable { nav.navigate(Routes.PREMIUM) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Go PRO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Settings") }
                }
            }

            item {
                AnimatedVisibility(active, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    val waiting = phase is Phase.Hosting
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.mint)
                            .clickable {
                                nav.navigate(
                                    when {
                                        c.transfers.mode == Mode.GROUP_HOST -> Routes.GROUP
                                        waiting -> Routes.RECEIVE
                                        else -> Routes.TRANSFER
                                    }
                                )
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text(if (waiting) "Waiting for a phone to connect" else "Connection active – tap to return", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                    }
                }
            }

            item { HeroCard(settings.premium, onUpgrade = { nav.navigate(Routes.PREMIUM) }) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ActionTile("Send", "Pick files & scan", Icons.Rounded.ArrowUpward, Brand.send, Modifier.weight(1f)) {
                        nav.withPerms(ctx, PermFlow.SEND, Routes.picker("send"))
                    }
                    ActionTile("Receive", "Show your QR code", Icons.Rounded.ArrowDownward, Brand.receive, Modifier.weight(1f)) {
                        nav.withPerms(ctx, PermFlow.RECEIVE, Routes.RECEIVE)
                    }
                }
            }

            item { SectionTitle("Tools") }
            item {
                val tools = listOf(
                    Tool("Group Share", Icons.Rounded.Groups, Color(0xFF7B4DFF), true) {
                        gate.request("Group Share") { nav.withPerms(ctx, PermFlow.SEND, Routes.picker("group")) }
                    },
                    Tool("Phone Clone", Icons.Rounded.PhonelinkSetup, Color(0xFF00A3FF), true) {
                        gate.request("Phone Clone") { nav.navigate(Routes.CLONE) }
                    },
                    Tool("Cleaner", Icons.Rounded.CleaningServices, Color(0xFF00B07A), true) {
                        gate.request("Cleaner") { nav.withPerms(ctx, PermFlow.FILES, Routes.CLEANER) }
                    },
                    Tool("Files", Icons.Rounded.FolderOpen, Color(0xFFFF8A00), false) {
                        nav.withPerms(ctx, PermFlow.FILES, Routes.FILES)
                    },
                    Tool("History", Icons.Rounded.History, Color(0xFF2F7BFF), false) { nav.navigate(Routes.HISTORY) },
                    Tool("Invite", Icons.Rounded.Share, Color(0xFFFF4D6D), false) {
                        val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
                            Intent.EXTRA_TEXT,
                            "I use FlashShare to send files at 5 GHz speed without internet. Get it: https://play.google.com/store/apps/details?id=${ctx.packageName}"
                        )
                        ctx.startActivity(Intent.createChooser(i, "Invite friends"))
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tools.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { t -> ToolTile(t, settings.premium, Modifier.weight(1f)) }
                        }
                    }
                }
            }

            item { StorageCard(storage) { nav.withPerms(ctx, PermFlow.FILES, Routes.FILES) } }

            if (history.isNotEmpty()) {
                item {
                    SectionTitle("Recent transfers") {
                        TextButton(onClick = { nav.navigate(Routes.HISTORY) }) { Text("See all") }
                    }
                }
                items(history.take(3), key = { it.id }) { e -> HistoryRow(e) { nav.navigate(Routes.HISTORY) } }
            }
        }
    }
}

@Composable
private fun HeroCard(premium: Boolean, onUpgrade: () -> Unit) {
    val t = rememberInfiniteTransition(label = "hero")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "s")
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2F5BFF), Color(0xFF6A3DFF), Color(0xFFB04DFF)),
                start = androidx.compose.ui.geometry.Offset(shift * 400f, 0f), end = androidx.compose.ui.geometry.Offset(800f + shift * 400f, 600f)))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("WI-FI DIRECT • 5/6 GHz", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Share without limits", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("No internet. No cables. No mobile data.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (premium) "Max speed unlocked – up to your Wi-Fi limit" else "Free: up to ${formatSpeed(25.0 * 1048576)}",
                    color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                if (!premium) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable(onClick = onUpgrade).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Unlock max speed", color = Brand.Violet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, icon: ImageVector, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val t = rememberInfiniteTransition(label = "tile")
    val float by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    Column(
        modifier.pressScale(src).height(168.dp).clip(RoundedCornerShape(28.dp)).background(brush)
            .clickable(interactionSource = src, indication = ripple(color = Color.White), onClick = onClick)
            .padding(18.dp),
    ) {
        Box(
            Modifier.size(56.dp).graphicsLayer { translationY = float }.clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.weight(1f))
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
    }
}

private class Tool(val title: String, val icon: ImageVector, val color: Color, val pro: Boolean, val onClick: () -> Unit)

@Composable
private fun ToolTile(t: Tool, premium: Boolean, modifier: Modifier) {
    AppCard(modifier, onClick = t.onClick) {
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(t.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(t.icon, null, tint = t.color, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(t.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
            if (t.pro && !premium) ProBadge(Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun StorageCard(info: StorageInfo, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                ProgressRing(info.usedFraction, Modifier.fillMaxSize(), stroke = 6.dp, brush = Brand.mint)
                Text("${(info.usedFraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Internal storage", style = MaterialTheme.typography.titleSmall)
                Text("${formatSize(info.used)} used of ${formatSize(info.total)} • ${formatSize(info.free)} free",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HistoryRow(e: HistoryEntry, onClick: () -> Unit) {
    val extra = LocalExtra.current
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                AvatarImpl(e.peerAvatarId, null, 44.dp)
                Box(
                    Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape)
                        .background(if (e.sent) Brand.Blue else Brand.Violet),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (e.sent) Icons.AutoMirrored.Rounded.CallMade else Icons.AutoMirrored.Rounded.CallReceived, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    (if (e.sent) "Sent to " else "Received from ") + e.peerName,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${e.files.size} file${if (e.files.size == 1) "" else "s"} • ${formatSize(e.totalBytes)} • ${formatDate(e.time)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                )
            }
            when (e.status) {
                "completed" -> Text(formatSpeed(e.avgSpeed), style = MaterialTheme.typography.labelMedium, color = extra.success, fontWeight = FontWeight.Bold)
                "cancelled" -> Text("Cancelled", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text("Failed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
