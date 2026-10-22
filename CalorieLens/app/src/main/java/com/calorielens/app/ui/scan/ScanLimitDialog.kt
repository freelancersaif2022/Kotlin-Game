package com.calorielens.app.ui.scan

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.calorielens.app.data.prefs.ScanQuota
import com.calorielens.app.ui.components.AppDialog
import com.calorielens.app.ui.components.GradientButton
import com.calorielens.app.ui.components.OutlineButton
import com.calorielens.app.ui.components.PremiumEmblem
import com.calorielens.app.ui.theme.AppTheme
import com.calorielens.app.ui.theme.Brand

@Composable
fun ScanLimitDialog(
    quota: ScanQuota,
    onWatchAd: () -> Unit,
    onPremium: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(onDismiss = onDismiss) {
        PremiumEmblem(size = 96.dp)
        Spacer(Modifier.height(12.dp))
        Text("Daily scans used up", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "You've used all ${quota.limit} free scans for today. Watch a short ad for ${ScanQuota.REWARD_BONUS_SCANS} more, or go Premium for unlimited scans.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        GradientButton(
            "Unlock unlimited scans",
            onPremium,
            brush = Brand.goldGradient,
            contentColor = Color(0xFF2A1A00),
            icon = Icons.Rounded.WorkspacePremium,
            shimmer = true,
            modifier = Modifier.fillMaxWidth(),
            height = 54.dp,
        )
        Spacer(Modifier.height(10.dp))
        OutlineButton(
            "Watch ad · +${ScanQuota.REWARD_BONUS_SCANS} scans",
            onWatchAd,
            Modifier.fillMaxWidth(),
            icon = Icons.Rounded.OndemandVideo,
        )
    }
}
