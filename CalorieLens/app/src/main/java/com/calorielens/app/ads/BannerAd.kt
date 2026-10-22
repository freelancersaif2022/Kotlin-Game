package com.calorielens.app.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Anchored adaptive AdMob banner. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(320)
        val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    adUnitId = AdUnits.BANNER
                    setAdSize(adSize)
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
