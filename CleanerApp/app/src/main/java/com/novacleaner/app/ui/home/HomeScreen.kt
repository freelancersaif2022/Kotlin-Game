package com.novacleaner.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novacleaner.app.ads.BannerAd
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.core.Format
import com.novacleaner.app.core.Settings
import com.novacleaner.app.data.StorageCategory
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.ProgressBar
import com.novacleaner.app.ui.components.RingGauge
import com.novacleaner.app.ui.components.SectionTitle
import com.novacleaner.app.ui.components.SparkleLogo
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.components.rememberPermissionRequester
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Tool
import com.novacleaner.app.ui.theme.Amber
import com.novacleaner.app.ui.theme.Emerald500
import com.novacleaner.app.ui.theme.Emerald700
import com.novacleaner.app.ui.theme.Gold
import com.novacleaner.app.ui.theme.GoldDeep
import com.novacleaner.app.ui.theme.Lime400
import com.novacleaner.app.ui.theme.Magenta
import com.novacleaner.app.ui.theme.Nova
import com.novacleaner.app.ui.theme.Rose
import com.novacleaner.app.ui.theme.Tangerine
import com.novacleaner.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private val categoryColors = mapOf(
    StorageCategory.IMAGES to Violet,
    StorageCategory.VIDEOS to Rose,
    StorageCategory.AUDIO to Magenta,
    StorageCategory.DOCUMENTS to Amber,
    StorageCategory.APKS to Lime400,
    StorageCategory.OTHER to Color(0xFF8FA8A0),
)

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val navigator = LocalNavigator.current
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by AppGraph.prefs.settings.collectAsStateWithLifecycle(initialValue = null)
    val premium by AppGraph.premium.collectAsStateWithLifecycle()
    val tour = remember { TourState() }
    var showTour by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    LaunchedEffect(settings?.tourDone) {
        val s = settings ?: return@LaunchedEffect
        if (!s.tourDone) {
            delay(750)
            showTour = true
        } else if (AppGraph.paywall.shouldShowOnLaunch()) {
            delay(900)
            AppGraph.paywall.markShown()
            navigator.openPremium()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState(), enabled = !showTour),
            ) {
                HomeHeader(premium = premium, tour = tour, onPremium = navigator::openPremium, onSettings = navigator::openSettings)
                HeroCard(state, tour) { navigator.openTool(Tool.JUNK) }
                Spacer(Modifier.height(18.dp))
                BreakdownCard(state)
                Spacer(Modifier.height(8.dp))
                SectionTitle("Tools")
                ToolGrid(tour) { navigator.openTool(it) }
                Spacer(Modifier.height(12.dp))
                InsightsRow(settings, state.batteryLevel, state.charging) { navigator.openTool(Tool.BATTERY) }
                if (!premium) {
                    Spacer(Modifier.height(14.dp))
                    PremiumPromo(onClick = navigator::openPremium)
                }
                Spacer(Modifier.height(24.dp))
            }
            BannerAd()
            Spacer(Modifier.navigationBarsPadding())
        }

        if (showTour) {
            val steps = buildList {
                add(TourStep("storage", "Your phone at a glance", "See used and free storage plus memory usage – updated every time you open the app."))
                add(TourStep("clean", "One-tap cleanup", "Scan for cache, temporary files, empty folders and leftover APKs, then clean them in seconds."))
                add(TourStep("tools", "All the tools you need", "Large files, duplicate photos, app manager, data usage, network speed and more."))
                if (!premium) add(TourStep("premium", "Go ad-free forever", "Remove every ad with a single one-time purchase. No subscription."))
                add(TourStep("settings", "Make it yours", "Change the theme, manage permissions, reminders and the speed meter."))
            }
            TourOverlay(tour, steps) {
                showTour = false
                scope.launch { AppGraph.prefs.setTourDone() }
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hello there"
}

@Composable
private fun HomeHeader(premium: Boolean, tour: TourState, onPremium: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Emerald500, Emerald700))),
            contentAlignment = Alignment.Center,
        ) { SparkleLogo(Modifier.size(34.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Nova Cleaner", style = MaterialTheme.typography.titleLarge)
        }
        if (!premium) {
            Box(
                Modifier
                    .tourTarget(tour, "premium")
                    .size(44.dp)
                    .shadow(8.dp, CircleShape, spotColor = GoldDeep, ambientColor = GoldDeep)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Gold, GoldDeep)))
                    .bounceClick(onClick = onPremium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, "Remove ads", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Box(
            Modifier
                .tourTarget(tour, "settings")
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Nova.colors.cardBorder, CircleShape)
                .bounceClick(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HeroCard(state: HomeState, tour: TourState, onClean: () -> Unit) {
    val storage = state.storage
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .tourTarget(tour, "storage")
            .shadow(18.dp, shape, spotColor = Emerald700, ambientColor = Emerald700)
            .clip(shape)
            .background(Nova.colors.heroBrush),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.45f, Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.05f), size.width * 0.3f, Offset(size.width * 0.05f, size.height * 1.0f))
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingGauge(
                    fraction = storage?.usedFraction ?: 0f,
                    modifier = Modifier.size(128.dp),
                    thickness = 13.dp,
                    colors = listOf(Color.White, Color(0xFFD7FBEF), Lime400),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            storage?.let { Format.percent(it.usedFraction) } ?: "--",
                            style = MaterialTheme.typography.headlineMedium, color = Color.White,
                        )
                        Text("Used", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("Internal storage", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        storage?.let { Format.bytes(it.used) } ?: "—",
                        style = MaterialTheme.typography.headlineSmall, color = Color.White,
                    )
                    Text(
                        storage?.let { "of ${Format.bytes(it.total)}" } ?: "",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(12.dp))
                    MiniStat(Icons.Rounded.PieChart, "Free", storage?.let { Format.bytes(it.free) } ?: "—")
                    Spacer(Modifier.height(6.dp))
                    val ram = state.ram
                    MiniStat(Icons.Rounded.Memory, "RAM", ram?.let { "${Format.percent(it.usedFraction)} used" } ?: "—")
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .tourTarget(tour, "clean")
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(6.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .bounceClick(onClick = onClean),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CleaningServices, null, tint = Emerald700, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Clean Now", style = MaterialTheme.typography.labelLarge, color = Emerald700)
            }
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label  ", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BreakdownCard(state: HomeState) {
    val request = rememberPermissionRequester(AppPermission.STORAGE)
    NovaCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Storage breakdown", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            state.storage?.let {
                Text("${Format.bytes(it.free)} free", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(14.dp))
        val breakdown = state.breakdown
        val total = state.storage?.total ?: 0L
        if (!state.hasStorageAccess) {
            Text(
                "Allow file access to see what's taking up your space.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .bounceClick(onClick = request).padding(horizontal = 16.dp, vertical = 10.dp),
            ) { Text("Allow access", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            return@NovaCard
        }
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (breakdown != null && total > 0) {
                StorageCategory.entries.forEach { cat ->
                    val fraction = (breakdown[cat] ?: 0L).toFloat() / total
                    if (fraction > 0.003f) {
                        Box(Modifier.weight(fraction).fillMaxSize().background(categoryColors.getValue(cat)))
                    }
                }
                val free = (state.storage?.free ?: 0L).toFloat() / total
                if (free > 0.003f) Spacer(Modifier.weight(free))
            }
        }
        Spacer(Modifier.height(14.dp))
        val rows = StorageCategory.entries.chunked(2)
        rows.forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                pair.forEach { cat ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(categoryColors.getValue(cat)))
                        Spacer(Modifier.width(8.dp))
                        Text(cat.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(
                            breakdown?.get(cat)?.let { Format.bytes(it) } ?: "…",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolGrid(tour: TourState, onOpen: (Tool) -> Unit) {
    val tools = Tool.entries
    Column(Modifier.padding(horizontal = 14.dp)) {
        tools.chunked(3).forEachIndexed { rowIndex, row ->
            Row(
                Modifier.fillMaxWidth().then(if (rowIndex == 0) Modifier.tourTarget(tour, "tools") else Modifier),
            ) {
                row.forEach { tool ->
                    ToolTile(tool, Modifier.weight(1f).padding(6.dp)) { onOpen(tool) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ToolTile(tool: Tool, modifier: Modifier, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60L * tool.ordinal); visible = true }
    AnimatedVisibility(
        visible, modifier = modifier,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 },
    ) {
        NovaCard(
            Modifier.fillMaxWidth(),
            onClick = onClick,
            padding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                GlossyIcon(tool.icon, tool.colors, size = 52.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    tool.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                    maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun InsightsRow(settings: Settings?, batteryLevel: Int, charging: Boolean, onBattery: () -> Unit) {
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NovaCard(Modifier.weight(1f), padding = PaddingValues(16.dp)) {
            Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text("Last cleanup", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(settings?.let { Format.relative(it.lastCleanAt) } ?: "—", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text("Total cleaned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(settings?.let { Format.bytes(it.totalCleanedBytes) } ?: "—", style = MaterialTheme.typography.titleSmall)
        }
        NovaCard(Modifier.weight(1f), onClick = onBattery, padding = PaddingValues(16.dp)) {
            Icon(if (charging) Icons.Rounded.Bolt else Icons.Rounded.BatteryChargingFull, null, tint = Tangerine)
            Spacer(Modifier.height(10.dp))
            Text("Battery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (batteryLevel >= 0) "$batteryLevel%" else "—", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ProgressBar(
                fraction = (batteryLevel.coerceAtLeast(0)) / 100f,
                modifier = Modifier.fillMaxWidth(),
                colors = Tool.BATTERY.colors,
                track = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(if (charging) "Charging" else "Tap for details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumPromo(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF2A1F00), Color(0xFF4A3300))))
            .border(1.dp, Brush.linearGradient(listOf(Gold, GoldDeep.copy(alpha = 0.3f))), shape)
            .bounceClick(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlossyIcon(Icons.Rounded.WorkspacePremium, listOf(Gold, GoldDeep), size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Go ad-free forever", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("One-time purchase • no subscription", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
        Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(18.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
    }
}
