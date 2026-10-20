package com.tvremote.universal.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvremote.universal.App
import com.tvremote.universal.data.TvDevice
import com.tvremote.universal.data.TvPlatform
import com.tvremote.universal.ui.components.AppBar
import com.tvremote.universal.ui.components.AppCard
import com.tvremote.universal.ui.components.HeroIcon
import com.tvremote.universal.ui.components.PlatformBadge
import com.tvremote.universal.ui.components.PrimaryButton
import com.tvremote.universal.ui.components.SectionTitle
import com.tvremote.universal.ui.components.TvGlyph
import com.tvremote.universal.ui.theme.AppTheme

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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppBar(
            title = "My TVs",
            subtitle = if (devices.isEmpty()) "Add a TV to get started" else "${devices.size} TV${if (devices.size == 1) "" else "s"} paired",
            actions = {
                if (!premium) {
                    IconButton(onClick = onPremium) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = "Premium", tint = AppTheme.extra.gold)
                    }
                }
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
            },
        )

        if (devices.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HeroIcon(Icons.Rounded.SettingsRemote, size = 200.dp)
                Spacer(Modifier.height(28.dp))
                Text("No TVs yet", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Connect your phone to the same Wi-Fi as your TV and we'll find it for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
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
                item { SectionTitle("Paired TVs") }
                items(sorted, key = { it.id }) { tv ->
                    TvRow(tv, onClick = { onOpen(tv.id) }, onMore = { onEdit(tv.id) })
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
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun TvRow(tv: TvDevice, onClick: () -> Unit, onMore: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TvGlyph(tv.platform)
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
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "TV settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumTeaser(onClick: () -> Unit) {
    val extra = AppTheme.extra
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(extra.heroStart, extra.heroEnd)))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = extra.gold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Unlock Premium", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    "Keyboard, touchpad, apps, power on & unlimited TVs. No ads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    "Try",
                    style = MaterialTheme.typography.labelLarge,
                    color = extra.heroStart,
                )
            }
        }
    }
}
