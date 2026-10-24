package com.pulse.messenger.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pulse.messenger.R
import com.pulse.messenger.data.PulseRepository
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.ChoiceDialog
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.EmptyState
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SettingsSwitch
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.chat.ContactPickerSheet
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.theme.PulseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val audience = listOf("Everyone", "My contacts", "My contacts except…", "Nobody")

@Composable
fun PrivacyScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    val blocked by repo.blocked.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }

    SettingsPage("Privacy", nav) {
        item { SectionHeader("Who can see my personal info") }
        item {
            SettingsItem("Last seen and online", subtitle = audience[s.lastSeen]) { dialog = "lastSeen" }
            SettingsItem("Profile photo", subtitle = audience[s.profilePhoto]) { dialog = "photo" }
            SettingsItem("About", subtitle = audience[s.aboutPrivacy]) { dialog = "about" }
            SettingsItem("Status", subtitle = audience[s.statusPrivacy]) { dialog = "status" }
            SettingsSwitch("Read receipts", s.readReceipts, { v -> settingsRepo.update { it.copy(readReceipts = v) } },
                subtitle = "If turned off, you won't send or receive read receipts. Read receipts are always sent for group chats.")
            ListDivider()
        }
        item { SectionHeader("Disappearing messages") }
        item {
            SettingsItem("Default message timer", subtitle = "Start new chats with disappearing messages set to your timer", icon = Icons.Outlined.Timer,
                trailing = { Text(PulseRepository.disappearingLabel(s.defaultTimer), color = PulseTheme.colors.textSecondary) }) { nav.navigate(Routes.disappearing()) }
            ListDivider()
        }
        item {
            SettingsItem("Groups", subtitle = audience[s.groupsPrivacy]) { dialog = "groups" }
            SettingsItem("Live location", subtitle = "None") { snackbar.show("You aren't sharing live location in any chats", SnackType.INFO) }
            SettingsSwitch("Silence unknown callers", s.callsSilenceUnknown, { v -> settingsRepo.update { it.copy(callsSilenceUnknown = v) } },
                subtitle = "Calls from unknown numbers will be silenced. They will still be shown in the Calls tab.")
            SettingsItem("Blocked contacts", subtitle = blocked.size.toString()) { nav.navigate(Routes.BLOCKED) }
            SettingsItem("App lock", subtitle = if (s.appLock) "Enabled" else "Disabled") { nav.navigate(Routes.APP_LOCK) }
            SettingsItem("Chat lock", subtitle = "Lock individual chats from the chat's contact info") { snackbar.show("Open a chat's info and turn on Chat lock", SnackType.INFO) }
            SettingsItem("Privacy checkup", subtitle = "Control your privacy and choose the right settings for you.") { nav.navigate(Routes.SECURITY) }
        }
    }

    dialog?.let { key ->
        val (title, current) = when (key) {
            "lastSeen" -> "Last seen" to s.lastSeen
            "photo" -> "Profile photo" to s.profilePhoto
            "about" -> "About" to s.aboutPrivacy
            "status" -> "Status privacy" to s.statusPrivacy
            else -> "Who can add me to groups" to s.groupsPrivacy
        }
        ChoiceDialog(
            title = title, options = audience.indices.toList(), selected = current, label = { audience[it] },
            description = if (key == "lastSeen") "If you don't share your last seen, you won't be able to see other people's last seen." else null,
            onSelect = { v ->
                settingsRepo.update {
                    when (key) {
                        "lastSeen" -> it.copy(lastSeen = v)
                        "photo" -> it.copy(profilePhoto = v)
                        "about" -> it.copy(aboutPrivacy = v)
                        "status" -> it.copy(statusPrivacy = v)
                        else -> it.copy(groupsPrivacy = v)
                    }
                }
                snackbar.show("$title: ${audience[v]}", SnackType.SUCCESS)
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
fun BlockedScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val blocked by repo.blocked.collectAsStateWithLifecycle()
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf(false) }
    var unblock by remember { mutableStateOf<String?>(null) }
    SettingsPage("Blocked contacts", nav, actions = {
        IconButton(onClick = { picker = true }) { Icon(Icons.Outlined.PersonAdd, "Block a contact") }
    }) {
        if (blocked.isEmpty()) {
            item { EmptyState(R.drawable.empty_blocked, "No blocked contacts", "Blocked contacts can't call you or send you messages.") }
        }
        items(blocked, key = { it }) { id ->
            val c = repo.contact(id) ?: return@items
            Row(
                Modifier.fillMaxWidth().clickable { unblock = id }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(c.avatar, c.name, 46.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    Text(c.phone, style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                }
                Icon(Icons.Outlined.Block, null, tint = PulseTheme.colors.danger)
            }
        }
        item { Hint("Blocked contacts will no longer be able to call you or send you messages. Tap a contact to unblock.") }
    }
    if (picker) {
        ContactPickerSheet(contacts.filter { it.id !in blocked }, onDismiss = { picker = false }, onPick = { c ->
            repo.setBlocked(c.id, true); snackbar.show("${c.name} blocked", SnackType.SUCCESS)
        }, title = "Block contact")
    }
    unblock?.let { id ->
        val name = repo.displayName(id)
        ConfirmDialog(
            title = "Unblock $name?", message = "$name will be able to call you and send you messages.", confirmText = "Unblock",
            onConfirm = { repo.setBlocked(id, false); snackbar.show("$name unblocked", SnackType.SUCCESS) },
            onDismiss = { unblock = null },
        )
    }
}

private val timers = listOf(24 * 3600L, 7 * 24 * 3600L, 90 * 24 * 3600L, 0L)

@Composable
fun DisappearingScreen(nav: NavController, chatId: String) {
    val repo = LocalRepo.current
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    val chats by repo.chats.collectAsStateWithLifecycle()
    val isDefault = chatId == "default"
    val chat = chats.firstOrNull { it.id == chatId }
    val current = if (isDefault) s.defaultTimer else chat?.disappearingSeconds ?: 0L
    SettingsPage(if (isDefault) "Default message timer" else "Disappearing messages", nav) {
        item {
            Illustration(R.drawable.illus_disappearing, 190)
            Text(
                if (isDefault) "Start new chats with a timer" else "Make messages in this chat disappear",
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "For more privacy and storage, all new messages will disappear from this chat for everyone after the selected duration, except when kept. Anyone in the chat can change this setting.",
                style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            SectionHeader("Message timer")
        }
        items(timers) { t ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    if (isDefault) settingsRepo.update { it.copy(defaultTimer = t) } else repo.setDisappearing(chatId, t)
                    snackbar.show(if (t == 0L) "Disappearing messages off" else "Messages will disappear after ${PulseRepository.disappearingLabel(t)}", SnackType.SUCCESS)
                }.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = t == current, onClick = null, modifier = Modifier.padding(12.dp), colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
                Text(PulseRepository.disappearingLabel(t), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun AppLockSettingsScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var setPin by remember { mutableStateOf(false) }
    SettingsPage("App lock", nav) {
        item {
            Illustration(R.drawable.illus_chat_lock, 190)
            SettingsSwitch(
                "Unlock with fingerprint or PIN", s.appLock,
                { v ->
                    if (v) setPin = true else {
                        settingsRepo.update { it.copy(appLock = false) }
                        snackbar.show("App lock turned off", SnackType.SUCCESS)
                    }
                },
                subtitle = "When enabled, you'll need to use your fingerprint or PIN to open Pulse. You can still answer calls if Pulse is locked.",
            )
            if (s.appLock) SettingsItem("Change PIN", subtitle = "4-digit PIN") { setPin = true }
        }
    }
    if (setPin) {
        PinSetupDialog(onDone = { pin ->
            settingsRepo.update { it.copy(appLock = true, appLockPin = pin) }
            snackbar.show("App lock is on", SnackType.SUCCESS)
        }, onDismiss = { setPin = false })
    }
}

@Composable
private fun PinSetupDialog(onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    com.pulse.messenger.ui.components.PulseDialog(
        onDismiss = onDismiss,
        title = if (first == null) "Set a 4-digit PIN" else "Confirm your PIN",
        message = if (error) "PINs didn't match. Try again." else "You'll use this PIN to unlock Pulse.",
        buttons = listOf(com.pulse.messenger.ui.components.DialogButton("Cancel", onClick = onDismiss)),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            PinDots(value.length, 4)
        }
        Keypad(
            onDigit = { d ->
                if (value.length < 4) value += d
                if (value.length == 4) {
                    val f = first
                    if (f == null) { first = value; value = ""; error = false }
                    else if (f == value) { onDone(value); onDismiss() }
                    else { error = true; first = null; value = "" }
                }
            },
            onBack = { value = value.dropLast(1) },
            onBiometric = null,
        )
    }
}

@Composable
private fun PinDots(filled: Int, total: Int, shake: Float = 0f) {
    Row(Modifier.offset { IntOffset(shake.roundToInt(), 0) }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(if (i < filled) MaterialTheme.colorScheme.primary else PulseTheme.colors.chip),
            )
        }
    }
}

@Composable
private fun Keypad(onDigit: (String) -> Unit, onBack: () -> Unit, onBiometric: (() -> Unit)?) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "back"))
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { k ->
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).clickable(enabled = k != "bio" || onBiometric != null) {
                            when (k) {
                                "back" -> onBack()
                                "bio" -> onBiometric?.invoke()
                                else -> onDigit(k)
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (k) {
                            "back" -> Icon(Icons.AutoMirrored.Filled.Backspace, "Delete", tint = PulseTheme.colors.textSecondary)
                            "bio" -> if (onBiometric != null) Icon(Icons.Filled.Fingerprint, "Fingerprint", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            else -> Text(k, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/** Full-screen lock shown on launch when App lock is enabled. */
@Composable
fun AppLockScreen(pin: String, onUnlock: () -> Unit) {
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var bio by remember { mutableIntStateOf(0) }
    LaunchedEffect(bio) { if (bio > 0) { delay(700); onUnlock() } }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()
            .clickable(interactionSource = null, indication = null) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(R.drawable.logo_full_color), null, Modifier.size(84.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Pulse is locked", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Text(if (error) "Wrong PIN. Try again." else if (bio > 0) "Verifying fingerprint…" else "Enter your PIN or use your fingerprint", color = if (error) PulseTheme.colors.danger else PulseTheme.colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        PinDots(value.length, 4, shake.value)
        Spacer(Modifier.weight(1f))
        Keypad(
            onDigit = { d ->
                if (value.length < 4) value += d
                if (value.length == 4) {
                    if (value == pin) onUnlock() else {
                        error = true
                        scope.launch {
                            for (x in listOf(24f, -24f, 16f, -16f, 8f, 0f)) shake.animateTo(x, tween(45))
                            value = ""
                        }
                    }
                } else error = false
            },
            onBack = { value = value.dropLast(1) },
            onBiometric = { bio++ },
        )
        Spacer(Modifier.height(24.dp))
    }
}
