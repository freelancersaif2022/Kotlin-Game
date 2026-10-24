package com.pdfmaster.app.pdf

/**
 * Edits drawn on top of a page. Coordinates are normalised to the page as it is displayed
 * (0..1, origin top-left) so they are independent of zoom level and page rotation.
 */
sealed interface PageMark {
    val page: Int
}

/** Freehand pen or highlighter stroke. [points] holds x,y pairs. [width] is relative to page width. */
data class InkMark(
    override val page: Int,
    val points: FloatArray,
    val color: Int,
    val width: Float,
    val alpha: Float = 1f,
) : PageMark

/** Text box. [x],[y] is the top-left corner, [size] is the font size relative to page width. */
data class TextMark(
    override val page: Int,
    val text: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Int,
    val background: Int? = null,
) : PageMark

/** Image (signature) placed at [x],[y] (top-left) with size [w] x [h], all normalised. */
data class ImageMark(
    override val page: Int,
    val path: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
) : PageMark
