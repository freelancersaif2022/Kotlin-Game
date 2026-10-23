package com.saif.pymaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saif.pymaster.R
import com.saif.pymaster.data.AppNotification
import com.saif.pymaster.data.AppState
import com.saif.pymaster.data.NotificationType
import com.saif.pymaster.data.Rules
import com.saif.pymaster.ui.MainViewModel
import com.saif.pymaster.ui.Navigator
import com.saif.pymaster.ui.Routes
import com.saif.pymaster.ui.components.AppCard
import com.saif.pymaster.ui.components.EmptyState
import com.saif.pymaster.ui.components.ScreenTopBar
import com.saif.pymaster.ui.theme.Brand
import com.saif.pymaster.ui.theme.LocalAppColors
import com.saif.pymaster.util.timeAgo

@Composable
fun NotificationsScreen(state: AppState?, vm: MainViewModel, nav: Navigator) {
    val s = state ?: return
    val colors = LocalAppColors.current
    val unread = s.notifications.count { !it.read }
    val todayStart = Rules.today()

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenTopBar(
            "Notifications",
            subtitle = if (unread > 0) "$unread unread" else "You're all caught up",
            onBack = nav::back,
        ) {
            if (unread > 0) {
                IconButton(onClick = { vm.markAllRead() }) {
                    Icon(Icons.Rounded.DoneAll, "Mark all as read", tint = Brand.Cyan)
                }
            }
        }
        if (s.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    R.drawable.empty_notifications,
                    "No notifications yet",
                    "Finish lessons, pass quizzes and complete projects — your achievements will show up here.",
                )
            }
            return@Column
        }
        val (today, earlier) = s.notifications.partition { localDay(it.time) == todayStart }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            if (today.isNotEmpty()) {
                item { GroupLabel("Today") }
                items(today, key = { it.id }) { NotificationItem(it, vm, nav) }
            }
            if (earlier.isNotEmpty()) {
                item { GroupLabel("Earlier") }
                items(earlier, key = { it.id }) { NotificationItem(it, vm, nav) }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

private fun localDay(millis: Long): Long =
    (millis + java.util.TimeZone.getDefault().getOffset(millis)) / 86_400_000L

@Composable
private fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
        color = Brand.TextMuted,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 4.dp),
    )
}

private fun style(type: String): Pair<ImageVector, Color> = when (type) {
    NotificationType.LESSON -> Icons.Rounded.CheckCircle to Brand.Success
    NotificationType.MODULE -> Icons.Rounded.School to Brand.PythonBlueBright
    NotificationType.COURSE -> Icons.Rounded.EmojiEvents to Brand.PythonYellow
    NotificationType.PROJECT -> Icons.Rounded.Rocket to Brand.Cyan
    NotificationType.CERTIFICATE -> Icons.Rounded.WorkspacePremium to Brand.PythonYellow
    NotificationType.QUIZ -> Icons.Rounded.Quiz to Brand.Warning
    NotificationType.QUIZ_UNLOCK -> Icons.Rounded.LockOpen to Brand.Purple
    else -> Icons.Rounded.Info to Brand.Cyan
}

@Composable
private fun NotificationItem(n: AppNotification, vm: MainViewModel, nav: Navigator) {
    val (icon, tint) = style(n.type)
    AppCard(
        Modifier
            .padding(vertical = 5.dp)
            .fillMaxWidth(),
        onClick = {
            vm.markNotificationRead(n.id)
            when (n.type) {
                NotificationType.QUIZ -> nav.to(Routes.DAILY_QUIZ)
                NotificationType.CERTIFICATE -> nav.to(Routes.CERTIFICATE)
                NotificationType.PROJECT, NotificationType.COURSE -> nav.to(Routes.PROJECTS)
                NotificationType.LESSON, NotificationType.MODULE, NotificationType.QUIZ_UNLOCK -> nav.tab(Routes.COURSE)
            }
        },
        padding = PaddingValues(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        border = if (n.read) LocalAppColors.current.stroke else tint.copy(alpha = 0.45f),
        background = if (n.read) LocalAppColors.current.surface else tint.copy(alpha = 0.07f).compositeOver(LocalAppColors.current.surface),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Brand.White,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!n.read) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
                    }
                }
                Spacer(Modifier.size(2.dp))
                Text(n.body, style = MaterialTheme.typography.bodySmall, color = Brand.TextSecondary)
                Spacer(Modifier.size(4.dp))
                Text(timeAgo(n.time), style = MaterialTheme.typography.labelSmall, color = Brand.TextMuted)
            }
            IconButton(onClick = { vm.deleteNotification(n.id) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Close, "Delete", tint = Brand.TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}
