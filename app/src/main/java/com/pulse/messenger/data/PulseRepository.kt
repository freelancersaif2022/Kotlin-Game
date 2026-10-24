package com.pulse.messenger.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * In-memory backend for the demo. All state is exposed as [StateFlow]s and every mutation
 * goes through this class, so the UI stays a pure function of the flows.
 */
class PulseRepository(
    private val scope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
) {
    private val _contacts = MutableStateFlow(MockData.contacts)
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _chats = MutableStateFlow(MockData.chats.toList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(MockData.messages.mapValues { it.value.toList() })
    val messages: StateFlow<Map<String, List<Message>>> = _messages.asStateFlow()

    private val _statuses = MutableStateFlow(MockData.statuses)
    val statuses: StateFlow<List<StatusUpdate>> = _statuses.asStateFlow()

    private val _myStatus = MutableStateFlow(MockData.myStatus)
    val myStatus: StateFlow<List<StatusItem>> = _myStatus.asStateFlow()

    private val _channels = MutableStateFlow(MockData.channels)
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _communities = MutableStateFlow(MockData.communities)
    val communities: StateFlow<List<Community>> = _communities.asStateFlow()

    private val _calls = MutableStateFlow(MockData.calls)
    val calls: StateFlow<List<CallLog>> = _calls.asStateFlow()

    private val _favoriteCalls = MutableStateFlow(MockData.favoriteCallIds)
    val favoriteCalls: StateFlow<List<String>> = _favoriteCalls.asStateFlow()

    private val _blocked = MutableStateFlow(MockData.blocked)
    val blocked: StateFlow<List<String>> = _blocked.asStateFlow()

    private val _linkedDevices = MutableStateFlow(MockData.linkedDevices)
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    /** Content waiting for a destination chat (camera capture, share from viewer…). */
    private val _pendingShare = MutableStateFlow<List<MessageContent>>(emptyList())
    val pendingShare: StateFlow<List<MessageContent>> = _pendingShare.asStateFlow()

    /** Members picked on the first step of "New group". */
    private val _newGroupMembers = MutableStateFlow<List<String>>(emptyList())
    val newGroupMembers: StateFlow<List<String>> = _newGroupMembers.asStateFlow()

    private var seq = 10_000
    private fun nextId(prefix: String = "x") = "$prefix${seq++}"
    private val replyJobs = mutableMapOf<String, Job>()

    // ---------------------------------------------------------------- lookups

    fun contact(id: String?): Contact? = id?.let { cid -> _contacts.value.firstOrNull { it.id == cid } }
    fun chat(id: String?): Chat? = id?.let { cid -> _chats.value.firstOrNull { it.id == cid } }
    fun messagesOf(chatId: String): List<Message> = _messages.value[chatId].orEmpty()
    fun message(chatId: String, id: String): Message? = messagesOf(chatId).firstOrNull { it.id == id }
    fun community(id: String?): Community? = id?.let { cid -> _communities.value.firstOrNull { it.id == cid } }
    fun channel(id: String): Channel? = _channels.value.firstOrNull { it.id == id }

    fun displayName(userId: String): String = when (userId) {
        ME -> "You"
        else -> contact(userId)?.name ?: "Unknown"
    }

    /** Returns the direct chat with [contactId], creating an empty one when needed. */
    fun directChatFor(contactId: String): Chat {
        _chats.value.firstOrNull { it.type == ChatType.DIRECT && it.contactId == contactId }?.let { return it }
        val c = contact(contactId) ?: error("Unknown contact $contactId")
        val chat = Chat(
            id = "chat_$contactId", type = ChatType.DIRECT, title = c.name, avatar = c.avatar,
            contactId = contactId, memberIds = listOf(ME, contactId),
            disappearingSeconds = settingsRepo.current.defaultTimer,
        )
        _chats.update { it + chat }
        _messages.update { it + (chat.id to emptyList()) }
        return chat
    }

    // ---------------------------------------------------------------- chats

    private fun updateChat(id: String, transform: (Chat) -> Chat) {
        _chats.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun updateChats(ids: Collection<String>, transform: (Chat) -> Chat) {
        _chats.update { list -> list.map { if (it.id in ids) transform(it) else it } }
    }

    fun setPinned(ids: Collection<String>, pinned: Boolean): Boolean {
        val already = _chats.value.count { it.isPinned && it.id !in ids }
        if (pinned && already + ids.size > 3) return false
        updateChats(ids) { it.copy(isPinned = pinned) }
        return true
    }

    fun setMuted(ids: Collection<String>, muted: Boolean) = updateChats(ids) { it.copy(isMuted = muted) }
    fun setArchived(ids: Collection<String>, archived: Boolean) =
        updateChats(ids) { it.copy(isArchived = archived, isPinned = if (archived) false else it.isPinned) }
    fun setFavorite(ids: Collection<String>, favorite: Boolean) = updateChats(ids) { it.copy(isFavorite = favorite) }
    fun setLocked(id: String, locked: Boolean) = updateChat(id) { it.copy(isLocked = locked) }
    fun setDisappearing(id: String, seconds: Long) {
        updateChat(id) { it.copy(disappearingSeconds = seconds) }
        val label = disappearingLabel(seconds)
        addSystem(id, if (seconds == 0L) "You turned off disappearing messages." else "You turned on disappearing messages. New messages will disappear from this chat $label after they're sent.")
    }
    fun setWallpaper(id: String, res: Int?) = updateChat(id) { it.copy(wallpaper = res) }
    fun setDraft(id: String, draft: String) {
        if (chat(id)?.draft != draft) updateChat(id) { it.copy(draft = draft) }
    }

    fun markRead(ids: Collection<String>) = updateChats(ids) { it.copy(unreadCount = 0, markedUnread = false) }
    fun markUnread(ids: Collection<String>) = updateChats(ids) { it.copy(markedUnread = true) }
    fun markAllRead() = _chats.update { list -> list.map { it.copy(unreadCount = 0, markedUnread = false) } }

    data class DeletedChats(val chats: List<Chat>, val messages: Map<String, List<Message>>)

    fun deleteChats(ids: Collection<String>): DeletedChats {
        val removed = _chats.value.filter { it.id in ids }
        val removedMessages = _messages.value.filterKeys { it in ids }
        _chats.update { list -> list.filterNot { it.id in ids } }
        _messages.update { it - ids.toSet() }
        return DeletedChats(removed, removedMessages)
    }

    fun restoreChats(deleted: DeletedChats) {
        _chats.update { it + deleted.chats }
        _messages.update { it + deleted.messages }
    }

    fun clearChat(id: String, keepStarred: Boolean) {
        _messages.update { map ->
            map + (id to map[id].orEmpty().filter { keepStarred && it.isStarred })
        }
    }

    fun exitGroup(id: String) {
        addSystem(id, "You left")
        updateChat(id) { it.copy(memberIds = it.memberIds - ME, adminIds = it.adminIds - ME) }
    }

    fun updateGroup(id: String, title: String? = null, description: String? = null, avatar: Int? = null) {
        val old = chat(id) ?: return
        updateChat(id) {
            it.copy(
                title = title ?: it.title,
                description = description ?: it.description,
                avatar = avatar ?: it.avatar,
            )
        }
        if (title != null && title != old.title) addSystem(id, "You changed the group name to \"$title\"")
        if (description != null && description != old.description) addSystem(id, "You changed the group description")
        if (avatar != null && avatar != old.avatar) addSystem(id, "You changed this group's icon")
    }

    fun addMembers(id: String, memberIds: List<String>) {
        if (memberIds.isEmpty()) return
        updateChat(id) { it.copy(memberIds = (it.memberIds + memberIds).distinct()) }
        addSystem(id, "You added ${memberIds.joinToString { displayName(it) }}")
    }

    fun removeMember(id: String, memberId: String) {
        updateChat(id) { it.copy(memberIds = it.memberIds - memberId, adminIds = it.adminIds - memberId) }
        addSystem(id, "You removed ${displayName(memberId)}")
    }

    fun toggleAdmin(id: String, memberId: String) {
        updateChat(id) {
            it.copy(adminIds = if (memberId in it.adminIds) it.adminIds - memberId else it.adminIds + memberId)
        }
    }

    fun setNewGroupMembers(ids: List<String>) {
        _newGroupMembers.value = ids
    }

    fun createGroup(title: String, avatar: Int?, members: List<String>, disappearing: Long = 0L): Chat {
        val chat = Chat(
            id = nextId("g_"), type = ChatType.GROUP, title = title, avatar = avatar,
            memberIds = listOf(ME) + members, adminIds = listOf(ME), createdAt = System.currentTimeMillis(),
            disappearingSeconds = disappearing,
        )
        _chats.update { it + chat }
        _messages.update { it + (chat.id to emptyList()) }
        addSystem(chat.id, "You created group \"$title\"")
        addSystem(chat.id, "You added ${members.joinToString { displayName(it) }}")
        return chat
    }

    fun createBroadcast(members: List<String>): Chat {
        val chat = Chat(
            id = nextId("bc_"), type = ChatType.BROADCAST, title = "${members.size} recipients", avatar = null,
            memberIds = listOf(ME) + members, createdAt = System.currentTimeMillis(),
        )
        _chats.update { it + chat }
        _messages.update { it + (chat.id to emptyList()) }
        addSystem(chat.id, "You created a broadcast list with ${members.size} recipients")
        return chat
    }

    fun createCommunity(name: String, description: String, avatar: Int): Community {
        val announcements = Chat(
            id = nextId("g_"), type = ChatType.GROUP, title = name, avatar = avatar,
            memberIds = listOf(ME), adminIds = listOf(ME), description = "Announcements",
            createdAt = System.currentTimeMillis(),
        )
        val community = Community(nextId("comm_"), name, avatar, description, emptyList(), announcements.id, 1)
        _chats.update { it + announcements.copy(communityId = community.id) }
        _messages.update { it + (announcements.id to emptyList()) }
        addSystem(announcements.id, "You created the community \"$name\"")
        _communities.update { listOf(community) + it }
        return community
    }

    fun addContact(name: String, phone: String): Contact {
        val c = Contact(nextId("c_"), name, phone, "Hey there! I am using Pulse.", null, lastSeen = System.currentTimeMillis())
        _contacts.update { it + c }
        return c
    }

    // ---------------------------------------------------------------- messages

    private fun addSystem(chatId: String, text: String) {
        val m = Message(nextId("m"), chatId, "system", MessageContent.System(text), System.currentTimeMillis())
        _messages.update { it + (chatId to it[chatId].orEmpty() + m) }
    }

    private fun updateMessage(chatId: String, id: String, transform: (Message) -> Message) {
        _messages.update { map ->
            val list = map[chatId] ?: return@update map
            map + (chatId to list.map { if (it.id == id) transform(it) else it })
        }
    }

    fun send(chatId: String, content: MessageContent, replyToId: String? = null, forwarded: Boolean = false): Message {
        val m = Message(
            id = nextId("m"), chatId = chatId, senderId = ME, content = content,
            timestamp = System.currentTimeMillis(), status = MessageStatus.SENDING,
            replyToId = replyToId, isForwarded = forwarded,
        )
        _messages.update { it + (chatId to it[chatId].orEmpty() + m) }
        updateChat(chatId) { it.copy(isArchived = if (settingsRepo.current.keepArchived) it.isArchived else false, draft = "") }
        simulateDelivery(chatId, m.id)
        return m
    }

    private fun simulateDelivery(chatId: String, messageId: String) {
        scope.launch {
            delay(450)
            updateMessage(chatId, messageId) { it.copy(status = MessageStatus.SENT) }
            delay(700)
            updateMessage(chatId, messageId) { it.copy(status = MessageStatus.DELIVERED) }
            val chat = chat(chatId) ?: return@launch
            if (chat.type == ChatType.BROADCAST) return@launch
            val peer = pickResponder(chat) ?: return@launch
            if (peer in _blocked.value) return@launch
            delay(1200L + Random.nextLong(1200))
            updateMessage(chatId, messageId) { it.copy(status = MessageStatus.READ) }
            scheduleReply(chatId, peer, messageId)
        }
    }

    private fun pickResponder(chat: Chat): String? = when (chat.type) {
        ChatType.DIRECT -> chat.contactId
        ChatType.GROUP -> chat.memberIds.filter { it != ME }.randomOrNull()
        ChatType.BROADCAST -> null
    }

    private fun scheduleReply(chatId: String, peer: String, toMessageId: String) {
        val contact = contact(peer)
        if (contact?.isBusiness == true && Random.nextBoolean()) return
        replyJobs[chatId]?.cancel()
        replyJobs[chatId] = scope.launch {
            delay(600)
            updateChat(chatId) { it.copy(typingUserId = peer) }
            delay(1500L + Random.nextLong(1800))
            updateChat(chatId) { it.copy(typingUserId = null) }
            val original = message(chatId, toMessageId)
            val text = (original?.content as? MessageContent.Text)?.text.orEmpty()
            val reply: MessageContent = when {
                original?.content is MessageContent.Image || original?.content is MessageContent.Video ->
                    MessageContent.Text(listOf("Wow 😍", "Love this!", "So nice!!", "Where is this?").random())
                original?.content is MessageContent.Voice ->
                    MessageContent.Text(listOf("Haha okay 😂", "Got it 👍", "Sure, will do").random())
                original?.content is MessageContent.Sticker ->
                    MessageContent.Sticker(MockData.stickerPacks.random().second.random())
                text.contains("?") -> MessageContent.Text(MockData.questionReplies.random())
                text.contains("hi", ignoreCase = true) || text.contains("hello", ignoreCase = true) ->
                    MessageContent.Text("Hey! 👋 How are you?")
                Random.nextInt(8) == 0 -> MessageContent.Sticker(MockData.stickerPacks.random().second.random())
                else -> MessageContent.Text(MockData.autoReplies.random())
            }
            val m = Message(nextId("m"), chatId, peer, reply, System.currentTimeMillis())
            _messages.update { it + (chatId to it[chatId].orEmpty() + m) }
            val chat = chat(chatId)
            if (chat != null && chat.id != activeChatId) {
                updateChat(chatId) { it.copy(unreadCount = it.unreadCount + 1) }
            }
            if (Random.nextInt(4) == 0) react(chatId, toMessageId, peer, listOf("❤️", "😂", "👍", "🔥").random())
        }
    }

    /** The chat currently on screen; incoming messages there don't count as unread. */
    @Volatile var activeChatId: String? = null

    fun edit(chatId: String, messageId: String, text: String) =
        updateMessage(chatId, messageId) { it.copy(content = MessageContent.Text(text), isEdited = true) }

    fun deleteForMe(chatId: String, ids: Collection<String>) {
        _messages.update { map -> map + (chatId to map[chatId].orEmpty().filterNot { it.id in ids }) }
    }

    fun deleteForEveryone(chatId: String, ids: Collection<String>) {
        _messages.update { map ->
            map + (chatId to map[chatId].orEmpty().map {
                if (it.id in ids) it.copy(content = MessageContent.Deleted, reactions = emptyMap(), isStarred = false, replyToId = null) else it
            })
        }
    }

    fun restoreMessages(chatId: String, removed: List<Message>) {
        _messages.update { map ->
            val ids = removed.map { it.id }.toSet()
            val merged = (map[chatId].orEmpty().filterNot { it.id in ids } + removed).sortedBy { it.timestamp }
            map + (chatId to merged)
        }
    }

    fun setStarred(chatId: String, ids: Collection<String>, starred: Boolean) {
        _messages.update { map ->
            map + (chatId to map[chatId].orEmpty().map { if (it.id in ids) it.copy(isStarred = starred) else it })
        }
    }

    fun react(chatId: String, messageId: String, userId: String, emoji: String?) {
        updateMessage(chatId, messageId) {
            val r = it.reactions.toMutableMap()
            if (emoji == null || r[userId] == emoji) r.remove(userId) else r[userId] = emoji
            it.copy(reactions = r)
        }
    }

    fun vote(chatId: String, messageId: String, optionIndex: Int) {
        updateMessage(chatId, messageId) { m ->
            val poll = m.content as? MessageContent.Poll ?: return@updateMessage m
            val options = poll.options.mapIndexed { i, o ->
                val has = ME in o.voterIds
                when {
                    i == optionIndex -> o.copy(voterIds = if (has) o.voterIds - ME else o.voterIds + ME)
                    !poll.allowMultiple -> o.copy(voterIds = o.voterIds - ME)
                    else -> o
                }
            }
            m.copy(content = poll.copy(options = options))
        }
    }

    fun forward(messages: List<Message>, chatIds: Collection<String>) {
        chatIds.forEach { chatId ->
            messages.forEach { send(chatId, it.content, forwarded = true) }
        }
    }

    fun starredMessages(): List<Message> =
        _messages.value.values.flatten().filter { it.isStarred }.sortedByDescending { it.timestamp }

    fun setPendingShare(content: List<MessageContent>) {
        _pendingShare.value = content
    }

    fun sendPendingTo(chatIds: Collection<String>) {
        val items = _pendingShare.value
        chatIds.forEach { id -> items.forEach { send(id, it) } }
        _pendingShare.value = emptyList()
    }

    // ---------------------------------------------------------------- contacts

    fun setBlocked(contactId: String, blocked: Boolean) {
        _blocked.update { if (blocked) (it + contactId).distinct() else it - contactId }
        _chats.value.firstOrNull { it.contactId == contactId }?.let {
            addSystem(it.id, if (blocked) "You blocked this contact. Tap to unblock." else "You unblocked this contact.")
        }
    }

    fun toggleFavoriteCall(contactId: String) {
        _favoriteCalls.update { if (contactId in it) it - contactId else it + contactId }
    }

    // ---------------------------------------------------------------- status

    fun markStatusViewed(contactId: String, viewedCount: Int) {
        _statuses.update { list ->
            list.map { if (it.contactId == contactId) it.copy(viewedCount = maxOf(it.viewedCount, viewedCount)) else it }
        }
    }

    fun setStatusMuted(contactId: String, muted: Boolean) {
        _statuses.update { list -> list.map { if (it.contactId == contactId) it.copy(isMuted = muted) else it } }
    }

    fun postStatus(item: StatusItem) {
        _myStatus.update { it + item.copy(id = nextId("my"), timestamp = System.currentTimeMillis()) }
        scope.launch {
            // A few contacts "view" the new status over time.
            val viewers = _contacts.value.filter { !it.isBusiness }.shuffled().take(8).map { it.id }
            viewers.forEachIndexed { i, v ->
                delay(2500L + i * 1800L)
                _myStatus.update { list ->
                    list.map { if (it.timestamp >= item.timestamp && v !in it.viewerIds) it.copy(viewerIds = it.viewerIds + v) else it }
                }
            }
        }
    }

    fun deleteMyStatus(id: String) = _myStatus.update { list -> list.filterNot { it.id == id } }

    // ---------------------------------------------------------------- channels

    fun setFollowing(channelId: String, following: Boolean) {
        _channels.update { list -> list.map { if (it.id == channelId) it.copy(isFollowing = following, unread = 0) else it } }
    }

    fun setChannelMuted(channelId: String, muted: Boolean) {
        _channels.update { list -> list.map { if (it.id == channelId) it.copy(isMuted = muted) else it } }
    }

    fun markChannelRead(channelId: String) {
        _channels.update { list -> list.map { if (it.id == channelId) it.copy(unread = 0) else it } }
    }

    fun reactToPost(channelId: String, postId: String, emoji: String?) {
        _channels.update { list ->
            list.map { ch ->
                if (ch.id != channelId) ch else ch.copy(posts = ch.posts.map { p ->
                    if (p.id != postId) p else {
                        val old = p.myReaction
                        val counts = p.reactions.toMap().toMutableMap()
                        if (old != null) counts[old] = (counts[old] ?: 1) - 1
                        val new = if (emoji == old) null else emoji
                        if (new != null) counts[new] = (counts[new] ?: 0) + 1
                        p.copy(myReaction = new, reactions = counts.filterValues { it > 0 }.toList().sortedByDescending { it.second })
                    }
                })
            }
        }
    }

    // ---------------------------------------------------------------- calls

    fun logCall(peerId: String, isGroup: Boolean, type: CallType, direction: CallDirection, durationSec: Int) {
        val log = CallLog(nextId("k"), peerId, isGroup, type, direction, System.currentTimeMillis(), durationSec)
        _calls.update { listOf(log) + it }
    }

    fun deleteCalls(ids: Collection<String>) = _calls.update { list -> list.filterNot { it.id in ids } }
    fun clearCallLog() = _calls.update { emptyList() }

    // ---------------------------------------------------------------- devices

    fun linkDevice(): LinkedDevice {
        val d = LinkedDevice(nextId("d"), listOf("Firefox on Linux", "Safari on macOS", "Edge on Windows", "Pulse for iPad").random(), "web", System.currentTimeMillis())
        _linkedDevices.update { it + d }
        return d
    }

    fun unlinkDevice(id: String) = _linkedDevices.update { list -> list.filterNot { it.id == id } }
    fun unlinkAll() = _linkedDevices.update { emptyList() }

    companion object {
        fun disappearingLabel(seconds: Long): String = when (seconds) {
            0L -> "Off"
            24 * 3600L -> "24 hours"
            7 * 24 * 3600L -> "7 days"
            90 * 24 * 3600L -> "90 days"
            else -> "${seconds / 3600} hours"
        }
    }
}
