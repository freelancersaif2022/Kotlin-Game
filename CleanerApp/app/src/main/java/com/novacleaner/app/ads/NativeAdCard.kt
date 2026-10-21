package com.novacleaner.app.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.novacleaner.app.BuildConfig
import com.novacleaner.app.core.AppGraph
import com.novacleaner.app.ui.theme.Nova

private class NativeColors(val title: Int, val body: Int, val accent: Int, val badge: Int)

/** Native ad styled like an app card. Appears (animated) only once an ad has loaded. */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, showMedia: Boolean = true) {
    val premium by AppGraph.premium.collectAsStateWithLifecycle()
    val ready by AppGraph.ads.ready.collectAsStateWithLifecycle()
    if (premium || !ready) return
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        var disposed = false
        val loader = AdLoader.Builder(context, BuildConfig.AD_NATIVE_ID)
            .forNativeAd { ad ->
                if (disposed) ad.destroy() else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
        onDispose {
            disposed = true
            nativeAd?.destroy()
        }
    }

    val colors = NativeColors(
        title = MaterialTheme.colorScheme.onSurface.toArgb(),
        body = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
        accent = MaterialTheme.colorScheme.primary.toArgb(),
        badge = Nova.colors.warning.toArgb(),
    )
    AnimatedVisibility(visible = nativeAd != null, enter = fadeIn() + expandVertically(), modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().border(1.dp, Nova.colors.cardBorder, RoundedCornerShape(22.dp)),
        ) {
            AndroidView(
                factory = { ctx -> buildNativeAdView(ctx, colors, showMedia) },
                update = { view -> nativeAd?.let { bindNativeAd(view, it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private class Holder(
    val headline: TextView,
    val body: TextView,
    val icon: ImageView,
    val advertiser: TextView,
    val cta: Button,
    val media: MediaView?,
)

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun buildNativeAdView(ctx: Context, colors: NativeColors, showMedia: Boolean): NativeAdView {
    val adView = NativeAdView(ctx)
    val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(16), ctx.dp(14), ctx.dp(16), ctx.dp(16))
    }
    val header = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val icon = ImageView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ctx.dp(44), ctx.dp(44))
        clipToOutline = true
        background = GradientDrawable().apply { cornerRadius = ctx.dp(12).toFloat(); setColor(0x11000000) }
    }
    val texts = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ctx.dp(12) }
    }
    val badgeRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val badge = TextView(ctx).apply {
        text = "Ad"
        textSize = 10f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(0xFF1A1300.toInt())
        setPadding(ctx.dp(6), ctx.dp(1), ctx.dp(6), ctx.dp(1))
        background = GradientDrawable().apply { cornerRadius = ctx.dp(6).toFloat(); setColor(colors.badge) }
    }
    val advertiser = TextView(ctx).apply {
        textSize = 12f
        setTextColor(colors.body)
        maxLines = 1
        setPadding(ctx.dp(6), 0, 0, 0)
    }
    badgeRow.addView(badge)
    badgeRow.addView(advertiser)
    val headline = TextView(ctx).apply {
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(colors.title)
        maxLines = 1
    }
    texts.addView(headline)
    texts.addView(badgeRow)
    header.addView(icon)
    header.addView(texts)
    root.addView(header)

    val media = if (showMedia) MediaView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(170)).apply { topMargin = ctx.dp(12) }
    } else null
    media?.let { root.addView(it) }

    val body = TextView(ctx).apply {
        textSize = 13f
        setTextColor(colors.body)
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(10) }
    }
    root.addView(body)
    val cta = Button(ctx).apply {
        isAllCaps = false
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(0xFFFFFFFF.toInt())
        stateListAnimator = null
        background = GradientDrawable().apply { cornerRadius = ctx.dp(14).toFloat(); setColor(colors.accent) }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(46)).apply { topMargin = ctx.dp(12) }
    }
    root.addView(cta)
    adView.addView(root)

    adView.headlineView = headline
    adView.bodyView = body
    adView.iconView = icon
    adView.advertiserView = advertiser
    adView.callToActionView = cta
    adView.mediaView = media
    adView.tag = Holder(headline, body, icon, advertiser, cta, media)
    return adView
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd) {
    val h = view.tag as? Holder ?: return
    h.headline.text = ad.headline
    h.body.text = ad.body
    h.body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    h.advertiser.text = ad.advertiser ?: ad.store ?: ""
    h.cta.text = ad.callToAction ?: "Open"
    h.cta.visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
    val iconDrawable = ad.icon?.drawable
    h.icon.setImageDrawable(iconDrawable)
    h.icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE
    h.media?.let { media ->
        val content = ad.mediaContent
        if (content != null) {
            media.mediaContent = content
            media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
            media.visibility = View.VISIBLE
        } else {
            media.visibility = View.GONE
        }
    }
    view.setNativeAd(ad)
}
