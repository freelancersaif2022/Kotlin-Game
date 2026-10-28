package com.lexora.english.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lexora.english.R
import com.lexora.english.data.AppNotification
import com.lexora.english.data.AppRepository
import com.lexora.english.data.AppState
import com.lexora.english.data.Notifier
import com.lexora.english.ui.Nav
import com.lexora.english.ui.components.CircleIconButton
import com.lexora.english.ui.components.EmptyState
import com.lexora.english.ui.components.IconTile
import com.lexora.english.ui.components.LexCard
import com.lexora.english.ui.components.ScreenHeader
import com.lexora.english.ui.components.SecondaryButton
import com.lexora.english.ui.theme.Brand
import com.lexora.english.ui.theme.Lex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotificationsScreen(state: AppState, nav: Nav) {
    val c = Lex.colors
    val context = LocalContext.current
    val repo = AppRepository.get(context)
    var confirmClear by remember { mutableStateOf(false) }

    // Re-check permission whenever the screen resumes (e.g. after returning from system settings).
    var permissionTick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) permissionTick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val canPost = remember(permissionTick) { Notifier.canPost(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionTick++ }

    LaunchedEffect(Unit) {
        // Opening the centre counts as seeing everything; keep items visible but mark them read shortly after.
        kotlinx.coroutines.delay(1500)
        repo.markAllRead()
    }

    val groups = remember(state.notifications) { groupByDay(state.notifications) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        ScreenHeader(
            "Notifications",
            { nav.back() },
            subtitle = if (state.notifications.isEmpty()) "You're all caught up" else "${state.notifications.count { !it.read }} unread",
        ) {
            if (state.notifications.isNotEmpty()) {
                CircleIconButton(Icons.Rounded.DoneAll, "Mark all read", { repo.markAllRead() })
                Spacer(Modifier.width(8.dp))
                CircleIconButton(Icons.Rounded.DeleteSweep, "Clear all", { confirmClear = true })
                Spacer(Modifier.width(8.dp))
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (!canPost) {
                item(key = "perm") {
                    LexCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        color = c.primarySoft,
                        elevated = false,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Icons.Rounded.NotificationsOff, c.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Phone alerts are off", style = MaterialTheme.typography.titleSmall, color = c.text)
                                Text(
                                    "Updates are still saved here. Turn on alerts to get your daily quiz on time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textSecondary,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SecondaryButton(
                            "Turn on notifications",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33 && !state.notificationPermissionAsked) {
                                    repo.setNotificationPermissionAsked()
                                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    try {
                                        context.startActivity(i)
                                    } catch (_: Exception) {
                                    }
                                }
                            },
                            icon = Icons.Rounded.NotificationsActive,
                            modifier = Modifier.fillMaxWidth(),
                            height = 44.dp,
                        )
                    }
                }
            }
            if (state.notifications.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        R.drawable.empty_notifications,
                        "No notifications yet",
                        "Complete lessons, projects and daily challenges — your updates will appear here.",
                        Modifier.padding(top = 32.dp),
                    )
                }
            }
            groups.forEach { (label, list) ->
                item(key = "h_$label") {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textMuted,
                        modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                items(list, key = { it.id }) { n ->
                    NotificationItem(
                        n,
                        onClick = { repo.markNotificationRead(n.id) },
                        onDelete = { repo.deleteNotification(n.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all notifications?") },
            text = { Text("This removes every saved notification from this phone.") },
            confirmButton = {
                TextButton(onClick = { repo.clearNotifications(); confirmClear = false }) { Text("Clear", color = c.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            containerColor = c.card,
            titleContentColor = c.text,
            textContentColor = c.textSecondary,
        )
    }
}

private fun groupByDay(list: List<AppNotification>): List<Pair<String, List<AppNotification>>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.US)
    return list.groupBy {
        val d = Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate()
        when (d) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> fmt.format(d)
        }
    }.toList()
}

private fun typeStyle(type: String): Pair<ImageVector, Color> = when (type) {
    "lesson" -> Icons.AutoMirrored.Rounded.MenuBook to Brand.Blue
    "module" -> Icons.Rounded.TaskAlt to Brand.Indigo
    "course" -> Icons.Rounded.School to Brand.Emerald
    "project" -> Icons.Rounded.EmojiEvents to Brand.Orange
    "certificate" -> Icons.Rounded.WorkspacePremium to Brand.Gold
    "daily" -> Icons.Rounded.Quiz to Brand.Amber
    "practice" -> Icons.Rounded.EditNote to Brand.Teal
    "streak" -> Icons.Rounded.LocalFireDepartment to Brand.Orange
    else -> Icons.Rounded.Celebration to Brand.Pink
}

private fun timeLabel(time: Long): String =
    DateTimeFormatter.ofPattern("h:mm a", Locale.US).format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()))

@Composable
private fun NotificationItem(n: AppNotification, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val c = Lex.colors
    val (icon, tint) = typeStyle(n.type)
    LexCard(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .animateContentSize(),
        onClick = onClick,
        color = if (n.read) c.card else c.primarySoft.copy(alpha = if (c.isDark) 0.6f else 0.55f),
        padding = PaddingValues(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon, tint, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.title, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f, fill = false))
                    if (!n.read) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(c.primary)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(n.body, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                Spacer(Modifier.height(6.dp))
                Text(timeLabel(n.time), style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, "Delete", tint = c.textMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}
