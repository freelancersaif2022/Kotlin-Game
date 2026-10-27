package com.pdfstudio.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.pdfstudio.app.DocState
import com.pdfstudio.app.data.PageBitmapCache
import com.pdfstudio.app.ui.components.PageImage
import com.pdfstudio.app.ui.components.rememberPageBitmap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val MAX_ZOOM = 4f
private const val MAX_RENDER_WIDTH = 2800

/** Vertically scrolling, pinch / double-tap zoomable preview of every page. */
@Composable
fun PdfPreview(
    doc: DocState,
    cache: PageBitmapCache,
    contentPadding: PaddingValues,
    onCurrentPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = doc.focusPage)
    val scope = rememberCoroutineScope()
    var flingJob by remember { mutableStateOf<Job?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val renderBucket by remember { derivedStateOf { if (scale > 1.6f) 2 else 1 } }
    val onPage by rememberUpdatedState(onCurrentPageChange)

    val currentPage by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.firstOrNull { it.offset <= center && it.offset + it.size >= center }?.index
                ?: listState.firstVisibleItemIndex
        }
    }
    LaunchedEffect(Unit) { snapshotFlow { currentPage }.collect { onPage(it) } }

    var indicatorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress to currentPage }.collectLatest { (scrolling, _) ->
            indicatorVisible = true
            if (!scrolling) {
                delay(1400)
                indicatorVisible = false
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val pageWidthPx = (constraints.maxWidth - with(density) { 32.dp.roundToPx() }).coerceAtLeast(1)

        fun clamp(o: Offset, s: Float): Offset {
            val mx = (s - 1f) * w / 2f
            val my = (s - 1f) * h / 2f
            return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
        }

        // Moves the zoomed pages; vertical movement beyond the zoom edge scrolls the list.
        fun panBy(delta: Offset) {
            val target = offset + delta
            val clamped = clamp(target, scale)
            offset = clamped
            val overflowY = target.y - clamped.y
            if (overflowY != 0f) listState.dispatchRawDelta(-overflowY / scale)
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(w, h) {
                    val decay = splineBasedDecay<Float>(this)
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        flingJob?.cancel()
                        val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                        var panning = false
                        var travelled = Offset.Zero
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                // Pinch: zoom in / out around the fingers.
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                                val c = Offset(w / 2f, h / 2f)
                                val anchor = (centroid - c - offset) / scale
                                offset = clamp(centroid - c - anchor * newScale + pan, newScale)
                                scale = newScale
                                event.changes.forEach { it.consume() }
                                panning = false
                            } else if (pressed == 1 && scale > 1.01f) {
                                // Zoomed in: one finger moves the pages freely in every direction.
                                val change = event.changes.first { it.pressed }
                                tracker.addPosition(change.uptimeMillis, change.position)
                                val delta = change.positionChange()
                                if (!panning) {
                                    travelled += delta
                                    if (travelled.getDistance() > slop) panning = true
                                }
                                if (panning) {
                                    panBy(delta)
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (panning && scale > 1.01f) {
                            val velocity = tracker.calculateVelocity()
                            flingJob = scope.launch {
                                var lastX = 0f
                                var lastY = 0f
                                launch {
                                    AnimationState(0f, velocity.x).animateDecay(decay) {
                                        panBy(Offset(value - lastX, 0f))
                                        lastX = value
                                    }
                                }
                                launch {
                                    AnimationState(0f, velocity.y).animateDecay(decay) {
                                        panBy(Offset(0f, value - lastY))
                                        lastY = value
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(w, h) {
                    detectTapGestures(onDoubleTap = { pos ->
                        flingJob?.cancel()
                        scope.launch {
                            val startScale = scale
                            val startOffset = offset
                            val target = if (startScale > 1.05f) 1f else 2.5f
                            val c = Offset(w / 2f, h / 2f)
                            val anchor = (pos - c - startOffset) / startScale
                            val targetOffset = if (target == 1f) Offset.Zero else clamp(pos - c - anchor * target, target)
                            animate(0f, 1f, animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { t, _ ->
                                scale = lerp(startScale, target, t)
                                offset = lerp(startOffset, targetOffset, t)
                            }
                        }
                    })
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            items(doc.pageCount, key = { it }) { index ->
                val size = doc.pageSizes[index]
                val renderWidth = (pageWidthPx * renderBucket).coerceAtMost(MAX_RENDER_WIDTH)
                val bitmap = rememberPageBitmap(doc, cache, index, renderWidth, fallbackWidth = pageWidthPx)
                PageImage(
                    bitmap = bitmap,
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(size.aspect)
                        .shadow(8.dp, RoundedCornerShape(4.dp), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.35f))
                        .clip(RoundedCornerShape(4.dp)),
                )
            }
        }

        AnimatedVisibility(
            visible = indicatorVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        ) {
            Text(
                "${currentPage + 1} / ${doc.pageCount}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xCC15161B))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
