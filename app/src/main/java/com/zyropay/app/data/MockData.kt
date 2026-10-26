package com.zyropay.app.data

import androidx.compose.ui.graphics.Color
import com.zyropay.app.R
import kotlin.random.Random

object MockData {

    private val TRC20 = Network("TRC20", "Tron (TRC20)", "TRC20", R.drawable.network_trc20, 1.0, 1.0, 10.0, 20, "≈ 2 mins", "T")
    private val ERC20 = Network("ERC20", "Ethereum (ERC20)", "ERC20", R.drawable.network_erc20, 4.5, 10.0, 20.0, 12, "≈ 5 mins", "0x")
    private val BEP20 = Network("BEP20", "BNB Smart Chain (BEP20)", "BEP20", R.drawable.network_bep20, 0.3, 1.0, 10.0, 15, "≈ 1 min", "0x")
    private val SOL = Network("SOL", "Solana", "SOL", R.drawable.network_solana, 0.8, 1.0, 10.0, 1, "≈ 30 secs", "")
    private val POLYGON = Network("POLYGON", "Polygon PoS", "POLYGON", R.drawable.network_polygon, 0.2, 1.0, 10.0, 128, "≈ 5 mins", "0x")
    private val TON = Network("TON", "The Open Network", "TON", R.drawable.network_ton, 0.1, 1.0, 5.0, 1, "≈ 30 secs", "UQ")

    private fun native(id: String, name: String, icon: Int, fee: Double, min: Double, conf: Int, eta: String, prefix: String) =
        Network(id, name, id, icon, fee, min, min * 2, conf, eta, prefix)

    val coins: List<Coin> = listOf(
        Coin("USDT", "Tether USD", R.drawable.coin_usdt, Color(0xFF26A17B), 2, 1.0, listOf(TRC20, ERC20, BEP20, SOL, POLYGON, TON), stable = true),
        Coin("USDC", "USD Coin", R.drawable.coin_usdc, Color(0xFF2775CA), 2, 1.0, listOf(ERC20, SOL, BEP20, POLYGON), stable = true),
        Coin("BTC", "Bitcoin", R.drawable.coin_btc, Color(0xFFF7931A), 6, 67_420.0, listOf(native("BTC", "Bitcoin", R.drawable.coin_btc, 0.00012, 0.0001, 2, "≈ 20 mins", "bc1q"))),
        Coin("ETH", "Ethereum", R.drawable.coin_eth, Color(0xFF627EEA), 5, 3_284.0, listOf(native("ETH", "Ethereum", R.drawable.network_erc20, 0.0012, 0.005, 12, "≈ 5 mins", "0x"), BEP20.copy(fee = 0.00009, minDeposit = 0.001, minWithdraw = 0.002))),
        Coin("BNB", "BNB", R.drawable.coin_bnb, Color(0xFFF3BA2F), 4, 584.0, listOf(BEP20.copy(fee = 0.0005, minDeposit = 0.01, minWithdraw = 0.02))),
        Coin("SOL", "Solana", R.drawable.coin_sol, Color(0xFF9945FF), 4, 152.0, listOf(SOL.copy(fee = 0.008, minDeposit = 0.05, minWithdraw = 0.1))),
        Coin("TRX", "TRON", R.drawable.coin_trx, Color(0xFFEB0029), 2, 0.162, listOf(TRC20.copy(id = "TRX", name = "TRON", fee = 1.0, minDeposit = 10.0, minWithdraw = 20.0))),
        Coin("TON", "Toncoin", R.drawable.coin_ton, Color(0xFF0098EA), 3, 5.42, listOf(TON.copy(fee = 0.05, minDeposit = 0.5, minWithdraw = 1.0))),
        Coin("MATIC", "Polygon", R.drawable.coin_matic, Color(0xFF8247E5), 2, 0.52, listOf(POLYGON.copy(id = "MATIC", fee = 0.1, minDeposit = 1.0, minWithdraw = 2.0))),
    )

    fun coin(symbol: String): Coin = coins.firstOrNull { it.symbol == symbol } ?: coins.first()

    val fiats = listOf(
        Fiat("USD", "US Dollar", "$", 1.0, R.drawable.flag_usd),
        Fiat("EUR", "Euro", "€", 0.92, R.drawable.flag_eur),
        Fiat("GBP", "British Pound", "£", 0.79, R.drawable.flag_gbp),
        Fiat("HKD", "Hong Kong Dollar", "HK$", 7.81, R.drawable.flag_hkd),
        Fiat("SGD", "Singapore Dollar", "S$", 1.34, R.drawable.flag_sgd),
        Fiat("AED", "UAE Dirham", "AED ", 3.67, R.drawable.flag_aed),
        Fiat("INR", "Indian Rupee", "₹", 83.4, R.drawable.flag_inr),
        Fiat("BDT", "Bangladeshi Taka", "৳", 119.5, R.drawable.flag_bdt),
        Fiat("JPY", "Japanese Yen", "¥", 151.2, R.drawable.flag_jpy),
        Fiat("CNY", "Chinese Yuan", "CN¥", 7.24, R.drawable.flag_cny),
    )

    fun fiat(code: String) = fiats.firstOrNull { it.code == code } ?: fiats.first()

    val languages = listOf(
        "English" to "English", "简体中文" to "Chinese (Simplified)", "繁體中文" to "Chinese (Traditional)",
        "Español" to "Spanish", "Français" to "French", "Deutsch" to "German", "Português" to "Portuguese",
        "Русский" to "Russian", "日本語" to "Japanese", "한국어" to "Korean", "Tiếng Việt" to "Vietnamese",
        "Bahasa Indonesia" to "Indonesian", "ไทย" to "Thai", "Türkçe" to "Turkish", "العربية" to "Arabic",
        "বাংলা" to "Bengali", "हिन्दी" to "Hindi",
    )

    val merchants = listOf(
        Merchant("Amazon Marketplace", R.drawable.merchant_shopping_bag, TxCategory.SHOPPING, "Seattle, US"),
        Merchant("Starbucks", R.drawable.merchant_coffee, TxCategory.FOOD, "Hong Kong"),
        Merchant("McDonald's", R.drawable.merchant_burger, TxCategory.FOOD, "Singapore"),
        Merchant("Uber Trip", R.drawable.merchant_taxi, TxCategory.TRANSPORT, "Dubai, AE"),
        Merchant("Emirates Airlines", R.drawable.merchant_airplane, TxCategory.TRAVEL, "Dubai, AE"),
        Merchant("Booking.com Hotel", R.drawable.merchant_hotel, TxCategory.TRAVEL, "Amsterdam, NL"),
        Merchant("Netflix", R.drawable.merchant_streaming, TxCategory.SUBSCRIPTION, "Los Gatos, US"),
        Merchant("Spotify Premium", R.drawable.merchant_music, TxCategory.SUBSCRIPTION, "Stockholm, SE"),
        Merchant("Steam Games", R.drawable.merchant_gaming, TxCategory.ENTERTAINMENT, "Bellevue, US"),
        Merchant("7-Eleven", R.drawable.merchant_grocery, TxCategory.SHOPPING, "Bangkok, TH"),
        Merchant("Watsons Pharmacy", R.drawable.merchant_pharmacy, TxCategory.SHOPPING, "Hong Kong"),
        Merchant("Apple Store", R.drawable.merchant_electronics, TxCategory.SHOPPING, "Cupertino, US"),
        Merchant("Shell Station", R.drawable.merchant_fuel, TxCategory.TRANSPORT, "London, UK"),
        Merchant("Fitness First", R.drawable.merchant_gym, TxCategory.ENTERTAINMENT, "Kuala Lumpur, MY"),
        Merchant("Google Cloud", R.drawable.merchant_cloud_service, TxCategory.UTILITIES, "Mountain View, US"),
        Merchant("Mobile Top-up", R.drawable.merchant_mobile_topup, TxCategory.UTILITIES, "Online"),
    )

    fun merchantIcon(name: String?): Int? = merchants.firstOrNull { it.name == name }?.icon

    val avatars = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4,
        R.drawable.avatar_5, R.drawable.avatar_6, R.drawable.avatar_7, R.drawable.avatar_8,
    )

    val p2pTraders = listOf(
        "CryptoKing_HK", "SwiftTrade", "BlueOcean OTC", "Mina Exchange", "TrustPay Pro", "Nova Desk",
        "Dragon Coins", "Atlas Trading", "Lotus P2P", "Zen Merchant", "Polar OTC", "Maple Crypto",
    )

    fun p2pOffers(buy: Boolean, fiat: String, coin: String): List<P2POffer> {
        val rnd = Random((fiat + coin + buy).hashCode())
        val base = coin(coin).basePrice * fiat(fiat).rate
        val methods = listOf("Bank Transfer", "FPS", "Wise", "Revolut", "PayPal", "Alipay", "Cash Deposit", "bKash", "UPI")
        return p2pTraders.mapIndexed { i, t ->
            val spread = 1 + (if (buy) 1 else -1) * (0.002 + rnd.nextDouble() * 0.018)
            P2POffer(
                id = "p2p_${buy}_$i",
                trader = t,
                avatar = i % 8,
                orders = 120 + rnd.nextInt(4800),
                completion = 92.0 + rnd.nextDouble() * 7.9,
                price = base * spread,
                fiat = fiat,
                available = (500 + rnd.nextInt(60_000)).toDouble() / if (coin == "BTC") 60_000.0 else if (coin == "ETH") 3_000.0 else 1.0,
                minLimit = listOf(50.0, 100.0, 200.0, 500.0)[rnd.nextInt(4)] * fiat(fiat).rate,
                maxLimit = listOf(2_000.0, 5_000.0, 10_000.0, 25_000.0)[rnd.nextInt(4)] * fiat(fiat).rate,
                methods = methods.shuffled(rnd).take(1 + rnd.nextInt(3)),
                verified = rnd.nextInt(10) > 2,
                responseMins = 1 + rnd.nextInt(14),
            )
        }.sortedBy { if (buy) it.price else -it.price }
    }

    val faqs = listOf(
        "Getting started" to listOf(
            "What is Zyro Pay?" to "Zyro Pay is a crypto wallet with a crypto-funded payment card. Deposit stablecoins or crypto, top up your card instantly and spend at millions of merchants online and in-store worldwide.",
            "How do I create an account?" to "Download the app, tap Create account, enter your email or phone number, verify it with the one-time code and set a secure password and 6-digit passcode.",
            "Which countries are supported?" to "Zyro Pay is available in 170+ countries and regions. Some features, like physical cards, may vary by location.",
        ),
        "Card" to listOf(
            "How do I apply for a card?" to "Go to the Card tab and tap Apply. Virtual cards are issued instantly after identity verification. Physical cards are delivered within 7–14 business days.",
            "What are the card fees?" to "Virtual cards cost 10 USDT one-off. Physical cards cost 100 USDT including worldwide shipping. There are no monthly fees.",
            "Can I add my card to mobile wallets?" to "Yes, your Zyro Pay card can be added to supported mobile wallets for contactless payments in-store.",
            "What should I do if my card is lost?" to "Freeze your card immediately from the Card tab, then request a replacement from Card settings. Your funds remain safe.",
        ),
        "Deposits & withdrawals" to listOf(
            "Which coins can I deposit?" to "USDT, USDC, BTC, ETH, BNB, SOL, TRX, TON and MATIC across multiple networks. Always make sure the network you send from matches the deposit network.",
            "How long does a deposit take?" to "Deposits are credited after the required number of network confirmations — typically 30 seconds to 20 minutes depending on the network.",
            "I sent to the wrong network" to "Funds sent on an unsupported network may be lost. Contact support with your transaction hash and we will do our best to help.",
        ),
        "Security" to listOf(
            "How is my account protected?" to "We use a 6-digit passcode, biometric login, two-factor authentication, device management and anti-phishing codes. Funds are held with institutional-grade custody.",
            "How do I enable 2FA?" to "Go to Me › Security › Two-factor authentication and follow the steps with an authenticator app.",
        ),
    )
}
