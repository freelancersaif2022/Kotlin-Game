package com.voxcast.mic.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.voxcast.mic.R
import com.voxcast.mic.ui.theme.VoxColors

/** Anchored adaptive banner. Renders nothing for Premium users. */
@Composable
fun BannerAd(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var loaded by remember { mutableStateOf(false) }
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = AdUnits.BANNER
            adListener = object : AdListener() {
                override fun onAdLoaded() { loaded = true }
                override fun onAdFailedToLoad(error: LoadAdError) { loaded = false }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }

    // The AdView has zero height until an ad arrives, so the slot grows in smoothly.
    Box(
        modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(if (loaded) VoxColors.Surface else androidx.compose.ui.graphics.Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}

/** Native ad styled to match the app, shown inside a glass card. */
@Composable
fun NativeAdCard(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var destroyed = false
        val loader = AdLoader.Builder(context, AdUnits.NATIVE)
            .forNativeAd { ad ->
                if (destroyed) ad.destroy() else { nativeAd?.destroy(); nativeAd = ad }
            }
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose {
            destroyed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    AnimatedVisibility(visible = nativeAd != null, enter = expandVertically() + fadeIn(), modifier = modifier) {
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(VoxColors.Surface)
                .border(1.dp, VoxColors.OutlineSoft, shape),
        ) {
            AndroidView(
                factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.native_ad_card, null) as NativeAdView },
                update = { view -> nativeAd?.let { bind(view, it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun bind(view: NativeAdView, ad: NativeAd) {
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val advertiser = view.findViewById<TextView>(R.id.ad_advertiser)
    val cta = view.findViewById<Button>(R.id.ad_call_to_action)
    val media = view.findViewById<MediaView>(R.id.ad_media)

    headline.text = ad.headline
    body.text = ad.body
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    advertiser.text = ad.advertiser ?: "Sponsored"
    cta.text = ad.callToAction ?: "Learn more"
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
    ad.icon?.drawable?.let { icon.setImageDrawable(it); icon.visibility = View.VISIBLE } ?: run { icon.visibility = View.GONE }
    media.mediaContent = ad.mediaContent
    media.visibility = if (ad.mediaContent != null) View.VISIBLE else View.GONE

    view.headlineView = headline
    view.bodyView = body
    view.iconView = icon
    view.advertiserView = advertiser
    view.callToActionView = cta
    view.mediaView = media
    view.setNativeAd(ad)
}
