package com.spectral.ghostdetector.ui.premium

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectral.ghostdetector.ui.ScreenScaffold
import com.spectral.ghostdetector.ui.components.GlassPanel
import com.spectral.ghostdetector.ui.components.NeonButton
import com.spectral.ghostdetector.ui.components.SectionLabel
import com.spectral.ghostdetector.ui.findActivity
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.theme.GColors

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val app = rememberApp()
    val context = LocalContext.current
    val premium by app.premium.collectAsStateWithLifecycle()
    val price by app.billing.price.collectAsStateWithLifecycle()
    val message by app.billing.message.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        app.billing.clearMessage()
        app.billing.connect()
    }

    val t = rememberInfiniteTransition(label = "crown")
    val glow by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g")

    ScreenScaffold(title = "Premium", onBack = onBack) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .size(130.dp)
                    .drawBehind {
                        drawCircle(GColors.Amber.copy(alpha = 0.18f * glow), radius = size.minDimension * 0.75f)
                        drawCircle(GColors.Amber.copy(alpha = 0.10f * glow), radius = size.minDimension * 0.95f)
                    }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(GColors.Amber.copy(alpha = 0.35f), GColors.Panel)))
                    .border(2.dp, GColors.Amber, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = GColors.Amber, modifier = Modifier.size(68.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("GHOST DETECTOR PRO", style = MaterialTheme.typography.headlineSmall, color = GColors.Amber)
            Spacer(Modifier.height(6.dp))
            Text(
                if (premium) "Thank you! Premium is active on this device." else "One-time purchase. Yours forever.",
                color = GColors.TextSecondary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            GlassPanel(Modifier.fillMaxWidth(), borderColor = GColors.Amber.copy(alpha = 0.5f)) {
                Perk(Icons.Rounded.Block, "Remove all ads", "No banner, native or full-screen ads anywhere.")
                Perk(Icons.Rounded.Radar, "Uninterrupted scanning", "Leave and re-enter the scanner without breaks.")
                Perk(Icons.Rounded.Speed, "Faster & lighter", "Less data and battery used by ad loading.")
                Perk(Icons.Rounded.Favorite, "Support the developer", "Help us build new ghost types and features.")
            }
            Spacer(Modifier.height(22.dp))
            if (premium) {
                NeonButton("Premium active", Modifier.fillMaxWidth(), icon = Icons.Rounded.CheckCircle, color = GColors.Amber) { onBack() }
            } else {
                NeonButton(
                    text = price?.let { "Unlock for $it" } ?: "Unlock premium",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.WorkspacePremium,
                    color = GColors.Amber,
                ) { context.findActivity()?.let { app.billing.purchase(it) } }
                Spacer(Modifier.height(12.dp))
                NeonButton("Restore purchase", Modifier.fillMaxWidth(), icon = Icons.Rounded.Restore, filled = false) {
                    app.billing.restore()
                }
            }
            message?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = GColors.NeonSoft, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("Payment is handled securely by Google Play", Modifier.padding(bottom = 20.dp))
        }
    }
}

@Composable
private fun Perk(icon: ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GColors.Amber, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GColors.TextSecondary)
        }
    }
}
