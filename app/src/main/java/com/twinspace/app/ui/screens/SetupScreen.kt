package com.twinspace.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinspace.app.core.SpaceStatus
import com.twinspace.app.ui.components.GradientButton
import com.twinspace.app.ui.components.IconTile
import com.twinspace.app.ui.components.TwinIllustration
import com.twinspace.app.ui.theme.Brand

@Composable
fun SetupScreen(
    status: SpaceStatus,
    canCreate: Boolean,
    settingUp: Boolean,
    onCreate: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Brand.Indigo.copy(alpha = 0.16f),
                    0.45f to colors.background,
                    1f to colors.background,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            TwinIllustration(size = 220.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Two of every app.\nOne phone.",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Twin Space creates a private, separate space on your phone. Apps you clone into it run with their own accounts, data and notifications.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Feature(Icons.Rounded.Shield, colors.primary, "Fully isolated", "Each clone has its own login, storage and settings.")
                Feature(Icons.Rounded.AddHome, colors.secondary, "Rename & shortcuts", "Give clones custom names and pin them to your home screen.")
                Feature(Icons.Rounded.PushPin, colors.tertiary, "Stays until you remove it", "Nothing is ever deleted automatically.")
                Feature(Icons.Rounded.Lock, Brand.Amber, "Private by design", "Optional app lock. No ads, no in-app purchases, no tracking.")
            }

            Spacer(Modifier.height(36.dp))

            when (status) {
                SpaceStatus.UNSUPPORTED -> Notice("This device doesn't support separate spaces (Android work profiles), so Twin Space can't run here.")
                SpaceStatus.FOREIGN -> Notice("Another app already manages this phone's work profile. Android allows only one, so remove it in Settings › Accounts first.")
                else -> {
                    GradientButton(
                        text = if (settingUp) "Finishing setup…" else "Create my space",
                        onClick = onCreate,
                        enabled = canCreate || settingUp,
                        loading = settingUp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (canCreate || settingUp) "Android will show a short setup screen. It takes about 30 seconds."
                        else "Space creation is blocked on this device (for example by a device policy).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, tint, size = 44.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Notice(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(Icons.Rounded.Block, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
