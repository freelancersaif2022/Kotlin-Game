package com.pdfstudio.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Custom line icons (24x24 grid, rounded 2px strokes) for the editing tools. */
object ToolIcons {
    val Split: ImageVector by lazy {
        lineIcon(
            "Split",
            // Upper half of the page with its folded corner.
            "M6 9.5V5a2 2 0 0 1 2-2h6l4 4v2.5",
            "M14 3v4h4",
            // Lower half of the page.
            "M6 15v4a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-4",
            // Dashed cut line.
            "M2.5 12.25h2.5M8.25 12.25h2.5M13.25 12.25h2.5M19 12.25h2.5",
        )
    }

    val Crop: ImageVector by lazy {
        lineIcon(
            "Crop",
            "M6 2v14a2 2 0 0 0 2 2h14",
            "M18 22V8a2 2 0 0 0-2-2H2",
        )
    }

    val Convert: ImageVector by lazy {
        lineIcon(
            "Convert",
            // Page outline with folded corner.
            "M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5z",
            "M14 2v4a2 2 0 0 0 2 2h4",
            // Sun and mountain: the page becomes a picture.
            "M10 11.5a1.75 1.75 0 1 0 0.01 0z",
            "M20 17l-2.3-2.3a2 2 0 0 0-2.8 0L8.5 21.5",
        )
    }

    private fun lineIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        for (d in paths) {
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
