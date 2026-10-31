package com.spectral.ghostdetector.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.spectral.ghostdetector.R
import com.spectral.ghostdetector.ui.rememberApp

/** Anchored adaptive banner. Hidden for premium users. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val app = rememberApp()
    val premium by app.premium.collectAsStateWithLifecycle()
    if (premium || !app.ads.ready) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                    adUnitId = AdIds.BANNER
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}

/** Native advanced ad styled to match the app. Renders nothing until an ad is loaded. */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val app = rememberApp()
    val context = LocalContext.current
    val premium by app.premium.collectAsStateWithLifecycle()
    val ready = app.ads.ready
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(ready, premium) {
        var disposed = false
        if (ready && !premium) {
            val loader = AdLoader.Builder(context, AdIds.NATIVE)
                .forNativeAd { ad ->
                    if (disposed) {
                        ad.destroy()
                    } else {
                        nativeAd?.destroy()
                        nativeAd = ad
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) = Unit
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()
            loader.loadAd(AdRequest.Builder().build())
        }
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd
    if (premium || ad == null) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, FrameLayout(ctx), false) as NativeAdView
        },
        update = { view -> bindNativeAd(view, ad) },
    )
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val media = view.findViewById<MediaView>(R.id.ad_media)
    val cta = view.findViewById<Button>(R.id.ad_cta)

    headline.text = ad.headline
    view.headlineView = headline

    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    view.bodyView = body

    val drawable = ad.icon?.drawable
    icon.setImageDrawable(drawable)
    icon.visibility = if (drawable == null) View.GONE else View.VISIBLE
    view.iconView = icon

    view.mediaView = media
    ad.mediaContent?.let { media.mediaContent = it }

    cta.text = ad.callToAction ?: "Open"
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
    view.callToActionView = cta

    view.setNativeAd(ad)
}
