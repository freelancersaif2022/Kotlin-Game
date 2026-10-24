package com.pulse.messenger.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pulse.messenger.ui.call.CallScreen
import com.pulse.messenger.ui.camera.CameraScreen
import com.pulse.messenger.ui.channel.ChannelScreen
import com.pulse.messenger.ui.channel.FindChannelsScreen
import com.pulse.messenger.ui.chat.ChatScreen
import com.pulse.messenger.ui.community.CommunityScreen
import com.pulse.messenger.ui.community.NewCommunityScreen
import com.pulse.messenger.ui.contact.ContactInfoScreen
import com.pulse.messenger.ui.contact.GroupInfoScreen
import com.pulse.messenger.ui.contact.MessageInfoScreen
import com.pulse.messenger.ui.home.HomeScreen
import com.pulse.messenger.ui.media.ChatMediaScreen
import com.pulse.messenger.ui.media.MediaViewerScreen
import com.pulse.messenger.ui.newchat.AddMembersScreen
import com.pulse.messenger.ui.newchat.ForwardScreen
import com.pulse.messenger.ui.newchat.NewBroadcastScreen
import com.pulse.messenger.ui.newchat.NewChatScreen
import com.pulse.messenger.ui.newchat.NewContactScreen
import com.pulse.messenger.ui.newchat.NewGroupInfoScreen
import com.pulse.messenger.ui.newchat.NewGroupScreen
import com.pulse.messenger.ui.onboarding.OtpScreen
import com.pulse.messenger.ui.onboarding.PhoneScreen
import com.pulse.messenger.ui.onboarding.ProfileSetupScreen
import com.pulse.messenger.ui.onboarding.RestoreScreen
import com.pulse.messenger.ui.onboarding.WelcomeScreen
import com.pulse.messenger.ui.search.SearchScreen
import com.pulse.messenger.ui.settings.AboutAppScreen
import com.pulse.messenger.ui.settings.AboutStatusScreen
import com.pulse.messenger.ui.settings.AccountScreen
import com.pulse.messenger.ui.settings.AppLockSettingsScreen
import com.pulse.messenger.ui.settings.ArchivedScreen
import com.pulse.messenger.ui.settings.BackupScreen
import com.pulse.messenger.ui.settings.BlockedScreen
import com.pulse.messenger.ui.settings.ChangeNumberScreen
import com.pulse.messenger.ui.settings.ChatsSettingsScreen
import com.pulse.messenger.ui.settings.DeleteAccountScreen
import com.pulse.messenger.ui.settings.DisappearingScreen
import com.pulse.messenger.ui.settings.HelpScreen
import com.pulse.messenger.ui.settings.LinkedDevicesScreen
import com.pulse.messenger.ui.settings.ManageStorageScreen
import com.pulse.messenger.ui.settings.NotificationsScreen
import com.pulse.messenger.ui.settings.PrivacyScreen
import com.pulse.messenger.ui.settings.ProfileScreen
import com.pulse.messenger.ui.settings.QrScreen
import com.pulse.messenger.ui.settings.SecurityScreen
import com.pulse.messenger.ui.settings.SettingsScreen
import com.pulse.messenger.ui.settings.StarredScreen
import com.pulse.messenger.ui.settings.StorageScreen
import com.pulse.messenger.ui.settings.TwoStepScreen
import com.pulse.messenger.ui.settings.WallpaperScreen
import com.pulse.messenger.ui.status.StatusViewerScreen
import com.pulse.messenger.ui.status.TextStatusScreen

object Routes {
    const val WELCOME = "welcome"
    const val PHONE = "phone"
    fun otp(phone: String) = "otp/${Uri.encode(phone)}"
    const val PROFILE_SETUP = "profileSetup"
    const val RESTORE = "restore"
    const val HOME = "home"
    fun chat(id: String) = "chat/$id"
    fun contact(id: String) = "contact/$id"
    fun group(id: String) = "group/$id"
    fun messageInfo(chatId: String, messageId: String) = "messageInfo/$chatId/$messageId"
    fun viewer(chatId: String, messageId: String) = "viewer/$chatId/$messageId"
    fun chatMedia(chatId: String, tab: Int = 0) = "chatMedia/$chatId/$tab"
    fun status(contactId: String) = "status/$contactId"
    const val TEXT_STATUS = "textStatus"
    fun camera(target: String) = "camera/${Uri.encode(target)}"
    fun channel(id: String) = "channel/$id"
    const val FIND_CHANNELS = "channels"
    fun community(id: String) = "community/$id"
    const val NEW_COMMUNITY = "newCommunity"
    fun call(peerId: String, video: Boolean, incoming: Boolean = false) = "call/$peerId/$video/$incoming"
    const val NEW_CHAT = "newChat"
    const val NEW_GROUP = "newGroup"
    const val NEW_GROUP_INFO = "newGroupInfo"
    const val NEW_BROADCAST = "newBroadcast"
    const val NEW_CONTACT = "newContact"
    fun forward(chatId: String, ids: List<String>) = "forward/$chatId/${ids.joinToString(",")}"
    const val SHARE_PICKER = "forward/_share/_"
    fun addMembers(chatId: String) = "addMembers/$chatId"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val ABOUT_STATUS = "aboutStatus"
    const val ACCOUNT = "account"
    const val PRIVACY = "privacy"
    const val BLOCKED = "blocked"
    const val CHATS_SETTINGS = "chatsSettings"
    fun wallpaper(chatId: String = "default") = "wallpaper/$chatId"
    const val NOTIFICATIONS = "notifications"
    const val STORAGE = "storage"
    const val MANAGE_STORAGE = "manageStorage"
    const val HELP = "help"
    const val LINKED_DEVICES = "linkedDevices"
    fun qr(tab: Int = 0) = "qr/$tab"
    const val TWO_STEP = "twoStep"
    const val SECURITY = "security"
    const val CHANGE_NUMBER = "changeNumber"
    const val DELETE_ACCOUNT = "deleteAccount"
    fun disappearing(chatId: String = "default") = "disappearing/$chatId"
    const val APP_LOCK = "appLock"
    const val ABOUT_APP = "aboutApp"
    const val STARRED = "starred"
    const val ARCHIVED = "archived"
    const val BACKUP = "backup"
}

/** Pops the current screen unless it is the root. */
fun NavController.back() {
    if (previousBackStackEntry != null) popBackStack()
}

fun NavController.goHomeClearingStack() {
    navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
}

private fun NavBackStackEntry.arg(name: String): String = Uri.decode(arguments?.getString(name).orEmpty())

private const val DURATION = 280

private fun NavGraphBuilder.screen(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route,
        enterTransition = {
            slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { it / 5 } + fadeIn(tween(DURATION))
        },
        exitTransition = {
            slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 12 } + fadeOut(tween(DURATION / 2))
        },
        popEnterTransition = {
            slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 12 } + fadeIn(tween(DURATION))
        },
        popExitTransition = {
            slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { it / 5 } + fadeOut(tween(DURATION / 2))
        },
    ) { content(it) }
}

/** Full-screen media (viewer, stories, camera, calls) rises from the bottom instead. */
private fun NavGraphBuilder.modal(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route,
        enterTransition = { slideInVertically(tween(DURATION)) { it / 6 } + fadeIn(tween(DURATION)) + scaleIn(tween(DURATION), 0.96f) },
        exitTransition = { fadeOut(tween(DURATION / 2)) },
        popEnterTransition = { fadeIn(tween(DURATION)) },
        popExitTransition = { slideOutVertically(tween(DURATION)) { it / 6 } + fadeOut(tween(DURATION)) + scaleOut(tween(DURATION), 0.96f) },
    ) { content(it) }
}

@Composable
fun PulseNavHost(startOnboarding: Boolean) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = if (startOnboarding) Routes.WELCOME else Routes.HOME) {
        screen(Routes.WELCOME) { WelcomeScreen(nav) }
        screen(Routes.PHONE) { PhoneScreen(nav) }
        screen("otp/{phone}") { OtpScreen(nav, it.arg("phone")) }
        screen(Routes.PROFILE_SETUP) { ProfileSetupScreen(nav) }
        screen(Routes.RESTORE) { RestoreScreen(nav) }

        composable(Routes.HOME, enterTransition = { fadeIn(tween(DURATION)) }, popEnterTransition = {
            slideInHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 12 } + fadeIn(tween(DURATION))
        }, exitTransition = {
            slideOutHorizontally(tween(DURATION, easing = FastOutSlowInEasing)) { -it / 12 } + fadeOut(tween(DURATION / 2))
        }) { HomeScreen(nav) }

        screen("chat/{chatId}") { ChatScreen(nav, it.arg("chatId")) }
        screen("contact/{id}") { ContactInfoScreen(nav, it.arg("id")) }
        screen("group/{id}") { GroupInfoScreen(nav, it.arg("id")) }
        screen("messageInfo/{chatId}/{messageId}") { MessageInfoScreen(nav, it.arg("chatId"), it.arg("messageId")) }
        modal("viewer/{chatId}/{messageId}") { MediaViewerScreen(nav, it.arg("chatId"), it.arg("messageId")) }
        screen("chatMedia/{chatId}/{tab}") { ChatMediaScreen(nav, it.arg("chatId"), it.arg("tab").toIntOrNull() ?: 0) }
        modal("status/{contactId}") { StatusViewerScreen(nav, it.arg("contactId")) }
        modal(Routes.TEXT_STATUS) { TextStatusScreen(nav) }
        modal("camera/{target}") { CameraScreen(nav, it.arg("target")) }
        screen("channel/{id}") { ChannelScreen(nav, it.arg("id")) }
        screen(Routes.FIND_CHANNELS) { FindChannelsScreen(nav) }
        screen("community/{id}") { CommunityScreen(nav, it.arg("id")) }
        screen(Routes.NEW_COMMUNITY) { NewCommunityScreen(nav) }
        modal("call/{peerId}/{video}/{incoming}") {
            CallScreen(nav, it.arg("peerId"), it.arg("video") == "true", it.arg("incoming") == "true")
        }
        screen(Routes.NEW_CHAT) { NewChatScreen(nav) }
        screen(Routes.NEW_GROUP) { NewGroupScreen(nav) }
        screen(Routes.NEW_GROUP_INFO) { NewGroupInfoScreen(nav) }
        screen(Routes.NEW_BROADCAST) { NewBroadcastScreen(nav) }
        screen(Routes.NEW_CONTACT) { NewContactScreen(nav) }
        screen("forward/{chatId}/{ids}") { ForwardScreen(nav, it.arg("chatId"), it.arg("ids").split(",").filter { s -> s.isNotBlank() }) }
        screen("addMembers/{chatId}") { AddMembersScreen(nav, it.arg("chatId")) }
        screen(Routes.SEARCH) { SearchScreen(nav) }

        screen(Routes.SETTINGS) { SettingsScreen(nav) }
        screen(Routes.PROFILE) { ProfileScreen(nav) }
        screen(Routes.ABOUT_STATUS) { AboutStatusScreen(nav) }
        screen(Routes.ACCOUNT) { AccountScreen(nav) }
        screen(Routes.PRIVACY) { PrivacyScreen(nav) }
        screen(Routes.BLOCKED) { BlockedScreen(nav) }
        screen(Routes.CHATS_SETTINGS) { ChatsSettingsScreen(nav) }
        screen("wallpaper/{chatId}") { WallpaperScreen(nav, it.arg("chatId")) }
        screen(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
        screen(Routes.STORAGE) { StorageScreen(nav) }
        screen(Routes.MANAGE_STORAGE) { ManageStorageScreen(nav) }
        screen(Routes.HELP) { HelpScreen(nav) }
        screen(Routes.LINKED_DEVICES) { LinkedDevicesScreen(nav) }
        screen("qr/{tab}") { QrScreen(nav, it.arg("tab").toIntOrNull() ?: 0) }
        screen(Routes.TWO_STEP) { TwoStepScreen(nav) }
        screen(Routes.SECURITY) { SecurityScreen(nav) }
        screen(Routes.CHANGE_NUMBER) { ChangeNumberScreen(nav) }
        screen(Routes.DELETE_ACCOUNT) { DeleteAccountScreen(nav) }
        screen("disappearing/{chatId}") { DisappearingScreen(nav, it.arg("chatId")) }
        screen(Routes.APP_LOCK) { AppLockSettingsScreen(nav) }
        screen(Routes.ABOUT_APP) { AboutAppScreen(nav) }
        screen(Routes.STARRED) { StarredScreen(nav) }
        screen(Routes.ARCHIVED) { ArchivedScreen(nav) }
        screen(Routes.BACKUP) { BackupScreen(nav) }
    }
}
