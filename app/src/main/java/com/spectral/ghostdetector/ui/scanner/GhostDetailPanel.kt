package com.spectral.ghostdetector.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import com.spectral.ghostdetector.data.GhostFactory
import com.spectral.ghostdetector.ui.components.Badge
import com.spectral.ghostdetector.ui.components.GlassPanel
import com.spectral.ghostdetector.ui.components.NeonButton
import com.spectral.ghostdetector.ui.components.SectionLabel
import com.spectral.ghostdetector.ui.components.ThreatBar
import com.spectral.ghostdetector.ui.components.soundClick
import com.spectral.ghostdetector.ui.components.threatColor
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts

@Composable
fun GhostDetailPanel(ghost: TrackedGhost, onListen: () -> Unit, onClose: () -> Unit) {
    val p = ghost.profile
    GlassPanel(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(10.dp),
        corner = 22.dp,
        borderColor = GColors.Neon.copy(alpha = 0.7f),
        padding = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("ENTITY #%03d · IDENTIFIED".format(ghost.id), color = GColors.Neon)
                Text(p.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = GColors.TextPrimary)
            }
            Icon(
                Icons.Rounded.Close, "Close", tint = GColors.TextSecondary,
                modifier = Modifier.size(26.dp).soundClick(onClick = onClose),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(p.type.label)
            Badge(GhostFactory.threatLabel(p.threat), threatColor(p.threat))
            Badge(p.mood, GColors.Amber)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Fact("APPROX. AGE", "${p.age} yrs")
            Fact("DIED", "c. ${p.diedYear}")
            Fact("ENERGY", "${p.energy}%")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Fact("EMF SIG.", "%.1f mG".format(p.emfSignature))
            Fact("DISTANCE", "%.1f m".format(p.distance))
            Fact("MATCH", "${p.match}%")
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel("LIKELY ORIGIN")
        Text(p.origin, color = GColors.TextPrimary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SectionLabel("THREAT LEVEL")
        Spacer(Modifier.height(4.dp))
        ThreatBar(p.threat, segmentWidth = 28.dp)
        Spacer(Modifier.height(10.dp))
        Text(p.type.description, color = GColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        NeonButton("Listen EVP", Modifier.fillMaxWidth(), icon = Icons.Rounded.GraphicEq, onClick = onListen)
        Spacer(Modifier.height(6.dp))
        Text(
            "Simulated profile · for entertainment only",
            color = GColors.TextMuted, fontFamily = GFonts.Mono, fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun RowScope.Fact(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        SectionLabel(label)
        Text(value, color = GColors.Neon, fontFamily = GFonts.Mono, fontSize = 16.sp)
    }
}
