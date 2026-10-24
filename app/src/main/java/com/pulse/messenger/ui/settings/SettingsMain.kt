package com.pulse.messenger.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhonelinkSetup
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pulse.messenger.R
import com.pulse.messenger.ui.LocalSettingsRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.ChoiceDialog
import com.pulse.messenger.ui.components.ConfirmDialog
import com.pulse.messenger.ui.components.InputDialog
import com.pulse.messenger.ui.components.ListDivider
import com.pulse.messenger.ui.components.LoadingDialog
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseSwitch
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.SearchTopBar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SettingsSwitch
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.onboarding.AvatarPickerSheet
import com.pulse.messenger.ui.onboarding.languages
import com.pulse.messenger.ui.theme.PulseTheme
import com.pulse.messenger.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Standard settings page: top bar + lazy list. */
@Composable
fun SettingsPage(
    title: String,
    nav: NavController,
    subtitle: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar(title, onBack = { nav.back() }, subtitle = subtitle, actions = actions) }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            content()
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp), color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}

@Composable
fun Illustration(res: Int, size: Int = 180) {
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        AsyncImage(model = res, contentDescription = null, modifier = Modifier.size(size.dp))
    }
}

@Composable
fun SettingsScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(false) }

    data class Entry(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)
    val entries = listOf(
        Entry("Account", "Security notifications, change number", Icons.Outlined.Key) { nav.navigate(Routes.ACCOUNT) },
        Entry("Privacy", "Block contacts, disappearing messages", Icons.Outlined.Lock) { nav.navigate(Routes.PRIVACY) },
        Entry("Avatar", "Create, edit, profile photo", Icons.Outlined.AccountCircle) { nav.navigate(Routes.PROFILE) },
        Entry("Lists", "Manage people and groups", Icons.Outlined.People) { nav.navigate(Routes.ARCHIVED) },
        Entry("Chats", "Theme, wallpapers, chat history", Icons.AutoMirrored.Outlined.Chat) { nav.navigate(Routes.CHATS_SETTINGS) },
        Entry("Notifications", "Message, group & call tones", Icons.Outlined.Notifications) { nav.navigate(Routes.NOTIFICATIONS) },
        Entry("Storage and data", "Network usage, auto-download", Icons.Outlined.DataUsage) { nav.navigate(Routes.STORAGE) },
        Entry("App language", "${s.language} (device's language)", Icons.Outlined.Language) { language = true },
        Entry("Help", "Help center, contact us, privacy policy", Icons.AutoMirrored.Outlined.HelpOutline) { nav.navigate(Routes.HELP) },
        Entry("Linked devices", "Use Pulse on web and desktop", Icons.Outlined.Devices) { nav.navigate(Routes.LINKED_DEVICES) },
        Entry("Starred messages", "Find messages you saved", Icons.Outlined.StarOutline) { nav.navigate(Routes.STARRED) },
        Entry("Invite a friend", "Share Pulse with friends", Icons.Outlined.People) {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Let's chat on Pulse! It's fast, simple and secure. Get it at https://pulse.app/download")
            }, "Invite a friend"))
        },
        Entry("App updates", "You're on the latest version", Icons.Outlined.ArrowCircleUp) { snackbar.show("Pulse is up to date", SnackType.SUCCESS) },
    )
    val visible = if (query.isBlank()) entries else entries.filter { it.title.contains(query, true) || it.subtitle.contains(query, true) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) {
            if (searching) SearchTopBar(query, { query = it }, onClose = { searching = false; query = "" }, placeholder = "Search settings")
            else PulseTopBar("Settings", onBack = { nav.back() }, actions = {
                IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, "Search settings") }
            })
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (query.isBlank()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clickable { nav.navigate(Routes.PROFILE) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(s.avatar, s.name, 64.dp, fallback = AvatarFallback.PERSON)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name.ifBlank { "Your name" }, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
                            Text(s.about, style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1)
                        }
                        IconButton(onClick = { nav.navigate(Routes.qr(0)) }) { Icon(Icons.Outlined.QrCode, "My QR code", tint = MaterialTheme.colorScheme.primary) }
                    }
                    ListDivider()
                }
            }
            items(visible, key = { it.title }) { e ->
                SettingsItem(e.title, subtitle = e.subtitle, icon = e.icon, onClick = e.action)
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("from", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
                    Text("PULSE LABS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
    if (language) {
        ChoiceDialog("App language", languages, s.language, { it }, onSelect = {
            settingsRepo.update { st -> st.copy(language = it) }
            snackbar.show("Language set to $it", SnackType.SUCCESS)
        }, onDismiss = { language = false })
    }
}

@Composable
fun ProfileScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var editEmail by remember { mutableStateOf(false) }
    SettingsPage("Profile", nav) {
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.clickable { picker = true }) {
                    Avatar(s.avatar, s.name, 150.dp, fallback = AvatarFallback.PERSON)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                            .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.CameraAlt, "Change photo", tint = Color.White) }
                }
            }
            Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { picker = true }.padding(8.dp))
        }
        item {
            SettingsItem("Name", subtitle = s.name, icon = Icons.Outlined.Person, trailing = { Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary) }) { editName = true }
            Hint("This is not your username or PIN. This name will be visible to your Pulse contacts.")
            SettingsItem("About", subtitle = s.about, icon = Icons.Outlined.Info, trailing = { Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary) }) { nav.navigate(Routes.ABOUT_STATUS) }
            SettingsItem("Phone", subtitle = s.phone.ifBlank { "+1 415 555 0100" }, icon = Icons.Outlined.Phone)
            SettingsItem("Email", subtitle = s.email.ifBlank { "Add email" }, icon = Icons.Outlined.Email) { editEmail = true }
        }
    }
    if (picker) {
        AvatarPickerSheet(
            onPick = { res -> settingsRepo.update { it.copy(avatar = res) }; snackbar.show("Profile photo updated", SnackType.SUCCESS) },
            onRemove = if (s.avatar != null) ({ settingsRepo.update { it.copy(avatar = null) }; snackbar.show("Profile photo removed", SnackType.SUCCESS) }) else null,
            onDismiss = { picker = false },
        )
    }
    if (editName) {
        InputDialog("Enter your name", s.name, maxLength = 25, onConfirm = { v -> settingsRepo.update { it.copy(name = v) }; snackbar.show("Name updated", SnackType.SUCCESS) }, onDismiss = { editName = false })
    }
    if (editEmail) {
        InputDialog("Email address", s.email, maxLength = 80, keyboardType = KeyboardType.Email, allowEmpty = true, placeholder = "name@example.com",
            onConfirm = { v ->
                if (v.isEmpty() || Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(v)) {
                    settingsRepo.update { it.copy(email = v) }
                    snackbar.show(if (v.isEmpty()) "Email removed" else "Verification email sent to $v", SnackType.SUCCESS)
                } else snackbar.show("Please enter a valid email address", SnackType.ERROR)
            }, onDismiss = { editEmail = false })
    }
}

private val aboutPresets = listOf(
    "Available", "Busy", "At school", "At the movies", "At work", "Battery about to die",
    "Can't talk, Pulse only", "In a meeting", "At the gym", "Sleeping", "Urgent calls only", "Hey there! I am using Pulse.",
)

@Composable
fun AboutStatusScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf(false) }
    SettingsPage("About", nav) {
        item { SectionHeader("Currently set to") }
        item {
            SettingsItem(s.about, trailing = { Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary) }) { edit = true }
            ListDivider()
        }
        item { SectionHeader("Select About") }
        items(aboutPresets) { p ->
            SettingsItem(p, trailing = { if (p == s.about) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }) {
                settingsRepo.update { it.copy(about = p) }
                snackbar.show("About updated", SnackType.SUCCESS)
            }
        }
    }
    if (edit) {
        InputDialog("Add About", s.about, maxLength = 139, onConfirm = { v -> settingsRepo.update { it.copy(about = v) }; snackbar.show("About updated", SnackType.SUCCESS) }, onDismiss = { edit = false })
    }
}

@Composable
fun AccountScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var requestDialog by remember { mutableStateOf(false) }
    var passkeyDialog by remember { mutableStateOf(false) }
    var logout by remember { mutableStateOf(false) }
    SettingsPage("Account", nav) {
        item {
            SettingsItem("Security notifications", icon = Icons.Outlined.Security) { nav.navigate(Routes.SECURITY) }
            SettingsItem("Passkeys", subtitle = if (s.passkey) "1 passkey" else null, icon = Icons.Outlined.Password) { passkeyDialog = true }
            SettingsItem("Email address", subtitle = s.email.ifBlank { null }, icon = Icons.Outlined.Email) { nav.navigate(Routes.PROFILE) }
            SettingsItem("Two-step verification", subtitle = if (s.twoStepPin.isNotEmpty()) "On" else "Off", icon = Icons.Outlined.PhonelinkSetup) { nav.navigate(Routes.TWO_STEP) }
            SettingsItem("Change number", icon = Icons.Outlined.Phone) { nav.navigate(Routes.CHANGE_NUMBER) }
            SettingsItem("Request account info", icon = Icons.Outlined.Description) { requestDialog = true }
            SettingsItem("Add account", icon = Icons.Outlined.Badge) { snackbar.show("You can add up to 2 accounts on one phone", SnackType.INFO) }
            SettingsItem("Delete account", icon = Icons.Outlined.DeleteForever, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) { nav.navigate(Routes.DELETE_ACCOUNT) }
            SettingsItem("Log out", icon = Icons.AutoMirrored.Outlined.Logout, iconTint = PulseTheme.colors.danger, titleColor = PulseTheme.colors.danger) { logout = true }
        }
    }
    if (requestDialog) {
        ConfirmDialog(
            title = "Request account report",
            message = "Create a report of your Pulse account information and settings, which you can access or port to another app. The report does not include your messages. It will be ready in about 3 days.",
            confirmText = "Request report", icon = Icons.Outlined.Description,
            onConfirm = { snackbar.show("Report requested. Ready on ${Format.date(System.currentTimeMillis() + 3 * 86_400_000L)}", SnackType.SUCCESS) },
            onDismiss = { requestDialog = false },
        )
    }
    if (passkeyDialog) {
        ConfirmDialog(
            title = if (s.passkey) "Revoke passkey?" else "Create a passkey",
            message = if (s.passkey) "You'll need to verify with SMS when you log in again." else "Passkeys let you verify your account with your fingerprint, face or screen lock instead of an SMS code.",
            confirmText = if (s.passkey) "Revoke" else "Create passkey", destructive = s.passkey, icon = Icons.Outlined.Password,
            onConfirm = {
                settingsRepo.update { it.copy(passkey = !s.passkey) }
                snackbar.show(if (s.passkey) "Passkey revoked" else "Passkey created", SnackType.SUCCESS)
            },
            onDismiss = { passkeyDialog = false },
        )
    }
    if (logout) {
        ConfirmDialog(
            title = "Log out of Pulse?", message = "Your chat history stays on this device. You'll need to verify your number to log in again.",
            confirmText = "Log out", destructive = true, icon = Icons.AutoMirrored.Outlined.Logout,
            onConfirm = {
                settingsRepo.update { it.copy(onboarded = false) }
                nav.navigate(Routes.WELCOME) { popUpTo(0) { inclusive = true } }
            },
            onDismiss = { logout = false },
        )
    }
}

@Composable
fun SecurityScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    SettingsPage("Security notifications", nav) {
        item {
            Illustration(R.drawable.onb_privacy_e2e, 170)
            Text("Your chats and calls are private", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "End-to-end encryption keeps your personal messages and calls between you and the people you choose. No one outside of the chat, not even Pulse, can read, listen to, or share them. This includes your:",
                style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            listOf("Text and voice messages", "Audio and video calls", "Photos, videos and documents", "Location sharing", "Status updates").forEach {
                Row(Modifier.padding(horizontal = 28.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            ListDivider()
            SettingsSwitch(
                "Show security notifications on this device", s.securityNotifications,
                { v -> settingsRepo.update { it.copy(securityNotifications = v) } },
                subtitle = "Get notified when your security code changes for a contact's phone in an end-to-end encrypted chat.",
            )
        }
    }
}

@Composable
fun TwoStepScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(if (s.twoStepPin.isEmpty()) 0 else 4) } // 0 intro, 1 pin, 2 confirm, 3 email, 4 enabled
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var disableDialog by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(step) { if (step in 1..3) runCatching { delay(200); focus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().navigationBarsPadding()) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("Two-step verification", onBack = { if (step in 1..3) step-- else nav.back() }) }
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "2fa", modifier = Modifier.weight(1f)) { st ->
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (st) {
                    0, 4 -> {
                        Illustration(R.drawable.illus_two_step, 200)
                        Text(
                            if (st == 4) "Two-step verification is on. You'll need to enter your PIN if you register your phone number on Pulse again."
                            else "For extra security, turn on two-step verification, which will require a PIN when registering your phone number with Pulse again.",
                            textAlign = TextAlign.Center, color = PulseTheme.colors.textSecondary,
                        )
                    }
                    1, 2 -> {
                        Spacer(Modifier.height(32.dp))
                        Text(if (st == 1) "Create a 6-digit PIN that you can remember" else "Confirm your PIN", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        PinField(if (st == 1) pin else confirm, focus) { v ->
                            error = null
                            if (st == 1) pin = v else confirm = v
                        }
                        error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = PulseTheme.colors.danger, style = MaterialTheme.typography.bodySmall) }
                    }
                    3 -> {
                        Spacer(Modifier.height(32.dp))
                        Text("Add an email address so you can reset your PIN if you forget it.", textAlign = TextAlign.Center, color = PulseTheme.colors.textSecondary)
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            email, { email = it }, label = { Text("Email") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    }
                }
            }
        }
        when (step) {
            0 -> PrimaryButton("Turn on", onClick = { step = 1 }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            1 -> PrimaryButton("Next", enabled = pin.length == 6, onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            2 -> PrimaryButton("Next", enabled = confirm.length == 6, onClick = {
                if (confirm == pin) step = 3 else { error = "PINs don't match. Try again."; confirm = "" }
            }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            3 -> Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.pulse.messenger.ui.components.SecondaryButton("Skip", onClick = {
                    settingsRepo.update { it.copy(twoStepPin = pin) }
                    snackbar.show("Two-step verification is on", SnackType.SUCCESS); step = 4
                }, modifier = Modifier.weight(1f))
                PrimaryButton("Save", onClick = {
                    settingsRepo.update { it.copy(twoStepPin = pin, email = email.ifBlank { it.email }) }
                    snackbar.show("Two-step verification is on", SnackType.SUCCESS); step = 4
                }, modifier = Modifier.weight(1f))
            }
            else -> Column(Modifier.padding(16.dp)) {
                com.pulse.messenger.ui.components.SecondaryButton("Change PIN", onClick = { pin = ""; confirm = ""; step = 1 }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                PrimaryButton("Turn off", onClick = { disableDialog = true }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (disableDialog) {
        ConfirmDialog(
            title = "Turn off two-step verification?", message = "Your account will be less secure without a PIN.",
            confirmText = "Turn off", destructive = true,
            onConfirm = { settingsRepo.update { it.copy(twoStepPin = "") }; pin = ""; confirm = ""; step = 0; snackbar.show("Two-step verification turned off", SnackType.SUCCESS) },
            onDismiss = { disableDialog = false },
        )
    }
}

/** Six circles representing a hidden PIN, backed by an invisible text field. */
@Composable
fun PinField(value: String, focus: FocusRequester, length: Int = 6, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(length)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focus),
        decorationBox = { inner ->
            // The real field stays composed (for focus and IME) but invisible.
            Box(Modifier.size(1.dp).graphicsLayer { alpha = 0f }) { inner() }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(length) { i ->
                    val filled = i < value.length
                    Box(
                        Modifier.size(18.dp).clip(CircleShape)
                            .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(2.dp, if (filled) MaterialTheme.colorScheme.primary else PulseTheme.colors.textSecondary, CircleShape),
                    )
                }
            }
        },
    )
}

@Composable
fun ChangeNumberScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    val snackbar = LocalSnackbar.current
    val s by settingsRepo.settings.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(0) }
    var old by remember { mutableStateOf(s.phone) }
    var new by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().navigationBarsPadding()) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("Change number", onBack = { if (step == 1) step = 0 else nav.back() }) }
        Column(Modifier.weight(1f).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (step == 0) {
                Illustration(R.drawable.onb_verify_phone, 170)
                Text("Changing your phone number will migrate your account info, groups & settings.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("Before proceeding, please confirm that you are able to receive SMS or calls at your new number.", textAlign = TextAlign.Center, color = PulseTheme.colors.textSecondary)
            } else {
                Spacer(Modifier.height(16.dp))
                val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                Text("Enter your old phone number with country code:", modifier = Modifier.fillMaxWidth())
                OutlinedTextField(old, { old = it }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                Spacer(Modifier.height(12.dp))
                Text("Enter your new phone number with country code:", modifier = Modifier.fillMaxWidth())
                OutlinedTextField(new, { new = it }, singleLine = true, placeholder = { Text("+1 415 555 0123") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
        PrimaryButton(
            "Next",
            enabled = step == 0 || (new.count { it.isDigit() } >= 8),
            onClick = {
                if (step == 0) step = 1 else scope.launch {
                    loading = true; delay(1500); loading = false
                    settingsRepo.update { it.copy(phone = new.trim()) }
                    snackbar.show("Your number was changed to ${new.trim()}", SnackType.SUCCESS)
                    nav.back()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }
    if (loading) LoadingDialog("Changing number…")
}

@Composable
fun DeleteAccountScreen(nav: NavController) {
    val settingsRepo = LocalSettingsRepo.current
    var phone by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding().navigationBarsPadding()) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("Delete this account", onBack = { nav.back() }) }
        Column(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DeleteForever, null, tint = PulseTheme.colors.danger)
                Spacer(Modifier.width(12.dp))
                Text("If you delete this account:", style = MaterialTheme.typography.titleMedium, color = PulseTheme.colors.danger)
            }
            Spacer(Modifier.height(12.dp))
            listOf(
                "The account will be deleted from Pulse and all your devices",
                "Your message history will be erased",
                "You will be removed from all your Pulse groups",
                "Your backup will be deleted",
                "Any channels you created will be deleted",
            ).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("To delete your account, confirm your phone number.", style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary)
            OutlinedTextField(
                phone, { phone = it }, label = { Text("Phone number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTheme.colors.danger),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        androidx.compose.material3.Button(
            onClick = { confirm = true },
            enabled = phone.count { it.isDigit() } >= 6,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PulseTheme.colors.danger),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
        ) { Text("Delete account", fontWeight = FontWeight.SemiBold) }
    }
    if (confirm) {
        ConfirmDialog(
            title = "Delete account permanently?", message = "This can't be undone. All your data on this device will be erased.",
            confirmText = "Delete", destructive = true, icon = Icons.Outlined.DeleteForever,
            onConfirm = {
                settingsRepo.reset()
                nav.navigate(Routes.WELCOME) { popUpTo(0) { inclusive = true } }
            },
            onDismiss = { confirm = false },
        )
    }
}
