package com.spectral.ghostdetector.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spectral.ghostdetector.BuildConfig
import com.spectral.ghostdetector.ui.ScreenScaffold
import com.spectral.ghostdetector.ui.components.DisclaimerText
import com.spectral.ghostdetector.ui.components.GlassPanel
import com.spectral.ghostdetector.ui.components.SectionLabel
import com.spectral.ghostdetector.ui.components.soundClick
import com.spectral.ghostdetector.ui.rememberApp
import com.spectral.ghostdetector.ui.theme.GColors
import kotlinx.coroutines.launch

/** Replace with your hosted privacy policy before publishing. */
private const val PRIVACY_URL = "https://example.com/ghost-detector/privacy"

@Composable
fun SettingsScreen(onBack: () -> Unit, onPremium: () -> Unit) {
    val app = rememberApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound by app.prefs.sound.collectAsStateWithLifecycle(true)
    val vibration by app.prefs.vibration.collectAsStateWithLifecycle(true)
    val nightVision by app.prefs.nightVision.collectAsStateWithLifecycle(false)
    val premium by app.premium.collectAsStateWithLifecycle()

    fun open(uri: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
        }
    }

    ScreenScaffold(title = "Settings", onBack = onBack) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            SectionLabel("Scanner", Modifier.padding(start = 6.dp, top = 6.dp, bottom = 6.dp))
            GlassPanel(Modifier.fillMaxWidth(), padding = 6.dp) {
                ToggleRow(Icons.Rounded.VolumeUp, "Sound effects", "Radar pings, EVP, alarms", sound) {
                    scope.launch { app.prefs.setSound(it) }
                }
                ToggleRow(Icons.Rounded.Vibration, "Vibration", "Haptic feedback on detections", vibration) {
                    scope.launch { app.prefs.setVibration(it) }
                }
                ToggleRow(Icons.Rounded.NightsStay, "Night vision by default", "Green NV filter on the camera", nightVision) {
                    scope.launch { app.prefs.setNightVision(it) }
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("App", Modifier.padding(start = 6.dp, bottom = 6.dp))
            GlassPanel(Modifier.fillMaxWidth(), padding = 6.dp) {
                LinkRow(Icons.Rounded.WorkspacePremium, if (premium) "Premium active" else "Remove ads", "Ghost Detector PRO", onPremium)
                LinkRow(Icons.Rounded.Star, "Rate us", "Enjoying the app? Leave a review") {
                    open("market://details?id=${context.packageName}")
                }
                LinkRow(Icons.Rounded.Share, "Share with friends", "Prank them with a ghost hunt") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "I just detected a ghost 👻 Try Ghost Detector: https://play.google.com/store/apps/details?id=${context.packageName}",
                        )
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                }
                LinkRow(Icons.Rounded.Policy, "Privacy policy", "How we handle data") { open(PRIVACY_URL) }
                LinkRow(Icons.Rounded.Info, "Version", BuildConfig.VERSION_NAME) {}
            }
            Spacer(Modifier.height(16.dp))
            GlassPanel(Modifier.fillMaxWidth(), borderColor = GColors.Amber.copy(alpha = 0.5f)) {
                Text("DISCLAIMER", style = MaterialTheme.typography.labelLarge, color = GColors.Amber)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ghost Detector is a prank / entertainment app. It does not and cannot detect real ghosts or spirits. " +
                        "All entities, names, ages, threat levels and sensor readings are randomly generated for fun.",
                    style = MaterialTheme.typography.bodyMedium, color = GColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(16.dp))
            DisclaimerText(Modifier.fillMaxWidth().padding(bottom = 20.dp))
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().soundClick { onChange(!checked) }.padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = GColors.Neon, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GColors.Void,
                checkedTrackColor = GColors.Neon,
                uncheckedThumbColor = GColors.TextSecondary,
                uncheckedTrackColor = GColors.Panel,
                uncheckedBorderColor = GColors.Stroke,
            ),
        )
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().soundClick(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = GColors.Neon, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = GColors.TextSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = GColors.TextSecondary)
    }
}
