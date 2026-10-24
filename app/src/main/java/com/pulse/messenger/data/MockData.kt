package com.pulse.messenger.data

import com.pulse.messenger.R
import kotlin.random.Random

/** Seed data for the offline demo. Everything is fictional. */
object MockData {

    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val DAY = 24 * HOUR

    val now: Long = System.currentTimeMillis()

    fun waveform(seed: Int, bars: Int = 42): List<Float> {
        val r = Random(seed)
        return List(bars) { i ->
            val envelope = 0.45f + 0.55f * kotlin.math.sin(i / bars.toFloat() * Math.PI).toFloat()
            (0.18f + r.nextFloat() * 0.82f) * envelope
        }
    }

    val avatarPeople = listOf(
        R.drawable.avatar_person_01, R.drawable.avatar_person_02, R.drawable.avatar_person_03, R.drawable.avatar_person_04,
        R.drawable.avatar_person_05, R.drawable.avatar_person_06, R.drawable.avatar_person_07, R.drawable.avatar_person_08,
        R.drawable.avatar_person_09, R.drawable.avatar_person_10, R.drawable.avatar_person_11, R.drawable.avatar_person_12,
        R.drawable.avatar_person_13, R.drawable.avatar_person_14, R.drawable.avatar_person_15, R.drawable.avatar_person_16,
        R.drawable.avatar_person_17, R.drawable.avatar_person_18, R.drawable.avatar_person_19, R.drawable.avatar_person_20,
        R.drawable.avatar_person_21, R.drawable.avatar_person_22, R.drawable.avatar_person_23, R.drawable.avatar_person_24,
        R.drawable.avatar_person_25, R.drawable.avatar_person_26, R.drawable.avatar_person_27, R.drawable.avatar_person_28,
        R.drawable.avatar_person_29, R.drawable.avatar_person_30, R.drawable.avatar_person_31, R.drawable.avatar_person_32,
        R.drawable.avatar_person_33, R.drawable.avatar_person_34, R.drawable.avatar_person_35, R.drawable.avatar_person_36,
        R.drawable.avatar_person_37, R.drawable.avatar_person_38, R.drawable.avatar_person_39, R.drawable.avatar_person_40,
    )

    val groupAvatars = listOf(
        R.drawable.avatar_group_01, R.drawable.avatar_group_02, R.drawable.avatar_group_03, R.drawable.avatar_group_04,
        R.drawable.avatar_group_05, R.drawable.avatar_group_06, R.drawable.avatar_group_07, R.drawable.avatar_group_08,
        R.drawable.avatar_group_09, R.drawable.avatar_group_10, R.drawable.avatar_group_11, R.drawable.avatar_group_12,
    )

    val photos = listOf(
        R.drawable.media_photo_01, R.drawable.media_photo_02, R.drawable.media_photo_03, R.drawable.media_photo_04,
        R.drawable.media_photo_05, R.drawable.media_photo_06, R.drawable.media_photo_07, R.drawable.media_photo_08,
        R.drawable.media_photo_09, R.drawable.media_photo_10, R.drawable.media_photo_11, R.drawable.media_photo_12,
        R.drawable.media_photo_13, R.drawable.media_photo_14, R.drawable.media_photo_15, R.drawable.media_photo_16,
        R.drawable.media_photo_17, R.drawable.media_photo_18, R.drawable.media_photo_19, R.drawable.media_photo_20,
        R.drawable.media_photo_21, R.drawable.media_photo_22, R.drawable.media_photo_23, R.drawable.media_photo_24,
        R.drawable.media_photo_25, R.drawable.media_photo_26, R.drawable.media_photo_27, R.drawable.media_photo_28,
        R.drawable.media_photo_29, R.drawable.media_photo_30,
    )

    val videoThumbs = listOf(
        R.drawable.media_video_thumb_01, R.drawable.media_video_thumb_02, R.drawable.media_video_thumb_03,
        R.drawable.media_video_thumb_04, R.drawable.media_video_thumb_05, R.drawable.media_video_thumb_06,
        R.drawable.media_video_thumb_07, R.drawable.media_video_thumb_08, R.drawable.media_video_thumb_09,
        R.drawable.media_video_thumb_10,
    )

    val statusPhotos = listOf(
        R.drawable.status_01, R.drawable.status_02, R.drawable.status_03, R.drawable.status_04, R.drawable.status_05,
        R.drawable.status_06, R.drawable.status_07, R.drawable.status_08, R.drawable.status_09, R.drawable.status_10,
        R.drawable.status_11, R.drawable.status_12, R.drawable.status_13, R.drawable.status_14, R.drawable.status_15,
        R.drawable.status_16, R.drawable.status_17, R.drawable.status_18, R.drawable.status_19, R.drawable.status_20,
    )

    val statusBackgrounds = listOf(
        R.drawable.status_text_bg_01, R.drawable.status_text_bg_02, R.drawable.status_text_bg_03, R.drawable.status_text_bg_04,
        R.drawable.status_text_bg_05, R.drawable.status_text_bg_06, R.drawable.status_text_bg_07, R.drawable.status_text_bg_08,
    )

    val wallpapersBright = listOf(
        R.drawable.wp_bright_01, R.drawable.wp_bright_02, R.drawable.wp_bright_03,
        R.drawable.wp_bright_04, R.drawable.wp_bright_05, R.drawable.wp_bright_06,
    )
    val wallpapersDark = listOf(
        R.drawable.wp_dark_01, R.drawable.wp_dark_02, R.drawable.wp_dark_03,
        R.drawable.wp_dark_04, R.drawable.wp_dark_05, R.drawable.wp_dark_06,
    )
    val wallpapersPhoto = listOf(
        R.drawable.wp_photo_01, R.drawable.wp_photo_02, R.drawable.wp_photo_03,
        R.drawable.wp_photo_04, R.drawable.wp_photo_05, R.drawable.wp_photo_06,
    )

    val stickerPacks: List<Pair<String, List<Int>>> = listOf(
        "Pulse Buddy" to listOf(
            R.drawable.stk_buddy_01, R.drawable.stk_buddy_02, R.drawable.stk_buddy_03, R.drawable.stk_buddy_04,
            R.drawable.stk_buddy_05, R.drawable.stk_buddy_06, R.drawable.stk_buddy_07, R.drawable.stk_buddy_08,
            R.drawable.stk_buddy_09, R.drawable.stk_buddy_10, R.drawable.stk_buddy_11, R.drawable.stk_buddy_12,
            R.drawable.stk_buddy_13, R.drawable.stk_buddy_14, R.drawable.stk_buddy_15, R.drawable.stk_buddy_16,
        ),
        "Cozy Cats" to listOf(
            R.drawable.stk_cats_01, R.drawable.stk_cats_02, R.drawable.stk_cats_03, R.drawable.stk_cats_04,
            R.drawable.stk_cats_05, R.drawable.stk_cats_06, R.drawable.stk_cats_07, R.drawable.stk_cats_08,
            R.drawable.stk_cats_09, R.drawable.stk_cats_10, R.drawable.stk_cats_11, R.drawable.stk_cats_12,
            R.drawable.stk_cats_13, R.drawable.stk_cats_14, R.drawable.stk_cats_15, R.drawable.stk_cats_16,
        ),
    )
    val stickerTrays = listOf(R.drawable.tray_buddy, R.drawable.tray_cats)

    // ---------------------------------------------------------------- contacts

    private val people = listOf(
        "Nusrat Jahan" to "Dreamer ✨ | Dhaka",
        "Adaeze Okafor" to "Grateful for every sunrise 🌅",
        "Mei Lin Chen" to "Product designer · coffee first",
        "Layla Haddad" to "Available",
        "Rosa Martínez" to "Family above all ❤️",
        "Mom" to "Always here for you 💕",
        "Margaret Collins" to "Gardening, grandkids & good books",
        "Anh Nguyen" to "Exploring one city at a time 🌏",
        "Kayla Baptiste" to "Good vibes only ☀️",
        "Amira Benali" to "Architect · Casablanca → Paris",
        "Farzana Rahman" to "Busy",
        "Ngozi Eze" to "Living by faith 🙏",
        "Yuki Tanaka" to "Tea, haiku and long walks",
        "Huda Al-Mansour" to "At work",
        "Carmen López" to "La vida es bella 🌺",
        "Priya Patel" to "Code. Dance. Repeat. 💃",
        "Sophie Müller" to "Berlin · UX researcher",
        "Maria Santos" to "Mama of two little rockets 🚀",
        "Marcus Campbell" to "Coach · Sunday league legend ⚽",
        "Youssef Amrani" to "In a meeting",
        "Dad" to "Hey there! I am using Pulse.",
        "Emmanuel Mensah" to "Blessed and grateful",
        "Kenji Watanabe" to "Gamer · Night owl 🦉",
        "Omar Khalil" to "Building something new 🛠️",
        "Diego Hernández" to "Photographer · Mexico City 📷",
        "Rahul Verma" to "Engineering manager @ Nimbus",
        "Thomas Becker" to "Cyclist. Engineer. Dad.",
        "Somchai Wongsakul" to "Sawasdee krub 🙏",
        "Winston Clarke" to "Retired and loving it",
        "Karim Haddad" to "Urgent calls only",
        "Tanvir Hasan" to "Final year CSE 🎓",
        "Kwame Asante" to "Guitar · Accra 🎸",
        "Jason Park" to "Data nerd 📊",
        "Zaid Nasser" to "Can't talk, Pulse only",
        "Alejandro Ruiz" to "Chef at Spice Route Kitchen",
        "Ethan Brooks" to "Dog dad 🐶",
        "Lucas Ferreira" to "Mountains are calling ⛰️",
        "Hannah Lee" to "Florist · Seoul 🌸",
        "Ryan Mitchell" to "Classic cars & coffee",
        "Isabella Rossi" to "Beach please 🏖️",
    )

    val contacts: List<Contact> = buildList {
        people.forEachIndexed { i, (name, about) ->
            val id = "c%02d".format(i + 1)
            add(
                Contact(
                    id = id,
                    name = name,
                    phone = phoneFor(i),
                    about = about,
                    avatar = avatarPeople[i],
                    isOnline = i % 4 == 0,
                    lastSeen = now - (i * 37L % 600) * MIN - 3 * MIN,
                    aboutUpdated = now - (i * 13L % 300) * DAY,
                ),
            )
        }
        add(Contact("c41", "Alex Johnson", "+1 415 555 0142", "Available", null, lastSeen = now - 2 * HOUR))
        add(Contact("c42", "Daniel Kim", "+82 10 5521 7788", "Hey there! I am using Pulse.", null, lastSeen = now - 5 * DAY))
        add(Contact("c43", "Sarah Wilson", "+44 7700 900461", "Out for lunch 🥗", null, isOnline = true))
        add(Contact("b01", "Sweet Crumbs Bakery", "+1 212 555 0198", "Fresh every morning 🥐 · Open 7am–8pm", R.drawable.avatar_business_01, isBusiness = true))
        add(Contact("b02", "Glow Studio Salon", "+1 212 555 0133", "Book your glow-up ✨", R.drawable.avatar_business_02, isBusiness = true))
        add(Contact("b03", "CarePlus Pharmacy", "+1 212 555 0177", "24/7 delivery available", R.drawable.avatar_business_03, isBusiness = true))
        add(Contact("b04", "SwiftDrop Courier", "+1 212 555 0110", "Track your parcel anytime", R.drawable.avatar_business_04, isBusiness = true))
        add(Contact("b05", "VoltHub Electronics", "+1 212 555 0165", "Gadgets you'll love ⚡", R.drawable.avatar_business_05, isBusiness = true))
        add(Contact("b06", "Spice Route Kitchen", "+1 212 555 0121", "Order online · Free delivery over $25", R.drawable.avatar_business_06, isBusiness = true))
    }

    private fun phoneFor(i: Int): String {
        val codes = listOf("+880 17", "+234 80", "+86 138", "+971 50", "+52 55", "+91 98", "+44 77", "+84 90", "+1 876", "+212 66")
        val n = (i * 7919 + 1234567) % 9_000_000 + 1_000_000
        val s = n.toString()
        return "${codes[i % codes.size]}${s.substring(0, 2)} ${s.substring(2, 5)} ${s.substring(5)}"
    }

    // ------------------------------------------------------------------- chats

    val chats = mutableListOf<Chat>()
    val messages = mutableMapOf<String, MutableList<Message>>()
    private var msgSeq = 0

    private class Conv(val chatId: String) {
        val list = mutableListOf<Message>()
        private fun add(sender: String, content: MessageContent, minutesAgo: Long, extra: (Message) -> Message = { it }) {
            msgSeq++
            list += extra(
                Message(
                    id = "m$msgSeq",
                    chatId = chatId,
                    senderId = sender,
                    content = content,
                    timestamp = now - minutesAgo * MIN,
                    status = MessageStatus.READ,
                ),
            )
        }

        fun me(text: String, ago: Long, status: MessageStatus = MessageStatus.READ, reply: Int? = null, react: Map<String, String> = emptyMap()) =
            add(ME, MessageContent.Text(text), ago) {
                it.copy(status = status, replyToId = reply?.let { idx -> list[idx].id }, reactions = react)
            }

        fun them(from: String, text: String, ago: Long, reply: Int? = null, react: Map<String, String> = emptyMap(), starred: Boolean = false) =
            add(from, MessageContent.Text(text), ago) {
                it.copy(replyToId = reply?.let { idx -> list[idx].id }, reactions = react, isStarred = starred)
            }

        fun content(from: String, c: MessageContent, ago: Long, react: Map<String, String> = emptyMap(), forwarded: Boolean = false, starred: Boolean = false) =
            add(from, c, ago) { it.copy(reactions = react, isForwarded = forwarded, isStarred = starred) }

        fun system(text: String, ago: Long) = add("system", MessageContent.System(text), ago)
    }

    private fun conv(chatId: String, block: Conv.() -> Unit) {
        val c = Conv(chatId).apply(block)
        messages[chatId] = c.list
    }

    private fun direct(contactId: String, unread: Int = 0, pinned: Boolean = false, muted: Boolean = false, archived: Boolean = false, favorite: Boolean = false) {
        val contact = contacts.first { it.id == contactId }
        chats += Chat(
            id = "chat_$contactId",
            type = ChatType.DIRECT,
            title = contact.name,
            avatar = contact.avatar,
            contactId = contactId,
            memberIds = listOf(ME, contactId),
            unreadCount = unread,
            isPinned = pinned,
            isMuted = muted,
            isArchived = archived,
            isFavorite = favorite,
        )
    }

    private fun group(
        id: String, title: String, avatar: Int, members: List<String>, description: String,
        unread: Int = 0, pinned: Boolean = false, muted: Boolean = false, archived: Boolean = false,
        favorite: Boolean = false, admins: List<String> = listOf(ME), community: String? = null,
    ) {
        chats += Chat(
            id = id, type = ChatType.GROUP, title = title, avatar = avatar,
            memberIds = listOf(ME) + members, adminIds = admins, description = description,
            createdAt = now - 200 * DAY, unreadCount = unread, isPinned = pinned, isMuted = muted,
            isArchived = archived, isFavorite = favorite, communityId = community,
        )
    }

    init {
        buildChats()
    }

    private fun buildChats() {
        // ---- Mom
        direct("c06", unread = 2, pinned = true, favorite = true)
        conv("chat_c06") {
            them("c06", "Good morning beta ☀️ Did you eat breakfast?", 60 * 26)
            me("Morning Mom! Yes, had oats and fruit 😄", 60 * 26 - 5)
            them("c06", "Good. Don't skip meals when you're busy at work", 60 * 26 - 7)
            content("c06", MessageContent.Image(R.drawable.media_photo_01, "Made your favourite curry today 😋"), 60 * 25, react = mapOf(ME to "😍"))
            me("Omg that looks amazing!! Save some for the weekend please 🙏", 60 * 25 - 2)
            content("c06", MessageContent.Voice(34, waveform(6)), 60 * 24)
            me("Haha okay okay, I'll call Aunty tonight", 60 * 24 - 4)
            them("c06", "Are you coming home on Saturday?", 42)
            them("c06", "Dad wants to take everyone to the lake 🚣", 41)
        }

        // ---- Design team group
        group(
            "g_design", "Design Team", R.drawable.avatar_group_02, listOf("c03", "c17", "c24", "c26", "c33"),
            "Pulse product design · Daily standup 10:00 · Figma links only please 🙏",
            unread = 5, pinned = true, admins = listOf(ME, "c26"),
        )
        conv("g_design") {
            system("Rahul Verma created group \"Design Team\"", 60 * 24 * 120)
            them("c26", "Morning all! Standup in 10 ⏰", 60 * 5)
            them("c03", "I've pushed the new onboarding flows to the file", 60 * 5 - 3)
            content("c03", MessageContent.Link(
                "Here's the prototype: https://proto.pulse.app/onboarding-v3",
                "https://proto.pulse.app/onboarding-v3", "Onboarding v3 · Prototype",
                "Interactive prototype · 14 screens · Updated 2 hours ago", R.drawable.media_link_preview_05,
            ), 60 * 5 - 4)
            me("Looks super clean! Love the new illustration style ✨", 60 * 5 - 10, reply = 2)
            them("c17", "Usability results from yesterday are in. 8/10 completed signup without help 🎉", 60 * 3, react = mapOf("c26" to "🎉", ME to "🔥", "c03" to "🎉"))
            content("c17", MessageContent.Document("Usability_Report_Oct.pdf", "2.4 MB", 18, "PDF", R.drawable.media_doc_thumb_03), 60 * 3 - 1)
            them("c24", "Nice! Can we look at the OTP screen drop-off later today?", 60 * 2)
            content("c33", MessageContent.Image(R.drawable.media_photo_25, "Whiteboard from the workshop"), 60 * 2 - 30)
            them("c26", "Let's sync at 3pm. I'll send an invite", 75)
            them("c33", "Works for me 👍", 70)
            them("c03", "Same here", 68)
            them("c24", "@You can you bring the latest metrics?", 12)
        }

        // ---- Priya
        direct("c16", unread = 0, favorite = true)
        conv("chat_c16") {
            them("c16", "Are we still on for dance class tonight? 💃", 60 * 30)
            me("Yesss! 7pm right?", 60 * 30 - 3)
            them("c16", "7:30 actually, they moved it", 60 * 30 - 4)
            me("Perfect, more time to grab food before 😂", 60 * 30 - 6)
            content("c16", MessageContent.Sticker(R.drawable.stk_buddy_03), 60 * 30 - 7)
            content(ME, MessageContent.Location(R.drawable.media_map_preview_02, "Groove Dance Studio", "221 Rhythm Ave, Midtown"), 60 * 29)
            them("c16", "See you there!", 60 * 29 - 2)
            me("Last night was so much fun!!", 95)
            them("c16", "Right?? My legs are dead today 😭", 90)
            content("c16", MessageContent.Video(R.drawable.media_video_thumb_06, 47, "The routine, for practice 😅"), 88)
            me("Saving this! Thanks 🙌", 30, status = MessageStatus.READ)
        }

        // ---- Family group
        group(
            "g_family", "Family ❤️", R.drawable.avatar_group_01, listOf("c06", "c21", "c11", "c31", "c01"),
            "Our little family corner 🏡", unread = 3, favorite = true, admins = listOf("c21", ME),
        )
        conv("g_family") {
            system("Dad created group \"Family ❤️\"", 60 * 24 * 400)
            them("c21", "Weekend plan: lake trip on Saturday, leaving at 8 AM sharp ⏰", 60 * 8)
            them("c01", "Can we stop for ice cream on the way? 🍦", 60 * 8 - 2, react = mapOf("c06" to "😂", "c31" to "👍"))
            them("c31", "I'm in! I'll bring the speaker 🎶", 60 * 7)
            content("c11", MessageContent.Poll(
                "What should we have for lunch at the lake?",
                listOf(
                    PollOption("BBQ 🍖", listOf("c21", "c31")),
                    PollOption("Sandwiches 🥪", listOf("c06")),
                    PollOption("Biryani from home 🍛", listOf("c01", "c11")),
                ),
                allowMultiple = false,
            ), 60 * 6)
            content("c21", MessageContent.Image(R.drawable.media_photo_14), 60 * 5)
            them("c21", "This is the spot 😍", 60 * 5 - 1)
            them("c06", "Beautiful! Everyone pack warm clothes please", 55)
            them("c01", "Yes Mom 🙄😂", 50)
        }

        // ---- Marcus (football coach)
        direct("c19", unread = 1)
        conv("chat_c19") {
            them("c19", "Yo! You playing Sunday?", 60 * 50)
            me("Wouldn't miss it ⚽", 60 * 50 - 10)
            them("c19", "Great, we need a solid midfielder. Bring the blue kit", 60 * 49)
            content("c19", MessageContent.Video(R.drawable.media_video_thumb_10, 23, "Highlights from last week 🔥"), 60 * 22)
            me("That last goal though 😂🔥", 60 * 22 - 5)
            them("c19", "Warm-up starts 8:30. Don't be late this time 😅", 18)
        }

        // ---- Sunday football group (muted)
        group(
            "g_football", "Sunday Football ⚽", R.drawable.avatar_group_03, listOf("c19", "c23", "c25", "c32", "c27", "c20", "c41"),
            "Every Sunday 9AM · Riverside Park pitch 3 · £5 per game", unread = 24, muted = true, admins = listOf("c19"),
        )
        conv("g_football") {
            them("c19", "Teams for Sunday 👇", 60 * 3)
            them("c19", "🔵 Blue: Diego, Kwame, Thomas, You\n⚪ White: Kenji, Youssef, Alex, Marcus", 60 * 3 - 1)
            them("c25", "Blue team is stacked 😤", 60 * 3 - 5)
            them("c32", "Bring it on 😂", 60 * 3 - 6)
            content("c23", MessageContent.Sticker(R.drawable.stk_cats_10), 60 * 2)
            them("c27", "Forecast says light rain 🌧️", 60 * 2 - 20)
            them("c20", "Rain makes it more fun!", 60 * 2 - 25)
            them("c41", "Who's got the extra ball?", 22)
        }

        // ---- Mei Lin
        direct("c03")
        conv("chat_c03") {
            them("c03", "Hey! Quick one, do you have the brand colour tokens?", 60 * 28)
            me("Sure! Primary #0E9F8A, Accent #34D1B6, Amber #FFB547", 60 * 28 - 3)
            them("c03", "You're a lifesaver 🙏", 60 * 28 - 4, react = mapOf(ME to "❤️"))
            content(ME, MessageContent.Document("Pulse_Brand_Guidelines.pdf", "5.8 MB", 32, "PDF", R.drawable.media_doc_thumb_04), 60 * 28 - 5)
            them("c03", "Coffee tomorrow? ☕", 60 * 4)
            me("Always yes to coffee", 60 * 4 - 2, status = MessageStatus.READ)
            content("c03", MessageContent.Image(R.drawable.media_photo_28), 60 * 3 + 20)
            them("c03", "Found this place near the office, latte art game strong", 60 * 3 + 19)
        }

        // ---- Bali trip
        group(
            "g_bali", "Bali Trip 2026 ✈️", R.drawable.avatar_group_05, listOf("c08", "c09", "c36", "c40", "c16"),
            "Dec 12 – Dec 20 · Budget sheet pinned · No work talk 🌴", unread = 0, admins = listOf(ME, "c08"),
        )
        conv("g_bali") {
            them("c08", "Flights booked!! ✈️✈️✈️", 60 * 24 * 3, react = mapOf(ME to "🎉", "c09" to "🎉", "c36" to "🔥", "c40" to "😍"))
            content("c08", MessageContent.Document("Bali_Itinerary_Final.pdf", "1.1 MB", 6, "PDF", R.drawable.media_doc_thumb_03), 60 * 24 * 3 - 2)
            content("c40", MessageContent.Image(R.drawable.media_photo_13, "Our villa is literally on this beach 🏖️"), 60 * 24 * 2)
            them("c09", "I can't wait 😭😭", 60 * 24 * 2 - 3)
            content("c36", MessageContent.Link(
                "Must do: https://wanderlust.example/bali-hidden-gems",
                "https://wanderlust.example/bali-hidden-gems", "12 Hidden Gems in Bali Locals Love",
                "Waterfalls, rice terraces and secret beaches away from the crowds.", R.drawable.media_link_preview_03,
            ), 60 * 24)
            me("Adding all of these to the list 📝", 60 * 23)
            them("c16", "Someone please remind me to renew my passport 🙈", 60 * 6)
            me("Reminder set for you 😂", 60 * 6 - 3, status = MessageStatus.READ)
        }

        // ---- Business: bakery
        direct("b01")
        conv("chat_b01") {
            system("This business uses a secure service from Pulse to manage this chat.", 60 * 24 * 6)
            me("Hi! Do you have the berry cake available for Saturday?", 60 * 24 * 5)
            them("b01", "Hello! 👋 Yes, we do. Would you like a 1kg or 2kg cake?", 60 * 24 * 5 - 4)
            me("1kg please, no writing on top", 60 * 24 * 5 - 8)
            content("b01", MessageContent.Image(R.drawable.media_photo_23, "Here's how it'll look 🎂"), 60 * 24 * 5 - 10)
            them("b01", "Your order #BK-2291 is confirmed. Pickup Saturday after 10am. Thank you! 🥐", 60 * 24 * 5 - 11)
            them("b01", "Reminder: your berry cake is ready for pickup today 😊", 60 * 9)
        }

        // ---- Kwame
        direct("c32")
        conv("chat_c32") {
            content("c32", MessageContent.Voice(18, waveform(32)), 60 * 12)
            me("Haha that riff is fire 🔥 send me the full recording", 60 * 12 - 6)
            content("c32", MessageContent.Video(R.drawable.media_video_thumb_06, 84), 60 * 11)
            them("c32", "Band practice Thursday?", 60 * 11 - 1)
            me("Thursday works", 60 * 11 - 8, status = MessageStatus.DELIVERED)
        }

        // ---- The band
        group(
            "g_band", "The Band 🎸", R.drawable.avatar_group_12, listOf("c32", "c25", "c23", "c38"),
            "Practice Thu 7pm · Gig at The Loft on the 30th", admins = listOf("c32"),
        )
        conv("g_band") {
            them("c32", "Setlist for the Loft gig 👇", 60 * 24 * 2)
            them("c32", "1. Midnight Drive\n2. Paper Moons\n3. Golden Hour\n4. Static Hearts\n5. Encore: Northern Lights", 60 * 24 * 2 - 1, starred = true)
            them("c38", "Love it! Can we swap 2 and 3?", 60 * 24 * 2 - 30)
            content("c25", MessageContent.Image(R.drawable.media_photo_12, "Last gig's crowd 🤘"), 60 * 24)
            them("c23", "We need new picks lol, I lost all mine", 60 * 20)
        }

        // ---- Omar
        direct("c24")
        conv("chat_c24") {
            them("c24", "Bro the MVP is live 🚀🚀", 60 * 24 * 2)
            me("LET'S GOOO 🎉 congrats man!!", 60 * 24 * 2 - 1)
            content("c24", MessageContent.Link(
                "Check it out: https://getnimbus.example",
                "https://getnimbus.example", "Nimbus — Plan your week in seconds",
                "The smart planner that learns how you work. Free for early users.", R.drawable.media_link_preview_01,
            ), 60 * 24 * 2 - 2)
            me("Signing up right now", 60 * 24 * 2 - 3)
            them("c24", "Feedback welcome, be brutal 😅", 60 * 24 * 2 - 5)
        }

        // ---- College squad
        group(
            "g_college", "College Squad 🎓", R.drawable.avatar_group_04, listOf("c31", "c23", "c01", "c16", "c42"),
            "Class of 2022 · Reunion planning", unread = 0, admins = listOf("c31"),
        )
        conv("g_college") {
            them("c31", "Reunion date poll!! Vote now 🗳️", 60 * 24 * 4)
            content("c31", MessageContent.Poll(
                "Which weekend works for the reunion?",
                listOf(
                    PollOption("Nov 15–16", listOf("c23", "c01", ME)),
                    PollOption("Nov 22–23", listOf("c16")),
                    PollOption("Dec 6–7", listOf("c42")),
                ),
                allowMultiple = true,
            ), 60 * 24 * 4 - 1)
            content("c01", MessageContent.Image(R.drawable.media_photo_22, "Throwback 😂"), 60 * 24 * 3, forwarded = true)
            them("c23", "We look like babies", 60 * 24 * 3 - 2)
            them("c42", "Who took this lol", 60 * 24 * 3 - 4)
        }

        // ---- Layla
        direct("c04")
        conv("chat_c04") {
            them("c04", "Thank you so much for the recommendation, the doctor was wonderful 🙏", 60 * 24 * 3)
            me("So glad to hear that! Hope you feel better soon 💐", 60 * 24 * 3 - 15)
            them("c04", "Already much better 😊", 60 * 24 * 3 - 20)
        }

        // ---- Courier business
        direct("b04", unread = 1)
        conv("chat_b04") {
            system("This business uses a secure service from Pulse to manage this chat.", 60 * 24 * 2)
            them("b04", "📦 Your parcel SD-88412 is out for delivery. Estimated arrival: 2:00 PM – 4:00 PM.", 60 * 2)
            content("b04", MessageContent.Location(R.drawable.media_map_preview_01, "Live courier location", "Updated 2 min ago", isLive = true), 60 * 2 - 1)
        }

        // ---- Thomas
        direct("c27")
        conv("chat_c27") {
            them("c27", "Ride this Saturday? 60km loop by the river 🚴", 60 * 24 * 6)
            me("Count me in, but go easy on me 😅", 60 * 24 * 6 - 10)
            content("c27", MessageContent.Video(R.drawable.media_video_thumb_07, 32), 60 * 24 * 5)
            them("c27", "New personal best!", 60 * 24 * 5 - 1, react = mapOf(ME to "👏"))
        }

        // ---- Gym buddies
        group(
            "g_gym", "Gym Buddies 💪", R.drawable.avatar_group_06, listOf("c12", "c20", "c35", "c09"),
            "5:30 AM club. No excuses.", archived = false, muted = false, admins = listOf("c35"),
        )
        conv("g_gym") {
            them("c35", "Leg day tomorrow 😈", 60 * 24 * 2)
            them("c09", "Noooo 😭", 60 * 24 * 2 - 1)
            content("c12", MessageContent.Sticker(R.drawable.stk_buddy_10), 60 * 24 * 2 - 2)
            them("c20", "See you at 5:30 sharp", 60 * 24 * 2 - 5)
        }

        // ---- Book club
        group(
            "g_books", "Book Club 📚", R.drawable.avatar_group_07, listOf("c07", "c13", "c17", "c38", "c05"),
            "One book a month · Meet last Friday at Margaret's", admins = listOf("c07"),
        )
        conv("g_books") {
            them("c07", "This month's pick: 'The Quiet Orchard' 🍎", 60 * 24 * 8)
            content("c13", MessageContent.Image(R.drawable.media_photo_10), 60 * 24 * 7)
            them("c13", "Picked up my copy!", 60 * 24 * 7 - 1)
            them("c17", "Halfway through, no spoilers please 🙈", 60 * 24 * 6)
        }

        // ---- Diego
        direct("c25")
        conv("chat_c25") {
            content("c25", MessageContent.Image(R.drawable.media_photo_04, "Golden hour at the lake 🌅"), 60 * 24 * 7)
            content("c25", MessageContent.Image(R.drawable.media_photo_21), 60 * 24 * 7 - 1)
            content("c25", MessageContent.Image(R.drawable.media_photo_15), 60 * 24 * 7 - 2)
            me("These are stunning! Which lens?", 60 * 24 * 7 - 10)
            them("c25", "35mm f/1.4, my favourite", 60 * 24 * 7 - 12)
        }

        // ---- Pharmacy
        direct("b03")
        conv("chat_b03") {
            system("This business uses a secure service from Pulse to manage this chat.", 60 * 24 * 10)
            me("Hi, is my prescription ready?", 60 * 24 * 9)
            them("b03", "Hello! Yes, your order is ready. Would you like home delivery?", 60 * 24 * 9 - 5)
            me("Yes please", 60 * 24 * 9 - 6)
            them("b03", "Delivered ✅ Thank you for choosing CarePlus 💚", 60 * 24 * 8)
        }

        // ---- Archived examples
        direct("c29", archived = true)
        conv("chat_c29") {
            them("c29", "Happy birthday young man! 🎂🎉", 60 * 24 * 40)
            me("Thank you Uncle Winston! 😊", 60 * 24 * 40 - 30)
        }
        group(
            "g_wedding", "Wedding Planning 💍", R.drawable.avatar_group_09, listOf("c05", "c15", "c18", "c09"),
            "Sofia & Leo · June 14", archived = true, admins = listOf("c05"),
        )
        conv("g_wedding") {
            them("c05", "Florist confirmed! 💐", 60 * 24 * 20)
            content("c18", MessageContent.Image(R.drawable.media_photo_06), 60 * 24 * 19)
            them("c15", "Qué bonito 😍", 60 * 24 * 19 - 3)
        }
        group(
            "g_gaming", "Gaming Squad 🎮", R.drawable.avatar_group_11, listOf("c23", "c31", "c33", "c42"),
            "Friday night raids", archived = true, unread = 4, admins = listOf("c23"),
        )
        conv("g_gaming") {
            them("c23", "Raid tonight 10pm?", 60 * 24)
            them("c33", "I'm in", 60 * 24 - 2)
            them("c42", "Same", 60 * 24 - 3)
            them("c31", "Bringing snacks 🍕", 60 * 24 - 10)
        }

        // ---- Community groups
        group(
            "g_neighbours", "Maple Street Neighbours", R.drawable.avatar_group_10, listOf("c07", "c29", "c22", "c14", "c28"),
            "Friendly neighbourhood updates 🏘️", admins = listOf("c07"), community = "comm_maple",
        )
        conv("g_neighbours") {
            them("c22", "Has anyone seen a grey cat? She's been in my garden all morning 🐱", 60 * 24 * 2)
            content("c22", MessageContent.Image(R.drawable.media_photo_02), 60 * 24 * 2 - 1)
            them("c14", "That's Luna from number 14! I'll let them know", 60 * 24 * 2 - 20)
            them("c07", "Street cleanup this Sunday 10am, volunteers welcome 🌿", 60 * 10)
        }
        group(
            "g_maple_ann", "Maple Heights Residents", R.drawable.avatar_community_02, listOf("c07", "c29", "c22", "c14", "c28", "c02"),
            "Announcements", admins = listOf("c07"), community = "comm_maple",
        )
        conv("g_maple_ann") {
            them("c07", "📢 Water maintenance on Thursday 9am–1pm. Please store water in advance.", 60 * 30)
            them("c07", "📢 Annual residents meeting moved to Nov 12, 6pm in the clubhouse.", 60 * 5)
        }
        group(
            "g_school_ann", "Greenwood School Parents", R.drawable.avatar_community_01, listOf("c18", "c10", "c05", "c26", "c11"),
            "Announcements", admins = listOf("c10"), community = "comm_school", unread = 1,
        )
        conv("g_school_ann") {
            them("c10", "📢 Sports Day is on Friday! Kids should wear house colours.", 60 * 6)
        }
        group(
            "g_class5b", "Class 5B Parents", R.drawable.avatar_group_08, listOf("c18", "c10", "c05", "c26"),
            "Class 5B · Ms. Clarke", admins = listOf("c10"), community = "comm_school",
        )
        conv("g_class5b") {
            them("c18", "Does anyone know if the science project is due Monday or Wednesday?", 60 * 20)
            them("c26", "Wednesday! Teacher confirmed in the diary", 60 * 19)
            content("c05", MessageContent.Document("Science_Project_Guidelines.docx", "84 KB", 2, "DOCX", R.drawable.media_doc_thumb_01), 60 * 18)
        }
        group(
            "g_founders_ann", "Founders Circle", R.drawable.avatar_community_03, listOf("c24", "c26", "c33", "c10"),
            "Announcements", admins = listOf("c24"), community = "comm_founders",
        )
        conv("g_founders_ann") {
            them("c24", "📢 Demo night next Thursday. 6 slots left, reply to grab one!", 60 * 24 * 2)
        }
        group(
            "g_runners_ann", "City Runners Club", R.drawable.avatar_community_04, listOf("c27", "c35", "c09", "c20"),
            "Announcements", admins = listOf("c27"), community = "comm_runners",
        )
        conv("g_runners_ann") {
            them("c27", "📢 Saturday 10K starts at the north gate, 7:00 AM. Hydrate! 💧", 60 * 24)
        }

        // ---- Broadcast list
        chats += Chat(
            id = "bc_1", type = ChatType.BROADCAST, title = "Festival wishes", avatar = null,
            memberIds = listOf(ME, "c02", "c07", "c14", "c22", "c29"), createdAt = now - 90 * DAY,
        )
        conv("bc_1") {
            me("Wishing you and your family a joyful festival season! 🪔✨", 60 * 24 * 12)
        }

        // ---- Unknown-ish quiet chats with no avatar
        direct("c43")
        conv("chat_c43") {
            them("c43", "Hi! This is Sarah from the conference. Great meeting you 😊", 60 * 24 * 15)
            me("Hey Sarah! Likewise, let's keep in touch", 60 * 24 * 15 - 60)
        }
        direct("c41")
        conv("chat_c41") {
            me("Did you get the keys?", 60 * 24 * 9)
            them("c41", "Yep, all good 👍", 60 * 24 * 9 - 2)
        }
    }

    // ---------------------------------------------------------------- statuses

    val statuses: List<StatusUpdate> = listOf(
        StatusUpdate("c08", listOf(
            StatusItem("s1", now - 25 * MIN, image = R.drawable.status_01, caption = "Views for days 🏔️"),
            StatusItem("s2", now - 20 * MIN, image = R.drawable.status_10),
        )),
        StatusUpdate("c16", listOf(
            StatusItem("s3", now - 55 * MIN, text = "Dance recital this Friday! Come support 💃✨", background = R.drawable.status_text_bg_01),
        )),
        StatusUpdate("c25", listOf(
            StatusItem("s4", now - 2 * HOUR, image = R.drawable.status_08, caption = "City lights 🌃"),
            StatusItem("s5", now - 90 * MIN, image = R.drawable.status_17),
            StatusItem("s6", now - 80 * MIN, image = R.drawable.status_19, caption = "Morning walk"),
        )),
        StatusUpdate("c36", listOf(
            StatusItem("s7", now - 3 * HOUR, image = R.drawable.status_13, caption = "Beach day with Max 🐶"),
        )),
        StatusUpdate("c18", listOf(
            StatusItem("s8", now - 4 * HOUR, image = R.drawable.status_15, caption = "Happy 6th birthday my love 🎂"),
            StatusItem("s9", now - 4 * HOUR + 5 * MIN, image = R.drawable.status_06),
        )),
        StatusUpdate("c12", listOf(
            StatusItem("s10", now - 5 * HOUR, image = R.drawable.status_12, caption = "Sunday reset 🧘🏾‍♀️"),
        ), viewedCount = 1),
        StatusUpdate("c03", listOf(
            StatusItem("s11", now - 6 * HOUR, image = R.drawable.status_11, caption = "Slow mornings ☕"),
            StatusItem("s12", now - 6 * HOUR + 10 * MIN, text = "Design is not just what it looks like. Design is how it works.", background = R.drawable.status_text_bg_02),
        ), viewedCount = 2),
        StatusUpdate("c35", listOf(
            StatusItem("s13", now - 8 * HOUR, image = R.drawable.status_02, caption = "New on the menu tonight 🌶️"),
        ), viewedCount = 1),
        StatusUpdate("c38", listOf(
            StatusItem("s14", now - 10 * HOUR, image = R.drawable.status_18),
            StatusItem("s15", now - 10 * HOUR + 2 * MIN, image = R.drawable.status_20, caption = "Winter project 🧶"),
        ), viewedCount = 2),
        StatusUpdate("c40", listOf(
            StatusItem("s16", now - 12 * HOUR, image = R.drawable.status_05, caption = "Sunset therapy 🌅"),
        ), viewedCount = 1),
        StatusUpdate("c27", listOf(
            StatusItem("s17", now - 14 * HOUR, image = R.drawable.status_07),
        ), viewedCount = 1, isMuted = true),
        StatusUpdate("c22", listOf(
            StatusItem("s18", now - 16 * HOUR, text = "Count your blessings, not your problems 🙏", background = R.drawable.status_text_bg_03),
        ), viewedCount = 1, isMuted = true),
    )

    val myStatus: List<StatusItem> = listOf(
        StatusItem("my1", now - 3 * HOUR, image = R.drawable.status_14, caption = "Sky was on fire today 🔥", viewerIds = listOf("c06", "c16", "c03", "c08", "c25", "c24")),
    )

    // ---------------------------------------------------------------- channels

    val channels: List<Channel> = listOf(
        Channel(
            "ch01", "Pulse Daily News", R.drawable.avatar_channel_01, 2_480_000, true,
            "Top stories, breaking news and the day's essential reads in one place.", isFollowing = true, unread = 3, category = "News",
            posts = listOf(
                ChannelPost("p1", "🌍 Global leaders agree on a new ocean protection framework covering 30% of international waters by 2030.", R.drawable.media_link_preview_07, now - 8 * HOUR, listOf("👍" to 12400, "❤️" to 3200, "🙏" to 980), 812_000),
                ChannelPost("p2", "📈 Markets close higher for the third straight day as tech stocks rally.", null, now - 4 * HOUR, listOf("👍" to 5400, "😮" to 620), 540_000),
                ChannelPost("p3", "🔬 Scientists unveil a battery that charges in 5 minutes and lasts a decade.", R.drawable.media_link_preview_08, now - 40 * MIN, listOf("😮" to 18000, "🔥" to 9100, "👍" to 4200), 1_020_000),
            ),
        ),
        Channel(
            "ch02", "TechWire", R.drawable.avatar_channel_02, 1_150_000, true,
            "Gadgets, apps and the future of tech. No hype, just the good stuff.", isFollowing = true, unread = 1, category = "Technology",
            posts = listOf(
                ChannelPost("p4", "The new generation of foldables is finally getting the crease right. Here's our hands-on first look 👇", R.drawable.media_link_preview_01, now - 26 * HOUR, listOf("🔥" to 3400, "👍" to 2100), 230_000),
                ChannelPost("p5", "5 hidden settings that will make your phone battery last all day 🔋", null, now - 2 * HOUR, listOf("❤️" to 5600, "🙏" to 900), 410_000),
            ),
        ),
        Channel(
            "ch03", "Matchday Live", R.drawable.avatar_channel_03, 3_900_000, true,
            "Live scores, highlights and transfer news from every league.", isFollowing = true, category = "Sports",
            posts = listOf(
                ChannelPost("p6", "⚽ FULL TIME: What a comeback! 3–2 after being two goals down at half-time.", R.drawable.media_link_preview_04, now - 20 * HOUR, listOf("🔥" to 22000, "😮" to 7000, "😢" to 3100), 1_800_000),
            ),
        ),
        Channel(
            "ch04", "Kitchen Stories", R.drawable.avatar_channel_04, 860_000, false,
            "Easy recipes, cooking hacks and weekly meal plans.", isFollowing = true, category = "Food",
            posts = listOf(
                ChannelPost("p7", "🥕 30-minute roasted veggie bowl that's perfect for meal prep. Recipe in the link!", R.drawable.media_link_preview_02, now - 30 * HOUR, listOf("😍" to 4100, "👍" to 1900), 160_000),
            ),
        ),
        Channel("ch05", "Wanderlust", R.drawable.avatar_channel_05, 1_420_000, true, "Hidden gems and travel guides from around the world.", isFollowing = false, category = "Travel",
            posts = listOf(ChannelPost("p8", "12 hidden gems in Bali that locals love 🌴", R.drawable.media_link_preview_03, now - 5 * HOUR, listOf("😍" to 8000), 300_000))),
        Channel("ch06", "Beat Drop", R.drawable.avatar_channel_06, 2_010_000, true, "New music Fridays, playlists and artist interviews.", isFollowing = false, category = "Music",
            posts = listOf(ChannelPost("p9", "🎧 This week's fresh finds playlist is live!", R.drawable.media_link_preview_06, now - 9 * HOUR, listOf("🔥" to 5100), 260_000))),
        Channel("ch07", "FitLife", R.drawable.avatar_channel_07, 640_000, false, "Daily workouts, nutrition tips and motivation.", isFollowing = false, category = "Fitness"),
        Channel("ch08", "Money Matters", R.drawable.avatar_channel_08, 930_000, true, "Personal finance made simple.", isFollowing = false, category = "Business"),
        Channel("ch09", "CinemaScope", R.drawable.avatar_channel_09, 1_770_000, true, "Trailers, reviews and box office buzz.", isFollowing = false, category = "Entertainment"),
        Channel("ch10", "Science Now", R.drawable.avatar_channel_10, 1_090_000, true, "Discoveries that change how we see the world.", isFollowing = false, category = "Science"),
    )

    // ------------------------------------------------------------- communities

    val communities: List<Community> = listOf(
        Community("comm_school", "Greenwood School Parents", R.drawable.avatar_community_01, "Official community for Greenwood Primary parents. Be kind, stay informed 💚", listOf("g_class5b"), "g_school_ann", 342),
        Community("comm_maple", "Maple Heights Residents", R.drawable.avatar_community_02, "Everything happening around Maple Heights.", listOf("g_neighbours"), "g_maple_ann", 128),
        Community("comm_founders", "Founders Circle", R.drawable.avatar_community_03, "Builders helping builders. Demo nights, intros and resources.", emptyList(), "g_founders_ann", 86),
        Community("comm_runners", "City Runners Club", R.drawable.avatar_community_04, "Weekly runs, races and recovery tips 🏃", emptyList(), "g_runners_ann", 210),
    )

    // ------------------------------------------------------------------- calls

    val calls: List<CallLog> = listOf(
        CallLog("k1", "c06", false, CallType.VOICE, CallDirection.INCOMING, now - 35 * MIN, 754),
        CallLog("k2", "c19", false, CallType.VIDEO, CallDirection.MISSED, now - 3 * HOUR, 0, count = 2),
        CallLog("k3", "g_design", true, CallType.VIDEO, CallDirection.OUTGOING, now - 5 * HOUR, 1820),
        CallLog("k4", "c16", false, CallType.VIDEO, CallDirection.OUTGOING, now - 26 * HOUR, 412),
        CallLog("k5", "c21", false, CallType.VOICE, CallDirection.INCOMING, now - 28 * HOUR, 96),
        CallLog("k6", "c03", false, CallType.VOICE, CallDirection.MISSED, now - 2 * DAY, 0),
        CallLog("k7", "g_family", true, CallType.VIDEO, CallDirection.INCOMING, now - 3 * DAY, 2410),
        CallLog("k8", "c24", false, CallType.VOICE, CallDirection.OUTGOING, now - 4 * DAY, 305),
        CallLog("k9", "b06", false, CallType.VOICE, CallDirection.OUTGOING, now - 5 * DAY, 64),
        CallLog("k10", "c32", false, CallType.VIDEO, CallDirection.INCOMING, now - 6 * DAY, 1133),
        CallLog("k11", "c08", false, CallType.VOICE, CallDirection.MISSED, now - 8 * DAY, 0, count = 3),
        CallLog("k12", "c27", false, CallType.VOICE, CallDirection.OUTGOING, now - 10 * DAY, 230),
    )

    val favoriteCallIds = listOf("c06", "c21", "c16")

    val linkedDevices = listOf(
        LinkedDevice("d1", "Chrome on Windows", "web", now - 2 * HOUR),
        LinkedDevice("d2", "Pulse for Mac", "desktop", now - 3 * DAY),
    )

    val blocked = listOf("c42")

    val deviceDocuments = listOf(
        DeviceDocument("Invoice_October_2026.pdf", "312 KB", 2, "PDF", R.drawable.media_doc_thumb_01),
        DeviceDocument("Resume_2026.pdf", "184 KB", 1, "PDF", R.drawable.media_doc_thumb_02),
        DeviceDocument("Q3_Report.pdf", "2.4 MB", 18, "PDF", R.drawable.media_doc_thumb_03),
        DeviceDocument("Product_Pitch.pptx", "6.1 MB", 24, "PPTX", R.drawable.media_doc_thumb_04),
        DeviceDocument("Budget_Planner.xlsx", "96 KB", 4, "XLSX", null),
        DeviceDocument("Meeting_Notes.docx", "42 KB", 3, "DOCX", null),
        DeviceDocument("Photos_Backup.zip", "48.2 MB", 0, "ZIP", null),
    )

    val nearbyPlaces = listOf(
        Place("Central Park Café", "12 Garden Road", "120 m"),
        Place("Riverside Park", "Riverside Dr", "450 m"),
        Place("Midtown Library", "88 Book Street", "600 m"),
        Place("Groove Dance Studio", "221 Rhythm Ave", "1.1 km"),
        Place("Spice Route Kitchen", "9 Market Lane", "1.4 km"),
    )

    val autoReplies = listOf(
        "Haha 😂", "Sounds good!", "Okay 👍", "Let me check and get back to you",
        "Sure, see you then!", "That's amazing 😍", "Really? Tell me more", "On it 💪",
        "Thanks! 🙏", "100% agree", "Wait what 😮", "Love that ❤️", "Perfect, thank you!",
        "I'll call you in a bit", "Omg yes!!", "No worries 😊",
    )

    val questionReplies = listOf(
        "Yes, definitely!", "Hmm, not sure yet. Let me think 🤔", "I think so!",
        "Probably later today", "Let me check and tell you", "Of course 😊",
    )
}
