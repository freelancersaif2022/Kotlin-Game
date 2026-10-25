package com.saif.imagetopdf.engine

/** Font families that can be drawn with the fonts every Android phone already has. */
enum class Family(val label: String) { SANS("Sans-serif"), SERIF("Serif"), MONO("Monospace") }

/** One word, positioned along its line. All sizes are in image pixels. */
data class Word(
    val text: String,
    val x: Float,          // left edge, measured along the line from the line origin
    val width: Float,
    val color: Int,
    val weight: Int,       // 300 light, 400 regular, 500 medium, 700 bold, 900 black
    val italic: Boolean,
)

/** One line of text. Origin is the top-left corner of its ink; angle in degrees. */
data class TextLine(
    val originX: Float,
    val originY: Float,
    val angle: Float,
    val height: Float,
    val baseline: Float,   // distance from the line origin down to the text baseline
    val fontSize: Float,
    val family: Family,
    val words: List<Word>,
)

/** A filled rectangle that rebuilds the background, boxes, bands and rule lines. */
data class ColorRect(val left: Float, val top: Float, val right: Float, val bottom: Float, val color: Int)

data class PageModel(
    val width: Int,
    val height: Int,
    val background: Int,
    val shapes: List<ColorRect>,
    val lines: List<TextLine>,
)

data class FontUsage(val family: Family, val weight: Int, val italic: Boolean, val sizePt: Int, val words: Int) {
    val label: String
        get() {
            val w = when {
                weight <= 300 -> "Light"
                weight <= 400 -> "Regular"
                weight <= 500 -> "Medium"
                weight <= 700 -> "Bold"
                else -> "Black"
            }
            return "${family.label} $w${if (italic) " Italic" else ""} · ${sizePt}pt"
        }
}

data class ConversionResult(
    val pdfPath: String,
    val lineCount: Int,
    val wordCount: Int,
    val shapeCount: Int,
    val fonts: List<FontUsage>,
)
