package com.lexora.english.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexora.english.BuildConfig
import com.lexora.english.R
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.AppImage
import com.lexora.english.ui.components.IconTile
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.PillSelector
import com.lexora.english.ui.components.ProfileAvatar
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import com.lexora.english.util.Speaker
import com.lexora.english.util.rememberHaptics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val fontSizes = listOf(0.9f to "Small", 1f to "Default", 1.12f to "Large", 1.25f to "Extra large")

@Composable
fun SettingsScreen(state: AppState, nav: Nav, listState: LazyListState) {
    val c = Lex.colors
    val context = LocalContext.current
    val repo = AppRepository.get(context)
    val haptics = rememberHaptics()
    val s = state.settings
    var dialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "title") {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge, color = c.text)
                Text("Personalise your Lexora experience", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
        }

        item(key = "profile") {
            val profile = state.profile
            Box(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
            ) {
                AppImage(R.drawable.profile_header_bg, Modifier.matchParentSize(), contentScale = ContentScale.Crop, crossfade = false)
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                        ) {
                            ProfileAvatar(profile, 72.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profile?.name ?: "Learner", style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${profile?.level ?: "Beginner"} · Goal: ${profile?.goal ?: "Career"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                            profile?.let {
                                Text(
                                    "Member since " + DateTimeFormatter.ofPattern("MMM yyyy", Locale.US).format(Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault())),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable { nav.editProfile() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Edit, null, tint = Brand.Blue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit profile", style = MaterialTheme.typography.labelLarge, color = Brand.Blue)
                        }
                        Spacer(Modifier.width(10.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable { nav.certificate() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Certificate", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }
        }

        item(key = "appearance") {
            SettingsGroup("Appearance") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.DarkMode, Brand.Indigo, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Theme", style = MaterialTheme.typography.titleSmall, color = c.text)
                }
                Spacer(Modifier.height(10.dp))
                PillSelector(
                    listOf("light", "dark", "system"), s.themeMode,
                    { when (it) { "light" -> "Light"; "dark" -> "Dark"; else -> "System" } },
                    { mode -> haptics.tick(); repo.updateSettings { it.copy(themeMode = mode) } },
                )
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.TextFields, Brand.Blue, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Font size", style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                    Text(fontSizes.minByOrNull { kotlin.math.abs(it.first - s.fontScale) }?.second ?: "Default", style = MaterialTheme.typography.labelLarge, color = c.primary)
                }
                val current = fontSizes.indexOfFirst { kotlin.math.abs(it.first - s.fontScale) < 0.01f }.coerceAtLeast(1)
                var slider by remember(current) { mutableFloatStateOf(current.toFloat()) }
                Slider(
                    value = slider,
                    onValueChange = { slider = it },
                    onValueChangeFinished = {
                        val i = slider.toInt().coerceIn(0, fontSizes.lastIndex)
                        haptics.tick()
                        repo.updateSettings { it.copy(fontScale = fontSizes[i].first) }
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.track),
                )
                Row {
                    Text("A", style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text("A", style = MaterialTheme.typography.titleLarge, color = c.textMuted)
                }
            }
        }

        item(key = "learning") {
            SettingsGroup("Learning") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Flag, Brand.Emerald, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Daily goal", style = MaterialTheme.typography.titleSmall, color = c.text)
                }
                Spacer(Modifier.height(10.dp))
                PillSelector(
                    listOf(5, 10, 15, 20, 30), state.profile?.dailyMinutes ?: 15, { "${it}m" },
                    { m -> haptics.tick(); state.profile?.let { p -> repo.saveProfile(p.copy(dailyMinutes = m)) } },
                )
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.AutoMirrored.Rounded.VolumeUp, Brand.Pink, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Voice speed", style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text("For examples, dialogues and listening", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    Text(
                        "Test",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { Speaker.speak("Hello! This is how Lexora sounds at this speed.") }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
                var rate by remember(s.speechRate) { mutableFloatStateOf(s.speechRate) }
                Slider(
                    value = rate,
                    onValueChange = { rate = it },
                    onValueChangeFinished = {
                        Speaker.rate = rate
                        repo.updateSettings { it.copy(speechRate = rate) }
                    },
                    valueRange = 0.6f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.track),
                )
                Row {
                    Text("Slower", style = MaterialTheme.typography.labelSmall, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text("Faster", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                }
            }
        }

        item(key = "notifications") {
            SettingsGroup("Notifications & feedback") {
                ToggleRow(Icons.Rounded.Quiz, Brand.Amber, "Daily quiz alerts", "Get notified when 5 new questions arrive", s.dailyQuizAlerts) { v ->
                    repo.updateSettings { it.copy(dailyQuizAlerts = v) }
                }
                Divider()
                ToggleRow(Icons.Rounded.NotificationsActive, Brand.Blue, "Progress alerts", "Lessons, modules, projects and certificate", s.achievementAlerts) { v ->
                    repo.updateSettings { it.copy(achievementAlerts = v) }
                }
                Divider()
                ToggleRow(Icons.Rounded.Vibration, Brand.Indigo, "Vibration", "Haptic feedback on answers and taps", s.vibration) { v ->
                    repo.updateSettings { it.copy(vibration = v) }
                    if (v) haptics.success()
                }
                Divider()
                NavRow(Icons.Rounded.Notifications, Brand.Teal, "System notification settings", "Sound, banners and lock screen") {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }

        item(key = "general") {
            SettingsGroup("General") {
                NavRow(Icons.Rounded.Replay, Brand.Blue, "Replay app tutorial", "See the quick tour of the home screen again") {
                    repo.setTutorialDone(false)
                }
                Divider()
                NavRow(Icons.Rounded.Share, Brand.Emerald, "Share Lexora", "Invite a friend to learn English with you") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "I'm learning English with Lexora — a complete A to Z course with real-life projects, 100% free and ad-free!")
                    }
                    context.startActivity(Intent.createChooser(send, "Share Lexora"))
                }
                Divider()
                NavRow(Icons.Rounded.PrivacyTip, Brand.Indigo, "Privacy", "Your data never leaves this phone") { dialog = "privacy" }
                Divider()
                NavRow(Icons.Rounded.Info, Brand.Sky, "About Lexora", "Version ${BuildConfig.VERSION_NAME}") { dialog = "about" }
                Divider()
                NavRow(Icons.Rounded.DeleteForever, c.error, "Reset progress", "Start the course again from the beginning", danger = true) { dialog = "reset" }
            }
        }

        item(key = "footer") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppImage(R.drawable.logo_mark, Modifier.size(36.dp), crossfade = false)
                Spacer(Modifier.height(6.dp))
                Text("Lexora ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, color = c.textSecondary)
                Text("100% free · No ads · No premium", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            }
        }
    }

    when (dialog) {
        "privacy" -> InfoDialog(
            "Privacy",
            "Lexora works completely offline. Your profile, photo, progress, notifications and certificate are stored only in this app's private storage on your phone. Nothing is uploaded, tracked or sold, and there are no ads.",
        ) { dialog = null }
        "about" -> InfoDialog(
            "About Lexora",
            "Lexora ${BuildConfig.VERSION_NAME}\n\nA complete English course from A to Z: ${com.lexora.english.content.Course.totalLessons} lessons in 12 modules, 15 real-life projects, daily quizzes, writing, speaking and listening practice — with a professional certificate signed by Saif A.\n\nAlways free. No ads. No premium.",
        ) { dialog = null }
        "reset" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Reset all progress?") },
            text = { Text("Lessons, quizzes, projects, practice history, notifications and your certificate will be deleted. Your profile and settings stay. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.resetProgress()
                    haptics.heavy()
                    dialog = null
                }) { Text("Reset", color = c.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
            containerColor = c.card,
            titleContentColor = c.text,
            textContentColor = c.textSecondary,
        )
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    val c = Lex.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        containerColor = c.card,
        titleContentColor = c.text,
        textContentColor = c.textSecondary,
    )
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val c = Lex.colors
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = c.textMuted,
            modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
        )
        LexCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun Divider() {
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Lex.colors.border)
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ToggleRow(icon: ImageVector, tint: Color, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Lex.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint, size = 38.dp, iconSize = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.textMuted,
                uncheckedTrackColor = c.track,
                uncheckedBorderColor = c.border,
            ),
        )
    }
}

@Composable
private fun NavRow(icon: ImageVector, tint: Color, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = Lex.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint, size = 38.dp, iconSize = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) c.error else c.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textMuted)
    }
}
