package com.saif.pymaster.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlagCircle
import androidx.compose.material.icons.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.Settings
import com.saif.pymaster.notify.Notifier
import com.saif.pymaster.ui.MainViewModel
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.AvatarImage
import com.saif.pymaster.ui.components.Tag
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.ui.theme.ThemeVariant
import com.saif.pymaster.util.LocalHaptics
import com.saif.pymaster.util.formatDate

private val fontScales = listOf(0.9f to "Small", 1f to "Default", 1.1f to "Large", 1.2f to "Huge")

@Composable
fun SettingsScreen(state: AppState?, vm: MainViewModel, nav: Navigator) {
    val s = state ?: return
    val settings = s.settings
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var versionTaps by remember { mutableIntStateOf(0) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.updateSettings { it.copy(notifications = granted) }
        if (!granted) Toast.makeText(context, "Allow notifications in system settings to receive alerts", Toast.LENGTH_LONG).show()
    }

    fun update(transform: (Settings) -> Settings) = vm.updateSettings(transform)

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        // Header image + profile card
        Box(Modifier.fillMaxWidth()) {
            Image(
                painterResource(R.drawable.settings_header), null,
                Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(Brush.verticalGradient(0f to Color(0x660B0F17), 1f to colors.background))
            )
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = Brand.White,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 12.dp),
            )
            AppCard(
                Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 130.dp)
                    .fillMaxWidth(),
                onClick = { nav.to(Routes.EDIT_PROFILE) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarImage(s.profile, 68.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.profile?.name ?: "Learner",
                            style = MaterialTheme.typography.titleMedium,
                            color = Brand.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${s.profile?.level ?: ""}  •  ${s.profile?.goal ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Brand.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        s.profile?.createdAt?.takeIf { it > 0 }?.let {
                            Text("Member since ${formatDate(it, "MMM yyyy")}", style = MaterialTheme.typography.labelSmall, color = Brand.TextMuted)
                        }
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brand.PythonBlue.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Edit, "Edit profile", tint = Brand.White, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("${s.progress.xp} XP", Brand.PythonYellow)
                    Tag("${s.progress.streak} day streak", Brand.Warning)
                    Tag("${s.progress.codeRuns} runs", Brand.Cyan)
                }
            }
        }

        // Profile
        Group("Profile") {
            NavRow(Icons.Rounded.Person, "Edit profile", "Name, avatar, photo, level and goal", Brand.PythonBlueBright) {
                nav.to(Routes.EDIT_PROFILE)
            }
        }

        // Appearance
        Group("Appearance") {
            RowHeader(Icons.Rounded.Palette, "Theme", "Dark themes designed for long sessions", Brand.Purple)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ThemeVariant.entries.forEach { v ->
                    val selected = settings.theme == v.id
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { haptics?.tick(); update { it.copy(theme = v.id) } },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.verticalGradient(listOf(v.colors.surfaceHigh, v.colors.background)))
                                .border(2.dp, if (selected) Brand.Cyan else Color(0x22FFFFFF), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Rounded.Check, null, tint = Brand.Cyan)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            v.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) Brand.White else Brand.TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
            Divider()
            RowHeader(Icons.Rounded.TextFields, "Font size", "Text size across the app", Brand.Cyan)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                fontScales.forEach { (scale, label) ->
                    val selected = settings.fontScale == scale
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Brand.PythonBlue.copy(alpha = 0.35f) else colors.surfaceHigh)
                            .border(1.dp, if (selected) Brand.Cyan else colors.stroke, RoundedCornerShape(12.dp))
                            .clickable { haptics?.tick(); update { it.copy(fontScale = scale) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aa", style = MaterialTheme.typography.titleSmall.copy(fontSize = (13 * scale).sp), color = Brand.White)
                            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.TextSecondary, maxLines = 1)
                        }
                    }
                }
            }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    RowHeader(Icons.Rounded.Code, "Terminal font", "${settings.terminalFontSize} sp", Brand.Success)
                }
                Stepper(
                    onMinus = { update { it.copy(terminalFontSize = (it.terminalFontSize - 1).coerceAtLeast(10)) } },
                    onPlus = { update { it.copy(terminalFontSize = (it.terminalFontSize + 1).coerceAtMost(22)) } },
                )
            }
        }

        // Preferences
        Group("Preferences") {
            SwitchRow(Icons.Rounded.Vibration, "Vibration", "Haptic feedback on taps and answers", Brand.Warning, settings.vibration) { on ->
                update { it.copy(vibration = on) }
            }
            Divider()
            SwitchRow(
                if (settings.notifications) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                "Notifications", "Lesson, module, project & certificate alerts", Brand.PythonYellow, settings.notifications,
            ) { on ->
                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.canPost(context)) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    update { it.copy(notifications = on) }
                }
            }
            Divider()
            SwitchRow(Icons.Rounded.Quiz, "Daily quiz reminder", "Get notified when a new daily quiz arrives", Brand.Cyan, settings.dailyQuizReminder) { on ->
                update { it.copy(dailyQuizReminder = on) }
            }
            Divider()
            SwitchRow(Icons.Rounded.FormatIndentIncrease, "Smart auto-indent", "Indent new lines in the terminal editor", Brand.Success, settings.autoIndent) { on ->
                update { it.copy(autoIndent = on) }
            }
        }

        // Learning
        Group("Learning") {
            RowHeader(Icons.Rounded.FlagCircle, "Daily study goal", "${s.profile?.dailyGoalMinutes ?: 20} minutes per day", Brand.PythonBlueBright)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(10, 20, 30, 60).forEach { mins ->
                    val selected = s.profile?.dailyGoalMinutes == mins
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Brand.PythonBlue.copy(alpha = 0.35f) else colors.surfaceHigh)
                            .border(1.dp, if (selected) Brand.Cyan else colors.stroke, RoundedCornerShape(12.dp))
                            .clickable {
                                haptics?.tick()
                                s.profile?.let { vm.saveProfile(it.copy(dailyGoalMinutes = mins), isNew = false) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$mins min", style = MaterialTheme.typography.labelMedium, color = if (selected) Brand.White else Brand.TextSecondary)
                    }
                }
            }
        }

        // Data
        Group("Data & privacy") {
            RowHeader(Icons.Rounded.Security, "Stored on this phone only", "Your profile, progress and notifications never leave your device.", Brand.Success)
            Divider()
            NavRow(Icons.Rounded.NotificationsOff, "Clear notifications", "${s.notifications.size} saved", Brand.TextSecondary) {
                confirmClear = true
            }
            Divider()
            NavRow(Icons.Rounded.DeleteForever, "Reset all progress", "Lessons, quizzes, projects, XP and certificate", Brand.Danger) {
                confirmReset = true
            }
        }

        // About
        Group("About") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        versionTaps++
                        if (settings.devUnlocked) {
                            if (versionTaps == 1) Toast.makeText(context, "Developer mode is already on", Toast.LENGTH_SHORT).show()
                        } else if (versionTaps in 4..6) {
                            Toast.makeText(context, "${7 - versionTaps} more taps to developer mode", Toast.LENGTH_SHORT).show()
                        } else if (versionTaps >= 7) {
                            vm.enableDeveloperMode()
                            haptics?.success()
                            Toast.makeText(context, "Developer mode: all content unlocked for testing", Toast.LENGTH_LONG).show()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.logo), null, Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PyMaster", style = MaterialTheme.typography.titleMedium, color = Brand.White)
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
                }
                if (settings.devUnlocked) Tag("DEV", Brand.Purple)
            }
            Divider()
            RowHeader(Icons.Rounded.Info, "Developer", "Designed & developed by Saif A.", Brand.Cyan)
            Divider()
            RowHeader(Icons.Rounded.VolunteerActivism, "100% free", "No ads. No premium. No account. Just learning.", Brand.PythonYellow)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Made with 🐍 and ❤️",
            style = MaterialTheme.typography.bodySmall,
            color = Brand.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = colors.surfaceHigh,
            title = { Text("Reset all progress?", color = Brand.White) },
            text = {
                Text(
                    "This permanently deletes your lessons, quiz scores, projects, XP, streak, certificate and notifications. Your profile is kept.",
                    color = Brand.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetProgress()
                    Toast.makeText(context, "Progress reset", Toast.LENGTH_SHORT).show()
                }) { Text("Reset", color = Brand.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = Brand.White) } },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = colors.surfaceHigh,
            title = { Text("Clear notifications?", color = Brand.White) },
            text = { Text("All saved notifications will be removed.", color = Brand.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearNotifications() }) { Text("Clear", color = Brand.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Brand.White) } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = Brand.TextMuted,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
    AppCard(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        padding = PaddingValues(16.dp),
        content = content,
    )
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(vertical = 14.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Brand.Divider)
    )
}

@Composable
private fun RowHeader(icon: ImageVector, title: String, subtitle: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Brand.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { RowHeader(icon, title, subtitle, tint) }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Brand.TextMuted)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, tint: Color, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHaptics.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { RowHeader(icon, title, subtitle, tint) }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { haptics?.tick(); onChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Brand.White,
                checkedTrackColor = Brand.PythonBlueBright,
                uncheckedThumbColor = Color(0xFF9AA4B2),
                uncheckedTrackColor = Color(0x22FFFFFF),
                uncheckedBorderColor = Color(0x33FFFFFF),
            ),
        )
    }
}

@Composable
private fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceHigh)
            .border(1.dp, colors.stroke, RoundedCornerShape(50)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinus, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Remove, "Smaller", tint = Brand.White) }
        Box(Modifier.width(1.dp).height(20.dp).background(colors.stroke))
        IconButton(onClick = onPlus, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Add, "Bigger", tint = Brand.White) }
    }
}
