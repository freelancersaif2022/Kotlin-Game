package com.pdfstudio.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.pdfstudio.app.DocState
import com.pdfstudio.app.data.CropRect
import com.pdfstudio.app.data.PageBitmapCache
import com.pdfstudio.app.ui.components.PageImage
import com.pdfstudio.app.ui.components.rememberPageBitmap
import com.pdfstudio.app.ui.components.ToolIcons
import com.pdfstudio.app.ui.theme.Brand
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

private enum class Handle { None, Move, Left, Top, Right, Bottom, TopLeft, TopRight, BottomLeft, BottomRight }

@Composable
fun CropScreen(
    doc: DocState,
    cache: PageBitmapCache,
    startPage: Int,
    onCancel: () -> Unit,
    onCrop: (Int, CropRect) -> Unit,
) {
    var page by remember { mutableIntStateOf(startPage) }
    var forward by remember { mutableStateOf(true) }
    var rect by remember { mutableStateOf(CropRect.Full) }
    val scope = rememberCoroutineScope()

    fun goTo(target: Int) {
        if (target !in 0 until doc.pageCount || target == page) return
        forward = target > page
        page = target
        rect = CropRect.Full
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "Crop page",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                if (doc.pageCount > 1) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { goTo(page - 1) }, enabled = page > 0, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous page")
                        }
                        Text(
                            "${page + 1} / ${doc.pageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { goTo(page + 1) }, enabled = page < doc.pageCount - 1, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next page")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(320)) { it / 4 * dir } + fadeIn(tween(260)))
                        .togetherWith(slideOutHorizontally(tween(320)) { -it / 4 * dir } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "cropPage",
            ) { p ->
                CropEditor(
                    doc = doc,
                    cache = cache,
                    page = p,
                    rect = if (p == page) rect else CropRect.Full,
                    onRectChange = { if (p == page) rect = it },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            Text(
                "Drag the corners or edges to crop freely",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = {
                        val start = rect
                        scope.launch {
                            animate(0f, 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { t, _ ->
                                rect = CropRect(lerp(start.left, 0f, t), lerp(start.top, 0f, t), lerp(start.right, 1f, t), lerp(start.bottom, 1f, t))
                            }
                        }
                    },
                    enabled = !rect.isFullPage,
                ) {
                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }
            }
            SheetButtons(
                confirmText = "Crop",
                confirmIcon = ToolIcons.Crop,
                confirmEnabled = !rect.isFullPage,
                onCancel = onCancel,
                onConfirm = { onCrop(page, rect) },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun CropEditor(
    doc: DocState,
    cache: PageBitmapCache,
    page: Int,
    rect: CropRect,
    onRectChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentRect by rememberUpdatedState(rect)
    val onChange by rememberUpdatedState(onRectChange)
    var dragging by remember { mutableStateOf(false) }
    val gridAlpha by animateFloatAsState(if (dragging) 1f else 0f, tween(220), label = "grid")
    val accent = Brand.Coral

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Leave room around the page so handles on the edges are easy to grab.
        val pad = 24.dp
        val availW = maxWidth - pad * 2
        val availH = maxHeight - pad * 2
        val aspect = doc.pageSizes[page].aspect
        val (w, h) = if (aspect > availW / availH) availW to availW / aspect else availH * aspect to availH
        val density = LocalDensity.current
        val renderWidth = with(density) { (w.toPx() * 1.5f).toInt() }.coerceIn(1, 2400)
        val bitmap = rememberPageBitmap(doc, cache, page, renderWidth)

        Box(
            Modifier
                .size(w + pad * 2, h + pad * 2)
                .pointerInput(page) {
                    val inset = pad.toPx()
                    val touch = 28.dp.toPx()
                    val minSize = 40.dp.toPx()
                    var handle = Handle.None
                    detectDragGestures(
                        onDragStart = { pos ->
                            val pw = size.width - inset * 2
                            val ph = size.height - inset * 2
                            handle = hitTest(currentRect, pos - Offset(inset, inset), pw, ph, touch)
                            dragging = handle != Handle.None
                        },
                        onDragEnd = {
                            handle = Handle.None
                            dragging = false
                        },
                        onDragCancel = {
                            handle = Handle.None
                            dragging = false
                        },
                    ) { change, drag ->
                        if (handle == Handle.None) return@detectDragGestures
                        change.consume()
                        val pw = size.width - inset * 2
                        val ph = size.height - inset * 2
                        onChange(applyDrag(currentRect, handle, drag.x / pw, drag.y / ph, minSize / pw, minSize / ph))
                    }
                }
                .padding(pad)
        ) {
            PageImage(
                bitmap = bitmap,
                contentDescription = "Page ${page + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(10.dp, RoundedCornerShape(2.dp))
                    .clip(RoundedCornerShape(2.dp)),
            )
            Canvas(Modifier.fillMaxSize()) {
                drawCropOverlay(rect, gridAlpha, accent)
            }
        }
    }
}

private fun hitTest(r: CropRect, p: Offset, w: Float, h: Float, touch: Float): Handle {
    val l = r.left * w
    val t = r.top * h
    val rr = r.right * w
    val b = r.bottom * h
    fun near(a: Float, b: Float) = abs(a - b) <= touch
    val nearL = near(p.x, l)
    val nearR = near(p.x, rr)
    val nearT = near(p.y, t)
    val nearB = near(p.y, b)
    val inX = p.x in (l - touch)..(rr + touch)
    val inY = p.y in (t - touch)..(b + touch)
    return when {
        nearL && nearT -> Handle.TopLeft
        nearR && nearT -> Handle.TopRight
        nearL && nearB -> Handle.BottomLeft
        nearR && nearB -> Handle.BottomRight
        nearL && inY -> Handle.Left
        nearR && inY -> Handle.Right
        nearT && inX -> Handle.Top
        nearB && inX -> Handle.Bottom
        p.x in l..rr && p.y in t..b -> Handle.Move
        else -> Handle.None
    }
}

private fun applyDrag(r: CropRect, handle: Handle, dx: Float, dy: Float, minW: Float, minH: Float): CropRect {
    var l = r.left
    var t = r.top
    var rt = r.right
    var b = r.bottom
    when (handle) {
        Handle.Move -> {
            val mx = dx.coerceIn(-l, 1f - rt)
            val my = dy.coerceIn(-t, 1f - b)
            l += mx; rt += mx; t += my; b += my
        }
        else -> {
            if (handle == Handle.Left || handle == Handle.TopLeft || handle == Handle.BottomLeft) l = (l + dx).coerceIn(0f, rt - minW)
            if (handle == Handle.Right || handle == Handle.TopRight || handle == Handle.BottomRight) rt = (rt + dx).coerceIn(l + minW, 1f)
            if (handle == Handle.Top || handle == Handle.TopLeft || handle == Handle.TopRight) t = (t + dy).coerceIn(0f, b - minH)
            if (handle == Handle.Bottom || handle == Handle.BottomLeft || handle == Handle.BottomRight) b = (b + dy).coerceIn(t + minH, 1f)
        }
    }
    return CropRect(l, t, rt, b)
}

private fun DrawScope.drawCropOverlay(r: CropRect, gridAlpha: Float, accent: Color) {
    val l = r.left * size.width
    val t = r.top * size.height
    val rr = r.right * size.width
    val b = r.bottom * size.height
    val scrim = Color.Black.copy(alpha = 0.55f)

    // Dim everything outside the crop area.
    drawRect(scrim, Offset(0f, 0f), Size(size.width, t))
    drawRect(scrim, Offset(0f, b), Size(size.width, size.height - b))
    drawRect(scrim, Offset(0f, t), Size(l, b - t))
    drawRect(scrim, Offset(rr, t), Size(size.width - rr, b - t))

    // Rule-of-thirds grid while dragging.
    if (gridAlpha > 0f) {
        val gridColor = Color.White.copy(alpha = 0.7f * gridAlpha)
        val stroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = l + (rr - l) * i / 3f
            val y = t + (b - t) * i / 3f
            drawLine(gridColor, Offset(x, t), Offset(x, b), stroke)
            drawLine(gridColor, Offset(l, y), Offset(rr, y), stroke)
        }
    }

    // Frame.
    drawRect(Color.White, Offset(l, t), Size(rr - l, b - t), style = Stroke(1.5.dp.toPx()))

    // Corner and edge handles.
    val thick = 4.dp.toPx()
    val len = min(22.dp.toPx(), min(rr - l, b - t) / 3f)
    val half = thick / 2f
    fun corner(x: Float, y: Float, sx: Float, sy: Float) {
        drawLine(accent, Offset(x - sx * half, y), Offset(x + sx * len, y), thick, StrokeCap.Round)
        drawLine(accent, Offset(x, y - sy * half), Offset(x, y + sy * len), thick, StrokeCap.Round)
    }
    corner(l, t, 1f, 1f)
    corner(rr, t, -1f, 1f)
    corner(l, b, 1f, -1f)
    corner(rr, b, -1f, -1f)

    val cx = (l + rr) / 2f
    val cy = (t + b) / 2f
    val edge = len * 0.8f
    drawLine(Color.White, Offset(cx - edge, t), Offset(cx + edge, t), thick, StrokeCap.Round)
    drawLine(Color.White, Offset(cx - edge, b), Offset(cx + edge, b), thick, StrokeCap.Round)
    drawLine(Color.White, Offset(l, cy - edge), Offset(l, cy + edge), thick, StrokeCap.Round)
    drawLine(Color.White, Offset(rr, cy - edge), Offset(rr, cy + edge), thick, StrokeCap.Round)
}
