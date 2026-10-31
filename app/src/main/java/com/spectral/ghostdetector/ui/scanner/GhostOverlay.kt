package com.spectral.ghostdetector.ui.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectral.ghostdetector.data.GhostFactory
import com.spectral.ghostdetector.ui.components.threatColor
import com.spectral.ghostdetector.ui.theme.GColors
import com.spectral.ghostdetector.ui.theme.GFonts
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Screen-space frame of a ghost (without lock-on scale). */
fun TrackedGhost.frameRect(size: Size): Rect {
    val bw = w * size.width
    val bh = bw * aspect
    val cx = x * size.width
    val cy = y * size.height
    return Rect(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f)
}

@Composable
fun GhostOverlay(
    engine: GhostEngine,
    selectedId: Long?,
    onGhostTapped: (TrackedGhost?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val styles = remember {
        LabelStyles(
            name = TextStyle(fontFamily = GFonts.Rajdhani, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GColors.Neon),
            info = TextStyle(fontFamily = GFonts.Mono, fontSize = 10.5.sp, color = GColors.NeonSoft),
            small = TextStyle(fontFamily = GFonts.Mono, fontSize = 9.5.sp, color = GColors.Neon),
        )
    }
    Canvas(
        modifier.pointerInput(engine) {
            detectTapGestures { pos ->
                val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                val hit = engine.ghosts.lastOrNull { g ->
                    g.alpha > 0.05f && g.frameRect(canvasSize).inflate(24.dp.toPx()).contains(pos)
                }
                onGhostTapped(hit)
            }
        }
    ) {
        engine.ghosts.forEach { g -> drawGhost(g, measurer, styles, g.id == selectedId) }
    }
}

class LabelStyles(val name: TextStyle, val info: TextStyle, val small: TextStyle)

private fun DrawScope.drawGhost(g: TrackedGhost, tm: TextMeasurer, st: LabelStyles, selected: Boolean) {
    val a = g.alpha.coerceIn(0f, 1f)
    if (a <= 0.01f) return
    val c = GColors.Neon
    val base = g.frameRect(size)
    val ease = 1f - (1f - g.lock) * (1f - g.lock)
    val scale = 1f + (1f - ease) * 0.9f
    val r = Rect(
        base.center.x - base.width / 2f * scale, base.center.y - base.height / 2f * scale,
        base.center.x + base.width / 2f * scale, base.center.y + base.height / 2f * scale,
    )

    // soft fill + hairline
    drawRect(
        Brush.verticalGradient(listOf(c.copy(alpha = 0.13f * a), c.copy(alpha = 0.03f * a)), startY = r.top, endY = r.bottom),
        topLeft = r.topLeft, size = r.size,
    )
    drawRect(c.copy(alpha = 0.40f * a), topLeft = r.topLeft, size = r.size, style = Stroke(1.dp.toPx()))

    // glowing corner brackets
    val len = min(r.width, r.height) * 0.22f
    val core = if (selected) 3.5.dp.toPx() else 2.6.dp.toPx()
    corners(r, len, c.copy(alpha = 0.10f * a), core + 9.dp.toPx())
    corners(r, len, c.copy(alpha = 0.22f * a), core + 4.dp.toPx())
    corners(r, len, c.copy(alpha = a), core)

    // horizontal scan line sweeping inside the frame
    val sy = r.top + ((g.time * 0.55f) % 1f) * r.height
    drawLine(
        Brush.horizontalGradient(listOf(Color.Transparent, c.copy(alpha = 0.8f * a), Color.Transparent), startX = r.left, endX = r.right),
        Offset(r.left, sy), Offset(r.right, sy), 1.5.dp.toPx(),
    )
    // center reticle
    val cx = r.center.x
    val cy = r.center.y
    val s = 8.dp.toPx()
    drawCircle(c.copy(alpha = 0.7f * a), radius = s * 0.9f, center = r.center, style = Stroke(1.dp.toPx()))
    drawLine(c.copy(alpha = 0.7f * a), Offset(cx - s * 1.8f, cy), Offset(cx - s * 0.5f, cy), 1.dp.toPx())
    drawLine(c.copy(alpha = 0.7f * a), Offset(cx + s * 0.5f, cy), Offset(cx + s * 1.8f, cy), 1.dp.toPx())
    drawLine(c.copy(alpha = 0.7f * a), Offset(cx, cy - s * 1.8f), Offset(cx, cy - s * 0.5f), 1.dp.toPx())
    drawLine(c.copy(alpha = 0.7f * a), Offset(cx, cy + s * 0.5f), Offset(cx, cy + s * 1.8f), 1.dp.toPx())

    if (g.lock < 0.98f) {
        val lockText = tm.measure("LOCKING…", st.small)
        drawText(lockText, topLeft = Offset(cx - lockText.size.width / 2f, r.bottom + 6.dp.toPx()), alpha = a)
        return
    }

    // ----- info label above the frame -----
    val p = g.profile
    val nameL = tm.measure(p.name, st.name)
    val ageL = tm.measure("AGE ≈ ${p.age} YRS · ${p.type.label.uppercase()}", st.info)
    val threatL = tm.measure(
        "THREAT ${GhostFactory.threatLabel(p.threat)}",
        st.info.copy(color = threatColor(p.threat)),
    )
    val pad = 8.dp.toPx()
    val segW = 9.dp.toPx()
    val segGap = 2.dp.toPx()
    val threatRowW = threatL.size.width + pad + 5 * (segW + segGap)
    val boxW = max(max(nameL.size.width, ageL.size.width).toFloat(), threatRowW) + pad * 2
    val boxH = nameL.size.height + ageL.size.height + threatL.size.height + pad * 1.6f
    var bx = (cx - boxW / 2f).coerceIn(6.dp.toPx(), size.width - boxW - 6.dp.toPx())
    var by = r.top - boxH - 8.dp.toPx()
    if (by < 90.dp.toPx()) by = r.bottom + 30.dp.toPx()
    if (bx < 0f) bx = 0f

    drawRoundRect(Color(0xCC020604).copy(alpha = 0.8f * a), Offset(bx, by), Size(boxW, boxH), CornerRadius(8.dp.toPx()))
    drawRoundRect(c.copy(alpha = 0.7f * a), Offset(bx, by), Size(boxW, boxH), CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx()))
    // connector line from label to frame
    val labelAbove = by < r.top
    drawLine(
        c.copy(alpha = 0.6f * a),
        Offset(cx, if (labelAbove) by + boxH else by),
        Offset(cx, if (labelAbove) r.top else r.bottom),
        1.dp.toPx(),
    )
    var ty = by + pad * 0.6f
    drawText(nameL, topLeft = Offset(bx + pad, ty), alpha = a); ty += nameL.size.height
    drawText(ageL, topLeft = Offset(bx + pad, ty), alpha = a); ty += ageL.size.height
    drawText(threatL, topLeft = Offset(bx + pad, ty), alpha = a)
    val segY = ty + threatL.size.height / 2f - 3.dp.toPx()
    var sx = bx + pad + threatL.size.width + pad
    for (i in 1..5) {
        val col = if (i <= p.threat) threatColor(p.threat) else GColors.TextMuted.copy(alpha = 0.4f)
        drawRoundRect(col.copy(alpha = col.alpha * a), Offset(sx, segY), Size(segW, 6.dp.toPx()), CornerRadius(2.dp.toPx()))
        sx += segW + segGap
    }

    // ----- bottom telemetry -----
    val dist = (p.distance + sin(g.time * 0.9f) * 0.3f).coerceAtLeast(0.4f)
    val tele = tm.measure("DIST %.1fm · MATCH %d%% · #%03d".format(dist, p.match, g.id), st.small)
    if (!labelAbove) return
    drawText(tele, topLeft = Offset(cx - tele.size.width / 2f, r.bottom + 6.dp.toPx()), alpha = a)
    if (!selected) {
        val hint = tm.measure("TAP FOR DETAILS", st.small.copy(color = GColors.TextSecondary))
        drawText(hint, topLeft = Offset(cx - hint.size.width / 2f, r.bottom + 8.dp.toPx() + tele.size.height), alpha = a * 0.8f)
    }
}

private fun DrawScope.corners(r: Rect, len: Float, color: Color, width: Float) {
    val cap = StrokeCap.Round
    drawLine(color, r.topLeft, Offset(r.left + len, r.top), width, cap)
    drawLine(color, r.topLeft, Offset(r.left, r.top + len), width, cap)
    drawLine(color, Offset(r.right, r.top), Offset(r.right - len, r.top), width, cap)
    drawLine(color, Offset(r.right, r.top), Offset(r.right, r.top + len), width, cap)
    drawLine(color, Offset(r.left, r.bottom), Offset(r.left + len, r.bottom), width, cap)
    drawLine(color, Offset(r.left, r.bottom), Offset(r.left, r.bottom - len), width, cap)
    drawLine(color, r.bottomRight, Offset(r.right - len, r.bottom), width, cap)
    drawLine(color, r.bottomRight, Offset(r.right, r.bottom - len), width, cap)
}
