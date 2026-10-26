package com.zyropay.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Hand-written JSON mapping for the mock store (keeps the APK free of serialization libraries). */
internal object Json {

    fun user(u: UserProfile) = JSONObject().apply {
        put("fullName", u.fullName); put("nickname", u.nickname); put("email", u.email); put("phone", u.phone)
        put("countryCode", u.countryCode); put("uid", u.uid); put("avatar", u.avatar); put("kyc", u.kycStatus.name)
        put("country", u.country); put("dob", u.dateOfBirth); put("address", u.address); put("vip", u.vipLevel)
        put("ref", u.referralCode); put("joined", u.joinedAt)
    }

    fun user(o: JSONObject) = UserProfile(
        fullName = o.optString("fullName"), nickname = o.optString("nickname"), email = o.optString("email"),
        phone = o.optString("phone"), countryCode = o.optString("countryCode", "+852"), uid = o.optString("uid"),
        avatar = o.optInt("avatar"), kycStatus = enumOr(o.optString("kyc"), KycStatus.NOT_STARTED),
        country = o.optString("country"), dateOfBirth = o.optString("dob"), address = o.optString("address"),
        vipLevel = o.optInt("vip", 1), referralCode = o.optString("ref"), joinedAt = o.optLong("joined"),
    )

    fun security(s: SecuritySettings) = JSONObject().apply {
        put("passcode", s.passcode); put("password", s.password); put("bio", s.biometricEnabled)
        put("twoFa", s.twoFaEnabled); put("twoFaSecret", s.twoFaSecret); put("anti", s.antiPhishingCode)
        put("whitelist", s.withdrawWhitelist)
    }

    fun security(o: JSONObject) = SecuritySettings(
        passcode = o.optString("passcode"), password = o.optString("password"), biometricEnabled = o.optBoolean("bio"),
        twoFaEnabled = o.optBoolean("twoFa"), twoFaSecret = o.optString("twoFaSecret"),
        antiPhishingCode = o.optString("anti"), withdrawWhitelist = o.optBoolean("whitelist"),
    )

    fun prefs(p: Preferences) = JSONObject().apply {
        put("theme", p.themeMode.name); put("currency", p.currency); put("language", p.language)
        put("hide", p.hideBalance); put("pTx", p.pushTransactions); put("pSec", p.pushSecurity)
        put("pPromo", p.pushPromotions); put("email", p.emailNotifications); put("haptics", p.haptics)
    }

    fun prefs(o: JSONObject) = Preferences(
        themeMode = enumOr(o.optString("theme"), ThemeMode.LIGHT), currency = o.optString("currency", "USD"),
        language = o.optString("language", "English"), hideBalance = o.optBoolean("hide"),
        pushTransactions = o.optBoolean("pTx", true), pushSecurity = o.optBoolean("pSec", true),
        pushPromotions = o.optBoolean("pPromo"), emailNotifications = o.optBoolean("email", true),
        haptics = o.optBoolean("haptics", true),
    )

    fun card(c: PaymentCard) = JSONObject().apply {
        put("id", c.id); put("kind", c.kind.name); put("design", c.design.name); put("holder", c.holder)
        put("number", c.number); put("expiry", c.expiry); put("cvv", c.cvv); put("balance", c.balance)
        put("frozen", c.frozen); put("online", c.onlinePayments); put("atm", c.atmWithdrawals)
        put("contactless", c.contactless); put("daily", c.dailyLimit); put("monthly", c.monthlyLimit)
        put("spentToday", c.spentToday); put("spentMonth", c.spentMonth); put("status", c.status.name)
        put("created", c.createdAt)
    }

    fun card(o: JSONObject) = PaymentCard(
        id = o.getString("id"), kind = enumOr(o.optString("kind"), CardKind.VIRTUAL),
        design = enumOr(o.optString("design"), CardDesign.VIOLET), holder = o.optString("holder"),
        number = o.optString("number"), expiry = o.optString("expiry"), cvv = o.optString("cvv"),
        balance = o.optDouble("balance", 0.0), frozen = o.optBoolean("frozen"), onlinePayments = o.optBoolean("online", true),
        atmWithdrawals = o.optBoolean("atm", true), contactless = o.optBoolean("contactless", true),
        dailyLimit = o.optDouble("daily", 5000.0), monthlyLimit = o.optDouble("monthly", 50000.0),
        spentToday = o.optDouble("spentToday", 0.0), spentMonth = o.optDouble("spentMonth", 0.0),
        status = enumOr(o.optString("status"), CardStatus.ACTIVE), createdAt = o.optLong("created"),
    )

    fun tx(t: Transaction) = JSONObject().apply {
        put("id", t.id); put("type", t.type.name); put("title", t.title); put("subtitle", t.subtitle)
        put("coin", t.coin); put("amount", t.amount); put("usd", t.usdValue); put("ts", t.timestamp)
        put("status", t.status.name); t.category?.let { put("cat", it.name) }; t.merchant?.let { put("merchant", it) }
        t.cardId?.let { put("card", it) }; t.network?.let { put("network", it) }; t.address?.let { put("address", it) }
        t.txHash?.let { put("hash", it) }; put("fee", t.fee); t.toCoin?.let { put("toCoin", it) }
        t.toAmount?.let { put("toAmount", it) }; t.note?.let { put("note", it) }
    }

    fun tx(o: JSONObject) = Transaction(
        id = o.getString("id"), type = enumOr(o.optString("type"), TxType.DEPOSIT), title = o.optString("title"),
        subtitle = o.optString("subtitle"), coin = o.optString("coin", "USDT"), amount = o.optDouble("amount", 0.0),
        usdValue = o.optDouble("usd", 0.0), timestamp = o.optLong("ts"), status = enumOr(o.optString("status"), TxStatus.SUCCESS),
        category = o.optStringOrNull("cat")?.let { enumOr(it, TxCategory.SHOPPING) }, merchant = o.optStringOrNull("merchant"),
        cardId = o.optStringOrNull("card"), network = o.optStringOrNull("network"), address = o.optStringOrNull("address"),
        txHash = o.optStringOrNull("hash"), fee = o.optDouble("fee", 0.0), toCoin = o.optStringOrNull("toCoin"),
        toAmount = if (o.has("toAmount")) o.optDouble("toAmount") else null, note = o.optStringOrNull("note"),
    )

    fun notification(n: AppNotification) = JSONObject().apply {
        put("id", n.id); put("kind", n.kind.name); put("title", n.title); put("body", n.body)
        put("ts", n.timestamp); put("read", n.read); n.txId?.let { put("tx", it) }
    }

    fun notification(o: JSONObject) = AppNotification(
        id = o.getString("id"), kind = enumOr(o.optString("kind"), NotificationKind.SYSTEM), title = o.optString("title"),
        body = o.optString("body"), timestamp = o.optLong("ts"), read = o.optBoolean("read"), txId = o.optStringOrNull("tx"),
    )

    fun balances(m: Map<String, Double>) = JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }

    fun balances(o: JSONObject): Map<String, Double> = buildMap { o.keys().forEach { put(it, o.optDouble(it, 0.0)) } }

    fun <T> list(items: List<T>, map: (T) -> JSONObject) = JSONArray().apply { items.forEach { put(map(it)) } }

    fun <T> list(a: JSONArray?, map: (JSONObject) -> T): List<T> {
        if (a == null) return emptyList()
        return (0 until a.length()).mapNotNull { i -> runCatching { map(a.getJSONObject(i)) }.getOrNull() }
    }

    private fun JSONObject.optStringOrNull(key: String): String? = if (has(key) && !isNull(key)) optString(key) else null

    inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        runCatching { enumValueOf<E>(name ?: "") }.getOrDefault(default)
}
