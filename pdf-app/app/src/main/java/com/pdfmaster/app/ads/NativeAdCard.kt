package com.pdfmaster.app.ads

import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.pdfmaster.app.ui.LocalAppDeps
import com.pdfmaster.app.ui.components.Shimmer
import com.pdfmaster.app.ui.theme.AppTheme
import com.pdfmaster.app.ui.theme.Brand

/** Native ad styled as a card that matches the file list. Hidden for Premium users. */
@Composable
fun NativeAdCard(modifier: Modifier = Modifier, showMedia: Boolean = true) {
    val ads = LocalAppDeps.current.ads
    val enabled by ads.adsEnabled.collectAsStateWithLifecycle()
    val ad by ads.nativeAd.collectAsStateWithLifecycle()
    val state by ads.nativeState.collectAsStateWithLifecycle()
    LaunchedEffect(enabled) { if (enabled) ads.loadNative() }
    val visible = enabled && state != NativeState.FAILED && state != NativeState.IDLE
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        ) {
            val current = ad
            if (current == null) {
                Box(Modifier.fillMaxWidth().height(if (showMedia) 260.dp else 120.dp)) {
                    Shimmer(Modifier.fillMaxWidth().height(if (showMedia) 260.dp else 120.dp))
                }
            } else {
                val ink = MaterialTheme.colorScheme.onSurface.toArgb()
                val muted = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                val surface = MaterialTheme.colorScheme.surfaceContainer.toArgb()
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx -> NativeAdLayout.create(ctx, ink, muted, surface, showMedia) },
                    update = { view -> NativeAdLayout.bind(view, current) },
                )
            }
        }
    }
}

private object NativeAdLayout {
    private const val TAG_HEADLINE = "headline"
    private const val TAG_BODY = "body"
    private const val TAG_ICON = "icon"
    private const val TAG_CTA = "cta"
    private const val TAG_ADVERTISER = "advertiser"
    private const val TAG_MEDIA = "media"

    private fun Context.dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    fun create(ctx: Context, ink: Int, muted: Int, surface: Int, showMedia: Boolean): NativeAdView {
        val adView = NativeAdView(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(14f), ctx.dp(14f), ctx.dp(14f), ctx.dp(14f))
        }
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(ctx).apply {
            tag = TAG_ICON
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { cornerRadius = ctx.dp(12f).toFloat(); setColor(surface) }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, ctx.dp(12f).toFloat())
                }
            }
        }
        header.addView(icon, LinearLayout.LayoutParams(ctx.dp(46f), ctx.dp(46f)))
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val badgeRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(ctx).apply {
            text = "Ad"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color(0xFF1B1D29).toArgb())
            setPadding(ctx.dp(6f), ctx.dp(1f), ctx.dp(6f), ctx.dp(1f))
            background = GradientDrawable().apply { cornerRadius = ctx.dp(4f).toFloat(); setColor(Brand.Gold.toArgb()) }
        }
        val advertiser = TextView(ctx).apply {
            tag = TAG_ADVERTISER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(muted)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(ctx.dp(6f), 0, 0, 0)
        }
        badgeRow.addView(badge)
        badgeRow.addView(advertiser)
        val headline = TextView(ctx).apply {
            tag = TAG_HEADLINE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(ink)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, ctx.dp(2f), 0, 0)
        }
        texts.addView(badgeRow)
        texts.addView(headline)
        header.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ctx.dp(12f) })
        root.addView(header)

        if (showMedia) {
            val media = MediaView(ctx).apply {
                tag = TAG_MEDIA
                setImageScaleType(ImageView.ScaleType.CENTER_CROP)
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, ctx.dp(12f).toFloat())
                    }
                }
            }
            root.addView(media, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(140f)).apply { topMargin = ctx.dp(12f) })
            adView.mediaView = media
        }

        val body = TextView(ctx).apply {
            tag = TAG_BODY
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(muted)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(0f, 1.15f)
        }
        root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(10f) })
        val cta = TextView(ctx).apply {
            tag = TAG_CTA
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Brand.RedBright.toArgb(), Brand.RedDeep.toArgb()),
            ).apply { cornerRadius = ctx.dp(12f).toFloat() }
        }
        root.addView(cta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(44f)).apply { topMargin = ctx.dp(12f) })

        adView.addView(root)
        adView.headlineView = headline
        adView.bodyView = body
        adView.iconView = icon
        adView.callToActionView = cta
        adView.advertiserView = advertiser
        return adView
    }

    fun bind(view: NativeAdView, ad: NativeAd) {
        (view.headlineView as TextView).text = ad.headline
        (view.bodyView as TextView).apply {
            text = ad.body
            visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        (view.callToActionView as TextView).apply {
            text = ad.callToAction ?: "Learn more"
        }
        (view.advertiserView as TextView).text = ad.advertiser ?: ad.store ?: "Sponsored"
        (view.iconView as ImageView).apply {
            val drawable = ad.icon?.drawable
            setImageDrawable(drawable)
            visibility = if (drawable == null) View.GONE else View.VISIBLE
        }
        view.mediaView?.let { media ->
            ad.mediaContent?.let { media.mediaContent = it }
            media.visibility = if (ad.mediaContent == null) View.GONE else View.VISIBLE
        }
        view.setNativeAd(ad)
    }
}
