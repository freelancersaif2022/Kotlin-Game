package com.voxcast.mic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxcast.mic.BuildConfig
import com.voxcast.mic.data.AudioQuality
import com.voxcast.mic.ui.components.IconBadge
import com.voxcast.mic.ui.components.Messenger
import com.voxcast.mic.ui.components.SectionLabel
import com.voxcast.mic.ui.components.VoxBackground
import com.voxcast.mic.ui.components.VoxDialog
import com.voxcast.mic.ui.components.bounceClick
import com.voxcast.mic.ui.components.glass
import com.voxcast.mic.ui.icons.VoxIcons
import com.voxcast.mic.ui.theme.VoxColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPremium: () -> Unit) {
    val context = LocalContext.current
    val app = context.voxApp
    val s by app.settings.settings.collectAsStateWithLifecycle()
    val premium = s.premium.isEntitled
    var editName by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    fun save(block: suspend () -> Unit) { app.appScope.launch { block() } }

    VoxBackground {
        Column(Modifier.fillMaxSize()) {
            VoxTopBar("Settings", onBack)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
            ) {
                // Premium banner.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bounceClick(onClick = onOpenPremium)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF2B2210), Color(0xFF16130C))))
                        .border(1.dp, VoxColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(VoxIcons.Crown, VoxColors.GoldGradient, size = 46.dp, iconTint = Color(0xFF2A1E05))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (premium) "Premium active" else "Upgrade to Premium", style = MaterialTheme.typography.titleMedium, color = VoxColors.Gold)
                        Text(if (premium) "Ad-free · Studio quality unlocked" else "Remove ads for \$1.99 / month", style = MaterialTheme.typography.bodySmall, color = VoxColors.TextDim)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = VoxColors.Gold)
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Device")
                Group {
                    NavRow(Icons.Rounded.Badge, VoxColors.Cyan, "Device name", s.deviceName) { editName = true }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Noise & voice")
                Group {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(VoxIcons.NoiseShield, null, tint = VoxColors.Success, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Noise reduction", style = MaterialTheme.typography.titleSmall, color = VoxColors.Text)
                        }
                        Spacer(Modifier.height(10.dp))
                        SegmentedNoise(s.noiseMode) { m -> save { app.settings.setNoiseMode(m) } }
                        Spacer(Modifier.height(8.dp))
                        Text(s.noiseMode.description, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint)
                    }
                    Divider()
                    ToggleRow(VoxIcons.Equalizer, VoxColors.Violet, "Echo cancellation", "Stops feedback when phones are close", s.echoCancel) { v -> save { app.settings.setEchoCancel(v) } }
                    Divider()
                    ToggleRow(VoxIcons.Bolt, VoxColors.Warning, "Auto gain", "Evens out loud and quiet voices", s.autoGain) { v -> save { app.settings.setAutoGain(v) } }
                    Divider()
                    ToggleRow(VoxIcons.Microphone, VoxColors.Cyan, "Voice clarity", "Presence boost for crisp speech", s.voiceEnhance) { v -> save { app.settings.setVoiceEnhance(v) } }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Audio quality")
                Group {
                    AudioQuality.entries.forEachIndexed { i, q ->
                        val locked = q.premium && !premium
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .bounceClick(scaleDown = 0.98f) {
                                    if (locked) {
                                        Messenger.info("Studio quality is Premium", "Upgrade to unlock 48 kHz")
                                        onOpenPremium()
                                    } else save { app.settings.setQuality(q) }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioDot(selected = s.quality == q)
                            Spacer(Modifier.width(14.dp))
                            Text(q.label, style = MaterialTheme.typography.bodyLarge, color = VoxColors.Text, modifier = Modifier.weight(1f))
                            if (locked) Icon(Icons.Rounded.Lock, null, tint = VoxColors.Gold, modifier = Modifier.size(18.dp))
                            else if (q.premium) Icon(VoxIcons.Crown, null, tint = VoxColors.Gold, modifier = Modifier.size(18.dp))
                        }
                        if (i < AudioQuality.entries.lastIndex) Divider()
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Controls")
                Group {
                    ToggleRow(Icons.Rounded.PanTool, VoxColors.Violet, "Push to talk", "Hold the mic to talk, release to mute", s.pushToTalk) { v -> save { app.settings.setPushToTalk(v) } }
                    Divider()
                    ToggleRow(Icons.Rounded.LightMode, VoxColors.Warning, "Keep screen on", "During live sessions", s.keepScreenOn) { v -> save { app.settings.setKeepScreenOn(v) } }
                    Divider()
                    ToggleRow(Icons.Rounded.Vibration, VoxColors.Cyan, "Haptic feedback", "Vibrate on mic actions", s.haptics) { v -> save { app.settings.setHaptics(v) } }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel("Help")
                Group {
                    NavRow(VoxIcons.WifiDirect, VoxColors.Violet, "Connection guide", "Wi-Fi, hotspot or Wi-Fi Direct") { showGuide = true }
                    Divider()
                    NavRow(Icons.Rounded.Info, VoxColors.TextDim, "About VoxCast", "Version ${BuildConfig.VERSION_NAME} · works fully offline") {
                        Messenger.info("VoxCast ${BuildConfig.VERSION_NAME}", "No servers, no accounts, audio stays on your network")
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    var nameDraft by remember(editName) { mutableStateOf(s.deviceName) }
    VoxDialog(
        visible = editName,
        onDismiss = { editName = false },
        icon = Icons.Rounded.Edit,
        title = "Device name",
        message = "Shown on the other phone when you connect.",
        confirmText = "Save",
        onConfirm = {
            editName = false
            save { app.settings.setDeviceName(nameDraft) }
            Messenger.success("Name updated")
        },
        dismissText = "Cancel",
    ) {
        OutlinedTextField(
            value = nameDraft,
            onValueChange = { nameDraft = it.take(32) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoxColors.Cyan,
                unfocusedBorderColor = VoxColors.Outline,
                cursorColor = VoxColors.Cyan,
                focusedTextColor = VoxColors.Text,
                unfocusedTextColor = VoxColors.Text,
            ),
        )
    }

    if (showGuide) NetworkGuideSheet(onDismiss = { showGuide = false })
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp)), verticalArrangement = Arrangement.Top) { content() }
}

@Composable
private fun Divider() {
    Spacer(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(VoxColors.OutlineSoft))
}

@Composable
private fun SmallBadge(icon: ImageVector, color: Color) {
    IconBadge(icon, Brush.linearGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.55f))), size = 36.dp)
}

@Composable
private fun ToggleRow(icon: ImageVector, color: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().bounceClick(scaleDown = 0.98f) { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallBadge(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = VoxColors.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VoxColors.Cyan, checkedThumbColor = Color.White, uncheckedTrackColor = VoxColors.SurfaceHighest),
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().bounceClick(scaleDown = 0.98f, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallBadge(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = VoxColors.Text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VoxColors.TextFaint, maxLines = 1)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = VoxColors.TextDim)
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(50))
            .border(2.dp, if (selected) VoxColors.Cyan else VoxColors.Outline, RoundedCornerShape(50))
            .padding(5.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) VoxColors.Cyan else Color.Transparent),
    )
}
