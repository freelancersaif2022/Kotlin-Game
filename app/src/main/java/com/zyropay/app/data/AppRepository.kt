package com.zyropay.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

/**
 * Single source of truth for the mock app. Everything lives in memory as StateFlows and is
 * persisted to SharedPreferences as JSON, so the app behaves like a real account across restarts.
 */
object AppRepository {

    private lateinit var sp: SharedPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var saveJob: Job? = null

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _onboardingSeen = MutableStateFlow(false)
    val onboardingSeen: StateFlow<Boolean> = _onboardingSeen.asStateFlow()

    private val _user = MutableStateFlow(UserProfile())
    val user: StateFlow<UserProfile> = _user.asStateFlow()

    private val _security = MutableStateFlow(SecuritySettings())
    val security: StateFlow<SecuritySettings> = _security.asStateFlow()

    private val _prefs = MutableStateFlow(Preferences())
    val prefs: StateFlow<Preferences> = _prefs.asStateFlow()

    private val _balances = MutableStateFlow<Map<String, Double>>(emptyMap())
    val balances: StateFlow<Map<String, Double>> = _balances.asStateFlow()

    private val _prices = MutableStateFlow(initialPrices())
    val prices: StateFlow<Map<String, Price>> = _prices.asStateFlow()

    private val _cards = MutableStateFlow<List<PaymentCard>>(emptyList())
    val cards: StateFlow<List<PaymentCard>> = _cards.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _chat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat.asStateFlow()

    private val _agentTyping = MutableStateFlow(false)
    val agentTyping: StateFlow<Boolean> = _agentTyping.asStateFlow()

    /** Set when the app returns from background and must show the lock screen. */
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _referrals = MutableStateFlow(3)
    val referrals: StateFlow<Int> = _referrals.asStateFlow()

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("zyro_store", Context.MODE_PRIVATE)
        _devices.value = seedDevices()
        load()
        startTicker()
    }

    // ------------------------------------------------------------------ persistence

    private fun load() {
        _onboardingSeen.value = sp.getBoolean("onboarding", false)
        _loggedIn.value = sp.getBoolean("loggedIn", false)
        sp.getString("prefs", null)?.let { runCatching { _prefs.value = Json.prefs(JSONObject(it)) } }
        val raw = sp.getString("account", null) ?: return
        runCatching {
            val o = JSONObject(raw)
            _user.value = Json.user(o.getJSONObject("user"))
            _security.value = Json.security(o.getJSONObject("security"))
            _balances.value = Json.balances(o.getJSONObject("balances"))
            _cards.value = Json.list(o.optJSONArray("cards")) { Json.card(it) }
            _transactions.value = Json.list(o.optJSONArray("tx")) { Json.tx(it) }
            _notifications.value = Json.list(o.optJSONArray("notifications")) { Json.notification(it) }
            _referrals.value = o.optInt("referrals", 3)
        }.onFailure { _loggedIn.value = false }
        _locked.value = _loggedIn.value && _security.value.passcode.isNotEmpty()
    }

    private fun save() {
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.IO) {
            delay(250)
            val account = JSONObject().apply {
                put("user", Json.user(_user.value))
                put("security", Json.security(_security.value))
                put("balances", Json.balances(_balances.value))
                put("cards", Json.list(_cards.value) { Json.card(it) })
                put("tx", Json.list(_transactions.value.take(300)) { Json.tx(it) })
                put("notifications", Json.list(_notifications.value.take(100)) { Json.notification(it) })
                put("referrals", _referrals.value)
            }
            sp.edit()
                .putBoolean("onboarding", _onboardingSeen.value)
                .putBoolean("loggedIn", _loggedIn.value)
                .putString("prefs", Json.prefs(_prefs.value).toString())
                .putString("account", account.toString())
                .apply()
        }
    }

    // ------------------------------------------------------------------ market ticker

    private fun initialPrices(): Map<String, Price> {
        val rnd = Random(42)
        return MockData.coins.associate { c ->
            var p = c.basePrice
            val history = ArrayList<Float>(48)
            repeat(48) {
                p *= if (c.stable) 1 + (rnd.nextDouble() - 0.5) * 0.0006 else 1 + (rnd.nextDouble() - 0.48) * 0.018
                history += p.toFloat()
            }
            val change = if (c.stable) (rnd.nextDouble() - 0.5) * 0.06 else (p / history.first() - 1) * 100
            c.symbol to Price(if (c.stable) 1.0 + (rnd.nextDouble() - 0.5) * 0.0004 else p, change, history)
        }
    }

    private fun startTicker() {
        scope.launch {
            while (isActive) {
                delay(3_000)
                _prices.update { current ->
                    current.mapValues { (sym, price) ->
                        val coin = MockData.coin(sym)
                        val drift = if (coin.stable) (Random.nextDouble() - 0.5) * 0.0002 else (Random.nextDouble() - 0.5) * 0.004
                        val next = if (coin.stable) (1.0 + drift).coerceIn(0.998, 1.002) else price.usd * (1 + drift)
                        val history = (price.history.drop(1) + next.toFloat())
                        val change = if (coin.stable) price.change24h + drift * 10 else price.change24h + drift * 100
                        Price(next, change.coerceIn(-15.0, 15.0), history)
                    }
                }
            }
        }
    }

    fun priceOf(symbol: String): Double = _prices.value[symbol]?.usd ?: MockData.coin(symbol).basePrice

    fun balanceOf(symbol: String): Double = _balances.value[symbol] ?: 0.0

    fun totalUsd(): Double = _balances.value.entries.sumOf { (k, v) -> v * priceOf(k) }

    // ------------------------------------------------------------------ session / auth

    fun completeOnboarding() {
        _onboardingSeen.value = true
        save()
    }

    fun isRegistered(email: String): Boolean = _user.value.email.equals(email, true) && _security.value.password.isNotEmpty()

    fun checkPassword(email: String, password: String): Boolean {
        if (!isRegistered(email)) return password.length >= 8
        return _security.value.password == password
    }

    /** Creates a brand-new account (fresh = sign-up; otherwise a fully set-up returning account). */
    fun createSession(emailOrPhone: String, password: String, fresh: Boolean, referral: String = "") {
        val existing = isRegistered(emailOrPhone)
        if (!existing) seedAccount(emailOrPhone, password, fresh)
        _loggedIn.value = true
        _locked.value = false
        addNotification(NotificationKind.SECURITY, "New login detected", "Your account was accessed from Android · ${android.os.Build.MODEL}. If this wasn't you, change your password immediately.")
        save()
    }

    fun logout() {
        _loggedIn.value = false
        _locked.value = false
        _security.update { it.copy(passcode = "", biometricEnabled = false) }
        save()
    }

    fun deleteAccountData() {
        _loggedIn.value = false
        _user.value = UserProfile()
        _security.value = SecuritySettings()
        _balances.value = emptyMap()
        _cards.value = emptyList()
        _transactions.value = emptyList()
        _notifications.value = emptyList()
        save()
    }

    fun setPasscode(code: String) {
        _security.update { it.copy(passcode = code) }
        save()
    }

    fun verifyPasscode(code: String) = _security.value.passcode == code

    fun setBiometric(enabled: Boolean) {
        _security.update { it.copy(biometricEnabled = enabled) }
        save()
    }

    fun changePassword(newPassword: String) {
        _security.update { it.copy(password = newPassword) }
        addNotification(NotificationKind.SECURITY, "Password changed", "Your login password was changed successfully. Withdrawals are paused for 24 hours for your safety.")
        save()
    }

    fun setTwoFa(enabled: Boolean, secret: String = "") {
        _security.update { it.copy(twoFaEnabled = enabled, twoFaSecret = secret) }
        addNotification(NotificationKind.SECURITY, if (enabled) "2FA enabled" else "2FA disabled", if (enabled) "Google Authenticator is now required for withdrawals and logins." else "Two-factor authentication was turned off. We recommend keeping it on.")
        save()
    }

    fun setAntiPhishing(code: String) {
        _security.update { it.copy(antiPhishingCode = code) }
        save()
    }

    fun setWhitelist(enabled: Boolean) {
        _security.update { it.copy(withdrawWhitelist = enabled) }
        save()
    }

    fun lock() {
        if (_loggedIn.value && _security.value.passcode.isNotEmpty()) _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    fun removeDevice(id: String) {
        _devices.update { list -> list.filterNot { it.id == id } }
    }

    // ------------------------------------------------------------------ preferences / profile

    fun updatePrefs(transform: (Preferences) -> Preferences) {
        _prefs.update(transform)
        save()
    }

    fun updateUser(transform: (UserProfile) -> UserProfile) {
        _user.update(transform)
        save()
    }

    // ------------------------------------------------------------------ KYC

    fun submitKyc(fullName: String, country: String, dob: String, address: String) {
        _user.update { it.copy(fullName = fullName, country = country, dateOfBirth = dob, address = address, kycStatus = KycStatus.PENDING) }
        addNotification(NotificationKind.SYSTEM, "Verification submitted", "We're reviewing your documents. This usually takes a few minutes.")
        save()
        scope.launch {
            delay(12_000)
            _user.update { it.copy(kycStatus = KycStatus.VERIFIED, vipLevel = max(it.vipLevel, 1)) }
            addNotification(NotificationKind.SYSTEM, "Identity verified 🎉", "Your identity has been verified. You can now apply for a Zyro Pay card and enjoy higher limits.")
            save()
        }
    }

    // ------------------------------------------------------------------ wallet operations

    private fun newId(prefix: String) = prefix + UUID.randomUUID().toString().replace("-", "").take(14).uppercase()

    private fun hash(prefix: String = "0x"): String {
        val chars = "0123456789abcdef"
        return prefix + (1..64).map { chars[Random.nextInt(16)] }.joinToString("")
    }

    fun depositAddress(coin: String, network: String): String {
        val net = MockData.coin(coin).networks.firstOrNull { it.id == network } ?: MockData.coin(coin).networks.first()
        val seed = Random((_user.value.uid + coin + network).hashCode())
        return when {
            net.addressPrefix == "T" -> "T" + base58(seed, 33)
            net.addressPrefix == "0x" -> "0x" + (1..40).map { "0123456789abcdef"[seed.nextInt(16)] }.joinToString("")
            net.addressPrefix == "bc1q" -> "bc1q" + (1..38).map { "023456789acdefghjklmnpqrstuvwxyz"[seed.nextInt(32)] }.joinToString("")
            net.addressPrefix == "UQ" -> "UQ" + base58(seed, 46)
            else -> base58(seed, 44)
        }
    }

    private fun base58(r: Random, n: Int): String {
        val a = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        return (1..n).map { a[r.nextInt(a.length)] }.joinToString("")
    }

    private fun addTx(tx: Transaction) {
        _transactions.update { listOf(tx) + it }
    }

    private fun updateTx(id: String, transform: (Transaction) -> Transaction) {
        _transactions.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun credit(symbol: String, amount: Double) {
        _balances.update { it + (symbol to ((it[symbol] ?: 0.0) + amount).coerceAtLeast(0.0)) }
    }

    /** Simulates an incoming on-chain deposit that confirms after a few seconds. */
    fun simulateDeposit(coin: String, network: String, amount: Double): String {
        val id = newId("DP")
        val tx = Transaction(
            id = id, type = TxType.DEPOSIT, title = "Deposit $coin", subtitle = "From external wallet", coin = coin,
            amount = amount, usdValue = amount * priceOf(coin), timestamp = System.currentTimeMillis(),
            status = TxStatus.PENDING, category = TxCategory.TRANSFER, network = network,
            address = depositAddress(coin, network), txHash = hash(),
        )
        addTx(tx)
        addNotification(NotificationKind.TRANSACTION, "Deposit detected", "${Fmt.coin(amount, coin)} is on the way. Waiting for network confirmations.", id)
        save()
        scope.launch {
            delay(6_000)
            updateTx(id) { it.copy(status = TxStatus.SUCCESS) }
            credit(coin, amount)
            addNotification(NotificationKind.TRANSACTION, "Deposit successful", "${Fmt.coin(amount, coin)} has been credited to your wallet.", id)
            save()
        }
        return id
    }

    fun withdraw(coin: String, network: String, address: String, amount: Double, fee: Double, memo: String?): String {
        val id = newId("WD")
        credit(coin, -(amount + fee))
        addTx(
            Transaction(
                id = id, type = TxType.WITHDRAW, title = "Withdraw $coin", subtitle = "To " + Fmt.shortAddress(address), coin = coin,
                amount = -amount, usdValue = amount * priceOf(coin), timestamp = System.currentTimeMillis(),
                status = TxStatus.PENDING, category = TxCategory.TRANSFER, network = network, address = address, fee = fee, note = memo,
            ),
        )
        addNotification(NotificationKind.TRANSACTION, "Withdrawal submitted", "Your withdrawal of ${Fmt.coin(amount, coin)} is being processed.", id)
        save()
        scope.launch {
            delay(8_000)
            updateTx(id) { it.copy(status = TxStatus.SUCCESS, txHash = hash()) }
            addNotification(NotificationKind.TRANSACTION, "Withdrawal completed", "${Fmt.coin(amount, coin)} was sent to ${Fmt.shortAddress(address)}.", id)
            save()
        }
        return id
    }

    fun sendInternal(coin: String, recipient: String, amount: Double, note: String?): String {
        val id = newId("TR")
        credit(coin, -amount)
        addTx(
            Transaction(
                id = id, type = TxType.SEND, title = "Sent to $recipient", subtitle = "Zyro Pay transfer · Free", coin = coin,
                amount = -amount, usdValue = amount * priceOf(coin), timestamp = System.currentTimeMillis(),
                category = TxCategory.TRANSFER, address = recipient, note = note,
            ),
        )
        addNotification(NotificationKind.TRANSACTION, "Transfer successful", "You sent ${Fmt.coin(amount, coin)} to $recipient.", id)
        save()
        return id
    }

    fun swapRate(from: String, to: String): Double = priceOf(from) / priceOf(to) * 0.998

    fun swap(from: String, to: String, amount: Double): String {
        val id = newId("SW")
        val received = amount * swapRate(from, to)
        credit(from, -amount)
        credit(to, received)
        addTx(
            Transaction(
                id = id, type = TxType.SWAP, title = "Swap $from → $to", subtitle = "Instant swap", coin = from,
                amount = -amount, usdValue = amount * priceOf(from), timestamp = System.currentTimeMillis(),
                toCoin = to, toAmount = received, fee = amount * 0.002,
            ),
        )
        addNotification(NotificationKind.TRANSACTION, "Swap completed", "${Fmt.coin(amount, from)} swapped to ${Fmt.coin(received, to)}.", id)
        save()
        return id
    }

    fun topUpCard(cardId: String, coin: String, amount: Double): String {
        val id = newId("TU")
        val usd = amount * priceOf(coin)
        credit(coin, -amount)
        _cards.update { list -> list.map { if (it.id == cardId) it.copy(balance = it.balance + usd) else it } }
        val card = _cards.value.firstOrNull { it.id == cardId }
        addTx(
            Transaction(
                id = id, type = TxType.CARD_TOP_UP, title = "Card top-up", subtitle = "${card?.title ?: "Card"} ·· ${card?.last4 ?: ""}",
                coin = coin, amount = -amount, usdValue = usd, timestamp = System.currentTimeMillis(), cardId = cardId,
                category = TxCategory.TRANSFER,
            ),
        )
        addNotification(NotificationKind.TRANSACTION, "Card topped up", "${Fmt.fiat(usd)} has been added to your card ending ${card?.last4}.", id)
        save()
        return id
    }

    fun payMerchant(merchant: Merchant, usd: Double, cardId: String?, coin: String): String {
        val id = newId("PY")
        if (cardId != null) {
            _cards.update { list -> list.map { if (it.id == cardId) it.copy(balance = it.balance - usd, spentToday = it.spentToday + usd, spentMonth = it.spentMonth + usd) else it } }
        } else {
            credit(coin, -usd / priceOf(coin))
        }
        addTx(
            Transaction(
                id = id, type = if (cardId != null) TxType.CARD_PAYMENT else TxType.PAY, title = merchant.name,
                subtitle = merchant.city, coin = if (cardId != null) "USD" else coin,
                amount = -(if (cardId != null) usd else usd / priceOf(coin)), usdValue = usd,
                timestamp = System.currentTimeMillis(), category = merchant.category, merchant = merchant.name, cardId = cardId,
            ),
        )
        val cashback = usd * 0.02
        credit("USDT", cashback)
        addTx(
            Transaction(
                id = newId("CB"), type = TxType.CASHBACK, title = "Cashback reward", subtitle = "2% on ${merchant.name}", coin = "USDT",
                amount = cashback, usdValue = cashback, timestamp = System.currentTimeMillis() + 1, category = TxCategory.CASHBACK,
            ),
        )
        addNotification(NotificationKind.TRANSACTION, "Payment successful", "You paid ${Fmt.fiat(usd)} to ${merchant.name}. You earned ${Fmt.fiat(cashback)} cashback.", id)
        save()
        return id
    }

    fun p2pTrade(offer: P2POffer, buy: Boolean, coin: String, coinAmount: Double): String {
        val id = newId("PP")
        credit(coin, if (buy) coinAmount else -coinAmount)
        val fiatTotal = coinAmount * offer.price
        addTx(
            Transaction(
                id = id, type = if (buy) TxType.P2P_BUY else TxType.P2P_SELL, title = (if (buy) "P2P buy · " else "P2P sell · ") + offer.trader,
                subtitle = "${MockData.fiat(offer.fiat).symbol}${Fmt.money(fiatTotal)} via ${offer.methods.first()}", coin = coin,
                amount = if (buy) coinAmount else -coinAmount, usdValue = coinAmount * priceOf(coin),
                timestamp = System.currentTimeMillis(), category = TxCategory.TRANSFER,
            ),
        )
        addNotification(NotificationKind.TRANSACTION, "P2P order completed", "${if (buy) "Bought" else "Sold"} ${Fmt.coin(coinAmount, coin)} with ${offer.trader}.", id)
        save()
        return id
    }

    // ------------------------------------------------------------------ cards

    private fun luhnNumber(prefix: String): String {
        val digits = (prefix + (1..(15 - prefix.length)).map { Random.nextInt(10) }.joinToString("")).map { it - '0' }
        var sum = 0
        digits.reversed().forEachIndexed { i, d ->
            var v = d
            if (i % 2 == 0) { v *= 2; if (v > 9) v -= 9 }
            sum += v
        }
        return digits.joinToString("") + ((10 - sum % 10) % 10)
    }

    private fun newCard(kind: CardKind, design: CardDesign, balance: Double, status: CardStatus = CardStatus.ACTIVE): PaymentCard {
        val year = (java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + 4) % 100
        return PaymentCard(
            id = newId("CD"), kind = kind, design = design, holder = _user.value.fullName.ifBlank { "ZYRO MEMBER" }.uppercase(),
            number = luhnNumber("489315"), expiry = "%02d/%02d".format(Random.nextInt(1, 13), year),
            cvv = "%03d".format(Random.nextInt(1000)), balance = balance, status = status,
        )
    }

    fun cardFee(kind: CardKind) = if (kind == CardKind.VIRTUAL) 10.0 else 100.0

    fun applyCard(kind: CardKind, design: CardDesign, shippingAddress: String?): String {
        val fee = cardFee(kind)
        credit("USDT", -fee)
        val card = newCard(kind, design, 0.0, if (kind == CardKind.PHYSICAL) CardStatus.SHIPPING else CardStatus.ACTIVE)
        _cards.update { it + card }
        addTx(
            Transaction(
                id = newId("CF"), type = TxType.CARD_FEE, title = "${card.title} issuance", subtitle = design.label, coin = "USDT",
                amount = -fee, usdValue = fee, timestamp = System.currentTimeMillis(), category = TxCategory.FEE, cardId = card.id,
                address = shippingAddress,
            ),
        )
        addNotification(
            NotificationKind.SYSTEM,
            if (kind == CardKind.VIRTUAL) "Your virtual card is ready" else "Physical card ordered",
            if (kind == CardKind.VIRTUAL) "Your ${design.label} card ending ${card.last4} is active. Top up and start spending." else "Your ${design.label} card is being produced and will ship within 3 business days.",
        )
        save()
        if (kind == CardKind.PHYSICAL) {
            scope.launch {
                delay(20_000)
                _cards.update { list -> list.map { if (it.id == card.id) it.copy(status = CardStatus.ACTIVE) else it } }
                addNotification(NotificationKind.SYSTEM, "Physical card activated", "Your card ending ${card.last4} has been delivered and activated.")
                save()
            }
        }
        return card.id
    }

    fun updateCard(id: String, transform: (PaymentCard) -> PaymentCard) {
        _cards.update { list -> list.map { if (it.id == id) transform(it) else it } }
        save()
    }

    fun setCardFrozen(id: String, frozen: Boolean) {
        updateCard(id) { it.copy(frozen = frozen) }
        val card = _cards.value.firstOrNull { it.id == id } ?: return
        addNotification(NotificationKind.SECURITY, if (frozen) "Card frozen" else "Card unfrozen", "Your card ending ${card.last4} has been ${if (frozen) "temporarily frozen. All transactions will be declined." else "unfrozen and is ready to use."}")
        save()
    }

    fun cancelCard(id: String) {
        val card = _cards.value.firstOrNull { it.id == id } ?: return
        if (card.balance > 0) credit("USDT", card.balance)
        _cards.update { list -> list.filterNot { it.id == id } }
        addNotification(NotificationKind.SECURITY, "Card cancelled", "Your card ending ${card.last4} was cancelled. Remaining balance has been returned to your USDT wallet.")
        save()
    }

    fun replaceCard(id: String) {
        val old = _cards.value.firstOrNull { it.id == id } ?: return
        val fresh = newCard(old.kind, old.design, old.balance, if (old.kind == CardKind.PHYSICAL) CardStatus.SHIPPING else CardStatus.ACTIVE)
        _cards.update { list -> list.map { if (it.id == id) fresh else it } }
        addNotification(NotificationKind.SECURITY, "Card replaced", "A new card ending ${fresh.last4} has replaced your old card. Update your saved payment details.")
        save()
    }

    // ------------------------------------------------------------------ notifications

    fun addNotification(kind: NotificationKind, title: String, body: String, txId: String? = null) {
        _notifications.update {
            listOf(AppNotification(newId("NT"), kind, title, body, System.currentTimeMillis(), false, txId)) + it
        }
    }

    fun markNotificationRead(id: String) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
        save()
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(read = true) } }
        save()
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
        save()
    }

    // ------------------------------------------------------------------ support chat

    fun startChatIfNeeded() {
        if (_chat.value.isNotEmpty()) return
        _chat.value = listOf(
            ChatMessage(1, false, "Hi ${_user.value.nickname.ifBlank { "there" }} 👋 I'm Mia from Zyro Pay Support.", System.currentTimeMillis() - 2000),
            ChatMessage(2, false, "How can I help you today? You can ask about cards, deposits, withdrawals or verification.", System.currentTimeMillis() - 1000),
        )
    }

    fun sendChat(text: String) {
        val now = System.currentTimeMillis()
        _chat.update { it + ChatMessage(now, true, text, now) }
        scope.launch {
            delay(700)
            _agentTyping.value = true
            delay(1_600 + Random.nextLong(1_200))
            _agentTyping.value = false
            val reply = supportReply(text.lowercase())
            _chat.update { it + ChatMessage(System.currentTimeMillis(), false, reply, System.currentTimeMillis()) }
        }
    }

    private fun supportReply(q: String): String = when {
        "card" in q && ("lost" in q || "stolen" in q) -> "I'm sorry to hear that. Please freeze your card right away from the Card tab. You can then request a replacement in Card settings › Replace card. Your balance stays safe."
        "card" in q -> "Our virtual card is issued instantly after verification (10 USDT) and the physical card ships worldwide (100 USDT). You can manage limits, freeze and view details from the Card tab."
        "deposit" in q -> "Deposits are credited after network confirmations — usually within 1–20 minutes. Please make sure the network you send from matches the deposit network you selected."
        "withdraw" in q -> "Withdrawals are processed within minutes. If yours is pending longer than 30 minutes, please share the transaction ID and I'll check it for you."
        "kyc" in q || "verif" in q -> "Identity verification usually takes a few minutes. You'll need a valid ID or passport and a quick selfie. You can start from Me › Identity verification."
        "fee" in q -> "Zyro Pay has no monthly fees. Card spending has a 1% FX fee for non-USD transactions and network fees apply for on-chain withdrawals."
        "thank" in q -> "You're welcome! Is there anything else I can help you with? 😊"
        "hi" in q || "hello" in q -> "Hello! How can I help you today?"
        else -> "Thanks for the details. I've created ticket #ZP${Random.nextInt(100000, 999999)} and a specialist will follow up shortly. Is there anything else I can help with meanwhile?"
    }

    // ------------------------------------------------------------------ referral

    fun simulateReferral() {
        _referrals.update { it + 1 }
        credit("USDT", 5.0)
        addTx(
            Transaction(
                id = newId("RW"), type = TxType.REWARD, title = "Referral reward", subtitle = "A friend joined Zyro Pay", coin = "USDT",
                amount = 5.0, usdValue = 5.0, timestamp = System.currentTimeMillis(), category = TxCategory.CASHBACK,
            ),
        )
        addNotification(NotificationKind.PROMO, "You earned 5 USDT 🎁", "A friend joined with your referral code. Keep inviting to earn more!")
        save()
    }

    // ------------------------------------------------------------------ seed

    private fun seedDevices(): List<Device> {
        val now = System.currentTimeMillis()
        return listOf(
            Device("d0", "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}", "Hong Kong · This device", now, true),
            Device("d1", "iPhone 15 Pro", "Singapore · Zyro Pay iOS 1.0.0", now - 3 * Fmt.DAY, false),
            Device("d2", "Chrome on macOS", "Dubai, AE · Web", now - 12 * Fmt.DAY, false),
        )
    }

    private fun seedAccount(login: String, password: String, fresh: Boolean) {
        val isEmail = "@" in login
        val handle = if (isEmail) login.substringBefore("@") else "user" + login.takeLast(4)
        val niceName = handle.split('.', '_', '-').filter { it.isNotBlank() }.joinToString(" ") { p -> p.replaceFirstChar { it.uppercase() } }
        val rnd = Random(System.currentTimeMillis())
        _user.value = UserProfile(
            fullName = if (fresh) "" else niceName.ifBlank { "Alex Morgan" },
            nickname = niceName.ifBlank { "Zyro User" },
            email = if (isEmail) login else "",
            phone = if (!isEmail) login else "",
            uid = (10_000_000 + rnd.nextInt(89_999_999)).toString(),
            avatar = rnd.nextInt(8),
            kycStatus = if (fresh) KycStatus.NOT_STARTED else KycStatus.VERIFIED,
            country = if (fresh) "" else "Hong Kong",
            vipLevel = if (fresh) 1 else 2,
            referralCode = "ZY" + (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[rnd.nextInt(32)] }.joinToString(""),
            joinedAt = System.currentTimeMillis() - if (fresh) 0 else 86 * Fmt.DAY,
        )
        _security.value = SecuritySettings(password = password)
        _referrals.value = if (fresh) 0 else 3

        val now = System.currentTimeMillis()
        val txs = ArrayList<Transaction>()
        if (fresh) {
            _balances.value = mapOf("USDT" to 250.0, "USDC" to 0.0, "BTC" to 0.0, "ETH" to 0.0, "BNB" to 0.0, "SOL" to 0.0, "TRX" to 0.0, "TON" to 0.0, "MATIC" to 0.0)
            _cards.value = emptyList()
            txs += Transaction(newId("RW"), TxType.REWARD, "Welcome bonus", "New member gift", "USDT", 50.0, 50.0, now - 60_000, category = TxCategory.CASHBACK)
            txs += Transaction(newId("DP"), TxType.DEPOSIT, "Deposit USDT", "From external wallet", "USDT", 200.0, 200.0, now - 120_000, category = TxCategory.TRANSFER, network = "TRC20", txHash = hash())
        } else {
            _balances.value = mapOf("USDT" to 3_842.56, "USDC" to 1_250.0, "BTC" to 0.04215, "ETH" to 1.2841, "BNB" to 2.15, "SOL" to 18.4, "TRX" to 2_150.0, "TON" to 85.0, "MATIC" to 0.0)
            val virtual = newCard(CardKind.VIRTUAL, CardDesign.VIOLET, 1_284.35)
            val physical = newCard(CardKind.PHYSICAL, CardDesign.BLACK, 640.0).copy(spentToday = 86.4, spentMonth = 1_920.0)
            _cards.value = listOf(virtual, physical)

            val spends = listOf(
                Triple("Starbucks", 6.80, 0), Triple("Uber Trip", 18.40, 0), Triple("Netflix", 15.49, 1),
                Triple("Amazon Marketplace", 129.99, 1), Triple("McDonald's", 11.20, 2), Triple("Spotify Premium", 10.99, 3),
                Triple("Emirates Airlines", 684.00, 4), Triple("Booking.com Hotel", 412.30, 5), Triple("7-Eleven", 8.45, 5),
                Triple("Apple Store", 249.00, 7), Triple("Shell Station", 54.10, 8), Triple("Steam Games", 29.99, 9),
                Triple("Google Cloud", 42.18, 11), Triple("Watsons Pharmacy", 23.60, 12), Triple("Fitness First", 65.00, 14),
                Triple("Starbucks", 5.90, 15), Triple("Mobile Top-up", 20.00, 17), Triple("Uber Trip", 22.75, 19),
            )
            spends.forEachIndexed { i, (name, usd, daysAgo) ->
                val m = MockData.merchants.first { it.name == name }
                val ts = now - daysAgo * Fmt.DAY - (i * 37 + 20) * 60_000L
                val card = if (i % 3 == 0) physical else virtual
                txs += Transaction(newId("PY"), TxType.CARD_PAYMENT, m.name, m.city, "USD", -usd, usd, ts, category = m.category, merchant = m.name, cardId = card.id)
                if (i % 2 == 0) txs += Transaction(newId("CB"), TxType.CASHBACK, "Cashback reward", "2% on ${m.name}", "USDT", usd * 0.02, usd * 0.02, ts + 1000, category = TxCategory.CASHBACK)
            }
            txs += Transaction(newId("TU"), TxType.CARD_TOP_UP, "Card top-up", "Virtual Card ·· ${virtual.last4}", "USDT", -500.0, 500.0, now - 2 * Fmt.DAY, cardId = virtual.id, category = TxCategory.TRANSFER)
            txs += Transaction(newId("DP"), TxType.DEPOSIT, "Deposit USDT", "From external wallet", "USDT", 2_000.0, 2_000.0, now - 3 * Fmt.DAY, category = TxCategory.TRANSFER, network = "TRC20", txHash = hash(), address = depositAddress("USDT", "TRC20"))
            txs += Transaction(newId("SW"), TxType.SWAP, "Swap USDT → BTC", "Instant swap", "USDT", -1_000.0, 1_000.0, now - 6 * Fmt.DAY, toCoin = "BTC", toAmount = 0.01483, fee = 2.0)
            txs += Transaction(newId("WD"), TxType.WITHDRAW, "Withdraw ETH", "To 0x8f3a…c21b9e", "ETH", -0.25, 0.25 * 3284, now - 10 * Fmt.DAY, category = TxCategory.TRANSFER, network = "ERC20", txHash = hash(), fee = 0.0012, address = "0x8f3a9d1e44b2c0aa71f6e3b9902d6c5e8ac21b9e")
            txs += Transaction(newId("TR"), TxType.RECEIVE, "Received from Sarah K.", "Zyro Pay transfer", "USDT", 150.0, 150.0, now - 13 * Fmt.DAY, category = TxCategory.TRANSFER)
            txs += Transaction(newId("RF"), TxType.REFUND, "Refund · Amazon Marketplace", "Order returned", "USD", 34.99, 34.99, now - 16 * Fmt.DAY, category = TxCategory.REFUND, merchant = "Amazon Marketplace", cardId = virtual.id)
            txs += Transaction(newId("DP"), TxType.DEPOSIT, "Deposit BTC", "From external wallet", "BTC", 0.0275, 0.0275 * 67420, now - 20 * Fmt.DAY, category = TxCategory.TRANSFER, network = "BTC", txHash = hash(""))
            txs += Transaction(newId("PP"), TxType.P2P_BUY, "P2P buy · SwiftTrade", "HK$7,850.00 via FPS", "USDT", 1_000.0, 1_000.0, now - 24 * Fmt.DAY, category = TxCategory.TRANSFER)
            txs += Transaction(newId("WD"), TxType.WITHDRAW, "Withdraw USDT", "To TQn9Y2…x8Lk2P", "USDT", -300.0, 300.0, now - 27 * Fmt.DAY, status = TxStatus.FAILED, network = "TRC20", address = "TQn9Y2khEsLJW1ChVWFMSMeRDow5x8Lk2P", note = "Address flagged by risk control. Funds returned.")
            txs += Transaction(newId("CF"), TxType.CARD_FEE, "Physical Card issuance", "Obsidian Metal", "USDT", -100.0, 100.0, now - 40 * Fmt.DAY, category = TxCategory.FEE, cardId = physical.id)
        }
        _transactions.value = txs.sortedByDescending { it.timestamp }

        _notifications.value = listOf(
            AppNotification(newId("NT"), NotificationKind.PROMO, "Earn 2% cashback on every spend", "Use your Zyro Pay card anywhere and get up to 2% back in USDT. No caps, no fees.", now - 30 * 60_000L),
            AppNotification(newId("NT"), NotificationKind.SYSTEM, "Welcome to Zyro Pay 🎉", "Your account is ready. Complete identity verification to unlock your card and higher limits.", now - 2 * 60_000L),
            AppNotification(newId("NT"), NotificationKind.SECURITY, "Secure your account", "Turn on biometric unlock and Google Authenticator 2FA for maximum protection.", now - 3 * 3_600_000L, read = !fresh),
            AppNotification(newId("NT"), NotificationKind.PROMO, "Invite friends, earn 5 USDT", "Share your referral code. You and your friend both get rewarded when they verify.", now - 26 * 3_600_000L, read = true),
            AppNotification(newId("NT"), NotificationKind.SYSTEM, "Scheduled maintenance", "TRC20 deposits may be delayed on Sunday 02:00–03:00 UTC due to network upgrade.", now - 4 * Fmt.DAY, read = true),
        ).sortedByDescending { it.timestamp }
    }
}
