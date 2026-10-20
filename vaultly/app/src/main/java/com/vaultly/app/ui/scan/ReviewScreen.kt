package com.vaultly.app.ui.scan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaultly.app.VaultlyApp
import com.vaultly.app.scan.Pt
import com.vaultly.app.scan.Quad
import com.vaultly.app.scan.ScanFilter
import com.vaultly.app.ui.components.DialogTone
import com.vaultly.app.ui.components.PrimaryButton
import com.vaultly.app.ui.components.VaultDialog
import com.vaultly.app.ui.theme.Palette
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private val CropRed = Color(0xFFFF3B47)
private val EditorBg = Color(0xFF0A0E17)

/** Two-step review after capture/import: adjust corners, then pick a clean-up filter. */
@Composable
fun ReviewScreen(
    app: VaultlyApp,
    src: String,
    hint: Quad?,
    fromCamera: Boolean,
    onRetake: () -> Unit,
    onDone: (String) -> Unit,
    onExit: () -> Unit,
) {
    val vm: ReviewViewModel = viewModel { ReviewViewModel(app, src, hint) }
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }

    BackHandler {
        if (s.step == ReviewStep.ENHANCE) vm.backToCrop() else confirmDiscard = true
    }

    Column(Modifier.fillMaxSize().background(EditorBg)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorIcon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") {
                if (s.step == ReviewStep.ENHANCE) vm.backToCrop() else confirmDiscard = true
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (s.step == ReviewStep.CROP) "Adjust corners" else "Clean up",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    if (s.step == ReviewStep.CROP) "Step 1 of 2" else "Step 2 of 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
            EditorIcon(Icons.Outlined.DeleteOutline, "Discard") { confirmDiscard = true }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                s.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Palette.GoldLight)
                s.failed -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ImageNotSupported, null, Modifier.size(40.dp), tint = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.height(10.dp))
                    Text("This image couldn't be opened.", color = Color.White.copy(alpha = 0.8f))
                }
                else -> AnimatedContent(s.step, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "step") { step ->
                    if (step == ReviewStep.CROP) {
                        CropEditor(s.source!!, s.quad, vm::moveCorner)
                    } else {
                        EnhancePreview(s)
                    }
                }
            }
        }

        // Bottom bar
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
            if (s.step == ReviewStep.CROP) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (fromCamera) ToolButton(Icons.Outlined.CameraAlt, "Retake") { vm.cleanupSource(); onRetake() }
                    ToolButton(Icons.Outlined.AutoFixHigh, "Auto detect", onClick = vm::autoDetect)
                    ToolButton(Icons.Outlined.CropFree, "Full page", onClick = vm::fullPage)
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Continue", vm::toEnhance, Modifier.fillMaxWidth(), enabled = !s.loading && !s.failed, gold = true)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScanFilter.entries.forEach { f ->
                        FilterTile(f, s.thumbs[f], f == s.filter, Modifier.weight(1f)) { vm.setFilter(f) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EditorIcon(Icons.AutoMirrored.Outlined.RotateRight, "Rotate", size = 58, onClick = vm::rotate)
                    Spacer(Modifier.width(12.dp))
                    PrimaryButton(
                        "Use this scan",
                        { vm.finish(onDone) },
                        Modifier.weight(1f),
                        icon = Icons.Outlined.Check,
                        loading = s.saving,
                        enabled = s.previews.isNotEmpty(),
                        gold = true,
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        VaultDialog(
            onDismiss = { confirmDiscard = false },
            icon = Icons.Outlined.DeleteOutline,
            tone = DialogTone.DANGER,
            title = "Discard this scan?",
            message = "The photo will be deleted and nothing will be added to your vault.",
            primaryText = "Discard scan",
            onPrimary = { confirmDiscard = false; vm.cleanupSource(); onExit() },
            secondaryText = "Keep editing",
        )
    }
}

@Composable
private fun CropEditor(image: ImageBitmap, quad: Quad, onMove: (Int, Pt) -> Unit) {
    val density = LocalDensity.current
    val currentQuad by rememberUpdatedState(quad)
    var active by remember { mutableIntStateOf(-1) }
    var finger by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val scale = min(cw / image.width, ch / image.height)
        val iw = image.width * scale
        val ih = image.height * scale
        val left = (cw - iw) / 2
        val top = (ch - ih) / 2
        fun toView(p: Pt) = Offset(left + p.x * iw, top + p.y * ih)
        val grab = with(density) { 44.dp.toPx() }

        Canvas(
            Modifier.fillMaxSize().pointerInput(image, scale) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val pts = currentQuad.points.map(::toView)
                        val nearest = pts.indices.minBy { hypot(pts[it].x - pos.x, pts[it].y - pos.y) }
                        val d = hypot(pts[nearest].x - pos.x, pts[nearest].y - pos.y)
                        active = if (d < grab * 1.6f) nearest else -1
                        if (active >= 0) finger = pts[nearest]
                    },
                    onDragEnd = { active = -1 },
                    onDragCancel = { active = -1 },
                    onDrag = { change, drag ->
                        if (active >= 0) {
                            change.consume()
                            finger += drag
                            onMove(active, Pt((finger.x - left) / iw, (finger.y - top) / ih))
                        }
                    },
                )
            },
        ) {
            drawImage(
                image,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(iw.roundToInt(), ih.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
            val pts = quad.points.map(::toView)
            val shape = Path().apply {
                moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) }; close()
            }
            val dim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(left, top, left + iw, top + ih))
                addPath(shape)
            }
            drawPath(dim, Color.Black.copy(alpha = 0.55f))
            drawPath(shape, CropRed.copy(alpha = 0.08f))
            drawPath(shape, CropRed, style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))
            // Edge midpoints as subtle guides.
            for (i in 0 until 4) {
                val a = pts[i]
                val b = pts[(i + 1) % 4]
                drawCircle(CropRed, 3.5.dp.toPx(), Offset((a.x + b.x) / 2, (a.y + b.y) / 2))
            }
            pts.forEachIndexed { i, p ->
                val r = if (i == active) 16.dp.toPx() else 12.dp.toPx()
                drawCircle(Color.Black.copy(alpha = 0.25f), r + 3.dp.toPx(), p)
                drawCircle(Color.White, r, p)
                drawCircle(CropRed, r, p, style = Stroke(3.dp.toPx()))
            }
        }

        // Magnifier while dragging, placed on the side away from the finger.
        if (active >= 0) {
            val p = toView(quad.points[active])
            val loupe = with(density) { 116.dp.toPx() }
            val onLeft = p.x > cw / 2
            val lx = if (onLeft) 0f else cw - loupe
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(lx + loupe / 2, loupe / 2)
                val circle = Path().apply { addOval(Rect(center, loupe / 2)) }
                val zoom = 2.6f
                clipPath(circle) {
                    drawRect(Color.Black, Offset(lx, 0f), androidx.compose.ui.geometry.Size(loupe, loupe))
                    val imgPx = Offset((p.x - left) / scale, (p.y - top) / scale)
                    val dstScale = scale * zoom
                    drawImage(
                        image,
                        dstOffset = IntOffset((center.x - imgPx.x * dstScale).roundToInt(), (center.y - imgPx.y * dstScale).roundToInt()),
                        dstSize = IntSize((image.width * dstScale).roundToInt(), (image.height * dstScale).roundToInt()),
                        filterQuality = FilterQuality.Medium,
                    )
                    // Quad edges inside the loupe.
                    val zp = quad.points.map(::toView).map { center + (it - p) * zoom }
                    val zpath = Path().apply { moveTo(zp[0].x, zp[0].y); zp.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                    drawPath(zpath, CropRed, style = Stroke(2.dp.toPx()))
                }
                drawCircle(Color.White, loupe / 2, center, style = Stroke(3.dp.toPx()))
                drawLine(CropRed, center - Offset(10.dp.toPx(), 0f), center + Offset(10.dp.toPx(), 0f), 2.dp.toPx())
                drawLine(CropRed, center - Offset(0f, 10.dp.toPx()), center + Offset(0f, 10.dp.toPx()), 2.dp.toPx())
            }
        }
    }
}

@Composable
private fun EnhancePreview(s: ReviewState) {
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.Center) {
        val img = s.previews[s.filter]
        if (img == null) {
            CircularProgressIndicator(color = Palette.GoldLight)
        } else {
            Crossfade(img, animationSpec = tween(220), label = "filter") { bmp ->
                Image(
                    bmp, "Scan preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }
}

@Composable
private fun FilterTile(filter: ScanFilter, thumb: ImageBitmap?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(78.dp).clip(shape).background(Color.White.copy(alpha = 0.06f))
                .border(if (selected) 2.dp else 1.dp, if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.12f), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumb != null) {
                Image(thumb, filter.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(11.dp)))
            } else {
                CircularProgressIndicator(Modifier.size(18.dp), color = Palette.GoldLight, strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            filter.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Palette.GoldLight else Color.White.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(21.dp), tint = Color.White) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun EditorIcon(icon: ImageVector, description: String, size: Int = 44, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.32f).dp)).background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(22.dp), tint = Color.White) }
}
