package com.novacleaner.app.ui.permissions

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novacleaner.app.core.AppPermission
import com.novacleaner.app.ui.components.GlossyIcon
import com.novacleaner.app.ui.components.GradientButton
import com.novacleaner.app.ui.components.NovaCard
import com.novacleaner.app.ui.components.bounceClick
import com.novacleaner.app.ui.components.rememberPermissionRequester
import com.novacleaner.app.ui.components.rememberPermissionState
import com.novacleaner.app.ui.navigation.LocalNavigator
import com.novacleaner.app.ui.navigation.Routes
import com.novacleaner.app.ui.navigation.Tool

@Composable
fun PermissionsScreen() {
    val navigator = LocalNavigator.current
    val storage by rememberPermissionState(AppPermission.STORAGE)
    val usage by rememberPermissionState(AppPermission.USAGE)
    val notifications by rememberPermissionState(AppPermission.NOTIFICATIONS)
    val showNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val allGranted = storage && usage && (!showNotifications || notifications)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(28.dp))
            GlossyIcon(Icons.Rounded.Lock, Tool.JUNK.colors, size = 64.dp)
            Spacer(Modifier.height(20.dp))
            Text("Let's get you set up", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Nova Cleaner needs a few permissions to analyse your storage and apps. Everything stays on your device.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PermissionCard(
                AppPermission.STORAGE, storage, Icons.Rounded.FolderOpen, Tool.LARGE_FILES.colors,
                "All files access", "Required to find junk, large files, duplicates and screenshots, and to delete what you choose.",
                required = true,
            )
            Spacer(Modifier.height(12.dp))
            PermissionCard(
                AppPermission.USAGE, usage, Icons.Rounded.BarChart, Tool.APPS.colors,
                "Usage access", "Shows app sizes, last-used dates and mobile / Wi-Fi data usage per app.",
                required = false,
            )
            if (showNotifications) {
                Spacer(Modifier.height(12.dp))
                PermissionCard(
                    AppPermission.NOTIFICATIONS, notifications, Icons.Rounded.Notifications, Tool.SPEED.colors,
                    "Notifications", "For the live network speed meter and occasional cleanup reminders.",
                    required = false,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        GradientButton(
            text = if (allGranted) "Continue" else if (storage) "Continue" else "Skip for now",
            onClick = { navigator.nav.navigate(Routes.HOME) { popUpTo(Routes.PERMISSIONS) { inclusive = true } } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun PermissionCard(
    permission: AppPermission,
    granted: Boolean,
    icon: ImageVector,
    colors: List<Color>,
    title: String,
    description: String,
    required: Boolean,
) {
    val request = rememberPermissionRequester(permission)
    NovaCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlossyIcon(icon, colors, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (required) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        AnimatedContent(granted, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() }, label = "perm") { ok ->
            if (ok) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Granted", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.last())
                        .bounceClick(onClick = request),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Allow", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}
