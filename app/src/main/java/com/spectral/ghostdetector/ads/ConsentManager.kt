package com.spectral.ghostdetector.ads

import android.app.Activity
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Google UMP consent (GDPR / US states). Ads are only initialized when allowed. */
object ConsentManager {
    fun gather(activity: Activity, onCanRequestAds: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (info.canRequestAds()) onCanRequestAds()
                }
            },
            { if (info.canRequestAds()) onCanRequestAds() },
        )
        if (info.canRequestAds()) onCanRequestAds()
    }
}
