package com.voxcast.mic.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * VoxCast's own duotone icon set. Each icon has a solid primary layer and a translucent
 * secondary layer, so it reads as a two-tone glyph and looks rich under gradient tints.
 */
object VoxIcons {

    private class Layer(val path: String, val alpha: Float = 1f, val stroke: Float = 0f)

    private fun icon(name: String, vararg layers: Layer): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            layers.forEach { l ->
                if (l.stroke > 0f) {
                    addPath(
                        pathData = addPathNodes(l.path),
                        stroke = SolidColor(Color.White),
                        strokeAlpha = l.alpha,
                        strokeLineWidth = l.stroke,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                } else {
                    addPath(pathData = addPathNodes(l.path), fill = SolidColor(Color.White), fillAlpha = l.alpha)
                }
            }
        }.build()

    val Microphone by lazy {
        icon(
            "Microphone",
            Layer("M12 2a4 4 0 0 0-4 4v6a4 4 0 0 0 8 0V6a4 4 0 0 0-4-4z"),
            Layer("M5 11a1 1 0 0 1 2 0 5 5 0 0 0 10 0 1 1 0 0 1 2 0 7 7 0 0 1-6 6.93V21h3a1 1 0 0 1 0 2H8a1 1 0 0 1 0-2h3v-3.07A7 7 0 0 1 5 11z", 0.55f),
            Layer("M10 6.5h4M10 9h4", 0.45f, 1.2f),
        )
    }

    val SpeakerBox by lazy {
        icon(
            "SpeakerBox",
            Layer("M7 2h10a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V5a3 3 0 0 1 3-3z", 0.38f),
            Layer("M12 10.5a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9zM12 4.3a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"),
        )
    }

    val QrScan by lazy {
        icon(
            "QrScan",
            Layer("M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3", 1f, 2f),
            Layer("M7 7h4v4H7zM13 7h4v4h-4zM7 13h4v4H7zM13.5 13.5h3.5v3.5h-3.5z", 0.6f),
        )
    }

    val Crown by lazy {
        icon(
            "Crown",
            Layer("M3 7.5l4.6 3.8L12 4l4.4 7.3L21 7.5 19.2 18H4.8L3 7.5z"),
            Layer("M5 19.6h14v2.2H5z", 0.6f),
        )
    }

    val WifiDirect by lazy {
        icon(
            "WifiDirect",
            Layer("M2 6a2 2 0 0 1 2-2h3.5a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6zM14.5 6a2 2 0 0 1 2-2H20a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3.5a2 2 0 0 1-2-2V6z", 0.4f),
            Layer("M9 10h6M13.2 8.2 15 10l-1.8 1.8M15 14H9M10.8 12.2 9 14l1.8 1.8", 1f, 1.6f),
        )
    }

    val Hotspot by lazy {
        icon(
            "Hotspot",
            Layer("M12 13.6a2.2 2.2 0 1 0 0-4.4 2.2 2.2 0 0 0 0 4.4z"),
            Layer("M8.5 15a5 5 0 0 1 0-7M15.5 8a5 5 0 0 1 0 7", 0.85f, 1.8f),
            Layer("M5.6 17.8a9 9 0 0 1 0-12.8M18.4 5a9 9 0 0 1 0 12.8", 0.45f, 1.8f),
        )
    }

    val Wifi by lazy {
        icon(
            "Wifi",
            Layer("M2.5 9a14.5 14.5 0 0 1 19 0", 0.4f, 2f),
            Layer("M5.5 12.6a10 10 0 0 1 13 0", 0.7f, 2f),
            Layer("M8.7 16.1a5.2 5.2 0 0 1 6.6 0", 1f, 2f),
            Layer("M12 20.6a1.4 1.4 0 1 0 0-2.8 1.4 1.4 0 0 0 0 2.8z"),
        )
    }

    val NoiseShield by lazy {
        icon(
            "NoiseShield",
            Layer("M12 2l8 3v6c0 5-3.4 9.4-8 11-4.6-1.6-8-6-8-11V5l8-3z", 0.35f),
            Layer("M7 12h1.6l1.5-3.2 2 6.4 1.9-4.2 1 1H17", 1f, 1.7f),
        )
    }

    val Bolt by lazy {
        icon(
            "Bolt",
            Layer("M13.5 2 4.5 13.5h6.5L10 22l9.5-12H13l.5-8z"),
        )
    }

    val Equalizer by lazy {
        icon(
            "Equalizer",
            Layer("M5 10v4M9 6v12M13 3v18M17 7v10M21 10v4", 1f, 2.2f),
        )
    }

    val NoAds by lazy {
        icon(
            "NoAds",
            Layer("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z", 0.3f),
            Layer("M7 9.5h10v5H7z", 0.75f),
            Layer("M5.5 5.5l13 13", 1f, 2f),
        )
    }

    val Studio by lazy {
        icon(
            "Studio",
            Layer("M3 12a9 9 0 0 1 18 0v5a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h2.9M3.1 12H6a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-5", 1f, 1.8f),
            Layer("M5 14h1.5v3H5zM17.5 14H19v3h-1.5z", 0.5f),
        )
    }

    val Boost by lazy {
        icon(
            "Boost",
            Layer("M3 9v6h4l5 5V4L7 9H3z", 0.45f),
            Layer("M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13", 1f, 1.9f),
        )
    }

    val Signal by lazy {
        icon(
            "Signal",
            Layer("M4 18v-2M9 18v-6M14 18V8M19 18V4", 1f, 2.4f),
        )
    }
}
