package com.pdfmaster.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.pdfmaster.app.ads.AdsManager
import com.pdfmaster.app.billing.BillingManager
import com.pdfmaster.app.data.prefs.AppPreferences
import com.pdfmaster.app.data.repo.FileRepository
import com.pdfmaster.app.pdf.ThumbnailLoader
import com.pdfmaster.app.premium.PremiumManager

/** App-wide singletons that many screens need, handed down once from the activity. */
@Immutable
class AppDeps(
    val ads: AdsManager,
    val premium: PremiumManager,
    val billing: BillingManager,
    val prefs: AppPreferences,
    val files: FileRepository,
    val thumbnails: ThumbnailLoader,
)

val LocalAppDeps = staticCompositionLocalOf<AppDeps> { error("AppDeps not provided") }

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberActivity(): Activity = LocalContext.current.findActivity() ?: error("No activity")
