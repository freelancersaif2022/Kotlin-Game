package com.flashshare.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.flashshare.app.MainActivity
import com.flashshare.app.container
import com.flashshare.app.data.Category
import com.flashshare.app.data.HistoryEntry
import com.flashshare.app.data.StorageInfo
import com.flashshare.app.transfer.Mode
import com.flashshare.app.transfer.Phase
import com.flashshare.app.ui.LocalFeatureGate
import com.flashshare.app.ui.Routes
import com.flashshare.app.ui.components.*
import com.flashshare.app.ui.theme.Brand
import com.flashshare.app.ui.theme.LocalExtra
import com.flashshare.app.ui.withPerms
import com.flashshare.app.util.PermFlow
import com.flashshare.app.util.formatDate
import com.flashshare.app.util.formatSize
import com.flashshare.app.util.formatSpeed
import com.flashshare.app.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The local user's avatar (custom photo or illustration), with a crown for Premium. */
@Composable
fun MeAvatar(size: Dp, modifier: Modifier = Modifier, ring: Boolean = false, showCrown: Boolean = true, goldRing: Boolean = false) {
    val ctx = LocalContext.current
    val prefs = ctx.container.prefs
    val s by prefs.state.collectAsStateWithLifecycle()
    val st = s ?: return
    val bmp by produceState<ImageBitmap?>(null, st.customAvatar, st.avatarVersion) {
        value = if (st.customAvatar) withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(prefs.avatarFile.path)?.asImageBitmap() }.getOrNull()
        } else null
    }
    AvatarImpl(st.avatarId, bmp, size, modifier, ring && !(goldRing && st.premium), crown = showCrown && st.premium, goldRing = goldRing && st.premium)
}

fun playLink(pkg: String) = "https://play.google.com/store/apps/details?id=$pkg"

@OptIn(ExperimentalMaterial3Api::class)
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
    var invite by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { storage = withContext(Dispatchers.IO) { c.cleaner.storage() } }

    LaunchedEffect(pendingShare) {
        if (pendingShare) {
            MainActivity.pendingShare.value = false
            nav.withPerms(ctx, PermFlow.SEND, Routes.scan("send"))
        }
    }
    val active = phase !is Phase.Idle && phase !is Phase.Error

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AmbientBlobs(Modifier.fillMaxWidth().height(420.dp))
        LazyColumn(
            Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeAvatar(52.dp, Modifier.clip(CircleShape).clickable { nav.navigate(Routes.profile(true)) }, ring = true, showCrown = false, goldRing = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(greeting(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(settings.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.size(46.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                            .clickable { nav.navigate(Routes.SETTINGS) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface) }
                }
            }

            item {
                AnimatedVisibility(active, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    val waiting = phase is Phase.Hosting
                    Row(
                        Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Brand.mint).shine(2600, 0.25f)
                            .clickable {
                                nav.navigate(
                                    when {
                                        c.transfers.mode == Mode.GROUP_HOST -> Routes.GROUP
                                        waiting && c.transfers.batches.value.isEmpty() -> Routes.RECEIVE
                                        else -> Routes.TRANSFER
                                    }
                                )
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadarPulse(Color.White, Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(if (waiting) "Waiting for a phone to connect" else "Connection active – tap to return", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
                    }
                }
            }

            item { HeroCard(settings.premium, onUpgrade = { nav.navigate(Routes.PREMIUM) }) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ActionCard("Send", "Pick files & scan", Icons.Rounded.ArrowUpward, Brand.send, Modifier.weight(1f)) {
                        nav.withPerms(ctx, PermFlow.SEND, Routes.picker("send"))
                    }
                    ActionCard("Receive", "Show your QR code", Icons.Rounded.ArrowDownward, Brand.receive, Modifier.weight(1f)) {
                        nav.withPerms(ctx, PermFlow.RECEIVE, Routes.RECEIVE)
                    }
                }
            }

            item { SectionTitle("Tools") }
            item {
                val tools = listOf(
                    Tool("Group Share", Icons.Rounded.Groups, IconBrushes.violet, true) {
                        gate.request("Group Share") { nav.withPerms(ctx, PermFlow.SEND, Routes.picker("group")) }
                    },
                    Tool("Phone Clone", Icons.Rounded.PhonelinkSetup, IconBrushes.cyan, true) {
                        gate.request("Phone Clone") { nav.navigate(Routes.CLONE) }
                    },
                    Tool("Cleaner", Icons.Rounded.RocketLaunch, IconBrushes.mint, true) {
                        gate.request("Cleaner") { nav.withPerms(ctx, PermFlow.FILES, Routes.CLEANER) }
                    },
                    Tool("Files", Icons.Rounded.FolderOpen, IconBrushes.orange, false) { nav.withPerms(ctx, PermFlow.FILES, Routes.FILES) },
                    Tool("History", Icons.Rounded.History, IconBrushes.blue, false) { nav.navigate(Routes.HISTORY) },
                    Tool("Invite", Icons.Rounded.CardGiftcard, IconBrushes.pink, false) { invite = true },
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tools.chunked(3).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEachIndexed { i, t -> ToolTile(t, settings.premium, Modifier.weight(1f).staggerIn(r * 3 + i)) }
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
                itemsIndexed(history.take(3), key = { _, e -> e.id }) { i, e -> HistoryRow(e, Modifier.staggerIn(i)) { nav.navigate(Routes.HISTORY) } }
            }
        }
    }

    if (invite) InviteSheet { invite = false }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Hello" }
}

@Composable
private fun HeroCard(premium: Boolean, onUpgrade: () -> Unit) {
    val t = rememberInfiniteTransition(label = "hero")
    val a by t.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "a")
    val gauge by t.animateFloat(if (premium) 0.78f else 0.3f, if (premium) 0.95f else 0.42f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    Box(
        Modifier.fillMaxWidth().shadow(22.dp, RoundedCornerShape(30.dp), spotColor = Brand.Violet, ambientColor = Brand.Blue)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF101536), Color(0xFF26206A), Color(0xFF4B2BB8))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            fun blob(x: Float, y: Float, r: Float, c: Color) = drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), Offset(x, y), r), r, Offset(x, y))
            blob(size.width * (0.85f + 0.08f * kotlin.math.cos(a)), size.height * (0.2f + 0.1f * kotlin.math.sin(a)), size.width * 0.55f, Color(0xFF19C3FF).copy(alpha = 0.35f))
            blob(size.width * (0.1f + 0.08f * kotlin.math.sin(a)), size.height * (0.95f + 0.05f * kotlin.math.cos(a)), size.width * 0.5f, Color(0xFFFF4D9D).copy(alpha = 0.28f))
            // fine grid for a "tech" texture
            val step = 22.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(Color.White.copy(alpha = 0.04f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
            var y = 0f
            while (y < size.height) { drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
        }
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF3DEBB0)))
                    Spacer(Modifier.width(6.dp))
                    Text("WI-FI DIRECT • 5/6 GHz", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text("Share without limits", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("No internet. No cables. No data.", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                if (premium) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Brand.premium).shine(2400, 0.25f).padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Max speed unlocked", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                } else {
                    val src = remember { MutableInteractionSource() }
                    Row(
                        Modifier.pressScale(src).shadow(12.dp, RoundedCornerShape(50), spotColor = Brand.Orange)
                            .clip(RoundedCornerShape(50)).background(Brand.premium).shine(2200)
                            .clickable(interactionSource = src, indication = ripple(color = Color.White), onClick = onUpgrade)
                            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Unlock Max Speed", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(contentAlignment = Alignment.Center) {
                SpeedGauge(gauge, if (premium) "MAX" else "25 MB/s", Modifier.size(96.dp))
            }
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: ImageVector, brush: Brush, modifier: Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val t = rememberInfiniteTransition(label = "card")
    val float by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    val ring by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "r")
    Box(
        modifier.pressScale(src).height(178.dp)
            .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color(0xFF3D3DFF))
            .clip(RoundedCornerShape(30.dp)).background(brush)
            .clickable(interactionSource = src, indication = ripple(color = Color.White), onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width * 0.95f, size.height * 0.05f)
            for (i in 1..4) drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.22f * i, c, style = Stroke(1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = (1f - ring) * 0.25f), size.width * (0.2f + 0.7f * ring), c, style = Stroke(2.dp.toPx()))
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), endY = size.height * 0.5f))
        }
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Box(
                Modifier.size(58.dp).graphicsLayer { translationY = float }.shadow(10.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.25f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, maxLines = 1, softWrap = false)
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private class Tool(val title: String, val icon: ImageVector, val brush: Brush, val pro: Boolean, val onClick: () -> Unit)

@Composable
private fun ToolTile(t: Tool, premium: Boolean, modifier: Modifier) {
    AppCard(modifier, onClick = t.onClick) {
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 6.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                GlossyIcon(t.icon, t.brush, 52.dp)
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
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                ProgressRing(info.usedFraction, Modifier.fillMaxSize(), stroke = 6.dp, brush = Brand.mint)
                Text("${(info.usedFraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Internal storage", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { info.usedFraction }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Brand.Mint, trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {},
                )
                Spacer(Modifier.height(6.dp))
                Text("${formatSize(info.used)} used • ${formatSize(info.free)} free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HistoryRow(e: HistoryEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val extra = LocalExtra.current
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                val first = e.files.firstOrNull()
                if (first != null) FileThumb(first.uri, Category.fromMime(first.mime, first.name), 50.dp, thumbPath = first.thumb, name = first.name, corner = 14.dp)
                else AvatarImpl(e.peerAvatarId, null, 50.dp)
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(22.dp).clip(CircleShape)
                        .background(if (e.sent) Brand.send else Brand.receive).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (e.sent) Icons.AutoMirrored.Rounded.CallMade else Icons.AutoMirrored.Rounded.CallReceived, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (e.files.size == 1) e.files[0].name else (if (e.sent) "Sent to " else "From ") + e.peerName,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val link = playLink(ctx.packageName)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GlossyIcon(Icons.Rounded.CardGiftcard, IconBrushes.pink, 72.dp)
            Spacer(Modifier.height(14.dp))
            Text("Invite friends to FlashShare", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("They scan this code to get the app – then you can share anything at 5 GHz speed.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.size(196.dp).shadow(10.dp, RoundedCornerShape(26.dp)).clip(RoundedCornerShape(26.dp)).background(Color.White).padding(16.dp)) {
                StyledQr(link, Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.Center).size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(3.dp)) { LogoTile(size = 38.dp) }
            }
            Spacer(Modifier.height(20.dp))
            GradientButton("Share invite link", icon = Icons.Rounded.Share, modifier = Modifier.fillMaxWidth(), onClick = {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "I send files at 5 GHz speed with FlashShare – no internet needed. Get it here: $link"), "Invite friends"))
            })
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("FlashShare", link))
                ctx.toast("Link copied")
            }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) {
                Icon(Icons.Rounded.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text("Copy link")
            }
        }
    }
}
