package com.pdfmaster.app.ui.viewer

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.pdfmaster.app.pdf.PdfDocumentSession

private val invertMatrix = ColorMatrix(
    floatArrayOf(
        -0.9f, 0f, 0f, 0f, 240f,
        0f, -0.9f, 0f, 0f, 240f,
        0f, 0f, -0.9f, 0f, 240f,
        0f, 0f, 0f, 1f, 0f,
    ),
)

/** One rendered page. Shows the closest cached bitmap first, then swaps in a sharper one. */
@Composable
fun PdfPage(
    session: PdfDocumentSession,
    index: Int,
    renderWidthPx: Int,
    modifier: Modifier = Modifier,
    night: Boolean = false,
) {
    val size = session.pageSizes[index]
    var bitmap by remember(session, index) { mutableStateOf<Bitmap?>(session.cached(index, renderWidthPx)) }
    LaunchedEffect(session, index, renderWidthPx) {
        if (renderWidthPx > 0) session.render(index, renderWidthPx)?.let { bitmap = it }
    }
    val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(180), label = "page")
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(size.width.toFloat() / size.height)
            .background(if (night) Color(0xFF151515) else Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp == null) {
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFCDD0DA),
            )
        } else {
            Image(
                bmp.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
                contentScale = ContentScale.FillBounds,
                colorFilter = if (night) ColorFilter.colorMatrix(invertMatrix) else null,
            )
        }
    }
}
