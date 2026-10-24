package com.pulse.messenger.data

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

const val ME = "me"

@Immutable
data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val about: String,
    @DrawableRes val avatar: Int?,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val isBusiness: Boolean = false,
    val aboutUpdated: Long = 0L,
)

enum class ChatType { DIRECT, GROUP, BROADCAST }

@Immutable
data class Chat(
    val id: String,
    val type: ChatType,
    val title: String,
    @DrawableRes val avatar: Int?,
    val contactId: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val description: String = "",
    val createdAt: Long = 0L,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val unreadCount: Int = 0,
    val markedUnread: Boolean = false,
    val disappearingSeconds: Long = 0L,
    @DrawableRes val wallpaper: Int? = null,
    val typingUserId: String? = null,
    val recordingUserId: String? = null,
    val draft: String = "",
    val communityId: String? = null,
)

enum class MessageStatus { SENDING, SENT, DELIVERED, READ }

@Immutable
data class PollOption(val text: String, val voterIds: List<String> = emptyList())

@Immutable
sealed interface MessageContent {
    data class Text(val text: String) : MessageContent
    data class Image(@DrawableRes val res: Int, val caption: String = "") : MessageContent
    data class Video(@DrawableRes val thumb: Int, val durationSec: Int, val caption: String = "") : MessageContent
    data class Voice(val durationSec: Int, val waveform: List<Float>) : MessageContent
    data class Document(
        val name: String,
        val sizeLabel: String,
        val pages: Int,
        val extension: String,
        @DrawableRes val thumb: Int? = null,
    ) : MessageContent
    data class Location(
        @DrawableRes val map: Int,
        val title: String,
        val address: String,
        val isLive: Boolean = false,
    ) : MessageContent
    data class ContactCard(val name: String, val phone: String, @DrawableRes val avatar: Int?) : MessageContent
    data class Sticker(@DrawableRes val res: Int) : MessageContent
    data class Link(
        val text: String,
        val url: String,
        val title: String,
        val description: String,
        @DrawableRes val image: Int,
    ) : MessageContent
    data class Poll(val question: String, val options: List<PollOption>, val allowMultiple: Boolean) : MessageContent
    data object Deleted : MessageContent
    data class System(val text: String) : MessageContent
}

@Immutable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: MessageContent,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.READ,
    val replyToId: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val isStarred: Boolean = false,
    val isForwarded: Boolean = false,
    val isEdited: Boolean = false,
) {
    val isMine: Boolean get() = senderId == ME
}

@Immutable
data class StatusItem(
    val id: String,
    val timestamp: Long,
    @DrawableRes val image: Int? = null,
    val text: String = "",
    @DrawableRes val background: Int? = null,
    val caption: String = "",
    val viewerIds: List<String> = emptyList(),
)

@Immutable
data class StatusUpdate(
    val contactId: String,
    val items: List<StatusItem>,
    val viewedCount: Int = 0,
    val isMuted: Boolean = false,
) {
    val allViewed: Boolean get() = viewedCount >= items.size
    val latest: StatusItem get() = items.last()
}

@Immutable
data class ChannelPost(
    val id: String,
    val text: String,
    @DrawableRes val image: Int? = null,
    val timestamp: Long,
    val reactions: List<Pair<String, Int>> = emptyList(),
    val views: Int = 0,
    val myReaction: String? = null,
)

@Immutable
data class Channel(
    val id: String,
    val name: String,
    @DrawableRes val avatar: Int,
    val followers: Int,
    val isVerified: Boolean,
    val description: String,
    val isFollowing: Boolean,
    val isMuted: Boolean = false,
    val unread: Int = 0,
    val posts: List<ChannelPost> = emptyList(),
    val category: String = "",
)

@Immutable
data class Community(
    val id: String,
    val name: String,
    @DrawableRes val avatar: Int,
    val description: String,
    val groupIds: List<String>,
    val announcementChatId: String,
    val memberCount: Int,
)

enum class CallType { VOICE, VIDEO }
enum class CallDirection { INCOMING, OUTGOING, MISSED }

@Immutable
data class CallLog(
    val id: String,
    val peerId: String,
    val isGroup: Boolean,
    val type: CallType,
    val direction: CallDirection,
    val timestamp: Long,
    val durationSec: Int,
    val count: Int = 1,
)

@Immutable
data class LinkedDevice(val id: String, val name: String, val platform: String, val lastActive: Long)

@Immutable
data class DeviceDocument(val name: String, val sizeLabel: String, val pages: Int, val extension: String, @DrawableRes val thumb: Int?)

@Immutable
data class Place(val name: String, val address: String, val distance: String)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
