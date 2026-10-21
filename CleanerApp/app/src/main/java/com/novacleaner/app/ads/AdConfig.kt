package com.novacleaner.app.ads

/**
 * Central place to tune ad pacing. Every interstitial/app-open decision reads from here so the
 * experience can be adjusted without touching screen code.
 */
object AdConfig {
    /** Minimum gap between any two full-screen ads (interstitial or app open). */
    const val FULLSCREEN_COOLDOWN_MS = 30_000L

    /** App-open on resume: the app must have been in background at least this long... */
    const val APP_OPEN_MIN_BACKGROUND_MS = 15_000L

    /** ...and at least this long since the previous app-open ad. */
    const val APP_OPEN_RESUME_COOLDOWN_MS = 3 * 60_000L

    /** Max time the splash waits for an app-open ad before continuing. */
    const val SPLASH_APP_OPEN_TIMEOUT_MS = 4_500L

    const val INTERSTITIAL_EXPIRY_MS = 55 * 60_000L
    const val APP_OPEN_EXPIRY_MS = 4 * 60 * 60_000L - 60_000L
}

/** Where an interstitial may be shown, and how often (every Nth eligible event). */
enum class AdPlacement(val frequency: Int) {
    /** Opening a tool from the home screen. */
    TOOL_OPEN(2),

    /** Leaving a tool back to home. */
    TOOL_EXIT(2),

    /** After a clean / delete / uninstall finished, before the result screen. */
    RESULT(1),
}
