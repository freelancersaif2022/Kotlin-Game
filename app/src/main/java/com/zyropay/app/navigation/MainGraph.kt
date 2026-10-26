package com.zyropay.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zyropay.app.ui.screens.card.CardApplyScreen
import com.zyropay.app.ui.screens.card.CardLimitsScreen
import com.zyropay.app.ui.screens.card.CardPinScreen
import com.zyropay.app.ui.screens.card.CardSettingsScreen
import com.zyropay.app.ui.screens.card.CardTopUpScreen
import com.zyropay.app.ui.screens.kyc.KycScreen
import com.zyropay.app.ui.screens.main.MainScreen
import com.zyropay.app.ui.screens.pay.PayMerchantScreen
import com.zyropay.app.ui.screens.pay.ScanScreen
import com.zyropay.app.ui.screens.profile.AntiPhishingScreen
import com.zyropay.app.ui.screens.profile.ChangePasswordScreen
import com.zyropay.app.ui.screens.profile.CurrencyScreen
import com.zyropay.app.ui.screens.profile.DevicesScreen
import com.zyropay.app.ui.screens.profile.LanguageScreen
import com.zyropay.app.ui.screens.profile.NotificationSettingsScreen
import com.zyropay.app.ui.screens.profile.PersonalInfoScreen
import com.zyropay.app.ui.screens.profile.ReferralScreen
import com.zyropay.app.ui.screens.profile.RewardsScreen
import com.zyropay.app.ui.screens.profile.SecurityScreen
import com.zyropay.app.ui.screens.profile.SettingsScreen
import com.zyropay.app.ui.screens.profile.TwoFaScreen
import com.zyropay.app.ui.screens.support.AboutScreen
import com.zyropay.app.ui.screens.support.HelpCenterScreen
import com.zyropay.app.ui.screens.support.LegalScreen
import com.zyropay.app.ui.screens.support.SupportChatScreen
import com.zyropay.app.ui.screens.transactions.NotificationsScreen
import com.zyropay.app.ui.screens.transactions.ResultScreen
import com.zyropay.app.ui.screens.transactions.TransactionDetailScreen
import com.zyropay.app.ui.screens.transactions.TransactionsScreen
import com.zyropay.app.ui.screens.wallet.CoinDetailScreen
import com.zyropay.app.ui.screens.wallet.DepositScreen
import com.zyropay.app.ui.screens.wallet.P2POrderScreen
import com.zyropay.app.ui.screens.wallet.P2PScreen
import com.zyropay.app.ui.screens.wallet.ReceiveScreen
import com.zyropay.app.ui.screens.wallet.SelectCoinScreen
import com.zyropay.app.ui.screens.wallet.SendScreen
import com.zyropay.app.ui.screens.wallet.SwapScreen
import com.zyropay.app.ui.screens.wallet.WithdrawScreen

private fun NavBackStackEntry.s(name: String, default: String = ""): String = arguments?.getString(name) ?: default

fun NavGraphBuilder.mainGraph(nav: NavController) {
    composable(Routes.MAIN, enterTransition = { fadeIn(tween(400)) }, popEnterTransition = { fadeIn(tween(300)) }) { MainScreen(nav) }

    composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
    composable(Routes.TRANSACTIONS, arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "all" })) {
        TransactionsScreen(nav, it.s("filter", "all"))
    }
    composable(Routes.TX_DETAIL) { TransactionDetailScreen(nav, it.s("id")) }
    composable(Routes.SELECT_COIN) { SelectCoinScreen(nav, it.s("purpose")) }
    composable(Routes.DEPOSIT) { DepositScreen(nav, it.s("coin", "USDT")) }
    composable(Routes.WITHDRAW) { WithdrawScreen(nav, it.s("coin", "USDT")) }
    composable(Routes.SEND) { SendScreen(nav, it.s("coin", "USDT")) }
    composable(Routes.RECEIVE) { ReceiveScreen(nav) }
    composable(Routes.SWAP, arguments = listOf(navArgument("from") { type = NavType.StringType; defaultValue = "USDT" })) {
        SwapScreen(nav, it.s("from", "USDT"))
    }
    composable(Routes.COIN) { CoinDetailScreen(nav, it.s("symbol", "BTC")) }
    composable(Routes.P2P) { P2PScreen(nav) }
    composable(Routes.P2P_ORDER) {
        P2POrderScreen(nav, it.s("offerId"), it.s("buy") == "true", it.s("coin", "USDT"), it.s("fiat", "HKD"))
    }

    composable(Routes.CARD_APPLY) { CardApplyScreen(nav) }
    composable(Routes.CARD_SETTINGS) { CardSettingsScreen(nav, it.s("id")) }
    composable(Routes.CARD_TOPUP) { CardTopUpScreen(nav, it.s("id")) }
    composable(Routes.CARD_LIMITS) { CardLimitsScreen(nav, it.s("id")) }
    composable(Routes.CARD_PIN) { CardPinScreen(nav, it.s("id")) }

    composable(
        Routes.SCAN,
        enterTransition = { slideInVertically(tween(320)) { it / 4 } + fadeIn(tween(320)) },
        exitTransition = { fadeOut(tween(200)) },
        popExitTransition = { slideOutVertically(tween(280)) { it / 4 } + fadeOut(tween(280)) },
    ) { ScanScreen(nav) }
    composable(Routes.PAY_MERCHANT) { PayMerchantScreen(nav, it.s("index", "0").toIntOrNull() ?: 0) }

    composable(Routes.PERSONAL_INFO) { PersonalInfoScreen(nav) }
    composable(Routes.SECURITY) { SecurityScreen(nav) }
    composable(Routes.CHANGE_PASSWORD) { ChangePasswordScreen(nav) }
    composable(Routes.TWO_FA) { TwoFaScreen(nav) }
    composable(Routes.DEVICES) { DevicesScreen(nav) }
    composable(Routes.ANTI_PHISHING) { AntiPhishingScreen(nav) }
    composable(Routes.SETTINGS) { SettingsScreen(nav) }
    composable(Routes.LANGUAGE) { LanguageScreen(nav) }
    composable(Routes.CURRENCY) { CurrencyScreen(nav) }
    composable(Routes.NOTIFICATION_SETTINGS) { NotificationSettingsScreen(nav) }
    composable(Routes.REWARDS) { RewardsScreen(nav) }
    composable(Routes.REFERRAL) { ReferralScreen(nav) }
    composable(Routes.HELP) { HelpCenterScreen(nav) }
    composable(Routes.CHAT) { SupportChatScreen(nav) }
    composable(Routes.ABOUT) { AboutScreen(nav) }
    composable(Routes.LEGAL) { LegalScreen(nav, it.s("doc", "terms")) }
    composable(Routes.KYC) { KycScreen(nav) }
    composable(
        Routes.RESULT,
        arguments = listOf(navArgument("ref") { type = NavType.StringType; defaultValue = "" }),
        enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.96f) },
    ) { ResultScreen(nav, it.s("kind"), it.s("ref")) }
}
