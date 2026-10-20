package com.tvremote.universal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tvremote.universal.App
import com.tvremote.universal.core.LocalNetwork
import com.tvremote.universal.data.DiscoveredTv
import com.tvremote.universal.data.Guides
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.tv.TvDiscovery
import com.tvremote.universal.ui.components.AppBar
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    onBack: () -> Unit,
    onSelect: (DiscoveredTv) -> Unit,
    onManual: () -> Unit,
    onHelp: () -> Unit,
) {
    val context = LocalContext.current
    val container = App.container
    val found = remember { mutableStateListOf<DiscoveredTv>() }
    var scanning by remember { mutableStateOf(true) }
    var scanId by remember { mutableIntStateOf(0) }
    val onWifi = remember(scanId) { LocalNetwork.isOnLocalNetwork(context) }

    LaunchedEffect(scanId) {
        scanning = true
        container.discovery.discover().collect { tv ->
            if (found.none { it.host == tv.host && it.platform == tv.platform }) found.add(tv)
        }
        scanning = false
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppBar(title = "Add a TV", onBack = onBack, actions = {
            IconButton(onClick = onHelp) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Help") }
        })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Radar(active = scanning, modifier = Modifier.size(190.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            scanning && found.isEmpty() -> "Looking for TVs on your Wi-Fi…"
                            scanning -> "Found ${found.size} — still looking…"
                            found.isEmpty() -> "No TVs found"
                            else -> "Found ${found.size} TV${if (found.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Make sure your TV is on and connected to the same Wi-Fi as this phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (!onWifi) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = AppTheme.extra.warning)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "You're not on Wi-Fi. Connect this phone to the same Wi-Fi network as your TV.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (found.isNotEmpty()) item { SectionTitle("TVs on your network") }
            items(found, key = { "${it.platform}|${it.host}" }) { tv ->
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically { it / 3 }) {
                    FoundTvRow(tv, paired = container.tvStore.findByHost(tv.platform, tv.host) != null) { onSelect(tv) }
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!scanning) {
                SecondaryButton(
                    "Scan again",
                    onClick = {
                        found.clear()
                        scanId++
                    },
                    icon = Icons.Rounded.Refresh,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = onManual) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Can't find your TV? Add it manually")
            }
        }
    }
}

@Composable
private fun FoundTvRow(tv: DiscoveredTv, paired: Boolean, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TvGlyph(tv.platform, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tv.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(tv.platform)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tv.model.ifBlank { tv.host },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (paired) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Paired", tint = AppTheme.extra.success)
                Spacer(Modifier.width(6.dp))
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Radar(active: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "radarProgress",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2
            if (active) {
                for (i in 0 until 3) {
                    val p = (progress + i / 3f) % 1f
                    drawCircle(color = primary.copy(alpha = (1f - p) * 0.28f), radius = maxR * (0.3f + 0.7f * p))
                }
            } else {
                drawCircle(color = primary.copy(alpha = 0.08f), radius = maxR)
                drawCircle(color = primary.copy(alpha = 0.10f), radius = maxR * 0.72f)
            }
        }
        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(primary, secondary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

// ---------------------------------------------------------------------------------- manual

@Composable
fun ManualAddScreen(onBack: () -> Unit, onContinue: (DiscoveredTv) -> Unit) {
    var brand by remember { mutableStateOf<Guides.Brand?>(null) }
    var platform by remember { mutableStateOf<TvPlatform?>(null) }
    var ip by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var probeFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ipValid = LocalNetwork.isValidIpv4(ip)

    fun proceed() {
        val p = platform ?: return
        onContinue(DiscoveredTv(p, ip.trim(), brand?.name?.let { "$it TV" } ?: p.short))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        AppBar(title = "Add manually", subtitle = "Choose your TV brand", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionTitle("1 · TV brand")
            BrandGrid(selected = brand) {
                brand = it
                platform = it.platforms.singleOrNull()
                probeFailed = false
            }
            brand?.note?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val b = brand
            if (b != null && b.platforms.size > 1) {
                Spacer(Modifier.height(18.dp))
                SectionTitle("2 · Which home screen does your TV have?")
                b.platforms.forEach { p ->
                    val selected = platform == p
                    AppCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(
                                2.dp,
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                MaterialTheme.shapes.large,
                            ),
                        onClick = { platform = p; probeFailed = false },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            TvGlyph(p, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, style = MaterialTheme.typography.titleSmall)
                                Text(Guides.platformHint(p), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            val p = platform
            if (p != null) {
                Spacer(Modifier.height(18.dp))
                SectionTitle("${if (b != null && b.platforms.size > 1) 3 else 2} · TV IP address")
                OutlinedTextField(
                    value = ip,
                    onValueChange = { v -> ip = v.filter { it.isDigit() || it == '.' }.take(15); probeFailed = false },
                    label = { Text("IP address, e.g. 192.168.1.25") },
                    singleLine = true,
                    isError = ip.isNotEmpty() && !ipValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Find it on your TV: ${Guides.of(p).findIp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (probeFailed) {
                    Spacer(Modifier.height(12.dp))
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "We couldn't reach a ${p.short} TV at $ip.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                "Check that the TV is on, the IP is correct and both devices use the same Wi-Fi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { proceed() }) { Text("Continue anyway") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            text = "Continue",
            enabled = platform != null && ipValid,
            loading = checking,
            onClick = {
                val target = platform ?: return@PrimaryButton
                scope.launch {
                    checking = true
                    val ok = TvDiscovery.probe(target, ip.trim())
                    checking = false
                    if (ok) proceed() else probeFailed = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun BrandGrid(selected: Guides.Brand?, onSelect: (Guides.Brand) -> Unit) {
    val rows = Guides.brands.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b ->
                    val isSelected = selected == b
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(shape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape,
                            )
                            .clickable { onSelect(b) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            b.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

