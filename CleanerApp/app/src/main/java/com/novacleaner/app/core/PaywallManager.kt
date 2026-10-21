package com.novacleaner.app.core

import kotlin.random.Random

/**
 * Decides when the "Remove Ads" paywall pops up on its own. It appears occasionally (random
 * chance), never twice in one session, never more than once every few hours and never for
 * users who already bought it.
 */
class PaywallManager(
    private val prefs: Prefs,
    private val isPremium: () -> Boolean,
) {
    enum class Trigger(val chance: Float) { LAUNCH(0.40f), AFTER_RESULT(0.30f) }

    private var shownThisSession = false
    private var launchEvaluated = false

    /** Evaluated once per app process, the first time home appears after the tour is done. */
    suspend fun shouldShowOnLaunch(): Boolean {
        if (launchEvaluated) return false
        launchEvaluated = true
        return shouldShow(Trigger.LAUNCH)
    }

    suspend fun shouldShow(trigger: Trigger): Boolean {
        if (isPremium() || shownThisSession) return false
        val settings = prefs.snapshot()
        if (!settings.tourDone) return false
        if (trigger == Trigger.LAUNCH && settings.launchCount < 2) return false
        if (System.currentTimeMillis() - settings.lastPaywallAt < MIN_GAP_MS) return false
        return Random.nextFloat() < trigger.chance
    }

    suspend fun markShown() {
        shownThisSession = true
        prefs.setLastPaywallAt(System.currentTimeMillis())
    }

    private companion object {
        const val MIN_GAP_MS = 6 * 60 * 60 * 1000L
    }
}
