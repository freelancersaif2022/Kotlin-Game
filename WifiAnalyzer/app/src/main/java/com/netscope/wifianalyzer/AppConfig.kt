package com.netscope.wifianalyzer

/**
 * Store and legal settings you must review before publishing.
 * AdMob IDs live in gradle.properties; subscription IDs must match Play Console.
 */
object AppConfig {
    /** Subscription product IDs. Create these in Play Console > Monetize > Subscriptions. */
    const val PRODUCT_MONTHLY = "premium_monthly"
    const val PRODUCT_YEARLY = "premium_yearly"

    /** Shown only until Play Billing returns the real localized prices. */
    const val FALLBACK_MONTHLY_PRICE = "$1.99"
    const val FALLBACK_YEARLY_PRICE = "$14.99"

    /** Host PRIVACY_POLICY.md (for example with GitHub Pages) and put the public URL here. */
    const val PRIVACY_POLICY_URL = "https://example.com/wifi-analyzer/privacy"
    const val TERMS_URL = "https://example.com/wifi-analyzer/terms"
    const val SUPPORT_EMAIL = "support@example.com"

    /** Rewarded unlock: watch this many ads to get premium for [REWARD_DURATION_MS]. */
    const val REWARD_ADS_REQUIRED = 3
    const val REWARD_DURATION_MS = 12L * 60 * 60 * 1000

    /** Interstitial frequency cap. */
    const val INTERSTITIAL_MIN_INTERVAL_MS = 75_000L

    /** App open ad is shown only after the app was in the background this long. */
    const val APP_OPEN_MIN_BACKGROUND_MS = 4L * 60 * 60 * 1000

    const val FREE_DEVICE_LIMIT = 5
    const val FREE_HISTORY_LIMIT = 5
}
