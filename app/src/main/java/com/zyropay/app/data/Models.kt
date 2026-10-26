package com.zyropay.app.data

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.zyropay.app.R

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Immutable
data class Network(
    val id: String,
    val name: String,
    val short: String,
    @DrawableRes val icon: Int,
    val fee: Double,
    val minDeposit: Double,
    val minWithdraw: Double,
    val confirmations: Int,
    val eta: String,
    val addressPrefix: String,
)

@Immutable
data class Coin(
    val symbol: String,
    val name: String,
    @DrawableRes val icon: Int,
    val color: Color,
    val decimals: Int,
    val basePrice: Double,
    val networks: List<Network>,
    val stable: Boolean = false,
)

@Immutable
data class Price(val usd: Double, val change24h: Double, val history: List<Float>)

enum class CardKind { VIRTUAL, PHYSICAL }

enum class CardDesign(@DrawableRes val art: Int, val label: String, val kind: CardKind, val lightText: Boolean = true) {
    VIOLET(R.drawable.card_virtual_violet, "Electric Violet", CardKind.VIRTUAL),
    MIDNIGHT(R.drawable.card_virtual_midnight, "Midnight", CardKind.VIRTUAL),
    AURORA(R.drawable.card_virtual_aurora, "Aurora", CardKind.VIRTUAL),
    BLACK(R.drawable.card_physical_black, "Obsidian Metal", CardKind.PHYSICAL),
    SILVER(R.drawable.card_physical_silver, "Platinum Silver", CardKind.PHYSICAL, lightText = false),
    GOLD(R.drawable.card_physical_gold, "Royal Gold", CardKind.PHYSICAL, lightText = false),
}

enum class CardStatus { ACTIVE, SHIPPING, CANCELLED }

@Immutable
data class PaymentCard(
    val id: String,
    val kind: CardKind,
    val design: CardDesign,
    val holder: String,
    val number: String,
    val expiry: String,
    val cvv: String,
    val balance: Double,
    val frozen: Boolean = false,
    val onlinePayments: Boolean = true,
    val atmWithdrawals: Boolean = true,
    val contactless: Boolean = true,
    val dailyLimit: Double = 5_000.0,
    val monthlyLimit: Double = 50_000.0,
    val spentToday: Double = 0.0,
    val spentMonth: Double = 0.0,
    val status: CardStatus = CardStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val last4: String get() = number.takeLast(4)
    val masked: String get() = "•••• •••• •••• $last4"
    val formattedNumber: String get() = number.chunked(4).joinToString(" ")
    val title: String get() = if (kind == CardKind.VIRTUAL) "Virtual Card" else "Physical Card"
}

enum class TxType(val label: String) {
    CARD_PAYMENT("Card payment"),
    DEPOSIT("Deposit"),
    WITHDRAW("Withdrawal"),
    SEND("Transfer out"),
    RECEIVE("Transfer in"),
    SWAP("Swap"),
    CARD_TOP_UP("Card top-up"),
    REFUND("Refund"),
    CASHBACK("Cashback"),
    P2P_BUY("P2P buy"),
    P2P_SELL("P2P sell"),
    PAY("QR payment"),
    CARD_FEE("Card fee"),
    REWARD("Reward"),
}

enum class TxStatus { SUCCESS, PENDING, FAILED }

enum class TxCategory(val label: String, @DrawableRes val icon: Int) {
    SHOPPING("Shopping", R.drawable.ic_category_shopping),
    FOOD("Food & Drinks", R.drawable.ic_category_food),
    TRAVEL("Travel", R.drawable.ic_category_travel),
    TRANSPORT("Transport", R.drawable.ic_category_transport),
    ENTERTAINMENT("Entertainment", R.drawable.ic_category_entertainment),
    SUBSCRIPTION("Subscriptions", R.drawable.ic_category_subscription),
    UTILITIES("Utilities", R.drawable.ic_category_utilities),
    TRANSFER("Transfer", R.drawable.ic_category_transfer),
    REFUND("Refund", R.drawable.ic_category_refund),
    FEE("Fees", R.drawable.ic_category_fee),
    CASHBACK("Cashback", R.drawable.ic_category_cashback),
    ATM("ATM", R.drawable.ic_category_atm),
}

@Immutable
data class Transaction(
    val id: String,
    val type: TxType,
    val title: String,
    val subtitle: String,
    val coin: String,
    val amount: Double,
    val usdValue: Double,
    val timestamp: Long,
    val status: TxStatus = TxStatus.SUCCESS,
    val category: TxCategory? = null,
    val merchant: String? = null,
    val cardId: String? = null,
    val network: String? = null,
    val address: String? = null,
    val txHash: String? = null,
    val fee: Double = 0.0,
    val toCoin: String? = null,
    val toAmount: Double? = null,
    val note: String? = null,
) {
    val isIncoming: Boolean get() = amount > 0
}

enum class NotificationKind(val label: String) { TRANSACTION("Transactions"), SECURITY("Security"), SYSTEM("System"), PROMO("Promotions") }

@Immutable
data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val timestamp: Long,
    val read: Boolean = false,
    val txId: String? = null,
)

enum class KycStatus { NOT_STARTED, PENDING, VERIFIED, REJECTED }

@Immutable
data class UserProfile(
    val fullName: String = "",
    val nickname: String = "",
    val email: String = "",
    val phone: String = "",
    val countryCode: String = "+852",
    val uid: String = "",
    val avatar: Int = 0,
    val kycStatus: KycStatus = KycStatus.NOT_STARTED,
    val country: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val vipLevel: Int = 1,
    val referralCode: String = "",
    val joinedAt: Long = System.currentTimeMillis(),
)

@Immutable
data class SecuritySettings(
    val passcode: String = "",
    val password: String = "",
    val biometricEnabled: Boolean = false,
    val twoFaEnabled: Boolean = false,
    val twoFaSecret: String = "",
    val antiPhishingCode: String = "",
    val withdrawWhitelist: Boolean = false,
)

@Immutable
data class Preferences(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val currency: String = "USD",
    val language: String = "English",
    val hideBalance: Boolean = false,
    val pushTransactions: Boolean = true,
    val pushSecurity: Boolean = true,
    val pushPromotions: Boolean = false,
    val emailNotifications: Boolean = true,
    val haptics: Boolean = true,
)

@Immutable
data class Device(val id: String, val name: String, val location: String, val lastActive: Long, val current: Boolean)

@Immutable
data class Fiat(val code: String, val name: String, val symbol: String, val rate: Double, @DrawableRes val flag: Int)

@Immutable
data class Merchant(val name: String, @DrawableRes val icon: Int, val category: TxCategory, val city: String)

@Immutable
data class P2POffer(
    val id: String,
    val trader: String,
    val avatar: Int,
    val orders: Int,
    val completion: Double,
    val price: Double,
    val fiat: String,
    val available: Double,
    val minLimit: Double,
    val maxLimit: Double,
    val methods: List<String>,
    val verified: Boolean,
    val responseMins: Int,
)

@Immutable
data class ChatMessage(val id: Long, val fromUser: Boolean, val text: String, val timestamp: Long)
