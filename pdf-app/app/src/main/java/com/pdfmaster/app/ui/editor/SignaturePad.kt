package com.pdfmaster.app.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdfmaster.app.ui.components.PrimaryButton
import com.pdfmaster.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil

private val inkColors = listOf(Color(0xFF111827), Color(0xFF1D4ED8), Color(0xFFC81E1E))

private class Stroke2(val color: Color, val points: List<Offset>)

@Composable
fun SignaturePadSheet(store: SignatureStore, onDismiss: () -> Unit, onPicked: (File) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(Unit) { saved = store.list() }
    val strokes = remember { mutableStateListOf<Stroke2>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var color by remember { mutableStateOf(inkColors.first()) }
    val strokePx = with(LocalDensity.current) { 3.dp.toPx() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            if (saved.isNotEmpty()) {
                Text("Saved signatures", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 20.dp, bottom = 10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(saved, key = { it.path }) { file ->
                        val bmp = remember(file) { BitmapFactory.decodeFile(file.path) }
                        Box(
                            Modifier.size(150.dp, 76.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
                                .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(14.dp)).clickable { onPicked(file) },
                        ) {
                            if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(Color(0x22000000))
                                    .clickable { scope.launch { store.delete(file); saved = store.list() } },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Close, "Delete", tint = Color(0xFF374151), modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Draw your signature", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                inkColors.forEach { c ->
                    Box(
                        Modifier.padding(start = 8.dp).size(28.dp).clip(CircleShape).background(c)
                            .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clickable { color = c },
                        contentAlignment = Alignment.Center,
                    ) { if (c == color) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(210.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
            ) {
                if (strokes.isEmpty() && current.isEmpty()) {
                    Text("Sign here", color = Color(0xFFB7BBC8), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.Center))
                }
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInputDraw(
                            onStart = { current = listOf(it) },
                            onMove = { current = current + it },
                            onEnd = {
                                if (current.isNotEmpty()) strokes.add(Stroke2(color, current))
                                current = emptyList()
                            },
                        ),
                ) {
                    val baseline = size.height * 0.72f
                    drawLine(Color(0xFFD5D8E2), Offset(24f, baseline), Offset(size.width - 24f, baseline), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                    (strokes + listOfNotNull(current.takeIf { it.isNotEmpty() }?.let { Stroke2(color, it) })).forEach { s ->
                        drawPath(s.points.toPath(), s.color, style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { strokes.clear() }, enabled = strokes.isNotEmpty()) {
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    "Save & place", enabled = strokes.isNotEmpty(), height = 50.dp, modifier = Modifier.width(190.dp),
                    onClick = {
                        val snapshot = strokes.toList()
                        scope.launch {
                            val bmp = withContext(Dispatchers.Default) { renderSignature(snapshot, strokePx) }
                            if (bmp != null) onPicked(store.save(bmp))
                        }
                    },
                )
            }
        }
    }
}

private fun List<Offset>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().x, first().y)
    if (size == 1) lineTo(first().x + 0.5f, first().y)
    for (i in 1 until size) {
        val p = this@toPath[i - 1]
        val c = this@toPath[i]
        quadraticTo(p.x, p.y, (p.x + c.x) / 2, (p.y + c.y) / 2)
    }
    lineTo(last().x, last().y)
}

private fun renderSignature(strokes: List<Stroke2>, strokePx: Float): Bitmap? {
    val pts = strokes.flatMap { it.points }
    if (pts.isEmpty()) return null
    val pad = strokePx * 3
    val minX = pts.minOf { it.x } - pad
    val minY = pts.minOf { it.y } - pad
    val maxX = pts.maxOf { it.x } + pad
    val maxY = pts.maxOf { it.y } + pad
    val scale = 2f
    val w = ceil((maxX - minX) * scale).toInt().coerceIn(8, 2400)
    val h = ceil((maxY - minY) * scale).toInt().coerceIn(8, 1600)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    strokes.forEach { s ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.color.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = strokePx * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = android.graphics.Path()
        s.points.forEachIndexed { i, p ->
            val x = (p.x - minX) * scale
            val y = (p.y - minY) * scale
            if (i == 0) path.moveTo(x, y) else {
                val prev = s.points[i - 1]
                val px = (prev.x - minX) * scale
                val py = (prev.y - minY) * scale
                path.quadTo(px, py, (px + x) / 2, (py + y) / 2)
            }
        }
        if (s.points.size == 1) path.lineTo((s.points[0].x - minX) * scale + 1, (s.points[0].y - minY) * scale)
        canvas.drawPath(path, paint)
    }
    return bmp
}

/** Drag-to-draw that also records single taps as dots. */
fun Modifier.pointerInputDraw(onStart: (Offset) -> Unit, onMove: (Offset) -> Unit, onEnd: () -> Unit): Modifier =
    this
        .pointerInput("draw") {
            detectDragGestures(
                onDragStart = { onStart(it) },
                onDragEnd = { onEnd() },
                onDragCancel = { onEnd() },
            ) { change, _ ->
                change.consume()
                onMove(change.position)
            }
        }
        .pointerInput("tap") {
            detectTapGestures { onStart(it); onEnd() }
        }
