package com.pdfstudio.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.pdfstudio.app.DocState
import com.pdfstudio.app.data.PageBitmapCache

/**
 * Loads a rendered page at [width] px, showing [fallbackWidth] from cache (if present) or a
 * shimmer until the sharp bitmap arrives.
 */
@Composable
fun rememberPageBitmap(doc: DocState, cache: PageBitmapCache, page: Int, width: Int, fallbackWidth: Int = width): Bitmap? {
    val state = produceState(
        initialValue = cache.get(doc.id, page, width) ?: cache.get(doc.id, page, fallbackWidth),
        doc.id, page, width,
    ) {
        if (width <= 0) return@produceState
        val cached = cache.get(doc.id, page, width)
        if (cached != null) {
            value = cached
            return@produceState
        }
        val bitmap = doc.renderer.render(page, width)
        if (bitmap != null) {
            cache.put(doc.id, page, width, bitmap)
            value = bitmap
        }
    }
    return state.value
}

@Composable
fun PageImage(bitmap: Bitmap?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(modifier.background(Color.White)) {
        val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(260), label = "fade")
        if (bitmap == null || alpha < 1f) Shimmer(Modifier.fillMaxSize())
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

@Composable
fun Shimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "progress",
    )
    Box(
        modifier.drawWithCache {
            val base = Color(0xFFF1F2F6)
            val highlight = Color(0xFFFFFFFF)
            onDrawBehind {
                drawRect(base)
                val x = progress * size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(base, highlight, base),
                        start = Offset(x - size.width * 0.5f, 0f),
                        end = Offset(x + size.width * 0.5f, size.height * 0.3f),
                    )
                )
            }
        }
    )
}
