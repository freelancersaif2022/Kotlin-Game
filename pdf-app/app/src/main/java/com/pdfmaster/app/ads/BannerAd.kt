package com.pdfmaster.app.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pdfmaster.app.BuildConfig
import com.pdfmaster.app.ui.LocalAppDeps

/** Anchored adaptive banner. Reserves its height while loading so content never jumps. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ads = LocalAppDeps.current.ads
    val enabled by ads.adsEnabled.collectAsStateWithLifecycle()
    AnimatedVisibility(visible = enabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        BannerContent(modifier)
    }
}

@Composable
private fun BannerContent(modifier: Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(widthDp) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp) }
    var loaded by remember { mutableStateOf(false) }
    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.AD_BANNER_ID
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(adView, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.fillMaxWidth().height(adSize.height.dp), contentAlignment = Alignment.Center) {
            if (!loaded) {
                Text(
                    "Advertisement",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
        }
    }
}
