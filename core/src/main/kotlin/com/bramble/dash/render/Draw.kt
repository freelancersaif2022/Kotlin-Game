package com.bramble.dash.render

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.bramble.dash.assets.RegionMeta

/**
 * Draws a trimmed atlas region so that the pack's pivot (bottom-left local px of the original
 * canvas) lands on (x, y). The trim offset is applied exactly once here; callers never add it.
 */
fun Batch.drawPivot(r: AtlasRegion, m: RegionMeta, x: Float, y: Float, scale: Float, flipX: Boolean = false, flipY: Boolean = false) {
    val w = r.packedWidth * scale
    val h = r.packedHeight * scale
    val left = pivotLeft(m, r.offsetX, r.packedWidth.toFloat(), x, scale, flipX)
    val bottom = pivotBottom(m, r.offsetY, y, scale)
    when {
        !flipX && !flipY -> draw(r, left, bottom, w, h)
        !flipX -> draw(r, left, bottom + h, w, -h)
        !flipY -> draw(r, left + w, bottom, -w, h)
        else -> draw(r, left + w, bottom + h, -w, -h)
    }
}

/**
 * Left edge of the trimmed rect so the canvas pivot lands on [x]. When mirrored, the canvas is
 * flipped around the pivot, so the trimmed rect moves to (origW - offsetX - packedW).
 */
fun pivotLeft(m: RegionMeta, offsetX: Float, packedW: Float, x: Float, scale: Float, flipX: Boolean): Float =
    if (!flipX) x - m.pivotX * scale + offsetX * scale
    else x - (m.origW - m.pivotX) * scale + (m.origW - offsetX - packedW) * scale

/** Bottom edge of the trimmed rect; atlas offsets are bottom-left based, like the pack's pivots. */
fun pivotBottom(m: RegionMeta, offsetY: Float, y: Float, scale: Float): Float = y - m.pivotY * scale + offsetY * scale

/** Draws an untrimmed-or-trimmed region filling its original size at (x, y) bottom-left. */
fun Batch.drawOrig(r: AtlasRegion, x: Float, y: Float, scaleX: Float, scaleY: Float = scaleX) {
    draw(r, x + r.offsetX * scaleX, y + r.offsetY * scaleY, r.packedWidth * scaleX, r.packedHeight * scaleY)
}

/** Draws a region centred on (cx, cy) using its original canvas size. */
fun Batch.drawCentered(r: AtlasRegion, cx: Float, cy: Float, scale: Float) {
    drawOrig(r, cx - r.originalWidth * scale / 2f, cy - r.originalHeight * scale / 2f, scale)
}
