package com.vaultly.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.vaultly.app.BuildConfig

/** Google UMP consent (GDPR/EEA, UK, US states). Ads are only requested once consent allows it. */
object Consent {
    fun gather(activity: Activity, onCanRequestAds: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        if (info.canRequestAds()) onCanRequestAds()
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (info.canRequestAds()) onCanRequestAds()
                }
            },
            {
                // Test builds still show Google's test ads when no consent form is configured yet.
                if (info.canRequestAds() || BuildConfig.MOCK_BILLING) onCanRequestAds()
            },
        )
    }

    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDone() }
    }
}
