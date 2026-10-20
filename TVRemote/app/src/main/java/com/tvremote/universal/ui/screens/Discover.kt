package com.tvremote.universal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.tvremote.universal.ui.components.IconTile
import com.tvremote.universal.ui.components.NumberedStep
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.RadarScanner
import com.tvremote.universal.ui.components.SecondaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.StatusPill
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.components.brandBrush
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
    val ownIp = remember(scanId) { LocalNetwork.ownIpv4(context) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(scanId) {
        scanning = true
        container.discovery.discover(durationMs = 10_000).collect { tv ->
            val idx = found.indexOfFirst { it.host == tv.host && it.platform == tv.platform }
            if (idx >= 0) found[idx] = tv else found.add(tv)
        }
        scanning = false
    }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        AppBar(title = "Add a TV", subtitle = "Searching your Wi-Fi network", onBack = onBack, actions = {
            IconButton(onClick = onHelp) { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Help") }
        })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    RadarScanner(active = scanning, found = found.size, modifier = Modifier.size(230.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        when {
                            scanning && found.isEmpty() -> "Looking for TVs…"
                            scanning -> "Found ${found.size} · still scanning"
                            found.isEmpty() -> "No TVs found yet"
                            else -> "Found ${found.size} TV${if (found.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onBackground,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (scanning) ScanProgress()
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onWifi) StatusPill("Wi-Fi connected", AppTheme.extra.success)
                        else StatusPill("Not on Wi-Fi", AppTheme.extra.warning)
                        if (ownIp != null) StatusPill("Phone $ownIp", scheme.primary)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (!onWifi) {
                item {
                    TipCard(
                        icon = Icons.Rounded.WifiOff,
                        tint = AppTheme.extra.warning,
                        title = "Connect to Wi-Fi",
                        text = "Your phone must be on the same Wi-Fi network as your TV to find and control it.",
                    )
                }
            }
            if (found.isNotEmpty()) item { SectionTitle("TVs on your network") }
            items(found, key = { "${it.platform}|${it.host}" }) { tv ->
                AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                    FoundTvRow(tv, paired = container.tvStore.findByHost(tv.platform, tv.host) != null) { onSelect(tv) }
                }
            }
            if (!scanning && found.isEmpty()) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Can't see your TV?", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            NumberedStep(1, "Turn the TV on (not standby) and open its home screen.")
                            NumberedStep(2, "Check the phone and TV use the same Wi-Fi — not a guest network.")
                            NumberedStep(3, "Turn off VPN apps on this phone, then tap Scan again.")
                            NumberedStep(4, "Still nothing? Add the TV manually with its IP address.")
                        }
                    }
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    if (scanning) "Scanning…" else "Scan again",
                    onClick = {
                        found.clear()
                        scanId++
                    },
                    icon = Icons.Rounded.Refresh,
                    enabled = !scanning,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    "Add manually",
                    onClick = onManual,
                    icon = Icons.Rounded.Edit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ScanProgress() {
    val t = rememberInfiniteTransition(label = "scan")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x")
    Box(
        Modifier.width(160.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .width(56.dp)
                .height(4.dp)
                .graphicsLayer { translationX = (x * 216f - 56f) * density }
                .clip(CircleShape)
                .background(brandBrush()),
        )
    }
}

@Composable
private fun FoundTvRow(tv: DiscoveredTv, paired: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TvGlyph(tv.platform, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tv.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(tv.platform)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tv.model.ifBlank { tv.host },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (paired) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Already paired", tint = AppTheme.extra.success)
                Spacer(Modifier.width(8.dp))
            }
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(brandBrush()).padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(if (paired) "Open" else "Pair", style = MaterialTheme.typography.labelLarge, color = Color.White) }
        }
    }
}

@Composable
fun TipCard(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, text: String, content: (@Composable () -> Unit)? = null) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon, tint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content?.invoke()
        }
    }
}

// ---------------------------------------------------------------------------------- manual

@Composable
fun ManualAddScreen(onBack: () -> Unit, onContinue: (DiscoveredTv) -> Unit) {
    var brand by remember { mutableStateOf<Guides.Brand?>(null) }
    var platform by remember { mutableStateOf<TvPlatform?>(null) }
    var ip by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var probeFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ipValid = LocalNetwork.isValidIpv4(ip)
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val ownIp = remember { LocalNetwork.ownIpv4(context) }

    fun proceed() {
        val p = platform ?: return
        onContinue(DiscoveredTv(p, ip.trim(), brand?.name?.let { "$it TV" } ?: p.short))
    }

    fun check() {
        val target = platform ?: return
        scope.launch {
            checking = true
            probeFailed = false
            val ok = TvDiscovery.probe(target, ip.trim())
            checking = false
            if (ok) proceed() else probeFailed = true
        }
    }

    Column(Modifier.fillMaxSize().background(scheme.background).imePadding()) {
        AppBar(title = "Add manually", subtitle = "Choose your TV, then enter its IP address", onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            StepHeader(1, "Which brand is your TV?", done = brand != null)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search brand") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            val brands = Guides.brands.filter { it.name.contains(query.trim(), ignoreCase = true) }
            BrandGrid(brands, selected = brand) {
                brand = it
                platform = it.platforms.singleOrNull()
                probeFailed = false
            }
            brand?.note?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }

            val b = brand
            if (b != null && b.platforms.size > 1) {
                Spacer(Modifier.height(20.dp))
                StepHeader(2, "Which home screen does it show?", done = platform != null)
                b.platforms.forEach { p ->
                    val selected = platform == p
                    AppCard(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        onClick = { platform = p; probeFailed = false },
                        border = androidx.compose.foundation.BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) scheme.primary else AppTheme.extra.panelBorder,
                        ),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            TvGlyph(p, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text(Guides.platformHint(p), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary)
                        }
                    }
                }
            }

            val p = platform
            if (p != null) {
                Spacer(Modifier.height(20.dp))
                StepHeader(if (b != null && b.platforms.size > 1) 3 else 2, "Enter the TV's IP address", done = ipValid)
                OutlinedTextField(
                    value = ip,
                    onValueChange = { v -> ip = v.filter { it.isDigit() || it == '.' }.take(15); probeFailed = false },
                    label = { Text("TV IP address") },
                    placeholder = { Text(ownIp?.substringBeforeLast('.')?.let { "$it.xx" } ?: "192.168.1.25") },
                    leadingIcon = { Icon(Icons.Rounded.Lan, contentDescription = null) },
                    singleLine = true,
                    isError = ip.isNotEmpty() && !ipValid,
                    supportingText = {
                        if (ip.isNotEmpty() && !ipValid) Text("Enter 4 numbers separated by dots, e.g. 192.168.1.25")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (ipValid) check() }),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                TipCard(
                    icon = Icons.Rounded.Router,
                    tint = scheme.primary,
                    title = "Where to find it on your ${p.short}",
                    text = Guides.of(p).findIp,
                )
                AnimatedVisibility(visible = probeFailed, enter = fadeIn() + expandVertically()) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        TipCard(
                            icon = Icons.Rounded.WifiOff,
                            tint = scheme.error,
                            title = "Couldn't reach a ${p.short} TV at $ip",
                            text = "The TV didn't answer. Check these and try again:",
                        ) {
                            Spacer(Modifier.height(8.dp))
                            NumberedStep(1, "The TV is turned on and showing its home screen.")
                            NumberedStep(2, "The IP address matches the one in the TV's network settings.")
                            NumberedStep(3, "Phone and TV are on the same Wi-Fi network.")
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton("Try again", onClick = { check() }, icon = Icons.Rounded.Refresh, modifier = Modifier.weight(1f))
                                SecondaryButton("Continue anyway", onClick = { proceed() }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            text = if (checking) "Checking your TV…" else "Continue",
            enabled = platform != null && ipValid,
            loading = checking,
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            onClick = { check() },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = AppTheme.extra.panel,
    unfocusedContainerColor = AppTheme.extra.panel,
    unfocusedBorderColor = AppTheme.extra.panelBorder,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun StepHeader(number: Int, title: String, done: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (done) Brush.linearGradient(listOf(AppTheme.extra.success, AppTheme.extra.success)) else brandBrush()),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            else Text("$number", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onBackground)
    }
}

@Composable
private fun BrandGrid(brands: List<Guides.Brand>, selected: Guides.Brand?, onSelect: (Guides.Brand) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val extra = AppTheme.extra
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        brands.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b ->
                    val isSelected = selected == b
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(shape)
                            .background(if (isSelected) scheme.primary.copy(alpha = 0.12f) else extra.panel)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) scheme.primary else extra.panelBorder, shape)
                            .clickable { onSelect(b) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TvGlyph(b.platforms.first(), size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            b.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) scheme.primary else scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (brands.isEmpty()) {
            Text(
                "No brand matches. Most other brands run Android TV — pick \"Skyworth / Other Android TV\".",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}

