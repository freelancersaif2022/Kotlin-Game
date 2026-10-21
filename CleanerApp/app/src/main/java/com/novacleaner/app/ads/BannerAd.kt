package com.novacleaner.app.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.novacleaner.app.BuildConfig
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.ui.theme.Nova

/** Anchored adaptive banner. Renders nothing for premium users or before the SDK is ready. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val premium by AppGraph.premium.collectAsStateWithLifecycle()
    val ready by AppGraph.ads.ready.collectAsStateWithLifecycle()
    if (premium || !ready) return
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = Nova.colors.cardBorder)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val widthDp = maxWidth.value.toInt()
            val adSize = remember(widthDp) {
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerSize(context, widthDp)
            }
            val adView = remember(widthDp) {
                AdView(context).apply {
                    setAdSize(adSize)
                    adUnitId = BuildConfig.AD_BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { adView.pause() }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { adView.resume() }
            DisposableEffect(adView) { onDispose { adView.destroy() } }
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth().height(adSize.height.dp),
            )
        }
    }
}
