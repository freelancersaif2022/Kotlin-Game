package com.voxcast.mic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voxcast.mic.net.LinkKind
import com.voxcast.mic.net.NetworkStatus
import com.voxcast.mic.net.NetworkUtils
import com.voxcast.mic.ui.components.GhostButton
import com.voxcast.mic.ui.components.IconBadge
import com.voxcast.mic.ui.components.StatusPill
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Polls local interfaces every 2 s (cheap) so the UI reflects Wi-Fi / hotspot changes live. */
@Composable
fun rememberNetworkStatus(): NetworkStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(NetworkStatus(false, false, false, emptyList())) }
    LaunchedEffect(Unit) {
        while (true) {
            status = withContext(Dispatchers.IO) { NetworkUtils.status(context) }
            delay(2_000)
        }
    }
    return status
}

fun NetworkStatus.icon(): ImageVector = when {
    wifiDirectOn -> VoxIcons.WifiDirect
    hotspotOn -> VoxIcons.Hotspot
    else -> VoxIcons.Wifi
}

fun linkLabel(kind: LinkKind) = when (kind) {
    LinkKind.WIFI -> "Wi-Fi"
    LinkKind.HOTSPOT -> "Hotspot"
    LinkKind.WIFI_DIRECT -> "Wi-Fi Direct"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkGuideSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val status = rememberNetworkStatus()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VoxColors.Surface,
        dragHandle = {
            Spacer(
                Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VoxColors.Outline),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Connect the two phones", style = MaterialTheme.typography.headlineSmall, color = VoxColors.Text)
            Text(
                "VoxCast works without internet or any server. Both phones just need to share one local network. Pick any of these:",
                style = MaterialTheme.typography.bodyMedium, color = VoxColors.TextDim,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (status.ready) "This phone: ${status.headline}" else "This phone: not on a network",
                    color = if (status.ready) VoxColors.Success else VoxColors.Warning,
                    pulsing = status.ready,
                )
            }
            status.links.forEach {
                Text("${linkLabel(it.kind)} · ${it.ip}", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint)
            }

            GuideOption(
                icon = VoxIcons.Wifi,
                brush = Brush.linearGradient(listOf(VoxColors.Cyan, VoxColors.CyanDeep)),
                title = "1 · Same Wi-Fi",
                body = "Connect both phones to the same Wi-Fi router. Best range and quality.",
                tag = "Recommended",
            )
            GuideOption(
                icon = VoxIcons.Hotspot,
                brush = Brush.linearGradient(listOf(VoxColors.Warning, Color(0xFFE07A1F))),
                title = "2 · Phone hotspot",
                body = "Turn on the hotspot on one phone and join it from the other phone. Either phone can be the hotspot. Mobile data is not used for audio.",
            )
            GuideOption(
                icon = VoxIcons.WifiDirect,
                brush = Brush.linearGradient(listOf(VoxColors.Violet, Color(0xFF4B3FD1))),
                title = "3 · Wi-Fi Direct",
                body = "No router needed. On the speaker phone choose Wi-Fi Direct. The microphone phone joins it automatically when you scan (Android 10+), just tap Connect when asked.",
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Wi-Fi settings", onClick = { context.openWifiSettings() }, icon = Icons.Rounded.Router, modifier = Modifier.weight(1f))
                GhostButton("Hotspot", onClick = { context.openHotspotSettings() }, icon = VoxIcons.Hotspot, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GuideOption(icon: ImageVector, brush: Brush, title: String, body: String, tag: String? = null) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(VoxColors.SurfaceHigh)
            .border(1.dp, VoxColors.OutlineSoft, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconBadge(icon, brush, size = 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = VoxColors.Text, fontWeight = FontWeight.Bold)
                if (tag != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = VoxColors.Background,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(VoxColors.Cyan)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
        }
    }
}
