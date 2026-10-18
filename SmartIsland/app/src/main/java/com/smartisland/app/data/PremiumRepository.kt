package com.smartisland.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Plan { NONE, MONTHLY, YEARLY }

data class PremiumState(
    val plan: Plan = Plan.NONE,
    val expiresAt: Long = 0L,
    val autoRenew: Boolean = false,
    val unlockedWidgets: Set<String> = emptySet(),
) {
    val isPremium: Boolean get() = plan != Plan.NONE && expiresAt > System.currentTimeMillis()

    fun isWidgetUnlocked(widgetId: String, premiumOnly: Boolean): Boolean =
        !premiumOnly || isPremium || widgetId in unlockedWidgets
}

/** Features gated behind the subscription. Island basics and size controls stay free. */
enum class PremiumFeature(val title: String) {
    NOTIFICATIONS("Notification alerts on the island"),
    VOLUME_HUD("Island volume HUD (replaces system panel)"),
    EDGE_PANEL("Floating power & volume edge panel"),
    STYLES("Accent colors and glow"),
}

/**
 * Stores the entitlement locally. With mock billing this is the source of truth; with real Google Play
 * Billing it acts as a cache that [com.smartisland.app.billing.BillingGateway] refreshes on launch.
 */
class PremiumRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("premium_state", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    val current: PremiumState get() = _state.value

    init {
        renewIfNeeded()
    }

    fun grant(plan: Plan, durationMs: Long) {
        val start = maxOf(System.currentTimeMillis(), current.expiresAt)
        commit(current.copy(plan = plan, expiresAt = start + durationMs, autoRenew = true))
    }

    fun cancelAutoRenew() = commit(current.copy(autoRenew = false))

    fun expireNow() = commit(current.copy(plan = Plan.NONE, expiresAt = 0L, autoRenew = false))

    fun unlockWidget(widgetId: String) =
        commit(current.copy(unlockedWidgets = current.unlockedWidgets + widgetId))

    fun resetAll() = commit(PremiumState())

    /** Mock subscriptions renew themselves the way Play would while auto-renew is on. */
    fun renewIfNeeded() {
        val s = current
        if (s.plan == Plan.NONE || s.expiresAt > System.currentTimeMillis()) return
        if (s.autoRenew) {
            val period = if (s.plan == Plan.YEARLY) YEAR_MS else MONTH_MS
            var expiry = s.expiresAt
            while (expiry <= System.currentTimeMillis()) expiry += period
            commit(s.copy(expiresAt = expiry))
        } else {
            commit(s.copy(plan = Plan.NONE, expiresAt = 0L))
        }
    }

    private fun commit(next: PremiumState) {
        _state.value = next
        prefs.edit()
            .putString(K_PLAN, next.plan.name)
            .putLong(K_EXPIRES, next.expiresAt)
            .putBoolean(K_RENEW, next.autoRenew)
            .putStringSet(K_WIDGETS, next.unlockedWidgets)
            .apply()
    }

    private fun read() = PremiumState(
        plan = runCatching { Plan.valueOf(prefs.getString(K_PLAN, null) ?: "NONE") }.getOrDefault(Plan.NONE),
        expiresAt = prefs.getLong(K_EXPIRES, 0L),
        autoRenew = prefs.getBoolean(K_RENEW, false),
        unlockedWidgets = prefs.getStringSet(K_WIDGETS, emptySet())?.toSet() ?: emptySet(),
    )

    companion object {
        const val MONTH_MS = 30L * 24 * 60 * 60 * 1000
        const val YEAR_MS = 365L * 24 * 60 * 60 * 1000

        private const val K_PLAN = "plan"
        private const val K_EXPIRES = "expires_at"
        private const val K_RENEW = "auto_renew"
        private const val K_WIDGETS = "unlocked_widgets"
    }
}
