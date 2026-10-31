package com.spectral.ghostdetector.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectral.ghostdetector.ads.BannerAd
import com.spectral.ghostdetector.data.GhostFactory
import com.spectral.ghostdetector.ui.ScreenScaffold
import com.spectral.ghostdetector.ui.components.Badge
import com.spectral.ghostdetector.ui.components.GlassIconButton
import com.spectral.ghostdetector.ui.components.GlassPanel
import com.spectral.ghostdetector.ui.components.NeonButton
import com.spectral.ghostdetector.ui.components.SectionLabel
import com.spectral.ghostdetector.ui.components.ThreatBar
import com.spectral.ghostdetector.ui.components.threatColor
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit, onScan: () -> Unit) {
    val app = rememberApp()
    val scope = rememberCoroutineScope()
    val log by app.prefs.history.collectAsStateWithLifecycle(emptyList())
    val fmt = remember { SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()) }

    ScreenScaffold(
        title = "Ghost Log",
        onBack = onBack,
        actions = {
            if (log.isNotEmpty()) {
                GlassIconButton(Icons.Rounded.DeleteSweep, "Clear log", tint = GColors.Danger) {
                    scope.launch { app.prefs.clearHistory() }
                }
            }
        },
    ) {
        Box(Modifier.weight(1f)) {
            if (log.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp).align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.Radar, null, tint = GColors.NeonDim, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("NO ENTITIES LOGGED YET", style = MaterialTheme.typography.labelLarge, color = GColors.TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Start a scan — every entity you detect is recorded here.",
                        color = GColors.TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    NeonButton("Start scan", icon = Icons.Rounded.Radar, onClick = onScan)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        SectionLabel("${log.size} RECORDED ENCOUNTERS", Modifier.padding(start = 4.dp, bottom = 2.dp))
                    }
                    items(log) { d ->
                        GlassPanel(Modifier.fillMaxWidth(), corner = 14.dp, padding = 12.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(d.name, style = MaterialTheme.typography.titleMedium, color = GColors.TextPrimary)
                                    Text(
                                        "AGE ≈ ${d.age} YRS · ${d.type.uppercase()} · ${d.mood.uppercase()}",
                                        color = GColors.NeonSoft, fontFamily = GFonts.Mono, fontSize = 10.sp,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Badge(GhostFactory.threatLabel(d.threat), threatColor(d.threat))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ThreatBar(d.threat, segmentWidth = 14.dp)
                                Spacer(Modifier.weight(1f))
                                Text(fmt.format(Date(d.timestamp)), color = GColors.TextMuted, fontFamily = GFonts.Mono, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        BannerAd()
    }
}
