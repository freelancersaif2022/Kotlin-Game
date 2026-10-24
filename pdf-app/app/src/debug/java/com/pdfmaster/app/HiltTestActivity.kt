package com.pdfmaster.app

import androidx.activity.ComponentActivity
import com.pdfmaster.app.ads.AdsManager
import com.pdfmaster.app.billing.BillingManager
import com.pdfmaster.app.data.prefs.AppPreferences
import com.pdfmaster.app.data.repo.FileRepository
import com.pdfmaster.app.pdf.ThumbnailLoader
import com.pdfmaster.app.premium.PremiumManager
import com.pdfmaster.app.ui.AppDeps
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Host activity for UI screenshot tests (debug builds only). */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity() {
    @Inject lateinit var ads: AdsManager
    @Inject lateinit var premium: PremiumManager
    @Inject lateinit var billing: BillingManager
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var files: FileRepository
    @Inject lateinit var thumbnails: ThumbnailLoader

    val deps: AppDeps get() = AppDeps(ads, premium, billing, prefs, files, thumbnails)
}
