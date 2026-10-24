package com.pulse.messenger.ui.newchat

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pulse.messenger.data.ChatType
import com.pulse.messenger.data.Contact
import com.pulse.messenger.data.MockData
import com.pulse.messenger.data.PulseRepository
import com.pulse.messenger.ui.LocalRepo
import com.pulse.messenger.ui.components.Avatar
import com.pulse.messenger.ui.components.AvatarFallback
import com.pulse.messenger.ui.components.ChoiceDialog
import com.pulse.messenger.ui.components.LocalSnackbar
import com.pulse.messenger.ui.components.PrimaryButton
import com.pulse.messenger.ui.components.PulseTopBar
import com.pulse.messenger.ui.components.SearchTopBar
import com.pulse.messenger.ui.components.SectionHeader
import com.pulse.messenger.ui.components.SettingsItem
import com.pulse.messenger.ui.components.SnackType
import com.pulse.messenger.ui.home.previewText
import com.pulse.messenger.ui.navigation.Routes
import com.pulse.messenger.ui.navigation.back
import com.pulse.messenger.ui.onboarding.AvatarPickerSheet
import com.pulse.messenger.ui.onboarding.countries
import com.pulse.messenger.ui.onboarding.CountryPickerSheet
import com.pulse.messenger.ui.theme.PulseTheme

@Composable
private fun ContactRow(c: Contact, selected: Boolean? = null, subtitle: String = c.about, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(c.avatar, c.name, 46.dp)
            if (selected == true) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, trailing: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
        if (trailing != null) Icon(trailing, null, tint = PulseTheme.colors.textSecondary)
    }
}

private fun sortedPeople(list: List<Contact>) = list.sortedBy { it.name.lowercase() }

@Composable
fun NewChatScreen(nav: NavController) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val people = remember(contacts, query) {
        sortedPeople(contacts).filter { query.isBlank() || it.name.contains(query, true) || it.phone.replace(" ", "").contains(query.replace(" ", "")) }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.statusBarsPadding()) {
            if (searching) {
                SearchTopBar(query, { query = it }, onClose = { searching = false; query = "" }, placeholder = "Search name or number…")
            } else {
                PulseTopBar("Select contact", subtitle = "${contacts.size} contacts", onBack = { nav.back() }, actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, "Search") }
                })
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (query.isBlank()) {
                item { ActionRow(Icons.Outlined.Groups, "New group") { nav.navigate(Routes.NEW_GROUP) } }
                item { ActionRow(Icons.Outlined.PersonAdd, "New contact", trailing = Icons.Outlined.QrCode) { nav.navigate(Routes.NEW_CONTACT) } }
                item { ActionRow(Icons.Outlined.Groups, "New community") { nav.navigate(Routes.NEW_COMMUNITY) } }
                item { ActionRow(Icons.Outlined.Campaign, "New broadcast") { nav.navigate(Routes.NEW_BROADCAST) } }
                item { SectionHeader("Contacts on Pulse") }
            }
            var lastLetter = ' '
            people.forEach { c ->
                val letter = c.name.first().uppercaseChar()
                if (query.isBlank() && letter != lastLetter) {
                    lastLetter = letter
                    item(key = "h_$letter") {
                        Text(letter.toString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 2.dp))
                    }
                }
                item(key = c.id) {
                    ContactRow(c) { nav.navigate(Routes.chat(repo.directChatFor(c.id).id)) { popUpTo(Routes.HOME) } }
                }
            }
            if (query.isNotBlank() && people.isEmpty()) {
                item { Text("No results found for \"$query\"", color = PulseTheme.colors.textSecondary, modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center) }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SettingsItem("Share invite link", icon = Icons.Outlined.Share) {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Let's chat on Pulse! It's fast, simple and secure. Download it at https://pulse.app/download")
                    }, "Invite a friend"))
                }
            }
        }
    }
}

/** Shared multi-select contact picker used by new group, broadcast, add members and forward. */
@Composable
private fun MultiPickScaffold(
    title: String,
    subtitle: String,
    people: List<Contact>,
    initial: List<String>,
    onBack: () -> Unit,
    fabIcon: ImageVector,
    minSelection: Int,
    onTooFew: () -> Unit,
    onDone: (List<String>) -> Unit,
    exclude: Set<String> = emptySet(),
) {
    var selected by rememberSaveable { mutableStateOf(initial) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(people, query) {
        sortedPeople(people).filter { it.id !in exclude && (query.isBlank() || it.name.contains(query, true)) }
    }
    val byId = remember(people) { people.associateBy { it.id } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(Modifier.statusBarsPadding()) {
                if (searching) SearchTopBar(query, { query = it }, onClose = { searching = false; query = "" })
                else PulseTopBar(title, subtitle = if (selected.isEmpty()) subtitle else "${selected.size} selected", onBack = onBack, actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, "Search") }
                })
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (selected.size < minSelection) onTooFew() else onDone(selected) },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp),
                modifier = Modifier.imePadding(),
            ) { Icon(fabIcon, "Next") }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            AnimatedVisibility(selected.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selected, key = { it }) { id ->
                        val c = byId[id] ?: return@items
                        Column(Modifier.width(64.dp).clickable { selected = selected - id }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Avatar(c.avatar, c.name, 52.dp)
                                Box(
                                    Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(PulseTheme.colors.textSecondary)
                                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(10.dp)) }
                            }
                            Text(c.name.substringBefore(" "), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(visible, key = { it.id }) { c ->
                    ContactRow(c, selected = c.id in selected) {
                        selected = if (c.id in selected) selected - c.id else selected + c.id
                    }
                }
            }
        }
    }
}

@Composable
fun NewGroupScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    MultiPickScaffold(
        title = "New group", subtitle = "Add members", people = contacts.filter { !it.isBusiness },
        initial = emptyList(), onBack = { nav.back() }, fabIcon = Icons.AutoMirrored.Filled.ArrowForward,
        minSelection = 1, onTooFew = { snackbar.show("At least 1 contact must be selected", SnackType.ERROR) },
        onDone = { repo.setNewGroupMembers(it); nav.navigate(Routes.NEW_GROUP_INFO) },
    )
}

private val timerOptions = listOf(0L, 24 * 3600L, 7 * 24 * 3600L, 90 * 24 * 3600L)

@Composable
fun NewGroupInfoScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val members by repo.newGroupMembers.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf<Int?>(null) }
    var picker by remember { mutableStateOf(false) }
    var timer by rememberSaveable { mutableStateOf(0L) }
    var timerDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Box(Modifier.statusBarsPadding()) { PulseTopBar("New group", onBack = { nav.back() }) } },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (name.isBlank()) {
                        snackbar.show("Provide a group name", SnackType.ERROR)
                    } else {
                        val chat = repo.createGroup(name.trim(), icon, members, timer)
                        snackbar.show("Group \"${chat.title}\" created", SnackType.SUCCESS)
                        nav.navigate(Routes.chat(chat.id)) { popUpTo(Routes.HOME) }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp),
                modifier = Modifier.imePadding(),
            ) { Icon(Icons.Filled.Check, "Create group") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(PulseTheme.colors.chip).clickable { picker = true },
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) Avatar(icon, name, 64.dp)
                    else Icon(Icons.Filled.CameraAlt, "Group icon", tint = PulseTheme.colors.textSecondary)
                }
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    name, { if (it.length <= 100) name = it },
                    placeholder = { Text("Group name (required)") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                )
            }
            SettingsItem("Disappearing messages", subtitle = PulseRepository.disappearingLabel(timer), icon = Icons.Outlined.Timer, trailing = {
                Icon(Icons.Outlined.ChevronRight, null, tint = PulseTheme.colors.textSecondary)
            }) { timerDialog = true }
            SectionHeader("Members: ${members.size}")
            members.forEach { id ->
                val c = repo.contact(id) ?: return@forEach
                ContactRow(c) {}
            }
            Spacer(Modifier.height(96.dp))
        }
    }
    if (picker) {
        AvatarPickerSheet(onPick = { icon = it }, onRemove = if (icon != null) ({ icon = null }) else null, onDismiss = { picker = false }, options = MockData.groupAvatars + MockData.photos, title = "Group icon")
    }
    if (timerDialog) {
        ChoiceDialog(
            title = "Disappearing messages", options = timerOptions, selected = timer,
            label = { PulseRepository.disappearingLabel(it) }, onSelect = { timer = it }, onDismiss = { timerDialog = false },
            description = "All new messages in this chat will disappear after the selected duration.",
        )
    }
}

@Composable
fun NewBroadcastScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    MultiPickScaffold(
        title = "New broadcast", subtitle = "Only contacts with your number in their address book will receive your broadcast messages.",
        people = contacts.filter { !it.isBusiness }, initial = emptyList(), onBack = { nav.back() },
        fabIcon = Icons.Filled.Check, minSelection = 2,
        onTooFew = { snackbar.show("At least 2 contacts must be selected", SnackType.ERROR) },
        onDone = {
            val chat = repo.createBroadcast(it)
            snackbar.show("Broadcast list created", SnackType.SUCCESS)
            nav.navigate(Routes.chat(chat.id)) { popUpTo(Routes.HOME) }
        },
    )
}

@Composable
fun AddMembersScreen(nav: NavController, chatId: String) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val contacts by repo.contacts.collectAsStateWithLifecycle()
    val chat = repo.chat(chatId)
    val existing = chat?.memberIds?.toSet().orEmpty()
    MultiPickScaffold(
        title = if (chat?.type == ChatType.BROADCAST) "Add recipients" else "Add members", subtitle = "${existing.size} members",
        people = contacts.filter { !it.isBusiness }, initial = emptyList(), onBack = { nav.back() },
        fabIcon = Icons.Filled.Check, minSelection = 1, exclude = existing,
        onTooFew = { snackbar.show("Select at least one contact", SnackType.ERROR) },
        onDone = { ids ->
            repo.addMembers(chatId, ids)
            snackbar.show(if (ids.size == 1) "${repo.displayName(ids.first())} added" else "${ids.size} members added", SnackType.SUCCESS)
            nav.back()
        },
    )
}

@Composable
fun NewContactScreen(nav: NavController) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var country by remember { mutableStateOf(countries.first { it.name == "United States" }) }
    var picker by remember { mutableStateOf(false) }
    val valid = first.isNotBlank() && phone.count { it.isDigit() } >= 6
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding().imePadding()) {
        Box(Modifier.statusBarsPadding()) { PulseTopBar("New contact", onBack = { nav.back() }) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
            OutlinedTextField(first, { first = it }, label = { Text("First name") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(last, { last = it }, label = { Text("Last name") }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { picker = true }.padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(country.flag); Spacer(Modifier.width(6.dp)); Text(country.code)
                }
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    phone, { v -> phone = v.filter { it.isDigit() || it == ' ' }.take(16) }, label = { Text("Phone") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp), colors = colors, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("This contact will be saved to your phone's address book.", style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary)
        }
        PrimaryButton("Save", enabled = valid, onClick = {
            val c = repo.addContact(listOf(first.trim(), last.trim()).filter { it.isNotEmpty() }.joinToString(" "), "${country.code} ${phone.trim()}")
            snackbar.show("${c.name} saved to contacts", SnackType.SUCCESS)
            nav.navigate(Routes.chat(repo.directChatFor(c.id).id)) { popUpTo(Routes.HOME) }
        }, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
    if (picker) CountryPickerSheet(onPick = { country = it }, onDismiss = { picker = false })
}

/** Forward messages (or send pending shared content when [sourceChatId] is "_share"). */
@Composable
fun ForwardScreen(nav: NavController, sourceChatId: String, messageIds: List<String>) {
    val repo = LocalRepo.current
    val snackbar = LocalSnackbar.current
    val chats by repo.chats.collectAsStateWithLifecycle()
    val pending by repo.pendingShare.collectAsStateWithLifecycle()
    val sharing = sourceChatId == "_share"
    val toForward = remember { if (sharing) emptyList() else messageIds.mapNotNull { repo.message(sourceChatId, it) } }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val targets = remember(chats, query) {
        chats.filter { (it.type != ChatType.GROUP || com.pulse.messenger.data.ME in it.memberIds) && (query.isBlank() || it.title.contains(query, true)) }
            .sortedByDescending { repo.messagesOf(it.id).lastOrNull()?.timestamp ?: 0L }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(Modifier.statusBarsPadding()) {
                if (searching) SearchTopBar(query, { query = it }, onClose = { searching = false; query = "" })
                else PulseTopBar(if (sharing) "Send to…" else "Forward to…", subtitle = if (selected.isNotEmpty()) "${selected.size} selected" else null, onBack = { nav.back() }, actions = {
                    IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, "Search") }
                })
            }
        },
        bottomBar = {
            AnimatedVisibility(selected.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        selected.joinToString(", ") { id -> chats.firstOrNull { it.id == id }?.title.orEmpty() },
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = PulseTheme.colors.textSecondary,
                    )
                    Spacer(Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (sharing) repo.sendPendingTo(selected) else repo.forward(toForward, selected)
                            val n = selected.size
                            snackbar.show(if (n == 1) "Sent" else "Sent to $n chats", SnackType.SUCCESS)
                            if (n == 1) nav.navigate(Routes.chat(selected.first())) { popUpTo(Routes.HOME) } else nav.back()
                        },
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = CircleShape,
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            val preview = if (sharing) pending.firstOrNull() else toForward.firstOrNull()?.content
            if (preview != null) {
                item {
                    Text(
                        (if (sharing) "Sharing: " else "Forwarding ${toForward.size} message${if (toForward.size > 1) "s" else ""}: ") + previewText(preview),
                        style = MaterialTheme.typography.bodySmall, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            item { SectionHeader("Recent chats") }
            items(targets, key = { it.id }) { chat ->
                val isSel = chat.id in selected
                Row(
                    Modifier.fillMaxWidth().clickable {
                        selected = when {
                            isSel -> selected - chat.id
                            selected.size >= 5 -> { snackbar.show("You can only share with up to 5 chats", SnackType.ERROR); selected }
                            else -> selected + chat.id
                        }
                    }.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Avatar(chat.avatar, chat.title, 46.dp, fallback = when (chat.type) {
                            ChatType.GROUP -> AvatarFallback.GROUP
                            ChatType.BROADCAST -> AvatarFallback.BROADCAST
                            else -> AvatarFallback.INITIALS
                        })
                        com.pulse.messenger.ui.components.Appear(isSel, enter = scaleIn(), exit = scaleOut(), modifier = Modifier.align(Alignment.BottomEnd)) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(chat.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                        val sub = when (chat.type) {
                            ChatType.DIRECT -> repo.contact(chat.contactId)?.about.orEmpty()
                            ChatType.GROUP -> "${chat.memberIds.size} members"
                            ChatType.BROADCAST -> "Broadcast list"
                        }
                        Text(sub, style = MaterialTheme.typography.bodyMedium, color = PulseTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
