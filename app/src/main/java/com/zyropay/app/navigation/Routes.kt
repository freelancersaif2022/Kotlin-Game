package com.zyropay.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val OTP = "otp/{target}/{purpose}"
    const val FORGOT = "forgot"
    const val RESET = "reset/{target}"
    const val CREATE_PASSCODE = "passcode/{mode}"
    const val BIOMETRIC_SETUP = "biometric_setup"
    const val NOTIFICATION_PERMISSION = "notification_permission"
    const val LOCK = "lock?resume={resume}"
    const val MAIN = "main"

    const val NOTIFICATIONS = "notifications"
    const val TRANSACTIONS = "transactions?filter={filter}"
    const val TX_DETAIL = "tx/{id}"
    const val SELECT_COIN = "select_coin/{purpose}"
    const val DEPOSIT = "deposit/{coin}"
    const val WITHDRAW = "withdraw/{coin}"
    const val SEND = "send/{coin}"
    const val RECEIVE = "receive"
    const val SWAP = "swap?from={from}"
    const val COIN = "coin/{symbol}"
    const val P2P = "p2p"
    const val P2P_ORDER = "p2p_order/{offerId}/{buy}/{coin}/{fiat}"

    const val CARD_APPLY = "card_apply"
    const val CARD_SETTINGS = "card_settings/{id}"
    const val CARD_TOPUP = "card_topup/{id}"
    const val CARD_LIMITS = "card_limits/{id}"
    const val CARD_PIN = "card_pin/{id}"

    const val SCAN = "scan"
    const val PAY_MERCHANT = "pay_merchant/{index}"

    const val PERSONAL_INFO = "personal_info"
    const val SECURITY = "security"
    const val CHANGE_PASSWORD = "change_password"
    const val TWO_FA = "two_fa"
    const val DEVICES = "devices"
    const val ANTI_PHISHING = "anti_phishing"
    const val SETTINGS = "settings"
    const val LANGUAGE = "language"
    const val CURRENCY = "currency"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val REWARDS = "rewards"
    const val REFERRAL = "referral"
    const val HELP = "help"
    const val CHAT = "chat"
    const val ABOUT = "about"
    const val LEGAL = "legal/{doc}"
    const val KYC = "kyc"
    const val RESULT = "result/{kind}?ref={ref}"

    fun otp(target: String, purpose: String) = "otp/${enc(target)}/$purpose"
    fun reset(target: String) = "reset/${enc(target)}"
    fun passcode(mode: String) = "passcode/$mode"
    fun lock(resume: Boolean) = "lock?resume=$resume"
    fun transactions(filter: String = "all") = "transactions?filter=$filter"
    fun tx(id: String) = "tx/$id"
    fun selectCoin(purpose: String) = "select_coin/$purpose"
    fun deposit(coin: String) = "deposit/$coin"
    fun withdraw(coin: String) = "withdraw/$coin"
    fun send(coin: String) = "send/$coin"
    fun swap(from: String = "USDT") = "swap?from=$from"
    fun coin(symbol: String) = "coin/$symbol"
    fun p2pOrder(offerId: String, buy: Boolean, coin: String, fiat: String) = "p2p_order/$offerId/$buy/$coin/$fiat"
    fun cardSettings(id: String) = "card_settings/$id"
    fun cardTopUp(id: String) = "card_topup/$id"
    fun cardLimits(id: String) = "card_limits/$id"
    fun cardPin(id: String) = "card_pin/$id"
    fun payMerchant(index: Int) = "pay_merchant/$index"
    fun legal(doc: String) = "legal/$doc"
    fun result(kind: String, ref: String = "") = "result/$kind?ref=${enc(ref)}"

    private fun enc(s: String) = android.net.Uri.encode(s)
}

/** Navigate and clear the whole back stack (e.g. after login / logout). */
fun NavController.navigateClear(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateSingle(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route) {
        launchSingleTop = true
        builder()
    }
}

/** Safe back: avoid double-pop crashes when users tap back rapidly. */
fun NavController.back() {
    if (previousBackStackEntry != null) popBackStack()
}
