package com.pulse.messenger.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.data.MockData
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.home.previewText
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.onboarding.AvatarPickerSheet
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format

@Composable
fun CommunityScreen(nav: NavController, communityId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val communities by repo.communities.collectAsStateWithLifecycle()
    val chats by repo.chats.collectAsStateWithLifecycle()
    val messages by repo.messages.collectAsStateWithLifecycle()
    val community = communities.firstOrNull { it.id == communityId }
    if (community == null) {
        LaunchedEffect(Unit) { nav.back() }
        return
    }
    var exitDialog by remember { mutableStateOf(false) }
    val announcement = chats.firstOrNull { it.id == community.announcementChatId }
    val groups = community.groupIds.mapNotNull { id -> chats.firstOrNull { it.id == id } }
    val suggestions = chats.filter { it.communityId == community.id && it.id != community.announcementChatId && it.id !in community.groupIds }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        item {
            Box(Modifier.statusBarsPadding()) { PulseTopBar("", onBack = { nav.back() }) }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(community.avatar, community.name, 96.dp, Modifier.clip(RoundedCornerShape(28.dp)))
                Spacer(Modifier.height(12.dp))
                Text(community.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                Text("Community · ${community.memberCount} members", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Text(community.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(Modifier.height(16.dp))
            }
            ListDivider()
        }
        if (announcement != null) {
            item {
                val last = messages[announcement.id]?.lastOrNull()
                Row(
                    Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(announcement.id)) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text("Announcements", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(last?.let { Format.chatListTime(it.timestamp) }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                        }
                        Text(last?.let { previewText(it.content) } ?: "Only admins can send messages", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item { SectionHeader("Groups you're in") }
        if (groups.isEmpty()) {
            item { Text("You haven't joined any groups in this community yet.", color = PulseTheme.colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        }
        items(groups, key = { it.id }) { g ->
            val last = messages[g.id]?.lastOrNull()
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate(Routes.chat(g.id)) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(g.avatar, g.title, 48.dp, fallback = AvatarFallback.GROUP)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.title, style = MaterialTheme.typography.titleSmall)
                    Text(last?.let { previewText(it.content) } ?: "${g.memberIds.size} members", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item { SectionHeader("Groups you can join") }
        items(suggestions, key = { "s" + it.id }) { g ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(g.avatar, g.title, 48.dp, fallback = AvatarFallback.GROUP)
                Spacer(Modifier.width(14.dp))
                Text(g.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
        }
        item {
            val sampleGroups = listOf("Volunteers", "Events & meetups", "Help desk")
            sampleGroups.forEachIndexed { i, name ->
                Row(
                    Modifier.fillMaxWidth().clickable { snackbar.show("Request to join \"$name\" sent to admins", SnackType.SUCCESS) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(MockData.groupAvatars[(i + 3) % MockData.groupAvatars.size], name, 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text("${12 + i * 9} members", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                    }
                    Text(
                        "Join", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ListDivider()
            SettingsItem("Invite members", icon = Icons.Outlined.Link) { snackbar.show("Community invite link copied", SnackType.SUCCESS) }
            SettingsItem("Add group", icon = Icons.Outlined.Add) { nav.navigate(Routes.NEW_GROUP) }
            SettingsItem("Exit community", icon = Icons.Outlined.ExitToApp, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) { exitDialog = true }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (exitDialog) {
        ConfirmDialog(
            title = "Exit ${community.name}?",
            message = "You'll exit the community and its announcement group. You'll stay in other groups unless you exit them.",
            confirmText = "Exit", destructive = true,
            onConfirm = { snackbar.show("You exited ${community.name}", SnackType.SUCCESS); nav.back() },
            onDismiss = { exitDialog = false },
        )
    }
}

@Composable
fun NewCommunityScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("Hi everyone! This community is for members to chat in topic-based groups and get important announcements.") }
    var icon by remember { mutableStateOf<Int?>(null) }
    var picker by remember { mutableStateOf(false) }
    var intro by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar(if (intro) "" else "New community", onBack = { if (intro) nav.back() else intro = true }) }
        if (intro) {
            Column(Modifier.weight(1f).padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = R.drawable.onb_communities, contentDescription = null, modifier = Modifier.size(240.dp))
                Text("Create a new community", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Bring together a neighbourhood, school or more. Create topic-based groups for members, and easily send them admin announcements.",
                    color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text("See example communities", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { nav.back() }.padding(8.dp))
            }
            PrimaryButton("Get started", onClick = { intro = false }, modifier = Modifier.fillMaxWidth().padding(16.dp))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.clickable { picker = true }) {
                    if (icon != null) Avatar(icon, name, 110.dp, Modifier.clip(RoundedCornerShape(30.dp)))
                    else Box(Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(PulseTheme.colors.chip), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CameraAlt, null, tint = PulseTheme.colors.textSecondary, modifier = Modifier.size(36.dp))
                    }
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Add, null, tint = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
                Text("Change photo", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    name, { if (it.length <= 100) name = it }, label = { Text("Community name") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    supportingText = { Text("${100 - name.length}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    description, { if (it.length <= 2048) description = it }, label = { Text("Description") }, minLines = 3,
                    shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("You can add existing groups or create new ones after your community is created.", fontSize = 13.sp, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center)
            }
            PrimaryButton(
                "Create community",
                enabled = name.isNotBlank(),
                onClick = {
                    val c = repo.createCommunity(name.trim(), description.trim(), icon ?: R.drawable.avatar_community_03)
                    snackbar.show("Community \"${c.name}\" created", SnackType.SUCCESS)
                    nav.navigate(Routes.community(c.id)) { popUpTo(Routes.HOME) }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
    if (picker) {
        AvatarPickerSheet(
            onPick = { icon = it }, onRemove = icon?.let { { icon = null } }, onDismiss = { picker = false },
            options = MockData.groupAvatars + listOf(R.drawable.avatar_community_01, R.drawable.avatar_community_02, R.drawable.avatar_community_03, R.drawable.avatar_community_04),
            title = "Community icon",
        )
    }
}
