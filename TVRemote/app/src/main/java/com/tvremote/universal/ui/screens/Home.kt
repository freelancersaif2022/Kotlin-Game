package com.tvremote.universal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.R
import com.tvremote.universal.data.TvDevice
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.tv.TvDiscovery
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.SignalIllustration
import com.tvremote.universal.ui.components.StatusPill
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.components.goldBrush
import com.tvremote.universal.ui.components.heroBrush
import com.tvremote.universal.ui.theme.AppTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

enum class Reachability { CHECKING, ONLINE, OFFLINE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onPremium: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val container = App.container
    val devices by container.tvStore.devices.collectAsStateWithLifecycle()
    val premium by container.premium.isPremium.collectAsStateWithLifecycle()
    val sorted = devices.sortedByDescending { it.lastUsed }
    val status = remember { mutableStateMapOf<String, Reachability>() }
    val scheme = MaterialTheme.colorScheme

    // Check which TVs are reachable right now (fast parallel port probes).
    LaunchedEffect(devices.map { it.id to it.host }) {
        devices.forEach { if (status[it.id] == null) status[it.id] = Reachability.CHECKING }
        coroutineScope {
            devices.map { d ->
                async {
                    val ok = TvDiscovery.probe(d.platform, d.host)
                    status[d.id] = if (ok) Reachability.ONLINE else Reachability.OFFLINE
                }
            }.awaitAll()
        }
    }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        // ------------------------------------------------------------ header
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(R.drawable.app_art),
                contentDescription = null,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("My TVs", style = MaterialTheme.typography.titleLarge, color = scheme.onBackground)
                Text(
                    if (devices.isEmpty()) "Universal Remote" else "${devices.size} TV${if (devices.size == 1) "" else "s"} paired",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            if (!premium) {
                Box(
                    Modifier
                        .size(38.dp)
                        .shadow(8.dp, CircleShape, ambientColor = AppTheme.extra.gold, spotColor = AppTheme.extra.gold)
                        .clip(CircleShape)
                        .background(goldBrush)
                        .clickable(onClick = onPremium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Diamond, contentDescription = "Go Premium", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = scheme.onBackground) }
        }

        if (devices.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SignalIllustration()
                Spacer(Modifier.height(20.dp))
                Text("Let's connect your TV", style = MaterialTheme.typography.headlineSmall, color = scheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Make sure your phone and TV are on the same Wi-Fi. We'll find your TV automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { TvPlatform.entries.forEach { PlatformBadge(it) } }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SectionTitle("Your TVs · tap to open the remote") }
                items(sorted, key = { it.id }) { tv ->
                    TvRow(tv, status[tv.id] ?: Reachability.CHECKING, onClick = { onOpen(tv.id) }, onMore = { onEdit(tv.id) })
                }
                if (!premium) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        PremiumTeaser(onPremium)
                    }
                }
            }
        }

        PrimaryButton(
            text = "Add a TV",
            icon = Icons.Rounded.Add,
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun TvRow(tv: TvDevice, reach: Reachability, onClick: () -> Unit, onMore: () -> Unit) {
    val extra = AppTheme.extra
    val scheme = MaterialTheme.colorScheme
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TvGlyph(tv.platform, size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tv.name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(tv.platform)
                    Spacer(Modifier.width(6.dp))
                    when (reach) {
                        Reachability.ONLINE -> StatusPill("Online", extra.success)
                        Reachability.OFFLINE -> StatusPill("Offline", scheme.onSurfaceVariant)
                        Reachability.CHECKING -> StatusPill("Checking", extra.warning)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    tv.model.ifBlank { tv.host },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "TV settings", tint = scheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .size(36.dp)
                    .shadow(4.dp, CircleShape, ambientColor = extra.glow, spotColor = extra.glow)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Open remote", tint = Color.White)
            }
        }
    }
}

@Composable
fun PremiumTeaser(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = AppTheme.extra.glow, spotColor = AppTheme.extra.glow)
            .clip(RoundedCornerShape(24.dp))
            .background(heroBrush())
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(goldBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Upgrade to Premium", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    "No ads · keyboard · touchpad · apps · power on · unlimited TVs",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("View", style = MaterialTheme.typography.labelLarge, color = Color(0xFF0A1A5C))
            }
        }
    }
}
